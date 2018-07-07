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
define(function testConfigNormalizerModule(require) {
    "use strict";
    var ConfigNormalizerToTest = require("transformer/configNormalizer");

    var getNormalizedConfig = function getNormalizedConfig(rawConfig) {
        var configNormalizer = ConfigNormalizerToTest();
        var normalizedConfig = configNormalizer.getNormalizedConfig({
            rawConfig : rawConfig
        });
        return normalizedConfig;
    };

    describe("Unit tests /editor/transformer/configNormalizer", function() {

        describe("Test ConfigNormalizerToTest.getNormalizedConfigs()", function() {

            it("Should transform input akn node into html node", function() {

                var rawConfig = {
                    akn : 'aknnode',
                    html : 'htmlnode',
                    transformer : {}
                };

                var expectedNormalizeConfig = {
                    "to" : [ {
                        "attrs" : [],
                        "fromPath" : "aknnode",
                        "fromParentPath" : "aknnode",
                        "from" : "aknnode",
                        "fromAttribute" : undefined,
                        "toPath" : "htmlnode",
                        "toParentPath" : "htmlnode",
                        "to" : "htmlnode",
                        "toAttribute" : undefined
                    } ],
                    "from" : [ {
                        "attrs" : [],
                        "fromPath" : "htmlnode",
                        "fromParentPath" : "htmlnode",
                        "from" : "htmlnode",
                        "fromAttribute" : undefined,
                        "toPath" : "aknnode",
                        "toParentPath" : "aknnode",
                        "to" : "aknnode",
                        "toAttribute" : undefined
                    } ],
                    transformer : {}
                };

                var normalizedConfig = getNormalizedConfig(rawConfig);
                expect(normalizedConfig).not.toBeNull();
                expect(normalizedConfig).toEqual(expectedNormalizeConfig);
            });

            it("Should transform input akn node with one child into html node with one child", function() {
                var rawConfig = {
                    akn : "aknnode",
                    html : "htmlnode",
                    sub : {
                        akn : "aknchild",
                        html : "htmlnode/htmlchild"
                    },
                    transformer : {}
                };

                var expectedNormalizeConfig = {
                    "to" : [ {
                        "attrs" : [],
                        "fromPath" : "aknnode",
                        "fromParentPath" : "aknnode",
                        "from" : "aknnode",
                        "fromAttribute" : undefined,
                        "toPath" : "htmlnode",
                        "toParentPath" : "htmlnode",
                        "to" : "htmlnode",
                        "toAttribute" : undefined
                    }, {
                        "attrs" : [],
                        "fromPath" : "aknnode/aknchild",
                        "fromParentPath" : "aknnode",
                        "from" : "aknchild",
                        "fromAttribute" : undefined,
                        "toPath" : "htmlnode/htmlchild",
                        "toParentPath" : "htmlnode",
                        "to" : "htmlchild",
                        "toAttribute" : undefined
                    } ],
                    "from" : [ {
                        "attrs" : [],
                        "fromPath" : "htmlnode",
                        "fromParentPath" : "htmlnode",
                        "from" : "htmlnode",
                        "fromAttribute" : undefined,
                        "toPath" : "aknnode",
                        "toParentPath" : "aknnode",
                        "to" : "aknnode",
                        "toAttribute" : undefined
                    }, {
                        "attrs" : [],
                        "fromPath" : "htmlnode/htmlchild",
                        "fromParentPath" : "htmlnode",
                        "from" : "htmlchild",
                        "fromAttribute" : undefined,
                        "toPath" : "aknnode/aknchild",
                        "toParentPath" : "aknnode",
                        "to" : "aknchild",
                        "toAttribute" : undefined
                    } ],
                    transformer : {}
                };

                var normalizedConfig = getNormalizedConfig(rawConfig);
                expect(normalizedConfig).not.toBeNull();
                expect(normalizedConfig).toEqual(expectedNormalizeConfig);
            });

            it("Should transform input akn node with sub children into html node", function() {
                var rawConfig = {
                    akn : "aknnode",
                    html : "htmlnode",
                    sub : {
                        akn : "aknchild",
                        html : "htmlnode/htmlchild",
                        sub : {
                            akn : "aknsubchild",
                            html : "htmlnode/htmlchild/htmlsubchild"
                        }
                    },
                    transformer : {}
                };

                var expectedNormalizeConfig = {
                    "to" : [ {
                        "attrs" : [],
                        "fromPath" : "aknnode",
                        "fromParentPath" : "aknnode",
                        "from" : "aknnode",
                        "fromAttribute" : undefined,
                        "toPath" : "htmlnode",
                        "toParentPath" : "htmlnode",
                        "to" : "htmlnode",
                        "toAttribute" : undefined
                    }, {
                        "attrs" : [],
                        "fromPath" : "aknnode/aknchild",
                        "fromParentPath" : "aknnode",
                        "from" : "aknchild",
                        "fromAttribute" : undefined,
                        "toPath" : "htmlnode/htmlchild",
                        "toParentPath" : "htmlnode",
                        "to" : "htmlchild",
                        "toAttribute" : undefined
                    }, {
                        "attrs" : [],
                        "fromPath" : "aknnode/aknchild/aknsubchild",
                        "fromParentPath" : "aknnode/aknchild",
                        "from" : "aknsubchild",
                        "fromAttribute" : undefined,
                        "toPath" : "htmlnode/htmlchild/htmlsubchild",
                        "toParentPath" : "htmlnode/htmlchild",
                        "to" : "htmlsubchild",
                        "toAttribute" : undefined
                    } ],
                    "from" : [ {
                        "attrs" : [],
                        "fromPath" : "htmlnode",
                        "fromParentPath" : "htmlnode",
                        "from" : "htmlnode",
                        "fromAttribute" : undefined,
                        "toPath" : "aknnode",
                        "toParentPath" : "aknnode",
                        "to" : "aknnode",
                        "toAttribute" : undefined
                    }, {
                        "attrs" : [],
                        "fromPath" : "htmlnode/htmlchild",
                        "fromParentPath" : "htmlnode",
                        "from" : "htmlchild",
                        "fromAttribute" : undefined,
                        "toPath" : "aknnode/aknchild",
                        "toParentPath" : "aknnode",
                        "to" : "aknchild",
                        "toAttribute" : undefined
                    }, {
                        "attrs" : [],
                        "fromPath" : "htmlnode/htmlchild/htmlsubchild",
                        "fromParentPath" : "htmlnode/htmlchild",
                        "from" : "htmlsubchild",
                        "fromAttribute" : undefined,
                        "toPath" : "aknnode/aknchild/aknsubchild",
                        "toParentPath" : "aknnode/aknchild",
                        "to" : "aknsubchild",
                        "toAttribute" : undefined
                    } ],
                    transformer : {}
                };

                var normalizedConfig = getNormalizedConfig(rawConfig);
                expect(normalizedConfig).not.toBeNull();
                expect(normalizedConfig).toEqual(expectedNormalizeConfig);
            });

            it("Should transform input akn node with one attribute into html node with one attribute", function() {

                var rawConfig = {
                    akn : 'aknnode',
                    html : 'htmlnode',
                    attr : [ {
                        akn : "aknattr",
                        html : "htmlattr"
                    } ],
                    transformer : {}
                };

                var expectedNormalizeConfig = {
                    "to" : [ {
                        "attrs" : [ {
                            "from" : "aknattr",
                            "fromValue" : undefined,
                            "to" : "htmlattr",
                            "toValue" : undefined,
                            "action" : "passAttributeTransformer"
                        } ],
                        "fromPath" : "aknnode",
                        "fromParentPath" : "aknnode",
                        "from" : "aknnode",
                        "fromAttribute" : undefined,
                        "toPath" : "htmlnode",
                        "toParentPath" : "htmlnode",
                        "to" : "htmlnode",
                        "toAttribute" : undefined
                    } ],
                    "from" : [ {
                        "attrs" : [ {
                            "to" : "aknattr",
                            "toValue" : undefined,
                            "from" : "htmlattr",
                            "fromValue" : undefined,
                            "action" : "passAttributeTransformer"
                        } ],
                        "fromPath" : "htmlnode",
                        "fromParentPath" : "htmlnode",
                        "from" : "htmlnode",
                        "fromAttribute" : undefined,
                        "toPath" : "aknnode",
                        "toParentPath" : "aknnode",
                        "to" : "aknnode",
                        "toAttribute" : undefined
                    } ],
                    transformer : {}
                };

                var normalizedConfig = getNormalizedConfig(rawConfig);
                expect(normalizedConfig).not.toBeNull();
                expect(normalizedConfig).toEqual(expectedNormalizeConfig);
            });

            it("Should have have attribute in akn and html node mapping.", function() {

                var rawConfig = {
                    akn : 'aknnode[aknAttr]',
                    html : 'htmlnode[htmlAttr=htmlAttrValue]',
                    attr : [ {
                        akn : "aknattr",
                        html : "htmlattr"
                    } ],
                    transformer : {}
                };

                var expectedNormalizeConfig = {
                    to : [ {
                        attrs : [ {
                            from : 'aknattr',
                            fromValue : undefined,
                            to : 'htmlattr',
                            toValue : undefined,
                            action : 'passAttributeTransformer'
                        } ],
                        fromPath : 'aknnode',
                        fromParentPath : 'aknnode',
                        from : 'aknnode',
                        fromAttribute : 'aknAttr',
                        toPath : 'htmlnode',
                        toParentPath : 'htmlnode',
                        to : 'htmlnode',
                        toAttribute : 'htmlAttr',
                        toAttributeValue : 'htmlAttrValue'
                    } ],
                    from : [ {
                        attrs : [ {
                            to : 'aknattr',
                            toValue : undefined,
                            from : 'htmlattr',
                            fromValue : undefined,
                            action : 'passAttributeTransformer'
                        } ],
                        fromPath : 'htmlnode',
                        fromParentPath : 'htmlnode',
                        from : 'htmlnode',
                        fromAttribute : 'htmlAttr',
                        fromAttributeValue : 'htmlAttrValue',
                        toPath : 'aknnode',
                        toParentPath : 'aknnode',
                        to : 'aknnode',
                        toAttribute : 'aknAttr',
                        toAttributeValue : 'htmlAttrValue'
                    } ],
                    transformer : {}
                }

                var normalizedConfig = getNormalizedConfig(rawConfig);
                expect(normalizedConfig).not.toBeNull();
                expect(normalizedConfig).toEqual(expectedNormalizeConfig);
            });

        });

    });

});