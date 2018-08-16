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
define(function aknCitationsProfileModule(require) {
    "use strict";
    
    // require profile dependencies, if needed
    // e.g. ckEditor, plugins or utilities
    var transformationConfigManager = require("transformer/transformationConfigManager");
    var pluginTools = require("plugins/pluginTools");
    var $ = require('jquery');
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
    plugins.push(require("plugins/aknRecitals/aknRecitalsPlugin"));
    plugins.push(require("plugins/aknRecital/aknRecitalPlugin"));
    plugins.push(require("plugins/aknHtmlSuperScript/aknHtmlSuperScriptPlugin"));
    plugins.push(require("plugins/aknHtmlSubScript/aknHtmlSubScriptPlugin"));
    plugins.push(require("plugins/leosAttrHandler/leosAttrHandlerPlugin"));
    plugins.push(require("plugins/leosPaste/leosPastePlugin"));
    plugins.push(require("plugins/leosCrossReference/leosCrossReferencePlugin"));
    plugins.push(require("plugins/leosFloatingSpace/leosFloatingSpacePlugin"));
    plugins.push(require("plugins/leosWidget/leosWidgetPlugin"));
    plugins.push(require("plugins/leosMessageBus/leosMessageBusPlugin"));
    plugins.push(require("plugins/leosDropHandler/leosDropHandlerPlugin"));
    plugins.push(require("plugins/leosXmlEntities/leosXmlEntitiesPlugin"));
    plugins.push(require("plugins/leosTextCaseChanger/leosTextCaseChangerPlugin"));
    plugins.push(require("plugins/leosSpecialChar/leosSpecialCharPlugin"));
    plugins.push(require("plugins/leosManualRenumbering/leosManualRenumberingPlugin"));
    plugins.push(require("plugins/leosPreventElementDeletion/leosPreventElementDeletionPlugin"));
    
    var pluginNames=[];
    var specificConfig={};
    $.each(plugins, function( index, value ) {
        pluginNames.push(value.name);
        specificConfig= $.extend( specificConfig,  value.specificConfig);
    });
    // holds ckEditor external plugins names
    var externalPluginsNames = [];
    pluginTools.addExternalPlugins(externalPluginsNames);
    var extraPlugins = pluginNames.concat(externalPluginsNames).join(",");
    var transformationConfigResolver = transformationConfigManager.getTransformationConfigResolverForPlugins(pluginNames);
    var leosPasteFilter = pluginTools.createFilterList(transformationConfigResolver);
    var leosEditorCss = pluginTools.toUrl("css/leosEditor.css");
    
    var customContentsCss = [leosEditorCss];
    
    var profileName = "AKN Recitals";

    // create profile configuration
    var profileConfig = {
        // user interface language localisation
        language: "en",
        // custom configuration to load (none if empty)
        customConfig: "",
        // comma-separated list of plugins to be loaded
        plugins: "toolbar,wysiwygarea,elementspath," +
                 "clipboard,undo,pastefromword,basicstyles,enterkey," +
                 "specialchar,button,dialog,dialogui,contextmenu,menubutton,widget",
        // comma-separated list of toolbar button names that must not be rendered
        removeButtons: "Underline,Strike,TextColor,PasteFromWord",
        // comma-separated list of additional plugins to be loaded
        extraPlugins: extraPlugins,
        // disable Advanced Content Filter (allow all content)
        allowedContent: true,
        //only allow elements configured in transformer
        pasteFilter:leosPasteFilter,
        defaultPasteElement:'recital/mp/text',
        //custom style sheet
        contentsCss: customContentsCss,
        // height of the editing area
        height : 522,
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
            name : "document",
            groups : [ "document", "doctools" ]
        }, {
            name : "clipboard",
            groups : [ "clipboard", "undo" ]
        }, {
            name : "editing",
            groups : ["selection" ]
        }, {
            name : "forms"
        }, {
            name : "basicstyles",
            groups : [ "basicstyles", "cleanup" ]
        }, {
            name : "paragraph"
        }, {
            name : "ref"
        }, {
            name : "insert"
        }, {
            name : "styles"
        }, {
            name : "tools"
        }, {
            name : "others"
        },{
            name : "mode"
        }, {
            name : "about"
        } ]
    };
    // adding the specific configs coming from the plugins.
    profileConfig = $.extend( profileConfig,  specificConfig);
    // create profile definition
    var profileDefinition = {
        name: profileName,
        config: profileConfig,
        transformationConfigResolver:transformationConfigResolver
    };

    return profileDefinition;
});
