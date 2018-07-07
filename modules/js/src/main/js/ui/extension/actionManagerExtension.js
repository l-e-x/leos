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
define(function actionManagerExtensionModule(require) {
    "use strict";

    // load module dependencies
    var CONFIG = require("core/leosConfig");
    var UTILS = require("core/leosUtils");
    var log = require("logger");
    var $ = require("jquery");
    var postal = require("postal");

    // configuration
    var EDITOR_CHANNEL_CFG = CONFIG.channels.editor;

    // handle extension initialization
    function _initExtension(connector) {
        log.debug("Initializing Action Manager extension...");

        // restrict scope to the extended target
        var rootElement = UTILS.getParentElement(connector);

        _setupEditorChannel(connector);
        _registerActionsHandler(connector, rootElement);

        log.debug("Registering Action Manager extension unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;
    }

    function _getWrappedId($wrap) {
        return $wrap.attr("data-wrapped-id");
    }

    function _getWrappedType($wrap) {
        return $wrap.attr("data-wrapped-type");
    }

    function _getWrappedEditable($wrap) {
        return _getDataDefaultedTrue($wrap, "wrapped-editable");
    }

    function _getWrappedDeletable($wrap) {
        return _getDataDefaultedTrue($wrap, "wrapped-deletable");
    }

    function _getDataDefaultedTrue($element, key) {
        var value = $element.data(key);
        if (typeof value != "boolean") {
            // true for undefined or empty string, false otherwise
            value =
                (typeof value == "undefined") ||
                (typeof value == "string" && value.length == 0);
        }
        return value;
    }

    function _registerActionsHandler(connector, rootElement) {
        // register delegated event handlers for content
        $(rootElement).on("dblclick.actions",
            ".leos-wrap[data-wrapped-editable!=false] .leos-wrap-content",
            _handleAction.bind(undefined, connector, "edit"));
        // register delegated event handlers for widgets
        $(rootElement).on("click.actions",
            ".leos-wrap [data-widget-type='insert.before']",
            _handleAction.bind(undefined, connector, "insert.before"));
        $(rootElement).on("click.actions",
            ".leos-wrap[data-wrapped-editable!=false] [data-widget-type='edit']",
            _handleAction.bind(undefined, connector, "edit"));
        $(rootElement).on("click.actions",
            ".leos-wrap[data-wrapped-deletable!=false] [data-widget-type='delete']",
            _handleAction.bind(undefined, connector, "delete"));
        $(rootElement).on("click.actions",
            ".leos-wrap [data-widget-type='insert.after']",
            _handleAction.bind(undefined, connector, "insert.after"));
    }

    function _handleAction(connector, action, event) {
        var $wrap = $(event.currentTarget).parents(".leos-wrap");
        var elementId = _getWrappedId($wrap);
        var elementType = _getWrappedType($wrap);
        var editable = _getWrappedEditable($wrap);
        var deletable = _getWrappedDeletable($wrap);
        var user = connector.user;
        if (_isValidAction(action, elementId, elementType, editable, deletable, user)) {
            var data = {
                action: action,
                elementId: elementId,
                elementType: elementType
            };
            var topic = "actions." + action + ".element";
            connector.editorChannel.publish(topic, data);
        }
    }

    function _isValidAction(action, elementId, elementType, editable, deletable, user) {
        if (action && elementId && elementType) {
            return ((!editable && action === "edit") ||
                (!deletable && action === "delete")) ? false : true;
        }
        return false;
    }

    // handle connector unregistration from server-side
    function _connectorUnregistrationListener() {
        log.debug("Unregistering Action Manager extension...");
        var connector = this;
        _teardownEditorChannel(connector);
        // clean connector
        connector.user = null;
    }

    function _setupEditorChannel(connector) {
        // retrieve editor channel
        connector.editorChannel = postal.channel(EDITOR_CHANNEL_CFG.name);
    }

    function _teardownEditorChannel(connector) {
        // clear editor channel
        connector.editorChannel = null;
    }

    return {
        init: _initExtension
    };
});
