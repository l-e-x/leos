/*
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
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
    var pluginName = "leosHierarchicalElementShiftEnterHandler";
    var LOG = require("logger");
    var SHIFT_ENTER = CKEDITOR.SHIFT + 13;
    // This array provide white list of elements in which shift+enter is allowed
    var ALLOWED_ELEMENTS = [ "span", "strong", "em", "u", "sup", "sub", "a", "p", "li" ];

    var pluginDefinition = {
        init : function init(editor) {
            editor.on('key', function(event) {
                if (event.data.keyCode === SHIFT_ENTER && isAllowedInThisContext(event)) {
                    LOG.debug("SHIFT_ENTER event intercepted: ", event);
                    var selection = event.editor.getSelection();
                    // grab the content from selection to the end of the current inline content
                    var contentAfterShiftEnter = getContentAfterShiftEnter(editor);
                    var startElement = selection.getStartElement();
                    // if the current inline content is not wrap in p, wrap it
                    var wrappingP = wrapCurrentInlineContent(startElement, editor);
                    // insert new subparagraph with extracted content in the next line
                    contentAfterShiftEnter.insertAfter(wrappingP);
                    // make selection at the begining of the new subparagraph
                    setNewSelection(editor, contentAfterShiftEnter);
                    event.editor.fire("contentChange");
                    event.cancel();
                };
            });
        }
    };

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
        if (isContentEmptyTextNode(content)) {
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

    var isContentEmptyTextNode = function isContentEmptyTextNode(fragment) {
        if (fragment.getChildCount() === 1) {
            var childElement = fragment.getChild(0);
            if (childElement.type === CKEDITOR.NODE_TEXT) {
                if (childElement.getText().trim() === "") {
                    return true;
                }
            }
        }

        if (fragment.getChildCount() === 0) {
            return true;
        }
        return false;
    };

    var setNewSelection = function setNewSelection(editor, content) {
        var rangeToSelect, firstChildElement = content.getFirst();
        rangeToSelect = editor.createRange();
        rangeToSelect.setStart(firstChildElement, 0);
        rangeToSelect.setEnd(firstChildElement, 0);
        rangeToSelect.select();
    }

    var isAllowedInThisContext = function isAllowedInThisContext(event) {
        var selection = event.editor.getSelection();
        var startElement = selection.getStartElement();
        var currentElement = startElement;

        // in order to check if the shift enter is allowed in current selection, take the start element and
        // check ancestors one by one and compare them against allowed_elements
        while (currentElement = currentElement.getParent()) {
            var elementName = currentElement.getName && currentElement.getName();
            if (elementName === "ol") {
                break;
            }
            if (ALLOWED_ELEMENTS.indexOf(elementName) < 0) {
                return false;
            }

        }
        return true;
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    return pluginDefinition;
});