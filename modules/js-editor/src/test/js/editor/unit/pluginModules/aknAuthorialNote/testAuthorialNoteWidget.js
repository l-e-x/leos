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
define(function testAknCitationPlugin(require) {
    "use strict";

    var $ = require("jquery");
    var authorialNoteWidget = require("plugins/aknAuthorialNote/authorialNoteWidget");
    var MARKER = "marker";
    var DATA_AKN_NAME = "data-akn-name";

    function _createElementForName(name) {
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
            it("Expect span(aknAuthorialNote) element to have value of 'marker' attribute 1 more than attribute of preceding span(aknAuthorialNote) element.",
                    function() {
                        var valueOfDataAknNumAttrOfFirstElement = 6;
                        var valueOfDataAknNumAttrOfSecondElement = 6;
                        var firstElement = _createElementForName("span");
                        firstElement.setAttribute(MARKER, valueOfDataAknNumAttrOfFirstElement);
                        firstElement.setAttribute(DATA_AKN_NAME, "aknAuthorialNote");
                        var secondElement = _createElementForName("span");
                        secondElement.setAttribute(MARKER, valueOfDataAknNumAttrOfSecondElement);
                        secondElement.setAttribute(DATA_AKN_NAME, "aknAuthorialNote");

                        var authNotes = [ firstElement, secondElement ];
                        var editor = {
                            editable: function editable() {
                                return {$: {}}
                            },
                            LEOS: {
                                lowestMarkerValue:6
                            }
                        };

                        var widget = {
                            editor: editor,
                            element: secondElement.$,
                            _getLowestMarkerValue: function() {
                                return valueOfDataAknNumAttrOfFirstElement;
                            }
                        };
                                               
                        spyOn($.fn, "find").and.returnValue($(authNotes));
                        
                        authorialNoteWidget._renumberAuthorialNotes.call(widget, widget.editor);
                        expect(firstElement.getAttribute(MARKER)).toEqual("6");
                        expect(firstElement.innerHTML).toEqual(6);
                        expect(secondElement.getAttribute(MARKER)).toEqual("7");
                        expect(secondElement.innerHTML).toEqual(7);
                    });
        });
    });
});