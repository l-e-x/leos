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
(function bootstrapLeosEditor(global) {

    // set global leos bootstrap configuration overrides
    var leosOverride = global.LEOS_BOOTSTRAP_CONFIG || {};
    leosOverride.logLevel = "debug";    //TODO logging level should be configurable according to the build target environment

    var scriptTag = document.createElement("script");
    scriptTag.setAttribute("type", "text/javascript");
    scriptTag.setAttribute("src", "VAADIN/js/editor/lib/requirejs_2.1.16/require.js");
    scriptTag.setAttribute("data-main", "VAADIN/js/editor/leosBootstrap.js");
    var head = document.head || document.getElementsByTagName("head")[0];
    head.appendChild(scriptTag);
}(this));