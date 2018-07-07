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
define(function testLeosNonEditablePlugin(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var leosNonEditablePluginToTest = require("plugins/leosNonEditable/leosNonEditablePlugin");

    var CONTENT_EDITABLE = "contenteditable";
    var EDITABLE = "editable";

    function _crateElementForName(name) {
        var product;
        if (name === "text") {
            product = new CKEDITOR.dom.text();
        } else {
            product = new CKEDITOR.dom.element(name);
        }
        return product;
    }

    describe(
            "Unit tests for plugins/leosNonEditablePlugin",
            function() {
                describe("Test the executeCommandDefinition() function.",
                        function() {
                            it("Expect  p(akn paragraph) element to have value of 'contenteditable' attribute equal to true when initially there is no 'contenteditable' attribute present when range is collapsed.",
                                    function() {
                                        var article = _crateElementForName("div");
                                        var paragraph = _crateElementForName("p");
                                        paragraph.setText("Text");

                                        article.append(paragraph);

                                        var editor = {
                                            getSelection: function getSelection() {
                                                return {
                                                    getRanges: function getRanges() {
                                                        var range = new CKEDITOR.dom.range(article);
                                                        range.setStartAt(paragraph, CKEDITOR.POSITION_AFTER_START);
                                                        range.setEndAt(paragraph, CKEDITOR.POSITION_AFTER_START);
                                                        return [range];
                                                    },
                                                    getStartElement: function getStartElement() {
                                                        return paragraph;
                                                    }
                                                };
                                            }
                                        };
                                        
                                        // DO THE ACTUAL CALL
                                        leosNonEditablePluginToTest.commandDefinition.exec(editor);
                                        
                                        expect(paragraph.getAttribute(CONTENT_EDITABLE)).not.toBe(null);
                                        expect(paragraph.getAttribute(CONTENT_EDITABLE)).toEqual("false");
                                    });

                            it("Expect  all p(akn paragraph) element to have value of 'contenteditable' attribute equal to true when initially there are no 'contenteditable' attribute present when range is not collapsed.",
                                    function() {
                                        var article = _crateElementForName("div");
                                        var paragraph1 = _crateElementForName("p");
                                        var paragraph2 = _crateElementForName("p");
                                        paragraph1.setText("Text1");
                                        paragraph2.setText("Text2");

                                        article.append(paragraph1);
                                        article.append(paragraph2);

                                        var editor = {
                                            getSelection: function getSelection() {
                                                return {
                                                    getRanges: function getRanges() {
                                                        var range = new CKEDITOR.dom.range(article);
                                                        range.setStartAt(paragraph1, CKEDITOR.POSITION_AFTER_START);
                                                        range.setEndAt(paragraph2, CKEDITOR.POSITION_AFTER_START);
                                                        range.collapsed = false;
                                                        return [range];
                                                    },
                                                    getStartElement: function getStartElement() {
                                                        return paragraph1;
                                                    }
                                                };
                                            }
                                        };
                                        
                                        //DO THE ACTUAL CALL
                                        leosNonEditablePluginToTest.commandDefinition.exec(editor);
                                        
                                        expect(paragraph1.getAttribute(CONTENT_EDITABLE)).not.toBe(null);
                                        expect(paragraph2.getAttribute(CONTENT_EDITABLE)).not.toBe(null);
                                        expect(paragraph1.getAttribute(CONTENT_EDITABLE)).toEqual("false");
                                        expect(paragraph2.getAttribute(CONTENT_EDITABLE)).toEqual("false");
                                    });
                        });
            });
});