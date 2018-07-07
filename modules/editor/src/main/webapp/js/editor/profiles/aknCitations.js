/*
 * Copyright 2015 European Commission
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
define(function aknCitationsProfileModule(require) {
    "use strict";
    
    // require profile dependencies, if needed
    // e.g. ckEditor, plugins or utilities
    var transformationConfigManager = require("transformer/transformationConfigManager");
    var pluginTools = require("plugins/pluginTools");
    
    var plugins = [];
    plugins.push(require("plugins/leosTransformer/leosTransformerPlugin"));
    plugins.push(require("plugins/leosFixNestedPs/leosFixNestedPsPlugin"));
    plugins.push(require("plugins/aknHtmlBold/aknHtmlBoldPlugin"));
    plugins.push(require("plugins/aknHtmlItalic/aknHtmlItalicPlugin"));
    plugins.push(require("plugins/leosShowblocks/leosShowblocksPlugin"));
    plugins.push(require("plugins/aknAuthorialNote/aknAuthorialNotePlugin"));
    plugins.push(require("plugins/aknCitations/aknCitationsPlugin"));
    plugins.push(require("plugins/aknCitation/aknCitationPlugin"));
    plugins.push(require("plugins/aknHtmlSuperScript/aknHtmlSuperScriptPlugin"));
    plugins.push(require("plugins/aknHtmlSubScript/aknHtmlSubScriptPlugin"));
    plugins.push(require("plugins/leosAttrHandler/leosAttrHandlerPlugin"));

    var pluginNames = plugins.map(function(p){return p.name;});
    var extraPlugins = pluginNames.join(",");
    var transformationConfigResolver = transformationConfigManager.getTransformationConfigResolverForPlugins(pluginNames);
    
    var customContentsCss = pluginTools.toUrl("css/leosEditor.css");
    
    var profileName = "AKN Citations";

    // create profile configuration
    var profileConfig = {
        // user interface language localisation
        language: "en",
        // custom configuration to load (none if empty)
        customConfig: "",
        // comma-separated list of plugins to be loaded
        plugins: "toolbar,wysiwygarea,elementspath," +
                 "clipboard,undo,basicstyles,enterkey,entities," +
                 "button,dialog,dialogui,sourcearea",
        
        // comma-separated list of toolbar button names that must not be rendered
        removeButtons: "Underline,Strike",
        // comma-separated list of additional plugins to be loaded
        extraPlugins: extraPlugins,
        // convert all entities into Unicode numerical decimal format
        entities_processNumerical: "force",
        // disable Advanced Content Filter (allow all content)
        allowedContent: true,
        //custom style sheet
        contentsCss: customContentsCss,
        // height of the editing area
        height : 522,

        // toolbar groups arrangement, optimised for a single toolbar row
        toolbarGroups: [
            {name: "document", groups: ["document", "doctools"]},
            {name: "clipboard", groups: ["clipboard", "undo"]},
            {name: "editing", groups: ["find", "selection"]},
            {name: "forms"},
            {name: "basicstyles", groups: ["basicstyles", "cleanup"]},
            {name: "paragraph"},
            {name: "links"},
            {name: "insert"},
            {name: "styles"},
            {name: "colors"},
            {name: "tools"},
            {name: "others"},
            {name: "mode"},
            {name: "about"}
        ]
    };

    // create profile definition
    var profileDefinition = {
        name: profileName,
        config: profileConfig,
        transformationConfigResolver:transformationConfigResolver
    };

    return profileDefinition;
});