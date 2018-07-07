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
define(function lockManagerExtensionModule(require) {
    "use strict";

    // load module dependencies
    var leosCore = require("js/leosCore");
    var log = require("logger");
    var postal = require("postal");
    var DiagnosticsWireTap = require("postal.diagnostics");

    // configuration
    var LOCKS_CHANNEL_CFG = leosCore.config.channels.locks;

    // handle extension initialization
    function _initExtension(connector) {
        log.debug("Initializing Lock Manager extension...");
        _setupLocksChannel(connector);

        log.debug("Registering Lock Manager extension state change listener...");
        connector.onStateChange = _connectorStateChangeListener;

        log.debug("Registering Lock Manager extension unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;
    }

    function _setupLocksChannel(connector) {
        // create channel for locks
        var channel = postal.channel(LOCKS_CHANNEL_CFG.name);
        connector.locksChannel = channel;

        // create wire tap for diagnostics
        if (LOCKS_CHANNEL_CFG.diagnostics) {
            var wireTap = new DiagnosticsWireTap({
                name: LOCKS_CHANNEL_CFG.name + "Logger",
                filters: [
                    {channel: LOCKS_CHANNEL_CFG.name}
                ],
                writer: function logMessage(msg) {
                    log.debug(LOCKS_CHANNEL_CFG.name, "channel message:\n", msg);
                }
            });
            connector.locksWireTap = wireTap;
        }
    }

    // handle connector state change from server-side
    function _connectorStateChangeListener() {
        log.debug("Lock Manager extension state changed...");
        var connector = this;
        var state = connector.getState();
        // prepare data
        var data = {
            user: state.user,
            locks: state.locks || []
        };
        // publish data
        connector.locksChannel.publish("locks.updated", data);
    }

    // handle connector unregistration from server-side
    function _connectorUnregistrationListener() {
        log.debug("Lock Manager extension unregistered...");
        var connector = this;
        _teardownLocksChannel(connector);
    }

    function _teardownLocksChannel(connector) {
        // remove locks wire tap
        if (connector.locksWireTap) {
            connector.locksWireTap.removeWireTap();
            connector.locksWireTap = null;
        }
        // clear locks channel
        connector.locksChannel = null;
    }

    return {
        init: _initExtension
    };
});