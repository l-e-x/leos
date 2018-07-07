/*
 * Copyright 2017 European Commission
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
define(function leosInlineSavePluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");

    var pluginName = "leosInlineSave";
    var TRISTATE_DISABLED = CKEDITOR.TRISTATE_DISABLED, TRISTATE_OFF = CKEDITOR.TRISTATE_OFF;
    var SAVE_CMD_NAME = "inlinesave";
    var SAVE_CLOSE_CMD_NAME = "inlinesaveclose";
    var iconSaveClose =  'icons/leosinlinesaveclose.png';
    var iconSave = 'icons/leosinlinesave.png';

    var pluginDefinition = {
        icons: pluginName.toLowerCase(),

        init: function(editor) {

            editor.ui.addButton('leosInlineSave', {
                label: 'Save',
                command: SAVE_CMD_NAME,
                toolbar: 'save',
                icon: this.path + iconSave,
            });

            editor.ui.addButton('leosInlineSaveClose', {
                label: 'Save Close',
                command: SAVE_CLOSE_CMD_NAME,
                toolbar: 'save',
                icon: this.path + iconSaveClose,
            });

            var saveCommand = editor.addCommand(SAVE_CMD_NAME, {
                exec: function(editor) {
                    if (this.state != TRISTATE_DISABLED) {
                        editor.fire("save", {
                            data: editor.getData()
                        });
                    }
                }
            });

            var saveCloseCommand = editor.addCommand(SAVE_CLOSE_CMD_NAME, {
                exec: function(editor) {
                    if (this.state != TRISTATE_DISABLED) {
                        editor.fire("save", {
                                data: editor.getData()
                        });
                        editor.once("receiveData",_doClose);
                    }
                }
            });

            function _doClose(event) {
                editor.fire("close");
            }

            editor.on('change', function(event) {
                if (event.editor.checkDirty()) {
                    saveCommand.setState(TRISTATE_OFF);
                    saveCloseCommand.setState(TRISTATE_OFF);
                }
            });

            editor.on('focus', function(event) {
                saveCommand.setState(editor.checkDirty() ? TRISTATE_OFF : TRISTATE_DISABLED);
                saveCloseCommand.setState(editor.checkDirty() ? TRISTATE_OFF : TRISTATE_DISABLED);
            }, null, null, 100); //listen to the event as late as possible
            
            // dataReady is fired after setData is called.
            editor.on('dataReady', function(event) {
                saveCommand.setState(editor.checkDirty() ? TRISTATE_OFF : TRISTATE_DISABLED);
                saveCloseCommand.setState(editor.checkDirty() ? TRISTATE_OFF : TRISTATE_DISABLED);
            }, null, null, 100); //listen to the event as late as possible
        }
    };
    
    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name: pluginName
    };

    return pluginModule;
});