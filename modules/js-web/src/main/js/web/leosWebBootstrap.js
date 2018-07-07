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
(function leosWebBootstrap(global, require) {
    "use strict";

    // require bootstrap dependencies and proceed after they become available
    require(["js/leosCoreBootstrap", "js/editor/leosEditorBootstrap"], function dependenciesBootstrapped() {

        // set web AMD loader configuration
        require.config({
            // path aliases for module names, relative to base URL
            paths: {
                // JavaScript libraries
                emitter: "lib/emitter_1.2.0/index",
                nouislider: "lib/noUiSlider_8.3.0/js/nouislider",
                wnumb: "lib/wNumb_1.0.2/wNumb",
                sliderPins: "js/web/sliderPins/js/sliderPins"
            },
            // packages for modules loaded from a directory structure
            packages: [
                {
                    name: "bootstrap",
                    location: "lib/bootstrap_3.3.6/js",
                    main: ""
                },
                {
                    name: "sideComments",
                    location: "lib/side-comments",
                    main: "js/main"
                }
            ],
            // shim configuration for loading non-AMD scripts
            shim: {
                "bootstrap/tooltip": {
                    deps: ["jquery"],
                    exports: "jQuery.fn.tooltip"
                },
                "bootstrap/popover": {
                    deps: ["jquery", "bootstrap/tooltip"],
                    exports: "jQuery.fn.popover"
                },
                "wnumb": {
                    exports: "wNumb"
                }
            }
        });
    });

    // define explicitly named bootstrap module
    define("js/web/leosWebBootstrap", {});

}(window, requirejs));