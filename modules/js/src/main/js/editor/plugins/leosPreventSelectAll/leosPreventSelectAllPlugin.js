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
define(function leosPreventSelectAllPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");

    var pluginName = "leosPreventSelectAll";
    var pluginDefinition = {
        init: function init(editor) {
            editor.on('contentDom', function(event) {
                var editor = event.editor,
                    editable = editor.editable();
                editable.attachListener(editable, 'keydown', function(event) {
                    if (event.data.getKeystroke() == CKEDITOR.CTRL + 65) {
                        event.data.preventDefault();
                        event.cancel();
                    }
                }, null, null, -9999);
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