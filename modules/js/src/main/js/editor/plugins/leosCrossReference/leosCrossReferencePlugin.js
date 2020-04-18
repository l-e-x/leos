/*
 * Copyright 2019 European Commission
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
define(function leosCrossReferencePluginModule(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var pluginTools = require("plugins/pluginTools");
    var jsTree = require("jsTree");
    var dialogDefinition = require("./leosCrossReferenceDialog");
    var leosCrossReferenceWidget = require("./leosCrossReferenceWidget");
    var leosCommandStateHandler = require("plugins/leosCommandStateHandler/leosCommandStateHandler");
    
    var pluginName = "leosCrossReference";

    var pluginDefinition = {
        icons : 'leoscrossreference',
        lang: 'en',
        requires : "dialog",
        init : function(editor) {

            // adds dialog
            pluginTools.addDialog(dialogDefinition.dialogName, dialogDefinition.initializeDialog);
            
            // adds widget
            editor.widgets.add(leosCrossReferenceWidget.name, leosCrossReferenceWidget.config);
            
            editor.ui.add('LeosCrossReference', CKEDITOR.UI_BUTTON, {
                label : 'Internal-reference',
                title : 'Internal reference',
                toolbar : 'ref,20',
                command : leosCrossReferenceWidget.name
            });
            
            editor.on('selectionChange', _onSelectionChange);

        }
    };
    
    function _onSelectionChange(event) {
        leosCommandStateHandler.changeCommandState(event, leosCrossReferenceWidget.name);
    }

    var transformationConfig = {
        akn :  "mref",
        html : "mref",
        attr : [{
            akn : "xml:id",
            html : "id" 
        }, {
            akn : "leos:origin",
            html : "data-origin"
        },{
            akn : "leos:broken",
            html : "leos:broken" 
        }, {
            html : "data-akn-name=mref"
        }],
        sub : [{
            akn : "text",
            html : "mref/text"
        },{     
           akn : "ref",
           html : "mref/ref",
           attr : [{
               akn : "xml:id",
               html : "id"
           }, {
               akn : "leos:origin",
               html : "data-origin"
           }, {
               html : "data-akn-name=ref"
           }, {
               akn : "href",
               html : "href"
           }, {
               akn : "documentref",
               html : "documentref"
           }],
           sub : {
               akn : "text",
               html : "mref/ref/text"
           }
        }]
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name : pluginName
    };
    return pluginModule;

});
