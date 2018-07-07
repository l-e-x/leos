/*
 * Copyright 2017 European Commission
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
define(function aknParagraphPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    
    var pluginName = "aknParagraph";

    var pluginDefinition = {
       init: function init(editor) {
           //Go through p elements to set "data-akn-name" attribute.
           editor.on("change", function(event) {
               if (event.editor.checkDirty()) {
                   event.editor.fire( 'lockSnapshot' );
                   var jqEditor = $(event.editor.editable().$);
    
                   var ps = jqEditor.find("div[data-akn-name=blockContainer] > p:not([data-akn-name])");
                   for (var i=0; i<ps.length; i++) {
                       ps[i].setAttribute("data-akn-name","aknParagraph");
                   }
                   event.editor.fire( 'unlockSnapshot' );
               }
           });
       }
    };
    pluginTools.addPlugin(pluginName, pluginDefinition);

    var transformationConfig = {
            akn: 'mp',
            html: 'p',
            attr: [{
                akn: "GUID",
                html: "id"
            }, {
                html: "data-akn-name=aknParagraph"
            }],
            sub: {
                akn: "text",
                html: "p/text"
            }
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    // return plugin module
    var pluginModule = {
        name: pluginName,
        transformationConfig: transformationConfig,
    };

    return pluginModule;
});