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
define(function leosCrossReferencePluginModule(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var pluginTools = require("plugins/pluginTools");
    var jsTree = require("jsTree");
    var dialogDefinition = require("./leosCrossReferenceDialog");
    var leosCrossReferenceWidget = require("./leosCrossReferenceWidget");
    
    
    var pluginName = "leosCrossReference";

    var pluginDefinition = {
        icons : 'leoscrossreference',
        requires : "dialog",
        init : function(editor) {

            // adds dialog
            pluginTools.addDialog(dialogDefinition.dialogName, dialogDefinition.initializeDialog);
            
            // adds widget
            editor.widgets.add(leosCrossReferenceWidget.name, leosCrossReferenceWidget.config);
            
            //creates dialog command
            var dialogCommand = editor.addCommand(dialogDefinition.dialogName, new CKEDITOR.dialogCommand(dialogDefinition.dialogName));

            // adds command for 'Cross-reference' button
            editor.addCommand('insertCrossReference', {
                exec : function insertCrossReference(editor) {
                    dialogCommand.exec();
                }
            });

            editor.ui.add('LeosCrossReference', CKEDITOR.UI_BUTTON, {
                label : 'Cross-reference',
                title : 'Cross-reference',
                toolbar : 'insert,' + 20,
                command : leosCrossReferenceWidget.name
            });

        }
    };

    var transformationConfig = {
        akn : "ref",
        html : 'a[data-akn-name=ref]',
        attr : [ {
            akn : "id",
            html : "id"
        }, {
            html : "data-akn-name=ref"
        }, {
            akn : "href",
            html : "href"
        } ],
        sub : {
            akn : "text",
            html : "a/text"
        }
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name : pluginName
    };
    return pluginModule;

});
