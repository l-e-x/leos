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
define(function leosAttrHandlerPluginModule(require) {
    "use strict";

    // load module dependencies
    var identityHandler = require("plugins/leosAttrHandler/leosIdentityHandlerModule");
    var pluginTools = require("plugins/pluginTools");

    var pluginName = "leosAttrHandler";

    var pluginDefinition = {

        init: function init(editor) {
            var oldSelectedElementDetails;
            editor.on('beforeCommandExec', function(e) {
                if (e.data.name === 'enter') {
                    oldSelectedElementDetails = identityHandler.getElementDetails(e.editor.getSelection().getStartElement());
                }
            });
            
            editor.on('afterCommandExec', function(e) {
                if (e.data.name === 'enter') {
                    var element = e.editor.getSelection().getStartElement();
                    if (element) {
                        var newSelectedElementDetails = identityHandler.getElementDetails(element);
                        identityHandler.handleIdentity(oldSelectedElementDetails.ancestors[0], oldSelectedElementDetails, newSelectedElementDetails.ancestors[0], newSelectedElementDetails);
                        editor.fire("change"); // for re-numbering
                    }
                }
            });
        },
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name: pluginName,
    };

    return pluginModule;
});