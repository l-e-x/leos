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
define(function transformationConfigManagerModule(require) {
    "use strict";
    var configNormalizerStamp = require("transformer/configNormalizer");
    var transformationConfigResolverStamp = require("transformer/transformationConfigResolver");

    var pluginNameToItsConfigs = {};

    var transformationConfigManager = {

        addTransformationConfigForPlugin: function addTransformationConfigForPlugin(config, pluginName) {
            var configNormalizer = this._getConfigNormalizer();
            var normalizedConfig = configNormalizer.getNormalizedConfig({
                rawConfig: config
            });
            pluginNameToItsConfigs[pluginName] = normalizedConfig;
        },
        _getConfigNormalizer: function _getConfigNormalizer() {
            return configNormalizerStamp();
        },

        getTransformationConfigResolverForPlugins: function getTransformationConfigResolverForPlugins(plugins) {
            var transformationConfigResolver = this._getConfigResolver();
            var configsForPlugins = [];
            plugins.forEach(function(pluginName) {
                configsForPlugins.push(pluginNameToItsConfigs[pluginName]);
            });
            transformationConfigResolver.init({
                transformationConfigs: configsForPlugins
            });
            return transformationConfigResolver;
        },
        _getConfigResolver: function _getConfigResolver() {
            return transformationConfigResolverStamp();
        }

    };
    return transformationConfigManager;
});