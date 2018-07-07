/*
 * Copyright 2015 European Commission
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
(function leosBootstrap(global) {
    "use strict";

    // ensure global namespace for LEOS to export functions and data
    global.LEOS = global.LEOS || {};

    // set overrides from global leos bootstrap configuration
    var leosOverride = global.LEOS_BOOTSTRAP_CONFIG || {};
    var leosWaitSeconds = (typeof leosOverride.waitSeconds === "number") ? leosOverride.waitSeconds : 10;
    var leosLogLevel = leosOverride.logLevel || "info";
    var leosBasePath = leosOverride.basePath;

    if (!leosBasePath) {
        // set leos base path from the path of this boot script,
        // determined from the src attribute of its <script> tag
        leosBasePath = (function getBootScriptPath() {
            // regex that matches the script filename
            var srcRegex = /(^|.*[\\\/])leosBootstrap.js(?:\?.*)?$/i;
            var scripts = document.getElementsByTagName("script");
            var match, path;

            if (scripts && scripts.length > 0) {
                for (var i in scripts) {
                    match = scripts[i].src.match(srcRegex);
                    if (match) {
                        path = match[1];
                        break;
                    }
                }
            }

            if (path && path.length > 0) {
                return path;
            } else {
                throw new Error("Unable to determine the path of the leos bootstrap script!");
            }
        }());
    }

    // set AMD loader global error callback
    require.onError = function amdLoadFailure(error) {
        if (console &&
            ((typeof console.error === "function") ||     // ensure error function is available
             (typeof console.error === "object"))) {      // workaround for IE9 typeof bug
            if (error.name) {
                // handle exceptions thrown with Error object
                console.error("AMD loading failed:", error.requireType);
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

    // set minimal AMD loader configuration
    require.config({
        // root path to use for all module lookup
        baseUrl: leosBasePath,
        // script loading timeout in seconds (0 = no timeout)
        waitSeconds: leosWaitSeconds,
        // extra query string arguments appended to URLs of resources
        // ts = current timestamp to force a browser cache refresh
        // FIXME replace timestamp with constant in production build
        urlArgs: "ts=" + (new Date()).getTime()
    });

    // load additional AMD loader configuration
    require(["leosBootstrapConfig"], function bootstrapConfigured() {
        // AMD loader has now been properly configured.

        // set the logging level for the application
        require(["logger"], function setLoggingLevel(log) {
            log.info("Setting logging level to %s...", leosLogLevel);
            log.setLevel(leosLogLevel);
        });

        // wait for DOM to be ready to complete bootstrap
        require(["logger", "domReady!"], function bootstrapCompleted(log) {
            // Bootstrap is done and DOM is ready.
            log.info("LEOS Editor bootstrap succeeded!");
        });
    });

    // export function that creates an editor instance with the specified profile
    global.LEOS.createEditor = function createLeosEditor(profileId, htmlElementId, successCallback) {
        require(["logger", "promise!ckEditor", "profiles/" + profileId], function createEditor(log, ckEditor, profile) {
            log.debug("Creating Leos Editor with profile [name=%s] and replacing HTML element [id=%s]...", profile.name, htmlElementId);

            // create CKEditor instance
            var ckInstance = ckEditor.replace(htmlElementId, profile.config);

            if (ckInstance) {
                // store LEOS profile
                ckInstance.LEOS = {
                    profile: profile
                };
                // execute success callback
                if (typeof successCallback === "function") {
                    successCallback(ckInstance);
                }
            } else {
                throw new Error("Unable to create Leos Editor instance!");
            }
        });
    };
}(this));