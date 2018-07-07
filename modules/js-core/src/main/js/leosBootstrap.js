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
(function leosBootstrap(global) {
    "use strict";

    // ensure LEOS global namespace to export application functions and data
    global.LEOS = global.LEOS || {};

    // default settings
    var defaults = {
        // standard MIME type for JavaScript
        scriptType: "application/javascript",
        // base URL to use for all modules/resources lookup
        baseUrl: _getBaseUrl(),
        // loading modules/resources timeout in seconds (0 = no timeout)
        waitSeconds: 60,
        // enforce define to improve catching load failures in IE
        enforceDefine: true,
        // set additional configuration to be passed to specified modules
        config: {
            "js/leosCoreBootstrap": {
                logLevel: "debug"
            }
        }
    };

    // configuration settings
    var configs = global.LEOS_BOOTSTRAP_CONFIG;

    // combined settings
    var settings = _extend({}, defaults, configs);

    // expose settings through LEOS configuration
    // this might be useful later on (e.g. debug)
    global.LEOS.config = settings;

    // set RequireJS global configuration variable, that
    // will be applied automatically when RequireJS loads
    global.require = global.LEOS.config;

    /**
     * Gets LEOS base URL from the path of this boot script,
     * determined from the src attribute of its <script> tag,
     * which must have its ID value set to "leosBootstrap".
     *
     * @return LEOS base URL
     */
    function _getBaseUrl() {
        // we cannot use document.currentScript because it's not supported by IE
        // regex that matches this script file: js/leosApplicationBootstrap.js
        var srcRegex = /(^|.*[\\\/])js\/leosBootstrap.js(?:\?.*)?$/i;
        var script = document.getElementById("leosBootstrap");
        var match, baseUrl;

        if (script && script.src) {
            match = script.src.match(srcRegex);
            baseUrl = match ? match[1] : undefined;
        }

        if (baseUrl && baseUrl.length > 0) {
            return baseUrl;
        } else {
            throw new Error("Unable to determine LEOS base URL!");
        }
    }

    /**
     * Extend a JavaScript target object with all the properties of source objects.
     * Source objects are processed in-order, so the last source argument will
     * override properties with the same name from previous source arguments.
     *
     * Note: only a shallow copy of properties is performed.
     *
     * @param target    target object that will be extended.
     * @param *sources  source objects with given properties.
     *
     * @return the extended target object
     */
    function _extend(target) {
        if (target === undefined || target === null) {
            throw new TypeError('Cannot convert undefined or null to object!');
        }

        var destination = Object(target);

        for (var index = 1; index < arguments.length; index++) {
            var source = arguments[index];
            if (source !== undefined && source !== null) {
                Object.keys(source).forEach(function copyValue(key) {
                    destination[key] = source[key];
                });
            }
        }

        return destination;
    }

}(window));