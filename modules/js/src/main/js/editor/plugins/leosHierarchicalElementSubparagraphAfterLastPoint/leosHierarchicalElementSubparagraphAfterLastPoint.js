/*
 * Copyright 2019 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
; // jshint ignore:line
define(function leosHierarchicalElementSubparagraphAfterLastPointModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var CKEDITOR = require("promise!ckEditor");
    var leosKeyHandler = require("plugins/leosKeyHandler/leosKeyHandler");
    var LOG = require("logger");
    var pluginName = "leosHierarchicalElementSubparagraphAfterLastPoint";
    
    var SHIFT_CTRL_ENTER = CKEDITOR.SHIFT + CKEDITOR.CTRL + 13;
    var SHIFT_CTRL_ENTER_ALLOWED = CKEDITOR.TRISTATE_OFF;
    var SHIFT_CTRL_ENTER_NOT_ALLOWED = CKEDITOR.TRISTATE_DISABLED;

    var DATA_AKN_NUM = "data-akn-num";
    var DATA_AKN_NAME = "data-akn-name";
    
    var LEVEL_ELEMENT_TYPE = "level";
    var AKN_LEVEL = "aknLevel";
    var AKN_LEVEL_ORDERED_LIST = "aknLevelOrderedList";
    
    var ARTICLE_ELEMENT_TYPE = "article";
    var AKN_NUMBERED_PARAGRAPH = "aknNumberedParagraph";
    var AKN_ORDERED_LIST = "aknOrderedList";
    
    var CMD_NAME = "leosHierarchicalElementSubparagraphAfterLastPoint";

    var pluginDefinition = {
        icons: pluginName.toLowerCase(),
        init : function init(editor) {
            editor.ui.addButton(pluginName, {
                label: 'Add Subparagraph',
                command: CMD_NAME,
                toolbar: 'add_subparagraph'
            });

            var addSubparagraphCommand = editor.addCommand(CMD_NAME, {
                exec: function(editor) {
                    _onShiftCtrlEnterKeyCommand(this, editor);
                }
            });

            leosKeyHandler.on({
                editor : editor,
                eventType : 'key',
                key : SHIFT_CTRL_ENTER,
                action : _onShiftCtrlEnterKey
            });

            editor.on("change", _handleCKEvent, null, addSubparagraphCommand);
            editor.on("selectionChange", _handleCKEvent, null, addSubparagraphCommand);
            $(editor.element.$).on("keyup mouseup", null, [editor, addSubparagraphCommand], _handleJQueryEvent);
        }
    };

    var _handleJQueryEvent = function _handleJQueryEvent(event) {
        var evtDataArray = event.data;
        var editor = evtDataArray[0];
        var cmd = evtDataArray[1];
        _setCurrentShiftCtrlEnterStatus(editor, cmd);
    }

    var _handleCKEvent = function _handleCKEvent(event) {
        var editor = event.editor;
        var cmd = event.listenerData;
        _setCurrentShiftCtrlEnterStatus(editor, cmd);
    }

    var _setCurrentShiftCtrlEnterStatus = function _setCurrentShiftCtrlEnterStatus(editor, cmd) {
        var shiftEnterStatus = SHIFT_CTRL_ENTER_NOT_ALLOWED;
        if (isShiftCtrlEnterAllowedInThisContext(editor)) {
            shiftEnterStatus = SHIFT_CTRL_ENTER_ALLOWED;
        }
        cmd.setState(shiftEnterStatus);
    }

    function _onShiftCtrlEnterKeyCommand(cmd, editor) {
        LOG.debug("SHIFT_CTRL_ENTER button clicked");
        _executeShiftCtrlEnter(editor);
    }
    
    function _executeShiftCtrlEnter(editor) {
        var selection = editor.getSelection();
        var startElement = leosKeyHandler.getSelectedElement(selection);
        
        //create an empty <p>
        var emptyElement = new CKEDITOR.dom.element('p');
        emptyElement.appendBogus();
        
        switch (editor.LEOS.elementType) {
            case LEVEL_ELEMENT_TYPE:
                // go up until reach the subparagraph level, and insert the empty element as last child of the level
                while (startElement.getParent() && startElement.getParent().getAttribute(DATA_AKN_NAME) != AKN_LEVEL) {
                    emptyElement.insertAfter(startElement);
                    startElement = startElement.getParent();
                }
                break;
            default:
                // go up until reach the subparagraph level, and insert the empty element as last child of the paragraph
                while (startElement.getParent() && startElement.getParent().getAttribute(DATA_AKN_NAME) != AKN_NUMBERED_PARAGRAPH) {
                    startElement = startElement.getParent();
                    emptyElement.insertAfter(startElement);
                }
        }
        
        // make selection at the beginning of the new subparagraph
        setNewSelection(editor, emptyElement);
        editor.fire("change");
    }

    function _onShiftCtrlEnterKey(context) {
        var event = context.event;
        var editor = event.editor;

        if (isShiftCtrlEnterAllowedInThisContext(editor)) {
            LOG.debug("SHIFT_CTRL_ENTER event intercepted: ", event);
            _executeShiftCtrlEnter(editor);
        } else {
            LOG.debug("SHIFT_CTRL_ENTER event intercepted but cancelled: ", event);
        }
        event.cancel();
    }
    
    var setNewSelection = function setNewSelection(editor, content) {
        var rangeToSelect, firstChildElement = content.getFirst();
        rangeToSelect = editor.createRange();
        rangeToSelect.setStart(firstChildElement, 0);
        rangeToSelect.setEnd(firstChildElement, 0);
        rangeToSelect.select();
    };
    
    var isShiftCtrlEnterAllowedInThisContext = function isShiftCtrlEnterAllowedInThisContext(editor) {
        var selection = editor.getSelection();
        if (!selection) {
            return false;
        }
        
        var currentElement = leosKeyHandler.getSelectedElement(selection);
        currentElement = currentElement.getAscendant("li", true);        
        if (!currentElement) {
            return false;
        }
        
        switch (editor.LEOS.elementType) {
            case LEVEL_ELEMENT_TYPE:
                if (!isElementInsideList(currentElement, LEVEL_ELEMENT_TYPE, AKN_LEVEL_ORDERED_LIST)
                        || !isLastElementInsideList(currentElement, LEVEL_ELEMENT_TYPE)) {
                    return false;
                }
                break;
            default:
                if (!_isNumberedParagraph(currentElement)) {
                    return false;
                }
                if (!isElementInsideList(currentElement, ARTICLE_ELEMENT_TYPE, AKN_ORDERED_LIST)
                        || !isLastElementInsideList(currentElement, ARTICLE_ELEMENT_TYPE)) {
                    return false;
                }
        }
        
        // false if we current element is not a leaf, so contains a nested list
        if ($(currentElement.$).find("ol").length != 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if the element is inside a numbered paragraph
     */
    function _isNumberedParagraph(el) {
        do {
            if (el.getAttribute(DATA_AKN_NUM) == null) {
                return false;
            }
            
            el = el.getAscendant("li")
        } while (el != null);
        
        return true;
    }
    
    /**
     * Check if the element is inside an ordered list (ol)
     */
    function isElementInsideList(el, elementType, orderedListName) {
        do {
            if (el.getAscendant("ol") && el.getAscendant("ol").getAttribute(DATA_AKN_NAME) === orderedListName) {
                return true;
            }
            el = el.getParent();
        } while (el.getAscendant(elementType, true) != null);
        return false;
    }
    
    /**
     * Check if the element passed as parameter is the last child between its siblings, and his ancestors too.
     */
    function isLastElementInsideList(el, elementType) {
        do {
            if (el.$ != el.getParent().getChild(el.getParent().getChildCount() - 1).$) {
                return false;
            }
            el = el.getAscendant("li")
        } while (el && ((elementType === ARTICLE_ELEMENT_TYPE && el.getAttribute(DATA_AKN_NAME) != AKN_NUMBERED_PARAGRAPH)
                || (elementType === LEVEL_ELEMENT_TYPE && el.getAscendant('ol') && el.getAscendant("ol").getAttribute(DATA_AKN_NAME) != AKN_LEVEL)));
        
        return true;
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    return pluginDefinition;
});