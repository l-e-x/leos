/*
 * Copyright 2015 European Commission
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
define(function testAknCitationPlugin(require) {
    "use strict";

    var authorialNoteWidget = require("plugins/aknAuthorialNote/authorialNoteWidget");
    var MARKER = "marker";

    function _crateElementForName(name) {
        var product;
        if (name === "text") {
            product = new CKEDITOR.dom.text();
        } else {
            product = new CKEDITOR.dom.element(name);
        }
        return product;
    }
    describe("Unit tests for: plugins/aknNumberedParagraph/aknNumberedParagraphPlugin", function() {
        describe("Test aknNumburedParagraphModule.renumberHandler.renumberingParagraph().", function() {
            it("Expect  p(akn paragraph) element to have value of 'marker' attribute 1 more than attribute of proceding p(akn paragraph) element. ",
                    function() {
                        var valueOfDataAknNumAttrOfFirstElement = 6;
                        var valueOfDataAknNumAttrOfSecondElement = 6;
                        var firstElement = _crateElementForName("p");
                        firstElement.setAttribute(MARKER, valueOfDataAknNumAttrOfFirstElement);
                        var secondElement = _crateElementForName("p");
                        secondElement.setAttribute(MARKER, valueOfDataAknNumAttrOfSecondElement);
                        var paragraphs = [ firstElement, secondElement ];

                        var event = {
                            editor : {
                                document : {
                                    $ : {
                                        getElementsByClassName : function getElementsByClassName(className) {
                                            if (className == "authorialnote") {
                                                return {
                                                    length : paragraphs.length,
                                                    item : function item(index) {
                                                        return paragraphs[index];
                                                    }
                                                };
                                            }
                                        }
                                    }

                                }
                            }
                        };

                        authorialNoteWidget._renumberAuthorialNotes(event.editor);
                        expect(firstElement.getAttribute(MARKER)).toEqual("1");
                        expect(secondElement.getAttribute(MARKER)).toEqual("2");
                    });
        });

    });

});