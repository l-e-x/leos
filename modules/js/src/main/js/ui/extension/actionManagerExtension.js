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

    var EDITABLE_ELEMENTS = "citation, recital, blockcontainer, division, clause";
    var EDITABLE_ELEMENTS_EC = EDITABLE_ELEMENTS + ", article";
    var EDITABLE_ELEMENTS_CN = EDITABLE_ELEMENTS + ", article[leos\\:origin='cn'], " +
    		"article[leos\\:origin='ec'] paragraph:not(:has(subparagraph, point)), " +
    		"article[leos\\:origin='ec'] subparagraph, " +
    		"article[leos\\:origin='ec'] point:not(:has(point, alinea)), " +
    		"article[leos\\:origin='ec'] alinea";

    var actionsListOpened = false;
    var zIndex = 1;

    // handle extension initialization
    function _initExtension(connector) {
        log.debug("Initializing Action Manager extension...");

        // restrict scope to the extended target
        let $rootElement = $(UTILS.getParentElement(connector));
        connector.actionsEnabled = true;
        connector.instanceType = connector.getState().instanceType;

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
    
    function _getOriginAttribute($element) {
        return $element.attr("leos:origin");
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
        if ($element.attr(key)) {
            return ($element.attr(key) === 'true');
        } else {
            return true;
        }
    }

    function _registerActionsHandler(connector, $rootElement) {
        // register delegated event handlers for content
        // LEOS-2764 Listening on 'mouseup' events for double clicks in place of
        //  'dblclick' to avoid that double click will be managed by annotate
        $rootElement.on("mouseup.actions", ".leos-editable-content", _handleDoubleClickAction.bind(undefined, connector, "edit"));
        // register delegated event handlers for widgets
        $rootElement.on("click.actions", "[data-widget-type='insert.before']", _handleAction.bind(undefined, connector, "insert.before"));
        $rootElement.on("click.actions", "[data-widget-type='edit']", _handleAction.bind(undefined, connector, "edit"));
        $rootElement.on("click.actions", "[data-widget-type='delete']", _handleAction.bind(undefined, connector, "delete"));
        $rootElement.on("click.actions", "[data-widget-type='insert.after']", _handleAction.bind(undefined, connector, "insert.after"));
    }

    function _registerActionTriggers(connector, $rootElement) {
        $rootElement.on("mouseenter.actions", _isCouncilInstance(connector) ? EDITABLE_ELEMENTS_CN : EDITABLE_ELEMENTS_EC, _attachActions.bind(undefined, connector));
        $rootElement.on("mouseleave.actions", _isCouncilInstance(connector) ? EDITABLE_ELEMENTS_CN : EDITABLE_ELEMENTS_EC, _detachActions.bind(undefined, connector));

        $rootElement.on("mouseenter.actions", ".leos-actions", _showActionsSingleIcon.bind(undefined, connector));
        $rootElement.on("mouseleave.actions", ".leos-actions", _hideActions.bind(undefined, connector));

        $rootElement.on("click.actions", ".leos-actions-icon", _showHideActionsList.bind(undefined, connector));
        $rootElement.on("mouseleave.actions", ".leos-actions-icon", _resetActionsOpened.bind(undefined, connector));

        $rootElement.on("mouseenter.actions", ".leos-actions-icon", _showActionsListWithDelay.bind(undefined, connector));
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

        _showActionButtons(actions, element, false);
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

    function _resetActionsOpened(connector, event) {
        actionsListOpened = false;
    }

    function _showActionsListWithDelay(connector, event) {
        var area = event.currentTarget;
        var delay = setTimeout(function () {
            _showActions(connector, event, true);
        }, 2000);
        area.onmouseout = function () {
            clearTimeout(delay);
        };
    }

    function _showActionsSingleIcon(connector, event) {
        event.stopPropagation();
        if (!connector.actionsEnabled) {
            return;
        }

        let actions = event.currentTarget;
        _showActionButtons(actions, actions.target, false)
    }

    function _showActions(connector, event, showActionsList) {
        event.stopPropagation();
        if (!connector.actionsEnabled) {
            return;
        }

        let actions = event.currentTarget;
        _showActionButtons(actions, actions.target, showActionsList)
    }

    function _hideActions(connector, event) {
        event.stopPropagation();

        let actions = event.currentTarget;
        _hideActionButtons(actions, actions.target)
    }

    function _showActionButtons(actions, element, showActionsList) {
        let $actions = $(actions);
        if ($actions.children().length) {
            let $element = $(element);
            $element.addClass("leos-editable-content");

            var elementHeigh = element.clientHeight;
            var remainingSpace = _getRemainingSpace($element, elementHeigh, showActionsList);
            var top = (remainingSpace / 3.33);

            $actions.css({
                top: $element.position().top - top,
                left: $element.position().left + element.offsetWidth - 5,
                height: elementHeigh + remainingSpace,
                zIndex: zIndex++ //last inserted has the precedence
            });
        }

        if(showActionsList) {
            if (actionsListOpened) {
                $actions.children().css({display: "none"})
                $($actions.children()[0]).css({display: "inline-block"})
            } else {
                $actions.children().css({display: "inline-block"})
                $($actions.children()[0]).css({display: "none"})
            }
            actionsListOpened = !actionsListOpened;
        } else {
            $(actions.children[0]).css({display: "inline-block"})
        }
    }

    function _getRemainingSpace($element, elementHeigh, showActionsList) {
        let heightForSingleIcon = 31;
        var totChildren = $element.next().children().length - 1;
        var actionButtonsHeigh = totChildren * heightForSingleIcon;

        var remainingSpace = 0;
        if (!actionsListOpened && showActionsList && (actionButtonsHeigh > elementHeigh)) {
            remainingSpace = actionButtonsHeigh - elementHeigh;
        }
        return remainingSpace;
    }

    function _showHideActionsList(connector, event) {
        event.stopPropagation();
        if (!connector.actionsEnabled) {
            return;
        }

        let icon = event.currentTarget;
        let actions = $(icon)[0].parentElement;
        let element = actions.previousSibling;
        _showActionButtons(actions, element, true)
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

    function _isCouncilInstance(connector) {
        return UTILS.COUNCIL_INSTANCE === connector.instanceType;
    }

    function _handleAction(connector, action, event) {
        // LEOS-2764 Set an attribute on these events to avoid them to be managed other components like annotate
        event.originalEvent.hostEventType = 'ckEvent';
        let $element = $(event.currentTarget);
        //disable buttons to not allow duplicate actions
        _disableActions($element);
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

    function _disableActions($element){
        $element.css("pointer-events", "none" );
        $element.parents('.leos-actions').children().css( "pointer-events", "none" );
    }

    function _enableActions(elementId) {
        let element = document.querySelector(`#${elementId}`);
        $(element).css( "pointer-events", "all" );
        $(element).next().children().css( "pointer-events", "all" );
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
        connector.enableActions = _enableActions;
    }

    function _teardownEditorChannel(connector) {
        // clear editor channel
        connector.editorChannel = null;
    }

    function _getActionButtons(connector, element) {
        let actions = element.actions;
        if (!actions) {
            let $element = $(element);
            let actionString = _generateActions($element, _getEditable($element), _getDeletable($element), connector);
            actions = ($.parseHTML(actionString))[0];
            $element.after(actions);
        }
        return actions;
    }
    
    function _insertBeforeAndAfterIcon($element, connector) {
        var insertBeforeAndAfter;
        let type = _getType($element);
        switch (type) {
            case 'citation':
            case 'recital':
            case 'article':
            case 'division': {
                insertBeforeAndAfter = !_isCouncilInstance(connector);//Preventing for council instance
                break;
            }
            default: insertBeforeAndAfter = false; //for all the rest false
        }
        return insertBeforeAndAfter;
    }
    
    function _isDeletable($element, deletable, connector) {
        let type = _getType($element);
        switch (type) {
            case 'clause': deletable = false; break;
            default: deletable = deletable && !_isCouncilInstance(connector);
        }
        return deletable;
    }

    function _generateActions($element, editable, deletable, connector) {
        let type = _getType($element);
        var insertBeforeAndAfter = _insertBeforeAndAfterIcon($element, connector);
        editable = editable || (editable && $element.attr('leos\:optionlist'));
        deletable = _isDeletable($element, deletable, connector);
        
        let template = ['<div class="leos-actions Vaadin-Icons">']; //FIXME: we can directly create elements
        
        if (insertBeforeAndAfter) {
            template.push(`<span data-widget-type="insert.before" title="Insert ${type} before">&#xe622</span>`);
        }
        if (editable) {
            template.push(`<span data-widget-type="edit" title="Edit text">&#xe7fa</span>`);
        }
        if (deletable) {
            template.push(`<span data-widget-type="delete" title="Delete ${type}">&#xe80b</span>`);
        }
        if(insertBeforeAndAfter) {
            template.push(`<span style="transform: rotate(180deg);" data-widget-type="insert.after" title="Insert ${type} after">&#xe623</span>`);
        }
        
        //add three dots only if any action is present
        if(template.length > 2){
            template.splice(1, 0, `<span class="leos-actions-icon" data-widget-type="show.all.actions" title="Show all actions">&#xe774</span>`);
        }
        
        template.push('</div>');
        return template.join('');
    }

    return {
        init: _initExtension
    };
});
