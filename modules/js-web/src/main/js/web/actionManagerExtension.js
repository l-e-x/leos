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
define(function actionManagerExtensionModule(require) {
    "use strict";

    // load module dependencies
    var leosCore = require("js/leosCore");
    var log = require("logger");
    var $ = require("jquery");
    var postal = require("postal");

    // configuration
    var LOCKS_CHANNEL_CFG = leosCore.config.channels.locks;
    var EDITOR_CHANNEL_CFG = leosCore.config.channels.editor;
    var LOCK_LEVELS = leosCore.config.lockLevels;
    var ACTION_MESSAGES = leosCore.config.messages.action;

    // utilities
    var UTILS = leosCore.utils;

    // handle extension initialization
    function _initExtension(connector) {
        log.debug("Initializing Action Manager extension...");

        // restrict scope to the extended target
        var rootElement = UTILS.getParentElement(connector);

        _setupEditorChannel(connector);
        _setupLocksSubscription(connector, rootElement);
        _registerLocksHandler(connector, rootElement);
        _registerActionsHandler(connector, rootElement);

        log.debug("Registering Action Manager extension unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;
    }

    function _setupLocksSubscription(connector, rootElement) {
        // retrieve locks channel
        var locksChannel = postal.channel(LOCKS_CHANNEL_CFG.name);
        connector.locksChannel = locksChannel;

        // subscribe to locks channel
        var locksSubscription = locksChannel.subscribe("locks.updated", _locksUpdated.bind(undefined, connector, rootElement));
        connector.locksSubscription = locksSubscription;
    }

    function _locksUpdated(connector, rootElement, data) {
        // store user
        connector.user = data.user;
        // store locks, grouped by level
        var locks = data.locks || [];
        connector.locks = locks.reduce(_categorizeLocksByLevel, {});
        // apply locks to visible widgets
        _refreshVisibleLocks(connector, rootElement);
    }

    function _categorizeLocksByLevel(catalog, lock) {
        if (!(lock.lockLevel in catalog)) {
            catalog[lock.lockLevel] = [];
        }
        catalog[lock.lockLevel].push(lock);
        return catalog;
    }

    function _refreshVisibleLocks(connector, rootElement) {
        // get any visible wraps
        var $wraps = $(rootElement).find(".leos-wrap:hover");
        // apply locks to each wrap
        $wraps.each(function(i, wrap) {
            _applyLocks(connector, $(wrap));
        });
    }

    function _registerLocksHandler(connector, rootElement) {
        // register delegated event handlers for wrapper
        $(rootElement).on("mouseenter", ".leos-wrap", _handleLocks.bind(undefined, connector));
    }

    function _handleLocks(connector, event) {
        var $wrap = $(event.currentTarget);
        _applyLocks(connector, $wrap);
    }

    function _applyLocks(connector, $wrap) {
        var user = connector.user;
        var documentLocks = connector.locks[LOCK_LEVELS.document] || [];
        var elementLocks = connector.locks[LOCK_LEVELS.element] || [];
        // process locks, according to precedence level
        if (documentLocks.length > 0) {
            _applyDocumentLocks($wrap, documentLocks);
        }
        else if (elementLocks.length > 0) {
            _applyElementLocks($wrap, elementLocks, user);
        }
        else {
            _applyDefaults($wrap);
        }
    }

    function _applyDocumentLocks($wrap, locks) {
        // get first document lock
        var lock = locks[0];
        // activate lock widget
        var $lockWidget = $wrap.find("[data-widget-type='lock']");
        $lockWidget.attr("title", ACTION_MESSAGES["lock.document"] + lock.userName);
        _activate($lockWidget);
        // inactivate other widgets
        var $otherWidgets = $wrap.find("[data-widget-type]").not($lockWidget);
        _inactivate($otherWidgets);
    }

    function _applyElementLocks($wrap, locks, user) {
        var elementId = _getWrappedId($wrap);
        var userLocks = _getUserLocks(locks, user);
        if (userLocks.length > 0) {
            // inactivate all widgets
            var $allWidgets = $wrap.find("[data-widget-type]");
            _inactivate($allWidgets);
        } else {
            var elementLocks = _getElementLocks(locks, elementId);
            if (elementLocks.length > 0) {
                // get first element lock
                var lock = elementLocks[0];
                // activate lock widget
                var $lockWidget = $wrap.find("[data-widget-type='lock']");
                $lockWidget.attr("title", ACTION_MESSAGES["lock.element"] + lock.userName);
                _activate($lockWidget);
                // inactivate other widgets
                var $otherWidgets = $wrap.find("[data-widget-type]").not($lockWidget);
                _inactivate($otherWidgets);
            } else {
                // activate edit widget
                var $editWidget = $wrap.find("[data-widget-type='edit']");
                _activate($editWidget);
                // inactivate other widgets
                var $miscWidgets = $wrap.find("[data-widget-type]").not($editWidget);
                _inactivate($miscWidgets);
            }
        }
    }

    function _getUserLocks(locks, user) {
        return locks.filter(function(lock) {
            return (lock.userLogin === this.id);
        }, user);
    }

    function _getElementLocks(locks, elementId) {
        return locks.filter(function(lock) {
            return (lock.elementId === this);
        }, elementId);
    }

    function _applyDefaults($wrap) {
        // inactivate lock widgets
        var $lockWidget = $wrap.find("[data-widget-type='lock']");
        _inactivate($lockWidget);
        // activate other widgets
        var $otherWidgets = $wrap.find("[data-widget-type]").not($lockWidget);
        _activate($otherWidgets);
    }

    function _getWrappedId($wrap) {
        return $wrap.data("wrapped-id");
    }

    function _getWrappedType($wrap) {
        return $wrap.data("wrapped-type");
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

    function _activate($widget) {
        $widget.addClass("leos-active").removeClass("leos-inactive");
    }

    function _inactivate($widget) {
        $widget.removeClass("leos-active").addClass("leos-inactive");
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
        var locks = connector.locks;
        var user = connector.user;
        if (_isValidAction(action, elementId, elementType, editable, deletable, locks, user)) {
            var data = {
                action: action,
                elementId: elementId,
                elementType: elementType
            };
            var topic = "actions." + action + ".element";
            connector.editorChannel.publish(topic, data);
        }
    }

    function _isValidAction(action, elementId, elementType, editable, deletable, locks, user) {
        if (action && elementId && elementType) {
            if ((!editable && action === "edit") ||
                (!deletable && action === "delete")) {
                // edit/delete action is invalid for non-editable/non-deletable element
                return false;
            }
            var documentLocks = locks[LOCK_LEVELS.document] || [];
            if (documentLocks.length > 0) {
                // all actions are invalid if document locks are present
                return false;
            }
            var elementLocks = locks[LOCK_LEVELS.element] || [];
            if (elementLocks.length == 0) {
                // all actions are valid if no element locks are present
                return true;
            }
            if (action !== "edit") {
                // other actions are invalid if element locks are present
                return false;
            }
            var userElementLocks = _getUserLocks(elementLocks, user);
            if (userElementLocks.length > 0) {
                // edit action is invalid if element locks are present for current user
                return false;
            }
            var currentElementLocks = _getElementLocks(elementLocks, elementId);
            if (currentElementLocks.length == 0) {
                // edit action is valid if no element locks are present for current element
                return true;
            }
        }
        return false;
    }

    // handle connector unregistration from server-side
    function _connectorUnregistrationListener() {
        log.debug("Action Manager extension unregistered...");
        var connector = this;
        _teardownEditorChannel(connector);
        _teardownLocksSubscription(connector);
        // clean connector
        connector.user = null;
        connector.locks = null;
    }

    function _teardownLocksSubscription(connector) {
        // unsubscribe from locks channel
        if (connector.locksSubscription) {
            connector.locksSubscription.unsubscribe();
            connector.locksSubscription = null;
        }
        // clear locks channel
        connector.locksChannel = null;
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