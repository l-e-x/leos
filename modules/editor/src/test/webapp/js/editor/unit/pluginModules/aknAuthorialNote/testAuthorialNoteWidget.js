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
    var DATA_AKN_NAME = "data-akn-name";

    function _crateElementForName(name) {
        var product;
        if (name === "text") {
            product = new CKEDITOR.dom.text();
        } else {
            product = new CKEDITOR.dom.element(name);
        }
        return product;
    }
    describe("Unit tests for: plugins/aknAuthorialNote/aknAuthorialNotePlugin", function() {
        describe("Test aknAuthorialNoteModule.", function() {
            it("Expect  span(aknAuthorialNote) element to have value of 'marker' attribute 1 more than attribute of proceding span(aknAuthorialNote) element. ",
                    function() {
                        var valueOfDataAknNumAttrOfFirstElement = 6;
                        var valueOfDataAknNumAttrOfSecondElement = 6;
                        var firstElement = _crateElementForName("span");
                        firstElement.setAttribute(MARKER, valueOfDataAknNumAttrOfFirstElement);
                        firstElement.setAttribute(DATA_AKN_NAME, "aknAuthorialNote");
                        var secondElement = _crateElementForName("span");
                        secondElement.setAttribute(MARKER, valueOfDataAknNumAttrOfSecondElement);
                        secondElement.setAttribute(DATA_AKN_NAME, "aknAuthorialNote");
                        var authNotes = [ firstElement, secondElement ];
                        var authNotesObject = {
                                get : function get(index) {
                                    return authNotes[index];
                                },
                                length : authNotes.length
                        };

                        
                        var editor = {
                            editable: function editable() {
                                return {$: {}}
                            }
                        };
                        
                        spyOn($.fn, "find").and.callFake(function() {
                            return authNotesObject;
                        });
                        
                        authorialNoteWidget._renumberAuthorialNotes(editor);
                        expect(firstElement.getAttribute(MARKER)).toEqual("6");
                        expect(secondElement.getAttribute(MARKER)).toEqual("7");
                    });
        });

    });

});