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
define(function leosBootstrapConfigModule() {
    "use strict";

    // configure AMD loader
    // the global requirejs needs to be used here,
    // because local require doesn't have config().
    requirejs.config({
        paths : {
            // AMD loader plugins
            domReady : "lib/requirejs-domready_2.0.1/domReady",
            promise : "lib/requirejs-promise_1.2.0/requirejs-promise",
            // JavaScript libraries
            npo : "lib/native-promise-only_0.8.1/npo",
            logger : "lib/loglevel_1.4.0/loglevel",
            lodash : "lib/lodash_2.4.1/lodash",
            stampit : "lib/stampit_1.1.0/stampit",
            jquery: "lib/jquery_2.1.4/jquery",
            ckEditor : "lib/ckeditor_4.5.3/ckeditor",
            ck_jquery: "lib/ckeditor_4.5.3/adapters/jquery",
            dateFormat : "lib/dateFormat_1.2.3/dateFormat",
            jsTree : "lib/jsTree_3.2.1/jstree",
            // Other paths
            plugins : "plugins",
            /*=> start section with external ckEditor plugins, 
             * they are not valid requirejs modules so they shouldn't be resolved as one
             * they are placed here as aliases to ease the process of updating to the new version.
             * In order to add new external ckEditor plugin put it in this section and prefix it with
             * 'ck_' value.
             */
            ck_scayt : "lib/plugins/scayt_4.5.3/plugin.js"
            /*<= end section with external ckEditor plugins */
        },
        shim : {
            ckEditor : {
                exports : "CKEDITOR",
                deps : [ "logger", "jquery" ],
                init : function promiseCkEditor(log) {
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
            ck_jquery: {
                exports: "jQuery.fn.ckeditor",
                deps: ["jquery", "promise!ckEditor"]
            }, 
            dateFormat : {
                    exports : "dateFormat" 
            }
        },
        // initial modules to load asynchronously asap
        deps : [ "npo", "logger", "jquery", "lodash" ],
        // enforce define to improve catching load failures in IE
        enforceDefine : true,
        // loading modules/resources timeout (default is 7 seconds)
        waitSeconds: 60
    });
});