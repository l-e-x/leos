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
define(function suggestionProfileModule(require) {
    "use strict";
    
    // require profile dependencies, if needed
    // e.g. ckEditor, plugins or utilities
    var transformationConfigManager = require("transformer/transformationConfigManager");
    var pluginTools = require("plugins/pluginTools");
    
    var plugins = [];
    plugins.push(require("plugins/leosInlineEditor/leosInlineEditorPlugin"));
    plugins.push(require("plugins/leosTransformer/leosTransformerPlugin"));
    plugins.push(require("plugins/leosFixNestedPs/leosFixNestedPsPlugin"));
    plugins.push(require("plugins/aknHtmlBold/aknHtmlBoldPlugin"));
    plugins.push(require("plugins/aknHtmlItalic/aknHtmlItalicPlugin"));
    plugins.push(require("plugins/aknAuthorialNote/aknAuthorialNotePlugin"));
    plugins.push(require("plugins/aknHtmlSuperScript/aknHtmlSuperScriptPlugin"));
    plugins.push(require("plugins/aknHtmlSubScript/aknHtmlSubScriptPlugin"));
    plugins.push(require("plugins/leosAttrHandler/leosAttrHandlerPlugin"));
    plugins.push(require("plugins/leosFloatingSpace/leosFloatingSpacePlugin"));
    plugins.push(require("plugins/leosWidget/leosWidgetPlugin"));
    plugins.push(require("plugins/leosComments/leosCommentsPlugin"));
    plugins.push(require("plugins/leosMathematicalFormula/leosMathematicalFormulaPlugin"));
    plugins.push(require("plugins/leosHighlight/leosHighlightPlugin"));
    plugins.push(require("plugins/aknHtmlAnchor/aknHtmlAnchorPlugin"));
    plugins.push(require("plugins/leosCrossReference/leosCrossReferencePlugin"));
    plugins.push(require("plugins/leosXmlEntities/leosXmlEntitiesPlugin"));

    plugins.push(require("plugins/leosHeadingSuggestion/leosHeadingSuggestionPlugin"));
    plugins.push(require("plugins/leosDocTitleSuggestion/leosDocTitleSuggestionPlugin"));


    var pluginNames = plugins.map(function(p){return p.name;});
    // holds ckEditor external plugins names
    var externalPluginsNames = [];
    pluginTools.addExternalPlugins(externalPluginsNames);
    var extraPlugins = pluginNames.concat(externalPluginsNames).join(",");
    var transformationConfigResolver = transformationConfigManager.getTransformationConfigResolverForPlugins(pluginNames);
    
    var profileName = "Suggestion";

    // create profile configuration
    var profileConfig = {
        // user interface language localisation
        language: "en",
        // custom configuration to load (none if empty)
        customConfig: "",
        // comma-separated list of plugins to be loaded
        plugins: "toolbar,wysiwygarea,elementspath," +
                 "clipboard,undo,basicstyles,enterkey," +
                 "button,dialog,dialogui,sourcedialog,mathjax",
        
        // comma-separated list of toolbar button names that must not be rendered
        removeButtons: "Underline,Strike,TextColor,LeosComment,Mathjax,LeosCrossReference",
        // comma-separated list of additional plugins to be loaded
        extraPlugins: extraPlugins,
        // disable Advanced Content Filter (allow all content)
        allowedContent: true,
        //show toolbar on startup
        startupFocus: true,
        //default enter mode
        enterMode: CKEDITOR.ENTER_BR,
      //MathJax plugin configuration - Sets the path to the MathJax library
        mathJaxLib: './webjars/MathJax/2.5.3/MathJax.js?config=default',
        // toolbar groups arrangement, optimised for a single toolbar row
        toolbarGroups : [ {
            name : "save"
        }, {
            name : "document",
            groups : [ "document", "doctools" ]
        }, {
            name : "clipboard",
            groups : [ "clipboard", "undo" ]
        }, {
            name : "editing",
            groups : [ "find", "selection" ]
        }, {
            name : "forms"
        }, {
            name : "basicstyles",
            groups : [ "basicstyles", "cleanup" ]
        }, {
            name : "paragraph"
        }, {
            name : "links"
        }, {
            name : "insert"
        }, {
            name : "styles"
        }, {
            name : "colors"
        }, {
            name : "tools"
        }, {
            name : "others"
        }, {
            name : "mode"
        }, {
            name : "about"
        } ],
    };

    // create profile definition
    var profileDefinition = {
        name: profileName,
        config: profileConfig,
        transformationConfigResolver:transformationConfigResolver
    };

    return profileDefinition;
});