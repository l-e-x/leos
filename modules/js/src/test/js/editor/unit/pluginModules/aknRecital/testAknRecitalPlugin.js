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
define(function testAknRecitalPlugin(require) {
    "use strict";
    var $ = require("jquery");
    var aknRecitalPluginToTest = require("plugins/aknRecital/aknRecitalPlugin");
    var DATA_AKN_NUM = "data-akn-num";
    var DATA_AKN_NUM_ORIGIN = "data-num-origin";
    var DATA_AKN_RECS_ORIGIN = "data-origin";
    var DATA_AKN_REC_ORIGIN = "data-origin";
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


    describe("Unit tests for plugins/aknRecitalPlugin", function () {
        var transformationConfigForRecital = '{"akn":"recital","html":"p","attr":[{"akn":"xml:id","html":"id"},{"akn":"leos:editable","html":"data-akn-attr-editable"},{"html":"data-akn-name=recital"}],"sub":[{"akn":"num","html":"p","attr":[{"akn":"xml:id","html":"data-akn-num-id"}],"sub":{"akn":"text","html":"p[data-akn-num]"}},{"akn":"mp","html":"p","attr":[{"akn":"xml:id","html":"data-akn-mp-id"},{"akn":"leos:origin","html":"data-mp-origin"}],"sub":{"akn":"text","html":"p/text"}}]}';

        describe("Tests if transformation config is valid.", function () {
            it("Expects uptodate transformation config.", function () {
                expect(JSON.stringify(aknRecitalPluginToTest.transformationConfig)).toEqual(transformationConfigForRecital);
            });
        });


        describe("Test the renumberRecital() function.", function () {
            it("Standard numbering: Expect  p(akn recital) element to have value of 'data-akn-num' attribute 1 more than attribute of proceding p(akn recital) element. ",
                function () {
                    var recitals = _crateElementForName('div');
                    recitals.setAttribute(DATA_AKN_NAME, "recitals");

                    var valueOfDataAknNumAttrOfFirstElement = 6;
                    var valueOfDataAknNumAttrOfSecondElement = 6;
                    var firstElement = _crateElementForName("p");
                    firstElement.setAttribute(DATA_AKN_NUM, valueOfDataAknNumAttrOfFirstElement);
                    firstElement.setAttribute(DATA_AKN_NAME, "recital");

                    var secondElement = _crateElementForName("p");
                    secondElement.setAttribute(DATA_AKN_NUM, valueOfDataAknNumAttrOfSecondElement);
                    secondElement.setAttribute(DATA_AKN_NAME, "recital");
                    var recitalList = [firstElement, secondElement];

                    var event = {
                        editor: {
                            editable: function () {
                                return $;
                            }
                        }
                    };

                    spyOn($.fn, "find").and.callFake(function(arg){
                        if(arg==="[data-akn-name='recitals']"){
                            return $(recitals);
                        }
                        else if(arg ==="*[data-akn-name='recital']"){
                            return recitalList;
                        }
                    });

                    //actual call
                    aknRecitalPluginToTest.renumberRecital(event);
                    expect(firstElement.getAttribute(DATA_AKN_NUM)).toEqual("(1)");
                    expect(secondElement.getAttribute(DATA_AKN_NUM)).toEqual("(2)");
                });
            it("Mandate numbering: Expect  p(akn recital) element to have value of 'data-akn-num' as '#' if origin is not set ",
                function () {

                    var recitals = _crateElementForName('div');
                    recitals.setAttribute(DATA_AKN_NAME, "recitals");
                    recitals.setAttribute(DATA_AKN_RECS_ORIGIN, "ec");

                    var firstElement = _crateElementForName("p");
                    firstElement.setAttribute(DATA_AKN_NUM, '(6)');
                    firstElement.setAttribute(DATA_AKN_NAME, "recital");
                    firstElement.setAttribute(DATA_AKN_NUM_ORIGIN, "ec");
                    firstElement.setAttribute(DATA_AKN_REC_ORIGIN, "ec");

                    var secondElement = _crateElementForName("p");
                    secondElement.setAttribute(DATA_AKN_NAME, "recital");
                    var recitalList = [firstElement, secondElement];

                    var event = {
                        editor: {
                            editable: function () {
                                return $;
                            }
                        }
                    };

                    spyOn($.fn, "find").and.callFake(function(arg){
                        if(arg==="[data-akn-name='recitals']"){
                            return $(recitals);
                        }
                        else if(arg ==="*[data-akn-name='recital']"){
                            return recitalList;
                        }
                    });

                    //actual call
                    aknRecitalPluginToTest.renumberRecital(event);
                    expect(firstElement.getAttribute(DATA_AKN_NUM)).toEqual("(6)");
                    expect(secondElement.getAttribute(DATA_AKN_NUM)).toEqual("(#)");
                });
            it("Mandate numbering: Expect  p(akn recital) element to have preset value of 'data-akn-num' if origin is set to cn",
                function () {

                    var recitals = _crateElementForName('div');
                    recitals.setAttribute(DATA_AKN_NAME, "recitals");
                    recitals.setAttribute(DATA_AKN_RECS_ORIGIN, "ec");

                    var firstElement = _crateElementForName("p");
                    firstElement.setAttribute(DATA_AKN_NUM, '(6)');
                    firstElement.setAttribute(DATA_AKN_NAME, "recital");
                    firstElement.setAttribute(DATA_AKN_NUM_ORIGIN, "ec");
                    firstElement.setAttribute(DATA_AKN_REC_ORIGIN, "ec");

                    var secondElement = _crateElementForName("p");
                    secondElement.setAttribute(DATA_AKN_NAME, "recital");
                    secondElement.setAttribute(DATA_AKN_NUM_ORIGIN, "cn");
                    secondElement.setAttribute(DATA_AKN_NUM, '(2ab)');

                    var recitalList = [firstElement, secondElement];

                    var event = {
                        editor: {
                            editable: function () {
                                return $;
                            }
                        }
                    };
                    spyOn($.fn, "find").and.callFake(function(arg){
                        if(arg==="[data-akn-name='recitals']"){
                            return $(recitals);
                        }
                        else if(arg ==="*[data-akn-name='recital']"){
                            return recitalList;
                        }
                    });

                    //actual call
                    aknRecitalPluginToTest.renumberRecital(event);
                    expect(firstElement.getAttribute(DATA_AKN_NUM)).toEqual("(6)");
                    expect(secondElement.getAttribute(DATA_AKN_NUM)).toEqual("(2ab)");
                });

        });
    });
});