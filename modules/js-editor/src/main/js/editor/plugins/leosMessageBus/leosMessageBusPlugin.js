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
define(function leosMessageBusPluginModule(require) {
    "use strict";

    // load module dependencies
    var postal = require("postal");
    var leosCore = require("js/leosCore");
    var pluginTools = require("plugins/pluginTools");
    var pluginName = "leosMessageBus";

    var EDITOR_CHANNEL = leosCore.config.channels.editor.name;

    var pluginDefinition = {

        init: function init(editor) {
            var editorChannel = postal.channel(EDITOR_CHANNEL);

            var contentChangeBuffer = CKEDITOR.tools.eventsBuffer(500, function _publishContentChange() {
                editorChannel.publish("contentChange", {});
            }, editor);

            editor.on("contentChange", contentChangeBuffer.input);
            editor.on("instanceReady", contentChangeBuffer.input);
            editor.on("destroy", contentChangeBuffer.input);
        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name: pluginName
    };

    return pluginModule;
});