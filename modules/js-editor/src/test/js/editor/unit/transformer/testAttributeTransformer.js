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
define(function testAttributeTransformerModule(require) {
    "use strict";

    var attributeTransformerFactoryToTest = require("transformer/attributeTransformer");

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
            "Unit Tests /transformer/attributeTransformer",
            function() {

                describe(
                        "Test getAttributeTransformerName() cases.",
                        function() {
                            it("Expect transformer action name to be 'addClassAttributeTransformer' when a 'class' attribute is passed in the configuration",
                                    function() {
                                        var normAttr = {
                                            to: "class",
                                            toValue: "xyz"
                                        };
                                        // DO THE ACTUAL CALL
                                        var transformerName = attributeTransformerFactoryToTest.getAttributeTransformerName(normAttr);
                                        expect(transformerName).toEqual("addClassAttributeTransformer");
                                    });
                            
                            it(
                                    "Expect transformer action name to be 'addClassAttributeTransformer' when 'class=classname*' attribute is passed in the configuration",
                                    function() {
                                        var normAttr = {
                                            to: "class",
                                            toValue: "classname*",
                                            from: "class",
                                            fromValue: "classname*"
                                        };
                                        // DO THE ACTUAL CALL
                                        var transformerName = attributeTransformerFactoryToTest.getAttributeTransformerName(normAttr);
                                        expect(transformerName).toEqual("addClassAttributeTransformer");
                                    });
                            
                            it(
                                    "Expect transformer action name to be 'passAttributeTransformer' when both 'akn' & 'html' attribute is passed in the configuration",
                                    function() {
                                        var normAttr = {
                                            from: "fromAttr",
                                            fromValue: "fromValue",
                                            to: "toAttr",
                                            toValue: "toValue"
                                        };
                                        // DO THE ACTUAL CALL
                                        var transformerName = attributeTransformerFactoryToTest.getAttributeTransformerName(normAttr);
                                        expect(transformerName).toEqual("passAttributeTransformer");
                                    });

                            it(
                                    "Expect transformer action name to be 'handleNonEditable' when  'akn=editable' or 'html=contenteditable' attribute is passed in the configuration",
                                    function() {
                                        var normAttr = {
                                            from: "editable",
                                            fromValue: "false",
                                            to: "contenteditable",
                                            toValue: "false"
                                        };
                                        // DO THE ACTUAL CALL
                                        var transformerName = attributeTransformerFactoryToTest.getAttributeTransformerName(normAttr);
                                        expect(transformerName).toEqual("handleNonEditable");
                                    });

                            it("Expect transformer action name to be 'addAttributeTransformer' when only 'html' attribute is passed in the configuration",
                                    function() {
                                        var normAttr = {
                                            to: "toAttr",
                                            toValue: "toValue"
                                        };
                                        // DO THE ACTUAL CALL
                                        var transformerName = attributeTransformerFactoryToTest.getAttributeTransformerName(normAttr);
                                        expect(transformerName).toEqual("addAttributeTransformer");
                                    });
                            
                            it("Expect transformer action name to be 'passAttributeTransformer' when to attribute is of 'class' value and from attribute is present.",  function() {
                                var normAttr = {
                                    to: "class",
                                    from : "whatever"
                                };
                                // DO THE ACTUAL CALL
                                var transformerName = attributeTransformerFactoryToTest.getAttributeTransformerName(normAttr);
                                expect(transformerName).toEqual("passAttributeTransformer");
                            });
                        });

                describe(
                        "Test getAttributeTransformer().perform() cases.",
                        function() {
                            it("Expect transformer action name to be 'addClassAttributeTransformer' & class attribute value to be set when a 'class' attribute is passed in the configuration",
                                    function() {

                                        var normAttr1 = {
                                            to: "class",
                                            toValue: "xyz"
                                        };
                                        var normAttr2 = {
                                            to: "class",
                                            toValue: "abc"
                                        };

                                        var element = {};
                                        var product = {
                                            attributes: [normAttr1.to, normAttr2.to],
                                            name: 'element'
                                        }

                                        // DO THE ACTUAL CALL
                                        var transformerName1 = attributeTransformerFactoryToTest.getAttributeTransformerName(normAttr1);
                                        var attributeTransformer1 = attributeTransformerFactoryToTest.getAttributeTransformer(transformerName1);
                                        attributeTransformer1.perform.call({
                                            toElement: product,
                                            fromElement: element,
                                            attrConfig: normAttr1
                                        });

                                        var transformerName2 = attributeTransformerFactoryToTest.getAttributeTransformerName(normAttr2);
                                        var attributeTransformer2 = attributeTransformerFactoryToTest.getAttributeTransformer(transformerName2);
                                        attributeTransformer2.perform.call({
                                            toElement: product,
                                            fromElement: element,
                                            attrConfig: normAttr2
                                        });

                                        expect(transformerName1).toEqual("addClassAttributeTransformer");
                                        expect(transformerName2).toEqual("addClassAttributeTransformer");

                                        expect(product.attributes['class']).toEqual("xyz abc");
                                    });

                            it("Expect transformer action name to be 'addClassAttributeTransformer' & class attribute value to be set only when a 'class' attribute is passed with a '*' in the configuration",
                                    function() {

                                        var normAttr = {
                                            to: "class",
                                            toValue: "xyz*",
                                            from: "class",
                                            fromValue: "xyz*"
                                        };

                                        var element = {
                                                attributes: [normAttr.to]
                                        };
                                        element.attributes[normAttr.to] = "xyzabc bla blabla";
                                        
                                        var product = {
                                            attributes: []
                                        }

                                        // DO THE ACTUAL CALL
                                        var transformerName = attributeTransformerFactoryToTest.getAttributeTransformerName(normAttr);
                                        var attributeTransformer = attributeTransformerFactoryToTest.getAttributeTransformer(transformerName);
                                        attributeTransformer.perform.call({
                                            toElement: product,
                                            fromElement: element,
                                            attrConfig: normAttr
                                        });
                                     
                                        expect(transformerName).toEqual("addClassAttributeTransformer");
                                        expect(product.attributes['class']).toEqual("xyzabc");
                                    });
                            
                            it("Expect transformer action name to be 'addClassAttributeTransformer' & class attribute value to be set when a 'class' attribute is passed with a '*' in the configuration along with the other classes",
                                    function() {

                                        var normAttr = {
                                            to: "class",
                                            toValue: "xyz* bla blabla",
                                            from: "class",
                                            fromValue: "xyz* bla blabla"
                                        };

                                        var element = {
                                                attributes: [normAttr.to]
                                        };
                                        element.attributes[normAttr.to] = "xyzabc bla blabla";
                                        
                                        var product = {
                                            attributes: []
                                        }

                                        // DO THE ACTUAL CALL
                                        var transformerName = attributeTransformerFactoryToTest.getAttributeTransformerName(normAttr);
                                        var attributeTransformer = attributeTransformerFactoryToTest.getAttributeTransformer(transformerName);
                                        attributeTransformer.perform.call({
                                            toElement: product,
                                            fromElement: element,
                                            attrConfig: normAttr
                                        });
                                     
                                        expect(transformerName).toEqual("addClassAttributeTransformer");
                                        expect(product.attributes['class']).toEqual("xyzabc bla blabla");
                                    });
                            
                            it(
                                    "Expect transformer action name to be 'passAttributeTransformer' & attribute value to be set when 'from' attribute is passed in the configuration",
                                    function() {
                                        var normAttr = {
                                            from: "fromAttr",
                                            fromValue: "fromValue",
                                            to: "toAttr",
                                            toValue: "toValue"
                                        };
                                        var element1 = {
                                            attributes: [normAttr.from]
                                        };
                                        element1.attributes[normAttr.from] = normAttr.fromValue;
                                        var element2 = {
                                            attributes: [normAttr.from]
                                        };

                                        var product1 = {
                                            attributes: []
                                        };
                                        var product2 = {
                                            attributes: []
                                        };

                                        // DO THE ACTUAL CALL
                                        var transformerName = attributeTransformerFactoryToTest.getAttributeTransformerName(normAttr);
                                        var attributeTransformer = attributeTransformerFactoryToTest.getAttributeTransformer(transformerName);
                                        attributeTransformer.perform.call({
                                            toElement: product1,
                                            fromElement: element1,
                                            attrConfig: normAttr
                                        });

                                        attributeTransformer.perform.call({
                                            toElement: product2,
                                            fromElement: element2,
                                            attrConfig: normAttr
                                        });

                                        expect(transformerName).toEqual("passAttributeTransformer");
                                        expect(product1.attributes['toAttr']).toEqual("fromValue");
                                        expect(product2.attributes['toAttr']).toEqual("toValue");
                                    });

                            it(
                                    "Expect transformer action name to be 'handleNonEditable' & contenteditable should be removed when 'editable=true' attribute is set",
                                    function() {
                                        var normAttr = {
                                            from: "editable",
                                            fromValue: "true",
                                            to: "contenteditable",
                                            toValue: "true"
                                        };
                                        var element = {
                                            attributes: [normAttr.from]
                                        };
                                        element.attributes[normAttr.from] = normAttr.fromValue;

                                        var product = {
                                            attributes: []
                                        };

                                        // DO THE ACTUAL CALL
                                        var transformerName = attributeTransformerFactoryToTest.getAttributeTransformerName(normAttr);
                                        var attributeTransformer = attributeTransformerFactoryToTest.getAttributeTransformer(transformerName);
                                        attributeTransformer.perform.call({
                                            toElement: product,
                                            fromElement: element,
                                            attrConfig: normAttr
                                        });

                                        expect(transformerName).toEqual("handleNonEditable");
                                        expect(product.attributes['contenteditable']).toBe(undefined);
                                    });

                            it("Expect transformer action name to be 'addAttributeTransformer' when only 'to' attribute is set", function() {
                                var normAttr = {
                                    to: "toAttr",
                                    toValue: "toValue"
                                };
                                var element = {};
                                var product = {
                                    attributes: []
                                };

                                // DO THE ACTUAL CALL
                                var transformerName = attributeTransformerFactoryToTest.getAttributeTransformerName(normAttr);
                                var attributeTransformer = attributeTransformerFactoryToTest.getAttributeTransformer(transformerName);
                                attributeTransformer.perform.call({
                                    toElement: product,
                                    fromElement: element,
                                    attrConfig: normAttr
                                });

                                expect(transformerName).toEqual("addAttributeTransformer");
                                expect(product.attributes['toAttr']).toEqual("toValue");
                            });

                        });

            });

});