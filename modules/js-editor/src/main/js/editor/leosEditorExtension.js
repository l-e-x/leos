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
define(function leosEditorExtensionModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var postal = require("postal");
    var DiagnosticsWireTap = require("postal.diagnostics");
    var leosCore = require("js/leosCore");
    var actionHandler = require("./core/actionHandler");
    var scrollAdapter = require("./core/scrollAdapter");
    var elementEditor = require("./core/elementEditor");
    var bookmarkHandler = require("./core/bookmarkHandler");

    // configuration
    var EDITOR_CHANNEL_CFG = leosCore.config.channels.editor;

    // handle extension initialization
    function _initExtension(connector) {
        log.debug("Initializing Leos Editor extension...");
        _setupEditorChannel(connector);
        actionHandler.setup(connector);
        scrollAdapter.setup(connector);
        elementEditor.setup(connector);
        bookmarkHandler.setup(connector);

        log.debug("Registering Leos Editor extension unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;
    }

    function _setupEditorChannel(connector) {
        // create channel for editor
        connector.editorChannel = postal.channel(EDITOR_CHANNEL_CFG.name);
        // create wire tap for diagnostics
        if (EDITOR_CHANNEL_CFG.diagnostics) {
            connector.editorWireTap = new DiagnosticsWireTap({
                name: EDITOR_CHANNEL_CFG.name + "Logger",
                filters: [
                    {channel: EDITOR_CHANNEL_CFG.name}
                ],
                writer: function logMessage(msg) {
                    log.debug(EDITOR_CHANNEL_CFG.name, "channel message:\n", msg);
                }
            });
        }
    }

    // handle connector unregistration from server-side
    function _connectorUnregistrationListener() {
        log.debug("Leos Editor extension unregistered...");
        var connector = this;
        bookmarkHandler.teardown(connector);
        elementEditor.teardown(connector);
        scrollAdapter.teardown(connector);
        actionHandler.teardown(connector);
        _teardownEditorChannel(connector);
    }

    function _teardownEditorChannel(connector) {
        // remove editor wire tap
        if (connector.editorWireTap) {
            connector.editorWireTap.removeWireTap();
            connector.editorWireTap = null;
        }
        // clear editor channel
        connector.editorChannel = null;
    }

    return {
        init: _initExtension
    };
});