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
(function leosCoreBootstrap(global, require) {
    "use strict";

    // set AMD loader global error callback
    require.onError = function amdLoadError(error) {
        if (console &&
            ((typeof console.error === "function") ||     // ensure error function is available
             (typeof console.error === "object"))) {      // KLUGE: workaround for IE9 typeof bug
            if (error.name) {
                // handle exceptions thrown with Error object
                console.error("AMD loading error:", error.requireType);
                console.error("Required modules:", error.requireModules);
                console.error("Error message:", error.message);
                // error stack (non-standard property)
                console.error("Stack trace:", (error.stack || "-"));
            } else {
                // handle exceptions thrown with any other object
                console.error("AMD loading failed:", error);
            }
        }
        // difficult to recover from this error,
        // rethrow the error to halt AMD loading
        throw error;
    };

    // set core AMD loader configuration
    require.config({
        // path aliases for module names, relative to base URL
        paths: {
            // AMD loader plugins
            domReady: "lib/requirejs-domready_2.0.1/domReady",
            promise: "lib/requirejs-promise_1.2.0/requirejs-promise",
            text: "lib/requirejs-text_2.0.14/text",
            // JavaScript polyfills
            npo: "lib/native-promise-only_0.8.1/npo",
            // JavaScript libraries
            logger: "lib/loglevel_1.4.0/loglevel",
            jquery: "lib/jquery_2.1.4/jquery",
            stampit: "lib/stampit_1.2.0/stampit",
            postal: "lib/postal.js_2.0.0/postal",
            "postal.diagnostics": "lib/postal.diagnostics/postal.diagnostics",
            mathjax: "webjars/MathJax/2.5.3/MathJax.js?config=default",
            dateFormat: "lib/dateFormat_1.2.3/dateFormat"
        },
        // packages for modules loaded from a directory structure
        packages: [
            {
                name: "lodash",
                location: "lib/lodash_4.15.0-amd",
                main: "main"
            },
            {
                name: "waypoints",
                location: "lib/waypoints_4.0.0",
                main: "jquery.waypoints"
            }
        ],
        // map given module ID into substitute module ID
        map: {
            "*": {
                "waypoint.inview": "waypoints/shortcuts/inview"
            }
        },
        // shim configuration for loading non-AMD scripts
        shim: {
            mathjax: {
                exports: "MathJax"
            },
            "waypoints/jquery.waypoints": {
                deps: ["jquery"],
                exports: "Waypoint"
            },
            "waypoints/shortcuts/inview": {
                deps: ["waypoints"],
                exports: "Waypoint.Inview"
            },
            dateFormat: {
                exports: "dateFormat"
            }
        },
        // initial modules to load asynchronously asap
        deps: ["npo"]
    });

    // define explicitly named bootstrap module
    define("js/leosCoreBootstrap", ["module", "logger"], function leosCoreBootstrapModule(module, logger) {
        // set the logging level for the LEOS application
        var logLevel = module.config().logLevel || "info";
        logger.info("Setting LEOS logging level to %s...", logLevel);
        logger.setLevel(logLevel);
        return {};
    });

}(window, requirejs));