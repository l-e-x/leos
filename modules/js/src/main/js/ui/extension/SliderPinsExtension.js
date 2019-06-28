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
define(function SliderPinsExtensionModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var sliderPins = require("sliderPins");
    var UTILS = require("core/leosUtils");

    function _initPins(connector) {
        log.debug("Initializing Slider Pins extension...");

        // restrict scope to the extended target
        connector.target = UTILS.getParentElement(connector);

        _updateParentPosition(connector.target);
        sliderPins.create(connector.target, connector.getState().configMap);

        log.debug("Registering Slider Pins unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;

        log.debug("Registering Slider Pins state change listener...");
        connector.onStateChange = _connectorStateChangeListener;
        
        connector.navigateSliderPins = _navigate;
    }


    function _updateParentPosition(target){
        target.parentNode.style.position="relative";
    }

    // handle connector unregistration on client-side
    function _connectorStateChangeListener() {
        var connector = this;
        log.debug("Slider Pins extension state changed...");
        sliderPins.destroy(connector.target);
        // KLUGE delay execution due to sync issues with target update
        setTimeout(sliderPins.create, 500, connector.target, connector.getState().configMap);
    }

    // handle connector unregistration on client-side
    function _connectorUnregistrationListener() {
        var connector = this;
        log.debug("Unregistering Slider Pins extension...");
        sliderPins.destroy(connector.target);
        connector.target = null;
    }

    function _navigate(direction) {
        var connector = this;
        sliderPins.navigate(connector.target, direction);
    }
    
    return {
        init: _initPins
    };
});
