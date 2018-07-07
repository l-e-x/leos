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
define(function leosDocTitleSuggestionPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");

    var pluginName = "leosDocTitleSuggestion";
    var cssPath = "css/" + pluginName + ".css";

    var pluginDefinition = {
        init: function init(editor) {
            editor.addContentsCss(pluginTools.getResourceUrl(pluginName, cssPath));
        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);


    var transformationConfig = {
        akn: "popup",
        html: 'span[data-akn-name=popupdocheading]',
        attr: [{
            akn: "id",
            html: "id"
        }, {
            akn: "refersto",
            html: "refersto"
        }, {
            html: "data-akn-name=popupdocheading"
        }, {
            akn: "leos:userid",
            html: "leos:userid"
        }, {
            akn: "leos:username",
            html: "leos:username"
        }, {
            akn: "leos:datetime",
            html: "leos:datetime"
        }, {
            akn: "leos:dg",
            html: "leos:dg"
        }],
        sub: {
            akn: "mp",
            html: "span/span[class=indicator]",
            attr: [{
                akn: "id",
                html: "data-akn-mp-id"
            }, {
                html: "class=indicator"
            }],
            sub: [{
                akn: "doctitle",
                html: "span/span/span[class=doctitle]",
                attr: [{
                    akn: "id",
                    html: "data-akn-doctitle-id"
                }, {
                    html: "class=doctitle"
                }],
                sub: {
                    akn: "docpurpose",
                    html: "span/span/span/span[class=docpurpose]",
                    attr: [{
                        akn: "id",
                        html: "data-akn-docpurpose-id"
                    }, {
                        html: "class=docpurpose"
                    }],
                    sub: {
                        akn: "text",
                        html: "span/span/span/span/text"
                    }
                }
            }]
        }
    };

    // return plugin module
    var pluginModule = {
        name: pluginName
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    return pluginModule;
});