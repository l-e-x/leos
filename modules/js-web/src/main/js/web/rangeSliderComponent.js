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
define(function rangeSliderComponentModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var noUiSlider = require("nouislider");
    var wNumb = require("wnumb");

    // handle slider initialization
    function _initSlider(connector) {
        var element = connector.getElement();
        var state = connector.getState();
        var steps = state.stepValues;
        var handles = state.handleValues;

        log.debug("Creating slider...", steps, handles);
        var slider = noUiSlider.create(element, {
            orientation: "horizontal",
            connect: true,
            range: _stepRange(steps),
            step: 1,
            start: handles,
            format: wNumb({
                decimals: 0,
                edit: _stepEdit,
                undo: _stepUndo
            }),
            pips: {
                mode: "steps",
                density: 3,
                format: wNumb({
                    edit: _stepEdit
                })
            }
        });

        log.debug("Registering connector unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;

        log.debug("Registering connector state change listener...");
        connector.onStateChange = _connectorStateChangeListener;

        log.debug("Registering slider change listener...");
        slider.on("change", function onChange(handleValues) {
            _sliderChangeListener(handleValues, connector);
        });

        function _stepRange(steps) {
            // step index range (0-based)
            return {
                "min": (steps ? 0 : -1),
                "max": (steps ? (steps.length -1) : -1)
            };
        }

        function _stepUndo(value) {
            // pre-decode step value (string -> string)
            // convert step value to step index
            return steps.indexOf(value).toString();
        }

        function _stepEdit(formattedValue, originalValue) {
            // post-encode step value ([string, number] -> string)
            // convert step index to step value
            return steps[originalValue];
        }
    }

    // handle connector unregistration on client-side
    function _connectorUnregistrationListener() {
        var connector = this;
        var element = connector.getElement();
        log.debug("Connector unregistered... destroying slider!");
        element.noUiSlider.destroy();
    }

    // handle connector state change from server-side
    function _connectorStateChangeListener() {
        var connector = this;
        var state = connector.getState();
        var handleValues = state.handleValues;
        var element = connector.getElement();
        log.debug("Connector state changed...", handleValues);
        // FIXME check if values actually changed to avoid infinite loops???
        element.noUiSlider.set(handleValues);
    }

    // handle slider change from client-side
    function _sliderChangeListener(handleValues, connector) {
        log.debug("Slider changed...", handleValues);
        connector.onSliderChange(handleValues);
        // FIXME update shared state to keep synchronization with server-side???
    }

    log.debug("Range Slider Component module is ready!");

    return {
        init: _initSlider
    };
});