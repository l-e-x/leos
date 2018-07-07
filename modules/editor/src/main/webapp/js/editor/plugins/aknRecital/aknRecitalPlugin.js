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
define(function aknRecitalPluginModule(require) {
    "use strict";

    // load module dependencies
    var LODASH = require("lodash");
    var pluginTools = require("plugins/pluginTools");

    var pluginName = "aknRecital";
    var cssPath = "css/" + pluginName + ".css";
    var DATA_AKN_NUM = "data-akn-num";
    var RECITAL_NAME_IN_HTML = "p";
    var LEFT_NUM_DELIMITER = "(";
    var RIGHT_NUM_DELIMITER = ")";

    var pluginDefinition = {
        init: function init(editor) {
            editor.addContentsCss(pluginTools.getResourceUrl(pluginName, cssPath));
            editor.on("change", function(event) {
                event.editor.fire( 'lockSnapshot');
                if (event.editor.mode != 'source') {
                    if (event.editor.checkDirty()) {
                        renumberRecital(event);
                    }
                }
                event.editor.fire( 'unlockSnapshot' );
            });
        }
    };
    
    function renumberRecital(event) {
        var recitals = event.editor.document.getElementsByTag(RECITAL_NAME_IN_HTML);
        if (recitals) {
            for (var index = 0; index < recitals.count(); index++) {
                var paraNum = index + 1;
                // it will be safer here to check if data-akn-num or data-akn-name attribute is present, at the moment though we can not guarantee  that
                // all p(recital)  elements will have it, so this check is not performed here
                recitals.getItem(index).setAttribute(DATA_AKN_NUM,LEFT_NUM_DELIMITER+paraNum+RIGHT_NUM_DELIMITER);
            }
        }
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    var RECITAL_NAME = "recital";

    var transformationConfig = {
        akn: RECITAL_NAME,
        html: "p",
        attr: [{
            akn: "id",
            html: "id"
        }, {
            akn: "leos:editable",
            html: "contenteditable"
        }, {
            html : ["data-akn-name", RECITAL_NAME].join("=")
        }],
        sub: [{
            akn: "num",
            html: "p",
            attr: [{
                akn: "id",
                html: "data-akn-num-id"
            }],
            sub: {
                akn: "text",
                html: "p[data-akn-num]"
            }
        }, {
            akn: "mp",
            html: "p",
            attr: [{
                akn: "id",
                html: "data-akn-mp-id"
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
        renumberRecital: renumberRecital
    };

    return pluginModule;
});