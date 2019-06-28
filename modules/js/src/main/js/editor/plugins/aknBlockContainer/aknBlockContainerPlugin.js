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
define(function aknBlockContainerPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var pluginName = "aknBlockContainer";

    var pluginDefinition = {
        init: function init(editor) {
            editor.on("toHtml", removeInitialSnapshot, null, null, 100);
        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    /*
     * Removes the initial snapshot which don't have 'blockcontainer'('div') as top level element
     */
    function removeInitialSnapshot(event) {
        if (event.editor.undoManager.snapshots.length > 0) {
            if (event.editor.undoManager.snapshots[0].contents.indexOf("div") < 0) {
                event.editor.undoManager.snapshots.shift();
            }
        }
    }


    var transformationConfig = {
        akn: "blockContainer",
        html: "div[data-akn-name=blockContainer]",
        attr: [{
            akn: "xml:id",
            html: "id"
        }, {
            akn : "leos:origin",
            html : "data-origin"
        }, {
            akn: "leos:editable",
            html: "leos:editable"
        }, {
            akn: "leos:deletable",
            html: "leos:deletable"
        }, {
            html: ["data-akn-name", "blockContainer"].join("=")
        }]
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    // return plugin module
    var pluginModule = {
        name: pluginName,
        transformationConfig: transformationConfig
    };

    return pluginModule;
});