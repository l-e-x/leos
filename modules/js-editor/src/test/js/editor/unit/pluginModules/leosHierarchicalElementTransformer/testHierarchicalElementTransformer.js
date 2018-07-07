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
define(function testHierarchicalElementTransformer(require) {
    "use strict";
    var leosHierarchicalElementTransformerStamp = require("plugins/leosHierarchicalElementTransformer/hierarchicalElementTransformer");
    var ckEditorFragmentFactory = require("specs/util/ckEditorFragmentFactory");
    /*
     * As sample data: <list><indent><num>1</num><alinea><content><mp>fdsa<b>fsa fda</b>sfd safsda fsad</mp></content></alinea><block>fdsa</block></indent></list>
     * is used on the akn side and the: <ul><li>fds<b>af</b>sa<block>fdasfdsa</block></li></ul> is used on html side.
     */
    describe(
            "Unit tests for: plugins/leosHierarchicalElementTransformer/hierarchicalElementTransformer",
            function() {

                function getTransformationContextForElement(element, fragment) {
                    var transformationContext = {
                        elementPath : getElementPath(element, fragment)
                    };
                    return transformationContext;
                }

                function fromFragmentToElement(fragment) {
                    var element = fragment.children[0];
                    return element;
                }

                var getElementName = function getElementName(element) {
                    var elementName = null;
                    if (element instanceof CKEDITOR.htmlParser.element) {
                        elementName = element.name;
                    } else if (element instanceof CKEDITOR.htmlParser.text) {
                        elementName = "text";

                    }
                    return elementName;
                };

                var getElementPath = function getElementPath(element, fragment) {
                    var hierarchicalPartsOfPath = [];
                    var currentElement = element;
                    do {
                        hierarchicalPartsOfPath.unshift(getElementName(element).toLowerCase());
                        if (currentElement === fragment) {
                            break;
                        } else {
                            currentElement = currentElement.parent;
                        }
                        // check if the path is calculated for current element's parent if yes use it
                        if (currentElement.hierarchicalPartsOfPath) {
                            hierarchicalPartsOfPath = currentElement.hierarchicalPartsOfPath.concat(hierarchicalPartsOfPath);
                            break;
                        }
                    } while (currentElement !== fragment);
                    element.hierarchicalPartsOfPath = hierarchicalPartsOfPath;
                    return hierarchicalPartsOfPath.join("/");
                }

                // common block for all tests =>
                var leosHierarchicalElementTransformer = leosHierarchicalElementTransformerStamp({
                    firstLevelConfig : {
                        akn : 'list',
                        html : 'ul',
                        attr : [ {
                            akn : "leos:editable",
                            html : "contenteditable"
                        }, {
                            akn : "id",
                            html : "id"
                        }, {
                            html : "data-akn-name=aknUnorderedList"
                        } ]
                    },
                    rootElementsForFrom : [ "list", "indent" ],
                    contentWrapperForFrom : "alinea",
                    rootElementsForTo : [ "ul", "li" ]
                });
                var transformationConfig = leosHierarchicalElementTransformer.getTransformationConfig();
                // <= end of common block

                describe("General Transformation tests", function() {
                    it("Transformation created succesfully", function() {
                        // check if transformation config created successfuly
                        expect(transformationConfig).not.toBeNull();
                    });
                });
                describe("Check if custom transformation done correctly.",
                        function() {
                            describe("From HTML to AKN side ", function() {
                                        it("Element only with inline content.", function() {
                                                    var elementToBeTransformed = fromFragmentToElement(ckEditorFragmentFactory.getCkFragmentForHtml("<ul><li>fdsafsa</li></ul>"));
                                                    var fromAction = transformationConfig.transformer.from.action;
                                                    var mapToChildProductsCounter = 0;

                                                    var fragmentMock = {
                                                        mapToProducts : function() {
                                                            expect(JSON.stringify(arguments[1])).toEqual(
                                                                    '[{"toPath":"list/indent","attrs":[{"from":"id","to":"id","action":"passAttributeTransformer"}]},{"toPath":"list/indent/num","attrs":[{"from":"data-akn-num-id","to":"id","action":"passAttributeTransformer"}]},{"toPath":"list/indent/num/text","fromAttribute":"data-akn-num"}]');
                                                        },
                                                        mapToChildProducts : function() {
                                                            if (mapToChildProductsCounter === 0) {
                                                                expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"list/indent","toChild":"content","attrs":[{"from":"data-akn-content-id","to":"id","action":"passAttributeTransformer"}]}');
                                                            } else if (mapToChildProductsCounter === 1) {
                                                                expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"list/indent/content","toChild":"mp","attrs":[{"from":"data-akn-mp-id","to":"id","action":"passAttributeTransformer"}]}');
                                                            } else if (mapToChildProductsCounter === 2) {
                                                                expect(JSON.stringify(arguments[1])).toEqual(
                                                                        '{"toPath":"list/indent/content/mp","toChild":"text","toChildTextValue":"fdsafsa"}');
                                                            } else {
                                                                // expect not to be called for mapToChildProductsCounter > 2
                                                                expect(true).toBe(false);
                                                            }
                                                            mapToChildProductsCounter++;

                                                        },
                                                        _getElementName : getElementName,
                                                        mapToNestedChildProduct : function() {
                                                            // expect not to be called cause we don't have any nested block
                                                            expect(true).toBe(false);
                                                        },
                                                        _ : {}
                                                    }

                                                    elementToBeTransformed.forEach(function(el) {
                                                        el.transformationContext = getTransformationContextForElement(el, elementToBeTransformed);
                                                        fromAction.call(fragmentMock, el);

                                                    });
                                                });
                                        it("Element with inline content, nested inline content not wrapped in p and block content .",
                                                function() {
                                                    var elementToBeTransformed = fromFragmentToElement(ckEditorFragmentFactory
                                                            .getCkFragmentForHtml("<ul><li>fds<strong>af</strong>sa<block>fdasfdsa</block></li></ul>"));
                                                    var fromAction = transformationConfig.transformer.from.action;
                                                    var mapToChildProductsCounter = 0;
                                                    var mapToNestedChildProductCounter = 0;
                                                    var fragmentMock = {
                                                        mapToProducts : function() {
                                                            expect(JSON.stringify(arguments[1]))
                                                                    .toEqual(
                                                                            '[{"toPath":"list/indent","attrs":[{"from":"id","to":"id","action":"passAttributeTransformer"}]},{"toPath":"list/indent/num","attrs":[{"from":"data-akn-num-id","to":"id","action":"passAttributeTransformer"}]},{"toPath":"list/indent/num/text","fromAttribute":"data-akn-num"}]');
                                                        },
                                                        mapToChildProducts : function() {
                                                            if (mapToChildProductsCounter === 0) {
                                                                expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"list/indent","toChild":"alinea","attrs":[{"from":"data-akn-alinea-id","to":"id","action":"passAttributeTransformer"}]}');
                                                            } else if (mapToChildProductsCounter === 1) {
                                                                expect(JSON.stringify(arguments[1])).toEqual(
                                                                        '{"toPath":"list/indent/alinea","toChild":"content","attrs":[{"from":"data-akn-content-id","to":"id","action":"passAttributeTransformer"}]}');
                                                            } else if (mapToChildProductsCounter === 2) {
                                                                expect(JSON.stringify(arguments[1])).toEqual(
                                                                        '{"toPath":"list/indent/alinea/content","toChild":"mp","attrs":[{"from":"data-akn-mp-id","to":"id","action":"passAttributeTransformer"}]}');
                                                            } else if (mapToChildProductsCounter === 3) {
                                                                expect(JSON.stringify(arguments[1])).toEqual(
                                                                        '{"toPath":"list/indent/alinea/content/mp","toChild":"text","toChildTextValue":"fds"}');
                                                            } else if (mapToChildProductsCounter === 4) {
                                                                expect(JSON.stringify(arguments[1])).toEqual(
                                                                        '{"toPath":"list/indent/alinea/content/mp","toChild":"text","toChildTextValue":"sa"}');
                                                            } else if (mapToChildProductsCounter === 5) {
                                                                expect(JSON.stringify(arguments[1])).toEqual(
                                                                        '{"toPath":"list/indent/alinea","toChild":"content","attrs":[{"from":"data-akn-content-id","to":"id","action":"passAttributeTransformer"}]}');
                                                            } else if (mapToChildProductsCounter === 6) {
                                                                expect(JSON.stringify(arguments[1])).toEqual(
                                                                        '{"toPath":"list/indent/alinea/content","toChild":"mp","attrs":[{"from":"data-akn-mp-id","to":"id","action":"passAttributeTransformer"}]}');
                                                            } else if (mapToChildProductsCounter === 7) {
                                                                expect(JSON.stringify(arguments[1])).toEqual(
                                                                        '{"toPath":"list/indent/alinea/content/mp","toChild":"text","toChildTextValue":"sa"}');
                                                            } else {
                                                                // expect not to be called for mapToChildProductsCounter > 7
                                                                expect(true).toBe(false);
                                                            }
                                                            mapToChildProductsCounter++;
                                                        },
                                                        _getElementName : getElementName,
                                                        mapToNestedChildProduct : function() {
                                                            if (mapToNestedChildProductCounter === 0) {
                                                                expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"list/indent/alinea/content/mp"}');
                                                            } else if (mapToNestedChildProductCounter === 1) {
                                                                expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"list/indent"}');
                                                            } else {
                                                                // expect not to be called for mapToChildProductsCounter > 1
                                                                expect(true).toBe(false);
                                                            }

                                                            mapToNestedChildProductCounter += 1;
                                                        },
                                                        _ : {}
                                                    }

                                                    elementToBeTransformed.forEach(function(el) {
                                                        el.transformationContext = getTransformationContextForElement(el, elementToBeTransformed);
                                                        fromAction.call(fragmentMock, el);

                                                    });
                                                });
                                        
                                        
                                        it("Element with inline content, nested inline content wrapped with p and block content .",
                                                function() {
                                                    var elementToBeTransformed = fromFragmentToElement(ckEditorFragmentFactory
                                                            .getCkFragmentForHtml("<ul><li><p>fds<strong>af</strong>sa</p><block>fdasfdsa</block></li></ul>"));
                                                    var fromAction = transformationConfig.transformer.from.action;
                                                    var mapToChildProductsCounter = 0;
                                                    var mapToNestedChildProductCounter = 0;
                                                    var fragmentMock = {
                                                        mapToProducts : function() {
                                                            expect(JSON.stringify(arguments[1])).toEqual(
                                                                    '[{"toPath":"list/indent","attrs":[{"from":"id","to":"id","action":"passAttributeTransformer"}]},{"toPath":"list/indent/num","attrs":[{"from":"data-akn-num-id","to":"id","action":"passAttributeTransformer"}]},{"toPath":"list/indent/num/text","fromAttribute":"data-akn-num"}]');
                                                        },
                                                        mapToChildProducts : function() {
                                                            if (mapToChildProductsCounter === 0) {
                                                                expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"list/indent","toChild":"alinea","attrs":[{"from":"data-akn-alinea-id","to":"id","action":"passAttributeTransformer"}]}');
                                                            } else if (mapToChildProductsCounter === 1) {
                                                                expect(JSON.stringify(arguments[1])).toEqual(
                                                                        '{"toPath":"list/indent/alinea","toChild":"content","attrs":[{"from":"data-akn-content-id","to":"id","action":"passAttributeTransformer"}]}');
                                                            } else if (mapToChildProductsCounter === 2) {
                                                                expect(JSON.stringify(arguments[1])).toEqual(
                                                                        '{"toPath":"list/indent/alinea/content","toChild":"mp","attrs":[{"from":"data-akn-mp-id","to":"id","action":"passAttributeTransformer"}]}');
                                                            } else if (mapToChildProductsCounter === 3) {
                                                                expect(JSON.stringify(arguments[1])).toEqual(
                                                                        '{"toPath":"list/indent/alinea/content/mp","toChild":"text","toChildTextValue":"fds"}');
                                                            } else if (mapToChildProductsCounter === 4) {
                                                                expect(JSON.stringify(arguments[1])).toEqual(
                                                                        '{"toPath":"list/indent/alinea/content/mp","toChild":"text","toChildTextValue":"sa"}');
                                                            } else if (mapToChildProductsCounter === 5) {
                                                                expect(JSON.stringify(arguments[1])).toEqual(
                                                                        '{"toPath":"list/indent/alinea","toChild":"content","attrs":[{"from":"data-akn-content-id","to":"id","action":"passAttributeTransformer"}]}');
                                                            } else if (mapToChildProductsCounter === 6) {
                                                                expect(JSON.stringify(arguments[1])).toEqual(
                                                                        '{"toPath":"list/indent/alinea/content","toChild":"mp","attrs":[{"from":"data-akn-mp-id","to":"id","action":"passAttributeTransformer"}]}');
                                                            } else if (mapToChildProductsCounter === 7) {
                                                                expect(JSON.stringify(arguments[1])).toEqual(
                                                                        '{"toPath":"list/indent/alinea/content/mp","toChild":"text","toChildTextValue":"sa"}');
                                                            } else {
                                                                // expect not to be called for mapToChildProductsCounter > 7
                                                                expect(true).toBe(false);
                                                            }
                                                            mapToChildProductsCounter++;
                                                        },
                                                        _getElementName : getElementName,
                                                        mapToNestedChildProduct : function() {
                                                            if (mapToNestedChildProductCounter === 0) {
                                                                expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"list/indent/alinea/content/mp"}');
                                                            } else if (mapToNestedChildProductCounter === 1) {
                                                                expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"list/indent"}');
                                                            } else {
                                                                // expect not to be called for mapToChildProductsCounter > 1
                                                                expect(true).toBe(false);
                                                            }

                                                            mapToNestedChildProductCounter += 1;
                                                        },
                                                        _ : {}
                                                    }

                                                    elementToBeTransformed.forEach(function(el) {
                                                        el.transformationContext = getTransformationContextForElement(el, elementToBeTransformed);
                                                        fromAction.call(fragmentMock, el);

                                                    });
                                                });
                                    })

                            describe("From AKN to HTML side ",
                                    function() {
                                        it("Element only with inline content.", function() {
                                            var elementToBeTransformed = fromFragmentToElement(ckEditorFragmentFactory
                                                    .getCkFragmentForHtml("<list><indent><num>1</num><content><mp>fdsafsa</mp></content></indent></list>"));
                                            var toAction = transformationConfig.transformer.to.action;
                                            var mapToProductsCounter = 0;
                                            var mapToChildProductsCounter = 0;

                                            var fragmentMock = {
                                                mapToProducts : function() {
                                                    if(mapToProductsCounter === 0) {
                                                        expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"ul/li","attrs":[{"from":"id","to":"data-akn-num-id","action":"passAttributeTransformer"}]}');
                                                    } else if (mapToProductsCounter === 1) {
                                                        expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"ul/li","toAttribute":"data-akn-num"}');
                                                    } else if (mapToProductsCounter === 2) {
                                                        expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"ul/li","attrs":[{"from":"id","to":"data-akn-content-id","action":"passAttributeTransformer"}]}');
                                                    } else if (mapToProductsCounter === 3) {
                                                        expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"ul/li","attrs":[{"from":"id","to":"data-akn-mp-id","action":"passAttributeTransformer"}]}');
                                                    } else {
                                                     // expect not to be called for mapToProductsCounter > 3
                                                        expect(true).toBe(false);
                                                    }
                                                    mapToProductsCounter++;
                                                },
                                                mapToChildProducts : function() {
                                                    if (mapToChildProductsCounter === 0) {
                                                        expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"ul","toChild":"li","attrs":[{"from":"id","to":"id","action":"passAttributeTransformer"}]}');
                                                    } else if (mapToChildProductsCounter === 1) {
                                                        expect(JSON.stringify(arguments[1])).toEqual(
                                                                '{"toPath":"ul/li","toChild":"text","toChildTextValue":"fdsafsa"}');
                                                    } else {
                                                        // expect not to be called for mapToChildProductsCounter > 1
                                                        expect(true).toBe(false);
                                                    }
                                                    mapToChildProductsCounter++;

                                                },
                                                _getElementName : getElementName,
                                                mapToNestedChildProduct : function() {
                                                    // expect not to be called
                                                    expect(true).toBe(false);
                                                },
                                                _ : {}
                                            }

                                            elementToBeTransformed.forEach(function(el) {
                                                el.transformationContext = getTransformationContextForElement(el, elementToBeTransformed);
                                                toAction.call(fragmentMock, el);

                                            });
                                        });

                                        it("Element only with inline content, block content and nested inline content.",
                                                function() {
                                                    var elementToBeTransformed = fromFragmentToElement(ckEditorFragmentFactory
                                                            .getCkFragmentForHtml("<list><indent><num>1</num><alinea><content><mp>fd<b>saf</b>sa</mp></content></alinea><block>fdas</block></indent></list>"));
                                                    var toAction = transformationConfig.transformer.to.action;
                                                    var mapToProductsCounter = 0;
                                                    var mapToChildProductsCounter = 0;
                                                    var mapToNestedChildProductCounter = 0;
                                                    
                                                    var fragmentMock = {
                                                            mapToProducts : function() {
                                                                if(mapToProductsCounter === 0) {
                                                                    expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"ul/li","attrs":[{"from":"id","to":"data-akn-num-id","action":"passAttributeTransformer"}]}');
                                                                } else if (mapToProductsCounter === 1) {
                                                                    expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"ul/li","toAttribute":"data-akn-num"}');
                                                                } else if (mapToProductsCounter === 2) {
                                                                    expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"ul/li/p","attrs":[{"from":"id","to":"data-akn-content-id","action":"passAttributeTransformer"}]}');
                                                                } else if (mapToProductsCounter === 3) {
                                                                    expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"ul/li/p","attrs":[{"from":"id","to":"data-akn-mp-id","action":"passAttributeTransformer"}]}');
                                                                } else {
                                                                 // expect not to be called for mapToProductsCounter > 3
                                                                    expect(true).toBe(false);
                                                                }
                                                                mapToProductsCounter++;
                                                            },
                                                        mapToChildProducts : function() {
                                                            if (mapToChildProductsCounter === 0) {
                                                                expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"ul","toChild":"li","attrs":[{"from":"id","to":"id","action":"passAttributeTransformer"}]}');
                                                            } else if (mapToChildProductsCounter === 1) {
                                                                expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"ul/li","toChild":"p","attrs":[{"from":"id","to":"data-akn-alinea-id","action":"passAttributeTransformer"}]}');
                                                            } else if (mapToChildProductsCounter === 2) {
                                                                expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"ul/li/p","toChild":"text","toChildTextValue":"fd"}');
                                                            } else if (mapToChildProductsCounter === 3) {
                                                                expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"ul/li/p","toChild":"text","toChildTextValue":"sa"}');
                                                            } else {
                                                                // expect not to be called for mapToChildProductsCounter > 3
                                                                expect(true).toBe(false);
                                                            }
                                                            mapToChildProductsCounter++;

                                                        },
                                                        _getElementName : getElementName,
                                                        mapToNestedChildProduct : function() {
                                                            if (mapToNestedChildProductCounter === 0) {
                                                                expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"ul/li/p"}');
                                                            } else if (mapToNestedChildProductCounter === 1) {
                                                                expect(JSON.stringify(arguments[1])).toEqual('{"toPath":"ul/li"}');
                                                            }  else {
                                                                // expect not to be called for mapToChildProductsCounter > 2
                                                                expect(true).toBe(false);
                                                            }
                                                            mapToNestedChildProductCounter += 1;
                                                        },
                                                        _ : {}
                                                    }

                                                    elementToBeTransformed.forEach(function(el) {
                                                        el.transformationContext = getTransformationContextForElement(el, elementToBeTransformed);
                                                        toAction.call(fragmentMock, el);

                                                    });
                                                });
                                    })

                        })

            });
});