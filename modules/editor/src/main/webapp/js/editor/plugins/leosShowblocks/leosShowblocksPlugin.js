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
define(function leosShowblocksPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var cssPath = "css/leosShowblocks.css";
    var pluginName = "leosShowblocks";

    // TODO implement translations
    var supportedElements = [{
        key: 'div'
    }, {
        key: 'div p'
    }, {
        key: 'ul'
    }, {
        key: 'ol'
    }, {
        key: 'ul li'
    }, {
        key: 'ol li'
    }];
    var pluginDefinition = {
        icons: pluginName.toLowerCase(), // %REMOVE_LINE_CORE%

        init: function init(editor) {
            editor.addContentsCss(pluginTools.getResourceUrl(pluginName, cssPath));

            if (editor.blockless) {
                return;
            }

            var command = editor.addCommand('leosShowBlocks', commandDefinition);
            command.canUndo = false;

            if (editor.config.startupOutlineBlocks) {
                command.setState(CKEDITOR.TRISTATE_ON);
            }

            editor.ui.addButton && editor.ui.addButton('LeosShowBlocks', {
                label: 'Show blocks',
                command: 'leosShowBlocks',
                toolbar: 'tools'
            });

            // Refresh the command on setData.
            editor.on('mode', function() {
                if (command.state != CKEDITOR.TRISTATE_DISABLED) {
                    command.refresh(editor);
                }
            });

            function onFocusBlur() {
                command.refresh(editor);
            }
            // Refresh the command on focus/blur in inline.
            if (editor.elementMode == CKEDITOR.ELEMENT_MODE_INLINE) {
                editor.on('focus', onFocusBlur);
                editor.on('blur', onFocusBlur);
            }

            // Refresh the command on setData.
            editor.on('contentDom', function(event) {
                if (command.state != CKEDITOR.TRISTATE_DISABLED) {
                    command.refresh(editor);
                }
            });
        }
    };

    var commandDefinition = {
        readOnly: 1,
        preserveState: true,
        editorFocus: false,

        exec: function executeCommandDefinition(editor) {
            this.toggleState();
            this.refresh(editor);
        },

        refresh: function refreshCommandDefinition(editor) {
            if (editor.document) {
                // Show blocks turns inactive after editor loses focus when in inline.
                var showBlocks = this.state == CKEDITOR.TRISTATE_ON && (editor.elementMode != CKEDITOR.ELEMENT_MODE_INLINE || editor.focusManager.hasFocus);

                var funcName = showBlocks ? 'attachClass' : 'removeClass';
                editor.editable()[funcName]('cke_show_blocks');
            }
        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name: pluginName
    };

    return pluginModule;
});