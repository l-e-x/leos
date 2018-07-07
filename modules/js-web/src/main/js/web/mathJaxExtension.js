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
define(function mathJaxExtensionModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var MathJax = require("mathjax");

    function _initMathJax(connector) {
        log.debug("Initializing MathJax extension...");

        // configure MathJax HTML-CSS output processor
        // to prevent the use of image fonts
        MathJax.Hub.Config({
            "HTML-CSS": {
                imageFont: null
            }
        });

        log.debug("Registering MathJax extension state change listener...");
        connector.onStateChange = _forceMathJaxRendering;
    }

    function _forceMathJaxRendering() {
        log.debug("Forcing MathJax rendering...");
        // enqueue an async typesetting request for MathJax
        MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
    }

    return {
        init: _initMathJax
    };
});