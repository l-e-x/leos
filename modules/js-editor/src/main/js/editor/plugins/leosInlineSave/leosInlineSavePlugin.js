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
define(function leosInlineSavePluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");

    var pluginName = "leosInlineSave";
    var TRISTATE_DISABLED = CKEDITOR.TRISTATE_DISABLED, TRISTATE_OFF = CKEDITOR.TRISTATE_OFF;

    var pluginDefinition = {
        icons: pluginName.toLowerCase(),

        init: function(editor) {

            editor.ui.addButton('leosInlineSave', {
                label: 'Save',
                command: 'inlinesave',
                toolbar: 'save'
            });

            var saveCommand = editor.addCommand('inlinesave', {
                exec: function(editor) {
                    if (this.uiItems[0].getState() != TRISTATE_DISABLED) {
                        editor.fire("save", {
                            data: editor.getData()
                        });
                    }
                }
            });

            editor.on('change', function(event) {
                if (event.editor.checkDirty() && saveCommand.uiItems[0]) {
                    saveCommand.uiItems[0].setState(TRISTATE_OFF);
                }
            });

            // dataReady is fired when the after setData is called.
            editor.on('btnStateChange', function(event) {
                editor.toolbar.some(function(group) {
                    if (group.name === "save") {
                        group.items.some(function(item) {
                            if (item.label === "Save") {
                                // Set the save button state on the ckeditor toolbar to toggle Active/Inactive state.
                                item.setState(event.editor.checkDirty() ? CKEDITOR.TRISTATE_OFF : CKEDITOR.TRISTATE_DISABLED);
                                return true;
                            }
                        });
                        return true;
                    }
                });
            });
        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name: pluginName
    };

    return pluginModule;
});