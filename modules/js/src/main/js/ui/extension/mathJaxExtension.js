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
define(function mathJaxExtensionModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var MathJax = require("mathjax");
    var UTILS = require("core/leosUtils");

    function _initMathJax(connector) {
        log.debug("Initializing MathJax extension...");

        // restrict scope to the extended target
        connector.target = UTILS.getParentElement(connector);

        // configure MathJax HTML-CSS output processor
        // to prevent the use of image fonts
        MathJax.Hub.Config({
            "HTML-CSS": {
                imageFont: null
            }
        });

        log.debug("Registering MathJax extension unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;

        log.debug("Registering MathJax extension state change listener...");
        connector.onStateChange = _connectorStateChangeListener;
    }

    // handle connector unregistration on client-side
    function _connectorUnregistrationListener() {
        var connector = this;
        log.debug("Unregistering MathJax extension...");
        // clean connector
        connector.target = null;
    }

    // handle connector state change on client-side
    function _connectorStateChangeListener() {
        var connector = this;
        log.debug("MathJax extension state changed...");
        // KLUGE delay execution due to sync issues with target update
        setTimeout(_forceMathJaxRendering, 500, connector.target);
    }

    function _forceMathJaxRendering(scope) {
        log.debug("Forcing MathJax rendering...");
        // enqueue an async typesetting request for MathJax with restricted scope
        MathJax.Hub.Queue(["Typeset", MathJax.Hub, scope]);
    }

    return {
        init: _initMathJax
    };
});
