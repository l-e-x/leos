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
define(function testAknRecitalPlugin(require) {
    "use strict";
    var aknRecitalPluginToTest = require("plugins/aknRecital/aknRecitalPlugin");
    var DATA_AKN_NUM = "data-akn-num";
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
    
    
    describe("Unit tests for plugins/aknRecitalPlugin", function() {
        var transformationConfigForRecital = '{"akn":"recital","html":"p","attr":[{"akn":"id","html":"id"},{"akn":"leos:editable","html":"contenteditable"},{"html":"data-akn-name=recital"},{"html":"class=recital"}],"sub":[{"akn":"num","html":"p","attr":[{"akn":"id","html":"data-akn-num-id"}],"sub":{"akn":"text","html":"p[data-akn-num]"}},{"akn":"mp","html":"p","attr":[{"akn":"id","html":"data-akn-mp-id"}],"sub":{"akn":"text","html":"p/text"}}]}';

        describe("Tests if transformation config is valid.", function() {
            it("Expects uptodate transformation config.", function() {
                expect(JSON.stringify(aknRecitalPluginToTest.transformationConfig)).toEqual(transformationConfigForRecital);
            });
        });
        
        
        describe("Test the renumberRecital() function.", function() {
            it("Expect  p(akn recital) element to have value of 'data-akn-num' attribute 1 more than attribute of proceding p(akn recital) element. ",
                    function() {
                        var valueOfDataAknNumAttrOfFirstElement = 6;
                        var valueOfDataAknNumAttrOfSecondElement = 6;
                        var firstElement = _crateElementForName("p");
                        firstElement.setAttribute(DATA_AKN_NUM, valueOfDataAknNumAttrOfFirstElement);
                        firstElement.setAttribute(DATA_AKN_NAME, "recital");
                        var secondElement = _crateElementForName("p");
                        secondElement.setAttribute(DATA_AKN_NUM, valueOfDataAknNumAttrOfSecondElement);
                        secondElement.setAttribute(DATA_AKN_NAME, "recital");
                        var recitals = [firstElement, secondElement];

                        var event = {
                            editor: {
                                editable: function() {
                                    return $;
                                }
                            }
                        };
                        spyOn($.fn, "find").and.returnValue(recitals);
                        
                        //actual call
                        aknRecitalPluginToTest.renumberRecital(event);
                        expect(firstElement.getAttribute(DATA_AKN_NUM)).toEqual("(1)");
                        expect(secondElement.getAttribute(DATA_AKN_NUM)).toEqual("(2)");
                    });
        });
        

    });

});