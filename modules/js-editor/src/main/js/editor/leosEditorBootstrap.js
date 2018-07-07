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
(function leosEditorBootstrap(global, require) {
    "use strict";

    // require bootstrap dependencies and proceed after they become available
    require(["js/leosCoreBootstrap"], function dependenciesBootstrapped() {

        // set editor AMD loader configuration
        require.config({
            // path aliases for module names, relative to base URL
            paths: {
                // JavaScript libraries
                ckEditor: "lib/ckeditor_4.5.10/ckeditor",
                "ckEditor.jquery": "lib/ckeditor_4.5.10/adapters/jquery",
                jsTree: "lib/jsTree_3.2.1/jstree",
                cuid: "lib/cuid_1.3.8/build/client-cuid",
                // LEOS modules
                //"editor.profiles": "js/editor/profiles",
                //"editor.plugins": "js/editor/plugins",
                /*=> start section with external ckEditor plugins,      // FIXME re-evaluate the need to keep this!!!
                 * they are not valid requirejs modules so they shouldn't be resolved as one
                 * they are placed here as aliases to ease the process of updating to the new version.
                 * In order to add new external ckEditor plugin put it in this section and prefix it with
                 * 'ck_' value.
                 */
                ck_scayt: "lib/ck_scayt_4.5.10/plugin.js"
                /*<= end section with external ckEditor plugins */
            },
            // map given module ID into substitute module ID
            map: {
                "*": {
                    core: "js/editor/core",                 // TODO remove mapping after changing the profiles and plugins
                    plugins: "js/editor/plugins",           // TODO remove mapping after changing the profiles and plugins
                    profiles: "js/editor/profiles",         // TODO remove mapping after changing the profiles and plugins
                    transformer: "js/editor/transformer"    // TODO remove mapping after changing the profiles and plugins
                }
            },
            // shim configuration for loading non-AMD scripts
            shim: {
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
            }
        });
    });

    // define explicitly named bootstrap module
    define("js/editor/leosEditorBootstrap", {});

}(window, requirejs));