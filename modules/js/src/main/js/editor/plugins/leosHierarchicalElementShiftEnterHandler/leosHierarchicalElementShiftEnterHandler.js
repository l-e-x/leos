/*
 * Copyright 2018 European Commission
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
define(function leosHierarchicalElementShiftEnterHandlerModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var leosKeyHandler = require("plugins/leosKeyHandler/leosKeyHandler");
    var identityHandler = require("plugins/leosAttrHandler/leosIdentityHandlerModule");
    var pluginName = "leosHierarchicalElementShiftEnterHandler";
    var LOG = require("logger");
    var SHIFT_ENTER = CKEDITOR.SHIFT + 13;

    var DATA_AKN_NUM = "data-akn-num";

    var pluginDefinition = {
        init : function init(editor) {
            leosKeyHandler.on({
                editor : editor,
                eventType : 'key',
                key : SHIFT_ENTER,
                action : _onShiftEnterKey
            });
        }
    };

    function _onShiftEnterKey(context) {
        var event = context.event;
        var editor = event.editor;
        var selection = editor.getSelection();
        var startElement = leosKeyHandler.getSelectedElement(selection);

        if (isShiftEnterAllowedInThisContext(startElement)) {
            LOG.debug("SHIFT_ENTER event intercepted: ", event);
            // grab the content from selection to the end of the current inline content
            var contentAfterShiftEnter = getContentAfterShiftEnter(editor);
            // if the current inline content is not wrap in p, wrap it
            var wrappingP = wrapCurrentInlineContent(startElement, editor);
            // insert new subparagraph with extracted content in the next line
            contentAfterShiftEnter.insertAfter(wrappingP);
            // make selection at the beginning of the new subparagraph
            setNewSelection(editor, contentAfterShiftEnter);
            var commonDetails = identityHandler.getElementDetails(contentAfterShiftEnter);
            identityHandler.handleIdentity(wrappingP, commonDetails, contentAfterShiftEnter, commonDetails);
            event.cancel();
            event.editor.fire("change");
        }
        else {
            LOG.debug("SHIFT_ENTER event intercepted but cancelled: ", event);
            event.cancel();
        }
    }

    var getInlineWrapper = function getInlineWrapper(el) {
        var inlineWrapper = el.getAscendant("p", true);
        if (!inlineWrapper) {
            inlineWrapper = el.getAscendant("li", true)
        }
        return inlineWrapper;
    };

    var wrapCurrentInlineContent = function wrapCurrentInlineContent(el, editor) {
        var inlineWrapper = el.getAscendant("p", true);
        if (!inlineWrapper) {
            inlineWrapper = el.getAscendant("li", true);
            if (inlineWrapper) {
                var liRangeContent = getFirstRange(editor).clone();
                liRangeContent.setStart(inlineWrapper, 0);
                var liContent = liRangeContent.extractContents();
                var pElement = new CKEDITOR.dom.element('p');
                pElement.append(liContent);
                var nestedBlock = getNestedBlockElement(inlineWrapper);
                if (nestedBlock) {
                    pElement.insertBefore(nestedBlock);
                } else {
                    inlineWrapper.append(pElement);
                }
                inlineWrapper = pElement;
            }
        }
        return inlineWrapper;
    }

    var getFirstRange = function getFirstRange(editor) {
        var selection = editor.getSelection();
        var startElement = selection.getStartElement();
        var firstRange = selection.getRanges()[0];
        return firstRange;
    };

    var getContentAfterShiftEnter = function getContentAfterShiftEnter(editor) {
        var selection = editor.getSelection();
        var startElement = selection.getStartElement();
        var firstRange = selection.getRanges()[0];
        var fromShiftEnterRange = getRangeAfterShiftEnter(firstRange, startElement);
        var content = fromShiftEnterRange.extractContents();
        var pElement = new CKEDITOR.dom.element('p');
        if (leosKeyHandler.isContentEmptyTextNode(content)) {
            pElement.appendBogus();
        } else {
            pElement.append(content);
        }
        return pElement;
    }

    var getRangeAfterShiftEnter = function getRangeAfterShiftEnter(firstRange, startElement) {
        var fromShiftEnterRange = firstRange.clone();
        var inlineWrapper = getInlineWrapper(startElement);
        var nestedBlockElements;
        var nestedBlock;
        if (inlineWrapper.getName() === 'li') {
            var nestedBlock = getNestedBlockElement(inlineWrapper);
        }

        if (nestedBlock) {
            fromShiftEnterRange.setEndBefore(nestedBlock, 0);
        } else {
            fromShiftEnterRange.setEndAt(inlineWrapper,  CKEDITOR.POSITION_BEFORE_END);
        }
        return fromShiftEnterRange;
    }

    var getNestedBlockElement = function getNestedBlockElement(liElement) {
        var nestedBlock = liElement.findOne("ol") || liElement.findOne("ul") || liElement.findOne("table");
        return nestedBlock;
    }
    
    var getFirstRange = function getFirstRange(editor) {
        var selection = editor.getSelection();
        var firstRange = selection.getRanges()[0];
        return firstRange;
    }

    var setNewSelection = function setNewSelection(editor, content) {
        var rangeToSelect, firstChildElement = content.getFirst();
        rangeToSelect = editor.createRange();
        rangeToSelect.setStart(firstChildElement, 0);
        rangeToSelect.setEnd(firstChildElement, 0);
        rangeToSelect.select();
    }

    //Shift-enter is allowed when not present in an unnumbered paragraph
    var isShiftEnterAllowedInThisContext = function isShiftEnterAllowedInThisContext(currentElement) {
        // If element is empty shift enter should be forbidden
	if (leosKeyHandler.isContentEmptyTextNode(currentElement)) {
            return false;
        }
        // in order to check if the shift enter is allowed in current selection, take the start element and
        // check ancestors one by one and compare them against allowed_elements
        do {
            var elementName = currentElement.getName && currentElement.getName();
            // Added in case of the unnumbered paragraph: shift-enter should be disabled
            if ((elementName === "li") && (currentElement.getAttribute("data-akn-name") == "aknNumberedParagraph") && (currentElement.getAttribute(DATA_AKN_NUM) === null)) {
                return false;
            }
            if (elementName === "ol") {
                break;
            }
    	} while (currentElement = currentElement.getParent());
        return true;
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    return pluginDefinition;
});