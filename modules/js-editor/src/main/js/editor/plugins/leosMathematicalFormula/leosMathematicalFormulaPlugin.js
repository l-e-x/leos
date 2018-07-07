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
define(function leosMathematicalFormulaPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");

    var pluginName = "leosMathematicalFormula";


    var pluginDefinition = {
        requires: "mathjax",
        init : function init(editor) {

        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);
    var transformationConfig = {
        akn : "inline[name=math-tex]",
        html : "span[class=math-tex]",
        attr : [{
            akn : "id",
            html : "id"
        }, {
            akn: "name=math-tex",
            html : "class=math-tex"
        }],
        sub : {
            akn : "text",
            html : "span/text"
        }

    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    // return plugin module
    var pluginModule = {
        name : pluginName,
        transformationConfig : transformationConfig
    };

    return pluginModule;
});