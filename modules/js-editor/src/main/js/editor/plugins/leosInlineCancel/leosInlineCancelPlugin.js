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
define(function leosInlineCancelPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var dialogDefinition = require("./leosInlineCancelDialog");

    var pluginName = "leosInlineCancel";
    
    var pluginDefinition = {
        icons: pluginName.toLowerCase(),
        
        init : function(editor) {
            
            // adds dialog
            pluginTools.addDialog(dialogDefinition.dialogName, dialogDefinition.initializeDialog);
            
            //creates dialog command
            var dialogCommand = editor.addCommand(dialogDefinition.dialogName, new CKEDITOR.dialogCommand(dialogDefinition.dialogName));
            
            
            editor.addCommand('inlinecancel', {
                exec : function(editor) {
                    if(editor.checkDirty()) {
                        dialogCommand.exec();
                    } else {
                        editor.fire("close");
                    }
                }
            });

            editor.ui.addButton('leosInlineCancel', {
                label : 'Cancel',
                command : 'inlinecancel',
                toolbar: 'save'
            });
        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name : pluginName
    };

    return pluginModule;
});