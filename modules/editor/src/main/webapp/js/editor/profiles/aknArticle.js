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
;   // jshint ignore:line
define(function aknArticleProfileModule(require) {
    "use strict";
    var transformationConfigManager = require("transformer/transformationConfigManager");
    var pluginTools = require("plugins/pluginTools");
    // require profile dependencies, if needed
    // e.g. ckEditor, plugins or utilities
    var plugins = [];
    plugins.push(require("plugins/aknHtmlTable/aknHtmlTablePlugin"));
    plugins.push(require("plugins/aknHtmlAnchor/aknHtmlAnchorPlugin"));
    plugins.push(require("plugins/aknArticle/aknArticlePlugin"));
    plugins.push(require("plugins/aknHtmlBold/aknHtmlBoldPlugin"));
    plugins.push(require("plugins/aknHtmlItalic/aknHtmlItalicPlugin"));
    plugins.push(require("plugins/aknHtmlUnderline/aknHtmlUnderlinePlugin"));
    plugins.push(require("plugins/aknOrderedList/aknOrderedListPlugin"));
    plugins.push(require("plugins/aknUnorderedList/aknUnorderedListPlugin"));
    plugins.push(require("plugins/aknNumberedParagraph/aknNumberedParagraphPlugin"));
    plugins.push(require("plugins/leosShowblocks/leosShowblocksPlugin"));
    plugins.push(require("plugins/aknAuthorialNote/aknAuthorialNotePlugin"));
    plugins.push(require("plugins/leosTransformer/leosTransformerPlugin"));
    plugins.push(require("plugins/leosFixNestedPs/leosFixNestedPsPlugin"));
    plugins.push(require("plugins/leosNonEditable/leosNonEditablePlugin"));
    plugins.push(require("plugins/leosMathematicalFormula/leosMathematicalFormulaPlugin"));
    plugins.push(require("plugins/aknHtmlSuperScript/aknHtmlSuperScriptPlugin"));
    plugins.push(require("plugins/aknHtmlSubScript/aknHtmlSubScriptPlugin"));
    plugins.push(require("plugins/leosWidget/leosWidgetPlugin"));
    plugins.push(require("plugins/leosAttrHandler/leosAttrHandlerPlugin"));
    plugins.push(require("plugins/indentlist/indentListPlugin"));
        
    var pluginNames = plugins.map(function(p){return p.name;});
    var transformationConfigResolver = transformationConfigManager.getTransformationConfigResolverForPlugins(pluginNames);
    //holds ckEditor external plugins names
    var externalPluginsNames = ["scayt", "lite"]; 
    pluginTools.addExternalPlugins(externalPluginsNames);
    var extraPlugins = pluginNames.concat(externalPluginsNames).join(",");
    
    var customContentsCss = pluginTools.toUrl("css/leosEditor.css");
    
    var profileName = "AKN Article";
    // create profile configuration
    var profileConfig = {
        // user interface language localization
        language: "en",
        // custom configuration to load (none if empty)
        customConfig: "",
        // comma-separated list of plugins to be loaded
        plugins: "toolbar,wysiwygarea,elementspath," +
        "clipboard,undo,enterkey,entities," +
        "button,dialog,dialogui,sourcearea," +
        "widget,lineutils,basicstyles," +
        "list,indentlist,indent," +
        "link,fakeobjects,find,specialchar,table,tableresize,tabletools,contextmenu,menubutton,mathjax,pastetext",
        // comma-separated list of plugins that must not be loaded
        removePlugins: "",
        // comma-separated list of additional plugins to be loaded
        extraPlugins: extraPlugins,
        // convert all entities into Unicode numerical decimal format
        entities_processNumerical: "force",
        // disable Advanced Content Filter (allow all content)
        allowedContent: true,
        //custom style sheet
        contentsCss: customContentsCss,
        //force Paste as plain text
        forcePasteAsPlainText: false,
        // toolbar groups arrangement, optimized for a single toolbar row
        toolbarGroups: [
            {name: "document", groups: ["document", "doctools"]},
            {name: "clipboard", groups: ["clipboard", "undo"]},
            {name: "editing", groups: ["find", "selection", "spellchecker"]},
            {name: "forms"},
            {name: "basicstyles", groups: ["basicstyles", "cleanup"]},
            {name: "paragraph", groups: ["indent", "blocks", "align", "bidi"]},
            {name: "links"},
            {name: "insert"},
            {name: "styles"},
            {name: "colors"},
            {name: "lite"},
            {name: "tools"},
            {name: "others"},
            {name: "mode"},
            {name: "about"}
        ],
        // comma-separated list of toolbar button names that must not be rendered
        removeButtons: "Underline,Strike,Anchor",
        // semicolon-separated list of dialog elements that must not be rendered
        // element is a string concatenation of dialog name + colon + tab name
        removeDialogTabs: "link:advanced;link:target",
        // height of the editing area
        height : 515,
        // LITE plugin configuration
        lite: {
            includes: [
                "js/rangy/rangy-core.js",
                "js/ice.js",
                "js/dom.js",
                "js/selection.js",
                "js/bookmark.js",
                "lite-interface.js"
            ],
            tooltips: {
                show: false
            },
            isTracking: false
        }
    };

    // create profile definition
    var profileDefinition = {
        name: profileName,
        config: profileConfig,
        transformationConfigResolver:transformationConfigResolver
    };

    return profileDefinition;
});