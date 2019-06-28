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
(function leosModulesBootstrap(global, require) {
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

    // set AMD loader configuration
    require.config({
        // path aliases for module names, relative to base URL
        paths: {
            // AMD loader plugins
            domReady: "lib/requirejs-domready_2.0.1/domReady",
            promise: "lib/requirejs-promise_1.2.0/requirejs-promise",
            text: "lib/requirejs-text_2.0.15/text",
            // JavaScript polyfills
            npo: "lib/native-promise-only_0.8.1/npo",
            // JavaScript libraries
            logger: "lib/loglevel_1.4.0/loglevel",
            jquery: "lib/jquery_3.2.1/jquery",
            stampit: "lib/stampit_1.2.0/stampit",
            postal: "lib/postal.js_2.0.0/postal",
            "postal.diagnostics": "lib/postal.diagnostics/postal.diagnostics",
            mathjax: "webjars/MathJax/2.7.0/MathJax.js?config=default",
            dateFormat: "lib/dateFormat_1.2.3/dateFormat",
            nouislider: "lib/noUiSlider_10.0.0/js/nouislider",
            wnumb: "lib/wNumb_1.1.0/wNumb",
            jsTree: "lib/jsTree_3.3.2/jstree",
            cuid: "lib/cuid_1.3.8/client-cuid",
            ckEditor: "lib/ckeditor_4.9.2/ckeditor",
            "ckEditor.jquery": "lib/ckeditor_4.9.2/adapters/jquery",
            // CKEditor External Plugins
            /*=> start section with external ckEditor plugins,      // FIXME re-evaluate the need to keep this!!!
             * they are not valid requirejs modules so they shouldn't be resolved as one
             * they are placed here as aliases to ease the process of updating to the new version.
             * In order to add new external ckEditor plugin put it in this section and prefix it with
             * 'ck_' value.
             */
            /*<= end section with external ckEditor plugins */
            // LEOS JavaScript libraries
            sliderPins: "js/lib/sliderPins/js/sliderPins",
            contentScroller:"js/lib/contentScroller",
            refToLink:"lib/Ref2Link_1.0.03/jquery-parsetext.min",
            // LEOS Core, Editor, Views, Components & Extensions
            core: "js/core",
            editor: "js/editor",
            view: "js/ui/view",
            component: "js/ui/component",
            extension: "js/ui/extension"
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
                "waypoint.inview": "waypoints/shortcuts/inview",
                plugins: "js/editor/plugins",           // TODO remove mapping after changing the profiles and plugins
                profiles: "js/editor/profiles",         // TODO remove mapping after changing the profiles and plugins
                transformer: "js/editor/transformer"    // TODO remove mapping after changing the profiles and plugins
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
            },
            "bootstrap/tooltip": {
                deps: ["jquery"],
                exports: "jQuery.fn.tooltip"
            },
            "bootstrap/popover": {
                deps: ["jquery", "bootstrap/tooltip"],
                exports: "jQuery.fn.popover"
            },
            wnumb: {
                exports: "wNumb"
            },
            ckEditor: {
                deps: ["logger"],
                exports: "CKEDITOR",
                init: function initCkEditor(log) {
                    var ckEditor = this.CKEDITOR;
                    function ckPromise(resolve, reject) {
                        function verifyCkEditorStatus() {
                            if (ckEditor && ckEditor.status && (ckEditor.status === "loaded")) {
                                log.debug("CKEditor ready! [status=%s]", ckEditor.status);
                                resolve(ckEditor);
                            } else {
                                log.debug("CKEditor not ready... [status=%s]", ckEditor.status);
                                setTimeout(verifyCkEditorStatus, 50);
                            }
                        }
                        // asynchronous immediate call
                        setTimeout(verifyCkEditorStatus, 0);
                    }
                    return new Promise(ckPromise);
                }
            },
            "ckEditor.jquery": {
                deps: ["jquery", "promise!ckEditor"],
                exports: "jQuery.fn.ckeditor"
            }
        },
        // initial modules to load asynchronously asap
        deps: ["npo", "logger", "jquery"]
    });

    // define explicitly named bootstrap module
    define("js/leosModulesBootstrap", ["module", "logger"], function moduleInitializer(module, logger) {
        // set the logging level for the LEOS application
        var logLevel = module.config().logLevel || "info";
        logger.info("Setting LEOS logging level to %s...", logLevel);
        logger.setLevel(logLevel);
        return {};
    });

}(window, requirejs));
