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
define(function userGuidanceExtensionModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var $ = require("jquery");
    var UTILS = require("core/leosUtils");

    function _init(connector) {
        log.debug("Initializing User Guidance extension...");
        // restrict scope to the extended target
        connector.target = UTILS.getParentElement(connector);

        log.debug("Registering User Guidance unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;

        log.debug("Registering User Guidance state change listener...");
        connector.onStateChange = _connectorStateChangeListener;

        connector.receiveUserGuidance = _receiveUserGuidance;
        connector.enableUserGuidance = _enableUserGuidance;
    }

    // handle connector state change on client-side
    function _connectorStateChangeListener() {
        var connector = this;
        log.debug("User Guidance extension state changed...");
        // KLUGE delay execution due to sync issues with target update
        setTimeout(_processGuidance, 500, connector);
    }

    function _receiveUserGuidance(userGuidance) {
        var connector = this;
        log.debug("User guidance array received..!");
        connector.guidanceArray = JSON.parse(userGuidance);
        _processGuidance(connector);
    }

    function _enableUserGuidance(enable) {
        var connector = this;
        log.debug("User guidance request with value:" + enable);
        connector.guidanceEnabled = enable;
        _processGuidance(connector);
    }

    function _processGuidance(connector) {
        if (connector.guidanceEnabled) {
            if (connector.guidanceArray) {
                _removeGuidance(connector);
                _injectGuidance(connector);
            } else {
                connector.requestUserGuidance();
            }
        } else {
            _removeGuidance(connector);
        }
    }

    function _injectGuidance(connector) {
        log.debug("Injecting user guidance..");
        connector.guidanceArray.forEach(function(guidance) {
            guidance.targets.forEach(function (target) {
               _injectAtTarget(target, guidance.content);
            });
        });
    }

    function _injectAtTarget(target, content) {
        var $target = _getTargetObject(target);
        if($target[0]) {
            var before = /BEFORE/i;
            if(before.test(target.position)) {
                $target.prepend(content);
            }else{
                $target.append(content);
            }
        }

        function _getTargetObject(target) {
            var wrapper = /((BEFORE)|(AFTER))-WRAPPER/i;
            if (wrapper.test(target.position)) {
                var targetObject = $("[data-wrapped-id=" + target.destinationId+ "]");
                if(targetObject.length === 0) {
                    // if the above selection is empty we try to find the element by id, e.g we remove the wrappers for some roles.
                    targetObject = $('#' + target.destinationId);
                }
                return targetObject;
            }
            else{
                return $('#' + target.destinationId);
            }
        }
    }

    function _removeGuidance(connector) {
        log.debug("Removing all user guidance..");
        $("guidance").remove();
    }

    // handle connector unregistration on client-side
    function _connectorUnregistrationListener() {
        var connector = this;
        log.debug("Unregistering User Guidance extension...");
        // clean connector
        connector.target = null;
        connector.guidanceArray = null;
        connector.guidanceEnabled = null;
    }

    return {
        init: _init
    };
});
