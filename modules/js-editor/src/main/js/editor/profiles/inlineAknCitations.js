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
define(function aknCitationsProfileModule(require) {
    "use strict";
    
    // require profile dependencies, if needed
    // e.g. ckEditor, plugins or utilities
    var transformationConfigManager = require("transformer/transformationConfigManager");
    var pluginTools = require("plugins/pluginTools");
    
    var plugins = [];
    plugins.push(require("plugins/leosInlineSave/leosInlineSavePlugin"));
    plugins.push(require("plugins/leosInlineCancel/leosInlineCancelPlugin"));
    plugins.push(require("plugins/leosInlineEditor/leosInlineEditorPlugin"));
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
    plugins.push(require("plugins/leosHighlight/leosHighlightPlugin"));
    plugins.push(require("plugins/leosComments/leosCommentsPlugin"));
    plugins.push(require("plugins/leosCommentAction/leosCommentActionPlugin"));
    plugins.push(require("plugins/leosCrossReference/leosCrossReferencePlugin"));
    plugins.push(require("plugins/leosFloatingSpace/leosFloatingSpacePlugin"));
    plugins.push(require("plugins/leosWidget/leosWidgetPlugin"));
    plugins.push(require("plugins/leosMessageBus/leosMessageBusPlugin"));
    plugins.push(require("plugins/leosDropHandler/leosDropHandlerPlugin"));
    plugins.push(require("plugins/leosXmlEntities/leosXmlEntitiesPlugin"));
    
    var pluginNames = plugins.map(function(p){return p.name;});
    // holds ckEditor external plugins names
    var externalPluginsNames = [];
    pluginTools.addExternalPlugins(externalPluginsNames);
    var extraPlugins = pluginNames.concat(externalPluginsNames).join(",");
    var transformationConfigResolver = transformationConfigManager.getTransformationConfigResolverForPlugins(pluginNames);
    
    var leosEditorCss = pluginTools.toUrl("css/leosEditor.css");
    var dataHintCss = pluginTools.toUrl("css/data-hint.css");
    
    var customContentsCss = [leosEditorCss, dataHintCss];
    
    var profileName = "AKN Citations";

    // create profile configuration
    var profileConfig = {
        // user interface language localisation
        language: "en",
        // custom configuration to load (none if empty)
        customConfig: "",
        // comma-separated list of plugins to be loaded
        plugins: "toolbar,wysiwygarea,elementspath," +
                 "clipboard,undo,basicstyles,enterkey," +
                 "button,dialog,dialogui,sourcedialog,colorbutton",
        // comma-separated list of toolbar button names that must not be rendered
        removeButtons: "Underline,Strike,TextColor",
        // comma-separated list of additional plugins to be loaded
        extraPlugins: extraPlugins,
        // disable Advanced Content Filter (allow all content)
        allowedContent: true,
        //custom style sheet
        contentsCss: customContentsCss,
        // height of the editing area
        height : 522,
        //show toolbar on startup
        startupFocus: true,
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
        // leos text highlight configuration
        colorButton_enableMore: false,
        colorButton_colors: 'yellow/FFFF00,green/00FF00,cyan/00FFFF,pink/FF00FF,blue/0000FF,red/FF0000,grey/C0C0C0',
        colorButton_backStyle: {
            element: 'span',
            attributes: {
                'class': 'leos-highlight-#(color)'
            },
            overrides: [{
                element: 'span',
                attributes: {
                    'class': /^leos-highlight/,
                    'refersto': '~leoshighlight',
                    'data-hgl-userId' : /[\w]*/,
                    'data-hgl-username' : /[\w]*/,
                    'data-hgl-datetime' : /[\w]*/
                }
            }]
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