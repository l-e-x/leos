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
define(function rangeSliderComponentModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var noUiSlider = require("nouislider");
    var wNumb = require("wnumb");
    var _ = require("lodash");

    // handle slider initialization
    function _init(connector) {
        log.debug("Registering Range Slider component unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;

        log.debug("Registering Range Slider component state change listener...");
        connector.onStateChange = _connectorStateChangeListener;
    }

    function _initSlider( element, steps, handles, colouredArea) {
        var connector = this;
        log.debug("Creating slider...", steps, colouredArea);
        var slider = noUiSlider.create(element, {
            orientation: "horizontal",
            connect: _getConnect(colouredArea),
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
                density: 100,
                format: wNumb({
                    edit: _stepEdit
                })
            }
        });

        function _stepUndo(value) {
            // pre-decode step value (string -> string)
            // convert step value to step index
            return steps.indexOf(value).toString();
        }

        function _stepEdit(formattedValue, originalValue) {
            // post-encode step value ([string, number] -> string)
            // convert step index to step value
            // round original value to ensure integer index
            return steps[_.round(originalValue)];
        }

        log.debug("Registering slider change listener...");
        slider.on("change", function onChange(handleValues) {
            _sliderChangeListener(handleValues, connector);
        });
    }

    function _stepRange(steps) {
        // step index range (0-based)
        return {
            "min": (steps ? 0 : -1),
            "max": (steps ? (steps.length -1) : -1)
        };
    }

    function _getConnect(colouredArea) {
        switch (colouredArea) {
            case 'LOWER':
                return [true, false];
            case 'UPPER':
                return [false, true];
            case 'ENCLOSED':
                return true;
            default:
                return true;
        }
    }

    // handle connector unregistration on client-side
    function _connectorUnregistrationListener() {
        var connector = this;
        log.debug("Unregistering Range Slider component...");
        var element = connector.getElement();
        if (element.noUiSlider) {
            element.noUiSlider.destroy();
        }
    }

    // handle connector state change from server-side
    function _connectorStateChangeListener() {
        var connector = this;
        log.debug("Range Slider component state changed called...");
        var state = connector.getState();
        var element = connector.getElement();
        var steps = state.stepValues;
        var handleValues = state.handleValues;

        if (element.noUiSlider) {
            element.noUiSlider.updateOptions({
                range: _stepRange(steps),
                format: wNumb({
                    decimals: 0,
                    edit: function(value) {
                        return steps[_.round(value)];
                    },
                    undo: function(value) {
                        return steps.indexOf(value).toString();
                    }
                }),
            });
            element.noUiSlider.pips({
                mode: "steps",
                density: 100,
                format: wNumb({
                    edit: function(value) {
                        return steps[_.round(value)];
                    }
                })
            })
            element.noUiSlider.set(handleValues);
        }
        else{
            _initSlider.apply(connector,[ element, steps, handleValues, state.colouredArea]);
        }
    }
    
    // handle slider change from client-side
    function _sliderChangeListener(handleValues, connector) {
        log.debug("Slider changed...", handleValues);
        connector.onSliderChange(handleValues);
    }

    return {
        init: _init
    };
});
