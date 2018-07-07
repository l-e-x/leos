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
define(function testTransformerModule(require) {
    "use strict";
    var transformerFragmentStamp = require("transformer/fragmentTransformer");
    var ckEditorFragmentFactory = require("specs/util/ckEditorFragmentFactory");

    describe(
            "Tests /transformer/fragmentTransformer/getTransformedElement()",
            function() {

                describe("Expect to throw exception on required params.", function() {
                    var fragment;
                    var transformationConfigResolver;
                    beforeEach(function() {
                        fragment = {};
                        transformationConfigResolver = {};
                    });

                    it("Required param: 'fragment' not provided.", function() {
                        expect(function() {
                            var transformerToTest = transformerFragmentStamp();
                            transformerToTest.getTransformedElement({
                                transformationConfigResolver: transformationConfigResolver,
                                direction: 'whatever',
                                fragment: undefined
                            });
                        }).toThrowError("Param with name: 'fragment' is required, please provide it.");

                    });

                    it("Param with name: 'transformationConfigResolver' is required, please provide it.", function() {
                        expect(function() {
                            var transformerToTest = transformerFragmentStamp();
                            transformerToTest.getTransformedElement({
                                transformationConfigResolver: undefined,
                                direction: 'whatever',
                                fragment: fragment
                            });
                        }).toThrowError("Param with name: 'transformationConfigResolver' is required, please provide it.");
                    });

                });

                describe(
                        "Transformation cases.",
                        function() {
                            // helper functions for testing
                            var getProductInHtml = function getProductInHtml(fragmentInHtml, transformationConfigs, direction) {
                                var transformationConfigResolver = {
                                    getConfig: function getConfig() {
                                        return transformationConfigs;
                                    }

                                };
                                var fragment = ckEditorFragmentFactory.getCkFragmentForHtml(fragmentInHtml);
                                var element = fragment.children[0];
                                var transformerFragmentToTest = transformerFragmentStamp();
                                //DO THE ACTUAL CALL
                                var product = transformerFragmentToTest.getTransformedElement({
                                    fragment: element,
                                    direction: 'whatever',
                                    transformationConfigResolver: transformationConfigResolver
                                });

                                var productInHtml = null;
                                if (product) {
                                    // wrap element in fragment in order to be
                                    // compatible with
                                    // ckEditorFragmentFactory.getHtmlForCkFragment
                                    var fragmentWrapper = ckEditorFragmentFactory.getCkFragmentForHtml("");
                                    fragmentWrapper.children.push(product);
                                    productInHtml = ckEditorFragmentFactory.getHtmlForCkFragment(fragmentWrapper);
                                }
                                return productInHtml;
                            };

                            var getHtmlTagsInArray = function getHtmlTagsInArray(params) {
                                var htmlTags = [];
                                if (params.elementName === "text") {
                                    htmlTags.push(params.textValue);
                                } else {
                                    htmlTags = htmlTags.concat(["<", params.elementName]);
                                    if (params.attributes) {
                                        for (var ii = 0; ii < params.attributes.length; ii++) {
                                            htmlTags.push(" ");
                                            htmlTags.push(params.attributes[ii].name);
                                            htmlTags.push("=");
                                            htmlTags.push("\"");
                                            htmlTags.push(params.attributes[ii].value);
                                            htmlTags.push("\"");
                                        }
                                    }
                                    htmlTags.push(">");

                                    if (params.children) {
                                        htmlTags = htmlTags.concat(params.children);
                                    }
                                    htmlTags = htmlTags.concat(["</", params.elementName, ">"]);
                                }

                                return htmlTags;

                            };

                            var toConfigPath = function toConfigPath() {
                                var pathAsArray = []
                                for (var i = 0; i < arguments.length; i++) {
                                    pathAsArray.push(arguments[i].toLowerCase());
                                }
                                return pathAsArray.join("/");
                            };

                            var fragmentInHtml;
                            var transformationConfigs;
                            var direction;
                            var product;

                            it("Return null product for element with non existent transformation configuration.", function() {
                                fragmentInHtml = "<firstLevel></firstLevel>";
                                transformationConfigs = null;
                                direction = {};
                                product = getProductInHtml(fragmentInHtml, transformationConfigs, direction);
                                expect(product).toBeNull();
                            });

                            it("Expect one level element to be transformed to the one level product for one to one transformation.", function() {
                                var firstLevelElement = "firstLevelElement";
                                fragmentInHtml = getHtmlTagsInArray({
                                    elementName: firstLevelElement
                                }).join("");
                                var firstLevelProduct = "firstLevelProduct";
                                var expectedProductInHtml = getHtmlTagsInArray({
                                    elementName: firstLevelProduct
                                }).join("");
                                transformationConfigs = [{
                                    fromPath: toConfigPath(firstLevelElement),
                                    from: firstLevelElement,
                                    toPath: toConfigPath(firstLevelProduct),
                                    to: firstLevelProduct
                                }];
                                direction = {};
                                product = getProductInHtml(fragmentInHtml, transformationConfigs, direction);
                                expect(product).toEqual(expectedProductInHtml);
                            });

                            it("Expect one level text element to be transformed to the one level text product for one to one transformation.", function() {
                                var firstLevelElement = "text";
                                fragmentInHtml = "some text";
                                var expectedProductInHtml = "some text";
                                transformationConfigs = [{
                                    fromPath: firstLevelElement,
                                    from: firstLevelElement,
                                    toPath: firstLevelElement,
                                    to: firstLevelElement
                                }];
                                direction = {};
                                product = getProductInHtml(fragmentInHtml, transformationConfigs, direction);
                                expect(product).toEqual(expectedProductInHtml);
                            });

                            it("Expect two level element to be transformed to the two level product for one to one transformations.", function() {
                                var firstLevelElement = "firstLevelElement";
                                var secondLevelElement = "secondLevelElement";
                                fragmentInHtml = getHtmlTagsInArray({
                                    elementName: firstLevelElement,
                                    children: getHtmlTagsInArray({
                                        elementName: secondLevelElement
                                    })
                                }

                                ).join("");
                                var firstLevelProduct = "firstLevelProduct";
                                var secondLevelProduct = "secondLevelProduct";
                                var expectedProductInHtml = getHtmlTagsInArray({
                                    elementName: firstLevelProduct,
                                    children: getHtmlTagsInArray({
                                        elementName: secondLevelProduct
                                    })
                                }

                                ).join("");
                                transformationConfigs = [{
                                    from: firstLevelElement,
                                    fromParentPath: toConfigPath(firstLevelElement),
                                    fromPath: toConfigPath(firstLevelElement),
                                    to: firstLevelProduct,
                                    toParentPath: toConfigPath(firstLevelProduct),
                                    toPath: toConfigPath(firstLevelProduct)
                                }, {
                                    from: secondLevelElement,
                                    fromParentPath: toConfigPath(firstLevelElement),
                                    fromPath: toConfigPath(firstLevelElement, secondLevelElement),
                                    to: secondLevelProduct,
                                    toParentPath: toConfigPath(firstLevelProduct),
                                    toPath: toConfigPath(firstLevelProduct, secondLevelProduct)

                                }];
                                direction = {};
                                product = getProductInHtml(fragmentInHtml, transformationConfigs, direction);
                                expect(product).toEqual(expectedProductInHtml);
                            });

                            it("Expect two level elements with unknown second level element to be transformed into first level element and second level element to be skipped.", function() {
                                var firstLevelElement = "firstLevelElement";
                                var secondLevelElement = "secondLevelElement";
                                var unknownSecondLevelElement = "unknownSecondLevelElement";
                                fragmentInHtml = getHtmlTagsInArray({
                                    elementName: firstLevelElement,
                                    children: getHtmlTagsInArray({
                                        elementName: unknownSecondLevelElement
                                    })
                                }

                                ).join("");
                                var firstLevelProduct = "firstLevelProduct";
                                var secondLevelProduct = "secondLevelProduct";
                                var expectedProductInHtml = getHtmlTagsInArray({
                                    elementName: firstLevelProduct
                                }

                                ).join("");
                                transformationConfigs = [{
                                    from: firstLevelElement,
                                    fromParentPath: toConfigPath(firstLevelElement),
                                    fromPath: toConfigPath(firstLevelElement),
                                    to: firstLevelProduct,
                                    toParentPath: toConfigPath(firstLevelProduct),
                                    toPath: toConfigPath(firstLevelProduct)
                                }, {
                                    from: secondLevelElement,
                                    fromParentPath: toConfigPath(firstLevelElement),
                                    fromPath: toConfigPath(firstLevelElement, secondLevelElement),
                                    to: secondLevelProduct,
                                    toParentPath: toConfigPath(firstLevelProduct),
                                    toPath: toConfigPath(firstLevelProduct, secondLevelProduct)

                                }];
                                direction = {};
                                product = getProductInHtml(fragmentInHtml, transformationConfigs, direction);
                                expect(product).toEqual(expectedProductInHtml);
                            });
                            
                            
                            
                            it("Expect two level element to be transformed to the one level product for two to one transformations.", function() {
                                var firstLevelElement = "firstLevelElement";
                                var secondLevelElement = "secondLevelElement";
                                fragmentInHtml = getHtmlTagsInArray({
                                    elementName: firstLevelElement,
                                    children: getHtmlTagsInArray({
                                        elementName: secondLevelElement
                                    })
                                }

                                ).join("");
                                var firstLevelProduct = "firstLevelProduct";
                                var expectedProductInHtml = getHtmlTagsInArray({
                                    elementName: firstLevelProduct
                                }).join("");
                                transformationConfigs = [{
                                    from: firstLevelElement,
                                    fromParentPath: toConfigPath(firstLevelElement),
                                    fromPath: toConfigPath(firstLevelElement),
                                    to: firstLevelProduct,
                                    toParentPath: toConfigPath(firstLevelProduct),
                                    toPath: toConfigPath(firstLevelProduct)
                                }, {
                                    from: secondLevelElement,
                                    fromParentPath: toConfigPath(firstLevelElement),
                                    fromPath: toConfigPath(firstLevelElement, secondLevelElement),
                                    to: firstLevelProduct,
                                    toParentPath: toConfigPath(firstLevelProduct),
                                    toPath: toConfigPath(firstLevelProduct)

                                }];
                                direction = {};
                                product = getProductInHtml(fragmentInHtml, transformationConfigs, direction);
                                expect(product).toEqual(expectedProductInHtml);
                            });

                            it("Expect one level element to be transformed to the two level product for one to two transformations.", function() {
                                var firstLevelElement = "firstLevelElement";
                                fragmentInHtml = getHtmlTagsInArray({
                                    elementName: firstLevelElement
                                }).join("");
                                var firstLevelProduct = "firstLevelProduct";
                                var secondLevelProduct = "secondLevelProduct";
                                var expectedProductInHtml = getHtmlTagsInArray({
                                    elementName: firstLevelProduct,
                                    children: getHtmlTagsInArray({
                                        elementName: secondLevelProduct
                                    })
                                }).join("");
                                transformationConfigs = [{
                                    from: firstLevelElement,
                                    fromParentPath: toConfigPath(firstLevelElement),
                                    fromPath: toConfigPath(firstLevelElement),
                                    to: firstLevelProduct,
                                    toParentPath: toConfigPath(firstLevelProduct),
                                    toPath: toConfigPath(firstLevelProduct)
                                }, {
                                    from: firstLevelElement,
                                    fromParentPath: toConfigPath(firstLevelElement),
                                    fromPath: toConfigPath(firstLevelElement),
                                    to: secondLevelProduct,
                                    toParentPath: toConfigPath(firstLevelProduct),
                                    toPath: toConfigPath(firstLevelProduct, secondLevelProduct)

                                }];
                                direction = {};
                                product = getProductInHtml(fragmentInHtml, transformationConfigs, direction);
                                expect(product).toEqual(expectedProductInHtml);
                            });

                            it(
                                    "Expect one level element with attribute to be transformed to the two level product with attribute moved to the second level text node.",
                                    function() {
                                        var firstLevelElement = "firstLevelElement";
                                        var attributeToBeMoved = "attributeToBeMoved";
                                        var valueOfAttributeToBeMoved = "valueOfAttributeToBeMoved";
                                        var attributes = [{
                                            name: attributeToBeMoved,
                                            value: valueOfAttributeToBeMoved
                                        }];
                                        fragmentInHtml = getHtmlTagsInArray({
                                            elementName: firstLevelElement,
                                            attributes: attributes
                                        }).join("");
                                        var firstLevelProduct = "firstLevelProduct";
                                        var secondLevelProduct = "text";
                                        var expectedProductInHtml = getHtmlTagsInArray({
                                            elementName: firstLevelProduct,
                                            children: getHtmlTagsInArray({
                                                elementName: secondLevelProduct,
                                                textValue: valueOfAttributeToBeMoved
                                            })
                                        }

                                        ).join("");
                                        transformationConfigs = [{
                                            from: firstLevelElement,
                                            fromParentPath: toConfigPath(firstLevelElement),
                                            fromPath: toConfigPath(firstLevelElement),
                                            to: firstLevelProduct,
                                            toParentPath: toConfigPath(firstLevelProduct),
                                            toPath: toConfigPath(firstLevelProduct)
                                        }, {
                                            from: firstLevelElement,
                                            fromParentPath: toConfigPath(firstLevelElement),
                                            fromPath: toConfigPath(firstLevelElement),
                                            fromAttribute: attributeToBeMoved,
                                            to: secondLevelProduct,
                                            toParentPath: toConfigPath(firstLevelProduct),
                                            toPath: toConfigPath(firstLevelProduct, secondLevelProduct)

                                        }];
                                        direction = {};
                                        product = getProductInHtml(fragmentInHtml, transformationConfigs, direction);
                                        expect(product).toEqual(expectedProductInHtml);
                                    });

                            it(
                                    "Expect two level element with second level element to be text and value of this text to be moved to the attribute of the transformed one level element.",
                                    function() {
                                        var firstLevelElement = "firstLevelElement";
                                        var secondLevelElement = "text";
                                        var secondLevelTextValue = "secondLevelTextValue";
                                        var fragmentInHtml = getHtmlTagsInArray({
                                            elementName: firstLevelElement,
                                            children: getHtmlTagsInArray({
                                                elementName: secondLevelElement,
                                                textValue: secondLevelTextValue
                                            })
                                        }

                                        ).join("");

                                        var firstLevelProduct = "firstLevelProduct";
                                        var productAttribute = "attributeToBeMoved";
                                        var productAttributes = [{
                                            name: productAttribute,
                                            value: secondLevelTextValue
                                        }];
                                        var expectedProductInHtml = getHtmlTagsInArray({
                                            elementName: firstLevelProduct,
                                            attributes: productAttributes
                                        }).join("");

                                        transformationConfigs = [{
                                            from: firstLevelElement,
                                            fromParentPath: toConfigPath(firstLevelElement),
                                            fromPath: toConfigPath(firstLevelElement),
                                            to: firstLevelProduct,
                                            toParentPath: toConfigPath(firstLevelProduct),
                                            toPath: toConfigPath(firstLevelProduct)
                                        }, {
                                            from: secondLevelElement,
                                            fromParentPath: toConfigPath(firstLevelElement),
                                            fromPath: toConfigPath(firstLevelElement, secondLevelElement),
                                            to: firstLevelProduct,
                                            toParentPath: toConfigPath(firstLevelProduct),
                                            toPath: toConfigPath(firstLevelProduct),
                                            toAttribute: productAttribute

                                        }];
                                        direction = {};
                                        product = getProductInHtml(fragmentInHtml, transformationConfigs, direction);
                                        expect(product).toEqual(expectedProductInHtml);
                                    });

                        });

            });

});