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
define(function actionHandlerModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");

    function _setupActionHandler(connector) {
        log.debug("Setting up action handler...");
        if (connector.editorChannel) {
            var channel = connector.editorChannel;
            var subscriptions = connector.actionSubscriptions || [];
            // subscribe to editor channel action topics
            var insertAction = channel.subscribe("actions.insert.*.element", _insertElementAction.bind(undefined, connector));
            subscriptions.push(insertAction);
            var editAction = channel.subscribe("actions.edit.element", _editElementAction.bind(undefined, connector));
            subscriptions.push(editAction);
            var deleteAction = channel.subscribe("actions.delete.element", _deleteElementAction.bind(undefined, connector));
            subscriptions.push(deleteAction);
            connector.actionSubscriptions = subscriptions;
        }
    }

    function _insertElementAction(connector, data) {
        log.debug("Insert element action...");
        var regExp = /(?:^insert)\.(\w+).*$/i;
        var matches = regExp.exec(data.action);
        if (matches) {
            data.position = matches[1];
            connector.insertElementAction(data);
        }
    }

    function _editElementAction(connector, data) {
        log.debug("Edit element action...");
        connector.editElementAction(data);
    }

    function _deleteElementAction(connector, data) {
        log.debug("Delete element action...");
        connector.deleteElementAction(data);
    }

    function _teardownActionHandler(connector) {
        log.debug("Tearing down action handler...");
        // unsubscribe from editor channel action topics
        if (connector.actionSubscriptions) {
            connector.actionSubscriptions.forEach(function(subscription) {
                subscription.unsubscribe();
            });
            connector.actionSubscriptions = null;
        }
    }

    return {
        setup: _setupActionHandler,
        teardown: _teardownActionHandler
    };
});