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
define(function testTransformationConfigResolverModule(require) {
    "use strict";
    var transformationConfigResolverStampToTest = require("transformer/transformationConfigResolver");
    var ckEditorFragmentFactory = require("specs/util/ckEditorFragmentFactory");
    var LODASH = require("lodash");

    var getHtmlTagsInArray = function getHtmlTagsInArray(params) {
        var htmlTags = [];
        if (params.elementName === "text") {
            htmlTags.push(params.textValue);
        } else {
            htmlTags = htmlTags.concat([ "<", params.elementName ]);
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
            htmlTags = htmlTags.concat([ "</", params.elementName, ">" ]);
        }

        return htmlTags;

    };

    var toConfigPath = function toConfigPath() {
        var pathAsArray = [];
        for (var i = 0; i < arguments.length; i++) {
            pathAsArray.push(arguments[i].toLowerCase());
        }
        return pathAsArray.join("/");
    };

    describe(
            "Unit tests for: editor/transformer/transformationConfigResolver",
            function() {

                describe("Test transformationConfigResolver.init() method.", function() {
                    var transformationConfigResolverToTest = transformationConfigResolverStampToTest();
                    it("Expects to throw exception when required param is not provided", function() {
                        expect(function() {
                            transformationConfigResolverToTest.init();
                        }).toThrowError("Param with name: 'transformationConfigs' is required, please provide it.");

                    });

                });

                describe(
                        "Test transformationConfigResolver.getConfig() method.",
                        function() {
                            it("Expect to resolve transformation config for one level element with unique transformation config present.", function() {
                                var fragmentInHtml;
                                var firstLevelElement = "firstLevelElement";
                                fragmentInHtml = getHtmlTagsInArray({
                                    elementName : firstLevelElement
                                }).join("");
                                var direction = "to";
                                // prepare transformation config
                                var firstLevelElementConfigs = {};
                                firstLevelElementConfigs[direction] = {};
                                firstLevelElementConfigs[direction] = [ {
                                    fromPath : toConfigPath(firstLevelElement),
                                    from : firstLevelElement
                                } ];

                                var transformationConfigs = [];
                                transformationConfigs.push(firstLevelElementConfigs);
                                // == end of prepare transformation config
                                var transformationConfigResolverToTest = transformationConfigResolverStampToTest();
                                transformationConfigResolverToTest.init({
                                    transformationConfigs : transformationConfigs
                                });

                                var fragment = ckEditorFragmentFactory.getCkFragmentForHtml(fragmentInHtml, direction);
                                // actual call
                                var resolvedConfig = transformationConfigResolverToTest.getConfig(fragment.children[0], direction);
                                expect(resolvedConfig).not.toBeUndefined();
                                expect(resolvedConfig).toEqual(firstLevelElementConfigs[direction]);
                            });

                            it("Expect not to resolve transformation config for one level element when no transformation config present.", function() {
                                var fragmentInHtml;
                                var firstLevelElement = "firstLevelElement";
                                var actualFirstLevelElement = "unknownFirstLevelElement";
                                fragmentInHtml = getHtmlTagsInArray({
                                    elementName : actualFirstLevelElement
                                }).join("");
                                var direction = "to";
                                // prepare transformation config
                                var firstLevelElementConfigs = {};
                                firstLevelElementConfigs[direction] = {};
                                firstLevelElementConfigs[direction] = [ {
                                    fromPath : toConfigPath(firstLevelElement),
                                    from : firstLevelElement
                                } ];

                                var transformationConfigs = [];
                                transformationConfigs.push(firstLevelElementConfigs);
                                // == end of prepare transformation config

                                var transformationConfigResolverToTest = transformationConfigResolverStampToTest();
                                transformationConfigResolverToTest.init({
                                    transformationConfigs : transformationConfigs
                                });
                                var fragment = ckEditorFragmentFactory.getCkFragmentForHtml(fragmentInHtml, direction);
                                // actual call
                                var resolvedConfig = transformationConfigResolverToTest.getConfig(fragment.children[0], direction);
                                expect(resolvedConfig).toBeUndefined();
                            });

                            it(
                                    "Expect not to resolve transformation config for one level element with transformation configs which covers also another element (two conflicting transformation configs).",
                                    function() {
                                        var fragmentInHtml;
                                        var firstLevelElement = "firstLevelElement";
                                        fragmentInHtml = getHtmlTagsInArray({
                                            elementName : firstLevelElement
                                        }).join("");
                                        var direction = "to";
                                        // prepare transformation config
                                        var firstLevelElementConfigs = {};
                                        firstLevelElementConfigs[direction] = {};
                                        firstLevelElementConfigs[direction] = [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement
                                        } ];

                                        var transformationConfigs = [];
                                        transformationConfigs.push(firstLevelElementConfigs);
                                        // and now we push the copy of firstLevelElementConfigs to create the conflict
                                        transformationConfigs.push(LODASH.cloneDeep(firstLevelElementConfigs));
                                        // == end of prepare transformation config

                                        var transformationConfigResolverToTest = transformationConfigResolverStampToTest();
                                        transformationConfigResolverToTest.init({
                                            transformationConfigs : transformationConfigs
                                        });

                                        var fragment = ckEditorFragmentFactory.getCkFragmentForHtml(fragmentInHtml, direction);
                                        // actual call
                                        var resolvedConfig = transformationConfigResolverToTest.getConfig(fragment.children[0], direction);
                                        expect(resolvedConfig).toBeUndefined();
                                    });

                            describe("Checking config resolution depending on the element attributes.", function() {
                                it("Expect to resolve config with attribute name for one config without attibute name and the other one with attribute name for element with attribute name.", function() {
                                    var fragmentInHtml;
                                    var firstLevelElement = "firstLevelElement";
                                    fragmentInHtml = getHtmlTagsInArray({
                                        elementName : firstLevelElement,
                                        attributes : [ {
                                            name : "data-akn-name"
                                        } ]
                                    }).join("");
                                    var direction = "to";
                                    var transformationConfigs = [ {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement
                                        } ]
                                    },
                                    {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement,
                                            fromAttribute: "data-akn-name"
                                        } ]
                                    }

                                    ];
                                    var transformationConfigResolverToTest = transformationConfigResolverStampToTest();
                                    transformationConfigResolverToTest.init({
                                        transformationConfigs : transformationConfigs
                                    });

                                    var fragment = ckEditorFragmentFactory.getCkFragmentForHtml(fragmentInHtml, direction);
                                    // actual call
                                    var resolvedConfig = transformationConfigResolverToTest.getConfig(fragment.children[0], direction);
                                    expect(resolvedConfig).not.toBeUndefined();
                                    expect(resolvedConfig).toEqual(transformationConfigs[1]['to']);
                                });
                                
                                it("Expect to return config without attribute name for one config without attibute name and the other one with attribute name for element without attribute name.", function() {
                                    var fragmentInHtml;
                                    var firstLevelElement = "firstLevelElement";
                                    fragmentInHtml = getHtmlTagsInArray({
                                        elementName : firstLevelElement
                                    }).join("");
                                    var direction = "to";
                                    var transformationConfigs = [ {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement
                                        } ]
                                    },
                                    {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement,
                                            fromAttribute: "data-akn-name"
                                        } ]
                                    }

                                    ];
                                    var transformationConfigResolverToTest = transformationConfigResolverStampToTest();
                                    transformationConfigResolverToTest.init({
                                        transformationConfigs : transformationConfigs
                                    });

                                    var fragment = ckEditorFragmentFactory.getCkFragmentForHtml(fragmentInHtml, direction);
                                    // actual call
                                    var resolvedConfig = transformationConfigResolverToTest.getConfig(fragment.children[0], direction);
                                    expect(resolvedConfig).not.toBeUndefined();
                                    expect(resolvedConfig).toEqual(transformationConfigs[0]['to']);
                                });
                                
                                
                                it("Expect to return config with attribute name and value for one config with attibute name and the other one with attribute name and value for element with attribute name and value.", function() {
                                    var fragmentInHtml;
                                    var firstLevelElement = "firstLevelElement";
                                    fragmentInHtml = getHtmlTagsInArray({
                                        elementName : firstLevelElement,
                                        attributes : [ {
                                            name : "data-akn-name",
                                            value: "akn1"
                                        } ]
                                    }).join("");
                                    var direction = "to";

                                    var transformationConfigs = [ {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement,
                                            fromAttribute: "data-akn-name"
                                        } ]
                                    },
                                    {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement,
                                            fromAttribute: "data-akn-name",
                                            fromAttributeValue: "akn1"
                                        } ]
                                    }

                                    ];
                                    var transformationConfigResolverToTest = transformationConfigResolverStampToTest();
                                    transformationConfigResolverToTest.init({
                                        transformationConfigs : transformationConfigs
                                    });

                                    var fragment = ckEditorFragmentFactory.getCkFragmentForHtml(fragmentInHtml, direction);
                                    // actual call
                                    var resolvedConfig = transformationConfigResolverToTest.getConfig(fragment.children[0], direction);
                                    expect(resolvedConfig).not.toBeUndefined();
                                    expect(resolvedConfig).toEqual(transformationConfigs[1]['to']);
                                });
                                
                                
                                it("Expect to return matched config with attribute name and value for one matched config with attibute name and value and the unmatched config with attribute name and value with attribute name and  value.", function() {
                                    var fragmentInHtml;
                                    var firstLevelElement = "firstLevelElement";
                                    fragmentInHtml = getHtmlTagsInArray({
                                        elementName : firstLevelElement,
                                        attributes : [ {
                                            name : "data-akn-name",
                                            value: "akn1"
                                        } ]
                                    }).join("");
                                    var direction = "to";

                                    var transformationConfigs = [ {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement,
                                            fromAttribute: "data-akn-name",
                                            fromAttributeValue: "akn1"
                                        } ]
                                    },
                                    {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement,
                                            fromAttribute: "data-akn-name",
                                            fromAttributeValue: "akn2"
                                        } ]
                                    }

                                    ];
                                    var transformationConfigResolverToTest = transformationConfigResolverStampToTest();
                                    transformationConfigResolverToTest.init({
                                        transformationConfigs : transformationConfigs
                                    });

                                    var fragment = ckEditorFragmentFactory.getCkFragmentForHtml(fragmentInHtml, direction);
                                    // actual call
                                    var resolvedConfig = transformationConfigResolverToTest.getConfig(fragment.children[0], direction);
                                    expect(resolvedConfig).not.toBeUndefined();
                                    expect(resolvedConfig).toEqual(transformationConfigs[0]['to']);
                                });
                                
                                it("Expect to return matched config with attribute name for one matched config with attibute name and not value and the unmatched config.", function() {
                                    var fragmentInHtml;
                                    var firstLevelElement = "firstLevelElement";
                                    fragmentInHtml = getHtmlTagsInArray({
                                        elementName : firstLevelElement,
                                        attributes : [ {
                                            name : "data-akn-name",
                                            value: "akn1"
                                        } ]
                                    }).join("");
                                    var direction = "to";

                                    var transformationConfigs = [ {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement,
                                            fromAttribute: "data-akn-name"
                                        } ]
                                    },
                                    {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement
                                        } ]
                                    }

                                    ];
                                    var transformationConfigResolverToTest = transformationConfigResolverStampToTest();
                                    transformationConfigResolverToTest.init({
                                        transformationConfigs : transformationConfigs
                                    });

                                    var fragment = ckEditorFragmentFactory.getCkFragmentForHtml(fragmentInHtml, direction);
                                    // actual call
                                    var resolvedConfig = transformationConfigResolverToTest.getConfig(fragment.children[0], direction);
                                    expect(resolvedConfig).not.toBeUndefined();
                                    expect(resolvedConfig).toEqual(transformationConfigs[0]['to']);
                                });
                                
                                
                                it("Expect to return undefined for two config with matching attributes names.", function() {
                                    var fragmentInHtml;
                                    var firstLevelElement = "firstLevelElement";
                                    fragmentInHtml = getHtmlTagsInArray({
                                        elementName : firstLevelElement,
                                        attributes : [ {
                                            name : "data-akn-name",
                                            value: "akn1"
                                        } ]
                                      
                                    }).join("");
                                    var direction = "to";

                                    var transformationConfigs = [ {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement,
                                            fromAttribute: "data-akn-name"
                                        } ]
                                    },
                                    {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement,
                                            fromAttribute: "data-akn-name"
                                        } ]
                                    }

                                    ];
                                    var transformationConfigResolverToTest = transformationConfigResolverStampToTest();
                                    transformationConfigResolverToTest.init({
                                        transformationConfigs : transformationConfigs
                                    });

                                    var fragment = ckEditorFragmentFactory.getCkFragmentForHtml(fragmentInHtml, direction);
                                    // actual call
                                    var resolvedConfig = transformationConfigResolverToTest.getConfig(fragment.children[0], direction);
                                    expect(resolvedConfig).toBeUndefined();
                                });
                                
                                
                                it("Expect to return undefined for two config with matching attributes names and its values.", function() {
                                    var fragmentInHtml;
                                    var firstLevelElement = "firstLevelElement";
                                    fragmentInHtml = getHtmlTagsInArray({
                                        elementName : firstLevelElement,
                                        attributes : [ {
                                            name : "data-akn-name",
                                            value: "akn1"
                                        } ]
                                      
                                    }).join("");
                                    var direction = "to";

                                    var transformationConfigs = [ {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement,
                                            fromAttribute: "data-akn-name",
                                            fromAttributeValue: "akn1"
                                                
                                        } ]
                                    },
                                    {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement,
                                            fromAttribute: "data-akn-name",
                                            fromAttributeValue: "akn1"
                                        } ]
                                    }

                                    ];
                                    var transformationConfigResolverToTest = transformationConfigResolverStampToTest();
                                    transformationConfigResolverToTest.init({
                                        transformationConfigs : transformationConfigs
                                    });

                                    var fragment = ckEditorFragmentFactory.getCkFragmentForHtml(fragmentInHtml, direction);
                                    // actual call
                                    var resolvedConfig = transformationConfigResolverToTest.getConfig(fragment.children[0], direction);
                                    expect(resolvedConfig).toBeUndefined();
                                });
                                
                                
                                it("Expect to return undefined for two config with unmatching attributes names.", function() {
                                    var fragmentInHtml;
                                    var firstLevelElement = "firstLevelElement";
                                    fragmentInHtml = getHtmlTagsInArray({
                                        elementName : firstLevelElement
                                    }).join("");
                                    var direction = "to";

                                    var transformationConfigs = [ {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement,
                                            fromAttribute: "data-akn-name"
                                        } ]
                                    },
                                    {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement,
                                            fromAttribute: "data-akn1-name"
                                        } ]
                                    }

                                    ];
                                    var transformationConfigResolverToTest = transformationConfigResolverStampToTest();
                                    transformationConfigResolverToTest.init({
                                        transformationConfigs : transformationConfigs
                                    });

                                    var fragment = ckEditorFragmentFactory.getCkFragmentForHtml(fragmentInHtml, direction);
                                    // actual call
                                    var resolvedConfig = transformationConfigResolverToTest.getConfig(fragment.children[0], direction);
                                    expect(resolvedConfig).toBeUndefined();
                                });
                                
                                
                                it("Expect to return undefined for two config with unmatching attributes names......", function() {
                                    var fragmentInHtml;
                                    var firstLevelElement = "firstLevelElement";
                                    fragmentInHtml = getHtmlTagsInArray({
                                        elementName : firstLevelElement,
                                        attributes : [ {
                                            name : "data-akn-name",
                                        }]
                                    }).join("");
                                    var direction = "to";

                                    var transformationConfigs = [ {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement,
                                            
                                        } ]
                                    },
                                    {
                                        "to" : [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement,
                                            
                                        } ]
                                    }

                                    ];
                                    var transformationConfigResolverToTest = transformationConfigResolverStampToTest();
                                    transformationConfigResolverToTest.init({
                                        transformationConfigs : transformationConfigs
                                    });

                                    var fragment = ckEditorFragmentFactory.getCkFragmentForHtml(fragmentInHtml, direction);
                                    // actual call
                                    var resolvedConfig = transformationConfigResolverToTest.getConfig(fragment.children[0], direction);
                                    expect(resolvedConfig).toBeUndefined();
                                });


                            });

                            it(
                                    "Expect  to resolve transformation config for two level element with  transformation config present and conflicting at first level but not at the second level.",
                                    function() {
                                        var fragmentInHtml;
                                        var firstLevelElement = "firstLevelElement";
                                        var secondLevelElement = "secondLevelElement";
                                        fragmentInHtml = getHtmlTagsInArray({
                                            elementName : firstLevelElement,
                                            children : getHtmlTagsInArray({
                                                elementName : secondLevelElement
                                            })
                                        }).join("");
                                        var direction = "to";
                                        // prepare transformation config
                                        var firstLevelElementConfigs = {};
                                        firstLevelElementConfigs[direction] = {};
                                        firstLevelElementConfigs[direction] = [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement
                                        }, {
                                            fromPath : toConfigPath(firstLevelElement, secondLevelElement),
                                            from : secondLevelElement
                                        } ];

                                        var firstLevelElementConfigsDuplicate = {};
                                        firstLevelElementConfigsDuplicate[direction] = {};
                                        firstLevelElementConfigsDuplicate[direction] = [ {
                                            fromPath : toConfigPath(firstLevelElement),
                                            from : firstLevelElement
                                        } ];

                                        var transformationConfigs = [];
                                        transformationConfigs.push(firstLevelElementConfigs);
                                        transformationConfigs.push(firstLevelElementConfigsDuplicate);
                                        // == end of prepare transformation config

                                        var transformationConfigResolverToTest = transformationConfigResolverStampToTest();
                                        transformationConfigResolverToTest.init({
                                            transformationConfigs : transformationConfigs
                                        });

                                        var fragment = ckEditorFragmentFactory.getCkFragmentForHtml(fragmentInHtml, direction);
                                        // actual call
                                        var resolvedConfig = transformationConfigResolverToTest.getConfig(fragment.children[0], direction);
                                        expect(resolvedConfig).not.toBeUndefined();
                                        expect(resolvedConfig).toEqual(firstLevelElementConfigs[direction]);
                                    });
                        });

            });

});