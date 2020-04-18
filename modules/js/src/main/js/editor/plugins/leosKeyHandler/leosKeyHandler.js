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
define(function leosKeyHandler(require) {
    "use strict";

    var CKEDITOR = require("promise!ckEditor");
    var LOG = require("logger");
    var REG_EXP_FOR_UNICODE_ZERO_WIDTH_SPACE_IN_HEX = /\u200B/g;
    var SPAN = "span";
    var BOGUS = "br";
    var SCAYT_MISSPELL_WORD_CLASS = "scayt-misspell-word";
    var SCAYT_GRAMM_PROBLEM_CLASS = "gramm-problem";

    var on = function onKey(onKeyContext) {
        onKeyContext.editor.on(onKeyContext.eventType, function(event) {
            var selection = onKeyContext.editor.getSelection(), ranges = selection && selection.getRanges();
            if (selection && event.data.keyCode === onKeyContext.key) {
                var firstRange;
                if (ranges && ranges.length > 0) {
                    firstRange = ranges[0];
                }
                var context = {
                    firstRange : firstRange,
                    event : event,
                    selection : selection,
                    editor : onKeyContext.editor
                };
                onKeyContext.action(context);
            }
        });
    };

    // Given a selected element, gets the the first non inline parent
    // Entry parameter of type: CKEDITOR.dom.node
    var getSelectedElement = function getSelectedElement(selection) {
        var selectedElement = selection.getStartElement();
        while (selectedElement && CKEDITOR.dtd.$inline.hasOwnProperty(selectedElement.getName())) {
            selectedElement = selectedElement.getParent();
        }
        return selectedElement;
    };

    //Check that an element is empty or not: 
    // Empty means:
    // 1. No text (spaces or no characters)
    // 2. Contains only one "br"
    // 3. Contains "span" spellchecker tag with no text or with a "br"
    // Entry parameter of type : CKEDITOR.dom.element or CKEDITOR.dom.text
    var isContentEmptyTextNode = function isContentEmptyTextNode(element) {
        if (element) {
            if (element instanceof CKEDITOR.dom.element || element instanceof CKEDITOR.dom.documentFragment) {
                for (var i = 0;i < element.getChildCount(); i++) {
                    var childElement = element.getChildren().getItem(i);
                    if ((childElement.type === CKEDITOR.NODE_TEXT) &&
                            (childElement.getText().trim().replace(REG_EXP_FOR_UNICODE_ZERO_WIDTH_SPACE_IN_HEX, '') !== "")) {
                        return false;
                    } else if ((childElement.type !== CKEDITOR.NODE_TEXT) && (childElement.getName().toLowerCase() === SPAN) &&
                            (childElement.hasClass(SCAYT_MISSPELL_WORD_CLASS) || childElement.hasClass(SCAYT_GRAMM_PROBLEM_CLASS))) {
                        if (!isContentEmptyTextNode(childElement)) {
                            return false;
                        }
                    } else if ((childElement.type !== CKEDITOR.NODE_TEXT) && (childElement.getName().toLowerCase() !== BOGUS)) {
                        return false;
                    }
                }
                return true;
            } else if (element instanceof CKEDITOR.dom.text) {
                return (element.getText().trim().replace(REG_EXP_FOR_UNICODE_ZERO_WIDTH_SPACE_IN_HEX, '') === "");
            }
        }
        return false;
    };

    return {
	on: on,
        isContentEmptyTextNode: isContentEmptyTextNode,
        getSelectedElement: getSelectedElement
    }
});