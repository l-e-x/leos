/*
 * Copyright 2018 European Commission
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

    // configuration
    const EDITABLE_ELEMENTS = "citations, recitals, article, clause, blockcontainer, division";

    // handle extension initialization
    function _initExtension(connector) {
        log.debug("Initializing Action Manager extension...");

        // restrict scope to the extended target
        let $rootElement = $(UTILS.getParentElement(connector));
        connector.actionsEnabled = true;

        _setupEditorChannel(connector, $rootElement);
        _registerEditorChannelSubscriptions(connector, $rootElement);
        _registerActionTriggers(connector, $rootElement);
        _registerActionsHandler(connector, $rootElement);

        log.debug("Registering Action Manager extension unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;
    }

    function _getElementId($element) {
        return $element.attr("id");
    }

    const htmlToXmlTagMap = {blockcontainer: "blockContainer"};

    function _getType($element) {
        let tagName = $element.prop('tagName').toLowerCase();
        return htmlToXmlTagMap[tagName] ? htmlToXmlTagMap[tagName] : tagName;
    }

    function _getEditable($element) {
        return _getDataDefaultedTrue($element, "leos:editable");
    }

    function _getDeletable($element) {
        return _getDataDefaultedTrue($element, "leos:deletable");
    }

    function _getDataDefaultedTrue($element, key) {
        var value = $element.data(key);
        if (typeof value !== "boolean") {
            // true for undefined or empty string, false otherwise
            value =
                (typeof value === "undefined") ||
                (typeof value === "string" && value.length === 0);
        }
        return value;
    }

    function _registerActionsHandler(connector, $rootElement) {
        // register delegated event handlers for content
        // LEOS-2764 Listening on 'mouseup' events for double clicks in place of 'dblclick' to avoid that double click will be managed by annotate
        $rootElement.on("mouseup.actions", ".leos-editable-content",
            _handleDoubleClickAction.bind(undefined, connector, "edit"));
        // register delegated event handlers for widgets
        $rootElement.on("click.actions", "[data-widget-type='insert.before']",
            _handleAction.bind(undefined, connector, "insert.before"));
        $rootElement.on("click.actions", "[data-widget-type='edit']",
            _handleAction.bind(undefined, connector, "edit"));
        $rootElement.on("click.actions", "[data-widget-type='delete']",
            _handleAction.bind(undefined, connector, "delete"));
        $rootElement.on("click.actions", "[data-widget-type='insert.after']",
            _handleAction.bind(undefined, connector, "insert.after"));
    }

    function _registerActionTriggers(connector, $rootElement) {
        $rootElement.on("mouseenter.actions", EDITABLE_ELEMENTS, _attachActions.bind(undefined, connector));
        $rootElement.on("mouseleave.actions", EDITABLE_ELEMENTS, _detachActions.bind(undefined, connector));

        $rootElement.on("mouseenter.actions", ".leos-actions", _showActions.bind(undefined, connector));
        $rootElement.on("mouseleave.actions", ".leos-actions", _hideActions.bind(undefined, connector));
    }

    function _registerEditorChannelSubscriptions(connector, $rootElement) {
        if (connector.editorChannel) {
            connector.editorChannel.subscribe("editor.open", _disableAllActions.bind(undefined, connector));
            connector.editorChannel.subscribe("editor.close", _enableAllActions.bind(undefined, connector));
        }
    }


    function _attachActions(connector, event) {
        event.stopPropagation();

        if (!connector.actionsEnabled) {
            return;
        }

        let element = event.currentTarget;
        let actions = _getActionButtons(connector, element);
        actions.target = element;
        element.actions = actions;

        _showActionButtons(actions, element);
    }

    function _detachActions(connector, event) {
        event.stopPropagation();
        let element = event.currentTarget;
        _hideActionButtons(element.actions, element);
    }

    function _disableAllActions(connector, data) {
        connector.actionsEnabled = false;
        let element = document.getElementById(data.elementId);
        _hideActionButtons(element.actions, element);
    }

    function _enableAllActions(connector, data) {
        connector.actionsEnabled = true;
    }

    function _showActions(connector, event) {
        event.stopPropagation();
        if (!connector.actionsEnabled) {
            return;
        }

        let actions = event.currentTarget;
        _showActionButtons(actions, actions.target)
    }

    function _hideActions(connector, event) {
        event.stopPropagation();

        let actions = event.currentTarget;
        _hideActionButtons(actions, actions.target)
    }

    function _showActionButtons(actions, element) {
        let $element = $(element);
        let $actions = $(actions);
        $actions.css({
            top: $element.position().top,
            height: element.clientHeight,
        });

        $element.addClass("leos-editable-content");
        $actions.children().css({display: "inline-block"})
    }

    function _hideActionButtons(actions, element) {
        $(actions).children().css({display: "none"});
        $(element).removeClass("leos-editable-content");
    }

    function _handleDoubleClickAction(connector, action, event) {
        if (event.detail > 1) /*Means 'double mouseup' == double click */ {
            _handleAction(connector, "edit", event)
        }
    }

    function _handleAction(connector, action, event) {
        // LEOS-2764 Set an attribute on these events to avoid them to be managed other components like annotate
        event.originalEvent.hostEventType = 'ckEvent';
        let $element = $(event.currentTarget);
        let originIsButton = $element.parents('.leos-actions')[0];
        if (originIsButton) {
            $element = $(originIsButton.target);
        }
        var elementId = _getElementId($element);
        var elementType = _getType($element);
        var editable = _getEditable($element);
        var deletable = _getDeletable($element);
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

    function _getActionButtons(connector, element) {
        let actions = element.actions;
        if (!actions) {
            let $element = $(element);
            let actionString = _generateActions($element, _getEditable($element), _getDeletable($element));
            actions = ($.parseHTML(actionString))[0];
            $element.after(actions);
        }
        return actions;
    }

    function _generateActions($element, editable, deletable) {
        let template = ['<div class="leos-actions Vaadin-Icons">']; //FIXME: we can directly create elements
        let type = _getType($element);
        switch (type) {
            case 'article':
            case 'division': {
                template.push(`<span data-widget-type="insert.before" title="Insert ${type} before">&#xe622</span>`);
                if (editable) {
                    template.push(`<span data-widget-type="edit" title="Edit text">&#xe7fa</span>`);
                }
                if (deletable) {
                    template.push(`<span data-widget-type="delete" title="Delete ${type}">&#xe80b</span>`);
                }
                template.push(`<span style="transform: rotate(180deg);" data-widget-type="insert.after" title="Insert ${type} after">&#xe623</span>`);
            }
                break;
            case 'clause': {
                if (editable && $element.attr('leos\:optionlist')) {
                    template.push('<span data-widget-type="edit" title="Edit text">&#xe7fa</span>');
                }
            }
                break;
            case 'blockContainer':
            case 'citations':
            case 'recitals': {
                if (editable) {
                    template.push('<span data-widget-type="edit" title="Edit text">&#xe7fa</span>');
                }
            }
                break;
        }
        template.push('</div>');
        return template.join('');
    }

    return {
        init: _initExtension
    };
});
