/*
 * Copyright 2018 European Commission
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
define(function aknInlineClauseProfileModule(require) {
    "use strict";

    // require profile dependencies, if needed
    // e.g. ckEditor, plugins or utilities
    var transformationConfigManager = require("transformer/transformationConfigManager");
    var pluginTools = require("plugins/pluginTools");
    var $ = require('jquery');

    var plugins = [];
    plugins.push(require("plugins/leosInlineSave/leosInlineSavePlugin"));
    plugins.push(require("plugins/leosInlineCancel/leosInlineCancelPlugin"));
    plugins.push(require("plugins/leosInlineEditor/leosInlineEditorPlugin"));   //required for blur/focus
    plugins.push(require("plugins/leosFloatingSpace/leosFloatingSpacePlugin")); //required for positioning inline editor toolbar
    plugins.push(require("plugins/leosXmlEntities/leosXmlEntitiesPlugin"));     //required for xml entities <--> html entities
    plugins.push(require("plugins/leosToolbar/leosToolbarPlugin"));
    plugins.push(require("plugins/leosAlternatives/leosAlternativesPlugin"));
    plugins.push(require("plugins/aknClause/aknClausePlugin"));
    plugins.push(require("plugins/leosTransformer/leosTransformerPlugin"));
    plugins.push(require("plugins/leosFixNestedPs/leosFixNestedPsPlugin"));
    plugins.push(require("plugins/leosTextCaseChanger/leosTextCaseChangerPlugin"));

    var pluginNames=[];
    var specificConfig={};
    $.each(plugins, function( index, value ) {
        pluginNames.push(value.name);
        specificConfig= $.extend( specificConfig,  value.specificConfig);
    });
    var transformationConfigResolver = transformationConfigManager.getTransformationConfigResolverForPlugins(pluginNames);
    
    // holds ckEditor external plugins names
    var externalPluginsNames = [];
    pluginTools.addExternalPlugins(externalPluginsNames);
    var extraPlugins = pluginNames.concat(externalPluginsNames).join(",");

    var profileName = "Inline AKN Clause";

    // create profile configuration
    var profileConfig = {
        // user interface language localisation
        language: "en",

        // custom configuration to load (none if empty)
        customConfig: "",
        // comma-separated list of plugins to be loaded
        plugins: "wysiwygarea,elementspath,undo,"
                 + "button,dialog,dialogui",
        // comma-separated list of toolbar button names that must not be rendered
        removeButtons: "",
        // comma-separated list of additional plugins to be loaded
        extraPlugins: extraPlugins,
        // disable Advanced Content Filter (allow all content)
        allowedContent: true,
        //show toolbar on startup
        startupFocus: 'end',

        //Use native spellchecker
        disableNativeSpellChecker: false,
        // LEOS-2887 removing tooltip title 
        title: false,
        // toolbar groups arrangement, optimised for a single toolbar row
        toolbarGroups : [ {
            name : "save"
        }, {
            name : "alternatives"
        }]
    };
    // adding the specific configs coming from the plugins.
    profileConfig = $.extend( profileConfig,  specificConfig);
    // create profile definition
    var profileDefinition = {
        name: profileName,
        config: profileConfig,
        transformationConfigResolver: transformationConfigResolver
    };

    return profileDefinition;
});
