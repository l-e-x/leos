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
define(function aknCitationMandatePluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var leosKeyHandler = require("plugins/leosKeyHandler/leosKeyHandler");

    var pluginName = "aknCitationMandate";
    var ENTER_KEY = 13;

    var pluginDefinition = {
        init: function init(editor) {
            leosKeyHandler.on({
                editor : editor,
                eventType : 'key',
                key : ENTER_KEY,
                action : _onEnterKey
            });          
        }
    };

    function _onEnterKey(context) {
        context.event.cancel();
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    var CITATION_NAME = "citation";

    var transformationConfig = {
        akn: CITATION_NAME,
        html: "p",
        attr: [ {
            akn: "xml:id",
            html: "id"
        }, {
            akn : "leos:origin",
            html : "data-origin"
        }, {
            akn: "refersTo",
            html: "data-refersto"
        }, {
            akn: "leos:editable",
            html : "data-akn-attr-editable"
        }, {
            akn : "leos:softaction",
            html : "data-akn-attr-softaction"
        }, {
            akn : "leos:softactionroot",
            html : "data-akn-attr-softactionroot"
        }, {
            akn : "leos:softuser",
            html : "data-akn-attr-softuser"
        }, {
            akn : "leos:softdate",
            html : "data-akn-attr-softdate"
        }, {
            akn : "leos:softmove_to",
            html : "data-akn-attr-softmove_to"
        }, {
            akn : "leos:softmove_from",
            html : "data-akn-attr-softmove_from"
        }, {
            akn : "leos:softmove_label",
            html : "data-akn-attr-softmove_label"
        }, {
            html : ["data-akn-name", CITATION_NAME].join("=")
        }],
        sub: {
            akn: "mp",
            html: "p",
            attr : [ {
                akn : "xml:id",
                html : "data-akn-mp-id"
            }, {
                akn : "leos:origin",
                html : "data-mp-origin"
            } ],
            sub: {
                akn: "text",
                html: "p/text"
            }
        }
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    // return plugin module
    var pluginModule = {
        name: pluginName,
        transformationConfig: transformationConfig
    };

    return pluginModule;
});