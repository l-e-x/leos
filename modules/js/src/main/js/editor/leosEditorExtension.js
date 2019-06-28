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
define(function leosEditorExtensionModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var postal = require("postal");
    var DiagnosticsWireTap = require("postal.diagnostics");
    var CONFIG = require("core/leosConfig");
    var actionHandler = require("./core/actionHandler");
    var toolbarPositionAdapter = require("./core/toolbarPositionAdapter");
    var elementEditor = require("./core/elementEditor");
    var bookmarkHandler = require("./core/bookmarkHandler");
    var $ = require("jquery");

    // configuration
    var EDITOR_CHANNEL_CFG = CONFIG.channels.editor;

    // handle extension initialization
    function _initExtension(connector) {
        log.debug("Initializing Leos Editor extension...");
        _setupEditorChannel(connector);
        actionHandler.setup(connector);
        toolbarPositionAdapter.setup(connector);
        elementEditor.setup(connector);
        bookmarkHandler.setup(connector);
        _registerUnLoadHandlers(connector);

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
        var connector = this;
        log.debug("Unregistering Leos Editor extension...");
        bookmarkHandler.teardown(connector);
        elementEditor.teardown(connector);
        toolbarPositionAdapter.teardown(connector);
        actionHandler.teardown(connector);
        _teardownEditorChannel(connector);
        _unregisterUnLoadHandlers;
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

    function _registerUnLoadHandlers(connector) {
        log.debug("Registering beforeunload handler...");
        $(window).on("beforeunload", function() {
            if ($("akomantoso .cke_editable").is("[data-wrapped-id]") ||
                    $("div.popupContent .leos-toc-tree").length) {
                return "Changes you made may not be saved."; // Browser shows it's own message and not this
            }
        });
        log.debug("Registering unload handler...");
        $(window).on("unload", _closeBrowser.bind(undefined, connector));
    }

    function _unregisterUnLoadHandlers() {
        log.debug("Unregistering unload handlers...");
        $(window).off("beforeunload");
        $(window).off("unload");
    }

    function _closeBrowser(connector) {
        // After the call to "closeBrowser" a delay it is needed to be added to allow
        // the call be launched. If this delay is not added the browser is closed without calling.
        connector.closeBrowser();
        _sleepFor(1000);
    }

    function _sleepFor(sleepDuration) {
        // Previously implemented with JS setTimeout, promises, etc. but it does not work when it is
        // called from browser "unload" event. Seems that JS native functions are aborted.
        var now = new Date().getTime();
        while (new Date().getTime() < now + sleepDuration) { /* do nothing */ }
    }

    return {
        init: _initExtension
    };
});
