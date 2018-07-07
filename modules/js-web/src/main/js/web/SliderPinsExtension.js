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
define(function SliderPinsExtensionModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var sliderPins = require("sliderPins");

    function _initPins(connector) {
        log.debug("Initializing Slider Pins extension...");

        _updateParentPosition(connector.getState().targetId);
        sliderPins.create(connector.getState().targetId, connector.getState().configMap);

        log.debug("Registering Slider Pin unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;

        log.debug("Registering Slider Pin state change listener...");
        connector.onStateChange = _connectorStateChangeListener;
        
        connector.navigateSliderPins = _navigate;
    }


    function _updateParentPosition(targetId){
        var target = document.getElementById(targetId);
        target.parentNode.style.position="relative";
    }

    // handle connector unregistration on client-side
    function _connectorStateChangeListener() {
        var connector = this;
        log.debug("Connector state Changes for Pins!");
        sliderPins.destroy(connector.getState().targetId);
        sliderPins.create(connector.getState().targetId, connector.getState().configMap);
    }

    // handle connector unregistration on client-side
    function _connectorUnregistrationListener() {
        var connector = this;
        log.debug("Connector unregistered... destroying Slider Pin!");
        sliderPins.destroy(connector.getState().targetId);
    }

    function _navigate(direction) {
        var connector = this;
        sliderPins.navigate(connector.getState().targetId, direction);
    }
    
    return {
        init: _initPins
    };
});
