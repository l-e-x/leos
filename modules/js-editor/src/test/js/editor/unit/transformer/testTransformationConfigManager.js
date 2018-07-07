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
define(function testTransformationConfigManagerModule(require) {
    "use strict";
    var transformationConfigManagerToTest = require("transformer/transformationConfigManager");

    var addTransformationConfigForPlugin = function test_addTransformationConfigForPlugin(config, pluginName) {
        transformationConfigManagerToTest.addTransformationConfigForPlugin(config, pluginName);
    };

    var getTransformationConfigResolverForPlugins = function test_getTransformationConfigResolverForPlugins(plugins) {
        var transformationConfigResolver = transformationConfigManagerToTest.getTransformationConfigResolverForPlugins(plugins);
        return transformationConfigResolver;
    };

    var getMockPluginConfiguration = {
        plugin1: {
            pluginName: "plugin1",
            rawConfig: {},
            normalizedConfig: {
                akn: "plugin1"
            }
        },
        plugin2: {
            pluginName: "plugin2",
            rawConfig: {},
            normalizedConfig: {
                akn: "plugin2"
            }
        },
        plugin3: {
            pluginName: "plugin3",
            rawConfig: {},
            normalizedConfig: {
                akn: "plugin3"
            }
        }
    };
    function setUpTransformationPluginForConfig() {
        // Mock Config Normalizer
        transformationConfigManagerToTest._getConfigNormalizer = function(config) {
            return {
                getNormalizedConfig: function(config) {
                    if (config.rawConfig === getMockPluginConfiguration.plugin1.rawConfig) {
                        return getMockPluginConfiguration.plugin1.normalizedConfig;
                    } else if (config.rawConfig === getMockPluginConfiguration.plugin2.rawConfig) {
                        return getMockPluginConfiguration.plugin2.normalizedConfig;
                    } else if (config.rawConfig === getMockPluginConfiguration.plugin3.rawConfig) {
                        return getMockPluginConfiguration.plugin3.normalizedConfig;
                    }
                }
            };
        };

        transformationConfigManagerToTest.addTransformationConfigForPlugin(getMockPluginConfiguration.plugin1.rawConfig,
                getMockPluginConfiguration.plugin1.pluginName);
        transformationConfigManagerToTest.addTransformationConfigForPlugin(getMockPluginConfiguration.plugin2.rawConfig,
                getMockPluginConfiguration.plugin2.pluginName);
        transformationConfigManagerToTest.addTransformationConfigForPlugin(getMockPluginConfiguration.plugin3.rawConfig,
                getMockPluginConfiguration.plugin3.pluginName);
    };

    describe("Unit Test /editor/unit/transformer/transformationConfigManager", function() {
        setUpTransformationPluginForConfig();
        
        it("Expect to return list of normalized configs should be equal to the list added to the transformation config resolver.", function() {

            var toBeExpected = [getMockPluginConfiguration.plugin1.normalizedConfig, getMockPluginConfiguration.plugin3.normalizedConfig];
            
            var normalizedConfigsPassedToResolver = [];
            // Mock Config Resolver
            transformationConfigManagerToTest._getConfigResolver = function(config) {
                return {
                    init: function(normalizedConfigs) {
                        normalizedConfigsPassedToResolver = normalizedConfigs.transformationConfigs;
                    }
                };
            };

            // DO THE ACTUAL CALL
            var resolver = transformationConfigManagerToTest.getTransformationConfigResolverForPlugins([getMockPluginConfiguration.plugin1.pluginName,
                    getMockPluginConfiguration.plugin3.pluginName]);

            expect(resolver).not.toBeNull();
            expect(normalizedConfigsPassedToResolver).toEqual(toBeExpected);

        });


        it("Expect to return list of normalized configs should not be equal to the list added to the transformation config resolver.", function() {

            var toBeExpected = [getMockPluginConfiguration.plugin1.normalizedConfig, getMockPluginConfiguration.plugin2.normalizedConfig];
            
            var normalizedConfigsPassedToResolver = [];
            // Mock Config Resolver
            transformationConfigManagerToTest._getConfigResolver = function(config) {
                return {
                    init: function(normalizedConfigs) {
                        normalizedConfigsPassedToResolver = normalizedConfigs.transformationConfigs;
                    }
                };
            };

            // DO THE ACTUAL CALL
            var resolver = transformationConfigManagerToTest.getTransformationConfigResolverForPlugins([getMockPluginConfiguration.plugin1.pluginName,
                    getMockPluginConfiguration.plugin3.pluginName]);

            expect(resolver).not.toBeNull();
            expect(normalizedConfigsPassedToResolver).not.toEqual(toBeExpected);

        });

        
    });

});