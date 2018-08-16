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
define(function aknRecitalPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var numberModule = require("plugins/leosNumber/recitalNumberModule");

    var pluginName = "aknRecital";

    var pluginDefinition = {
        init: function init(editor) {
            editor.on("change", function(event) {
                event.editor.fire( 'lockSnapshot');
                if (event.editor.mode != 'source') {
                    if (event.editor.checkDirty()) {
                        numberModule.numberRecitals(event);
                    }
                }
                event.editor.fire( 'unlockSnapshot' );
            });
        }
    };


    pluginTools.addPlugin(pluginName, pluginDefinition);

    var RECITAL_NAME = "recital";

    var transformationConfig = {
        akn: RECITAL_NAME,
        html: "p",
        attr: [{
            akn: "GUID",
            html: "id"
        }, {
            akn: "leos:editable",
            html: "contenteditable"
        }, {
            html: ["data-akn-name", RECITAL_NAME].join("=")
        }, {
            akn: "leos:origin",
            html: "data-origin"
        }],
        sub: [{
            akn: "num",
            html: "p",
            attr: [{
                akn: "GUID",
                html: "data-akn-num-id"
            }, {
                akn: "leos:origin",
                html: "data-num-origin"
            }],
            sub: {
                akn: "text",
                html: "p[data-akn-num]"
            }
        }, {
            akn: "mp",
            html: "p",
            attr: [{
                akn: "GUID",
                html: "data-akn-mp-id"
            }, {
                akn: "leos:origin",
                html: "data-mp-origin"
            }],
            sub: {
                akn: "text",
                html: "p/text"
            }
        }]
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    // return plugin module
    var pluginModule = {
        name: pluginName,
        transformationConfig: transformationConfig,
        renumberRecital:numberModule.numberRecitals
    };

    return pluginModule;
});