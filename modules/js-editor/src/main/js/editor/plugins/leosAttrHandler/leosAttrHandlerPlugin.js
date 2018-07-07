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
define(function leosAttrHandlerPluginModule(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var LOG = require("logger");
    var pluginTools = require("plugins/pluginTools");

    var pluginName = "leosAttrHandler";

    var pluginDefinition = {

        init: function init(editor) {
            editor.on('afterCommandExec', function(e) {
                if (e.data.name === 'enter') {
                    var element = e.editor.getSelection().getStartElement();
                    if (element) {
                        //Check to avoid nested inline elements
                        while (!element.isBlockBoundary()) {
                            element = element.getParent();
                        }
                        if (element && element.$.attributes) {
                            // Regular expression to match attributes similar to - "data-akn-num-id"
                            var regex = new RegExp("data-akn-\\w+-id");
                            var attributes = [];
                            
                            for (var idx = 0; idx < element.$.attributes.length; idx++) {
                                if (element.$.attributes[idx].name.match(regex)) {
                                    attributes.push(element.$.attributes[idx].name);
                                }
                            }
                            element.removeAttributes(attributes);
                        }
                        editor.fire("change"); // for re-numbering
                    }
                }
            });
        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name: pluginName,
    };

    return pluginModule;
});