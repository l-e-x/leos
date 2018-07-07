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
define(function aknHtmlImagePluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");

    var pluginName = "aknHtmlImage";
    var pluginDefinition = {
            init : function init(editor) {
                //To hide 'Alignment', 'Border', 'Url'etc. in the dialog box
                editor.on('dialogShow', function(event) {
                    var dialog = event.data;
                    if (dialog.getName() === 'base64imageDialog') {
                        var general = ['url', 'urlcheckbox', 'filecheckbox'];
                        general.forEach( function(item) {
                            dialog.getContentElement('tab-source', item).getElement().hide();
                        });
                        
                        var advance = ['align', 'border', 'vmargin', 'hmargin'];
                        advance.forEach( function(item) {
                            dialog.getContentElement('tab-properties', item).getElement().hide();
                        });
                        dialog.resize(310,100);
                    }
                });
            }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    var transformationConfig = {
        akn:  "img",
        html: "img",
        attr: [{
            akn: "GUID",
            html: "id"
        }, {
            akn: "src",
            html: "src"
        }, {
            akn: "alt",
            html: "alt"
        }, {
            akn: "width",
            html: "width"
        }, {
            akn: "height",
            html: "height"
        }, {
            akn: "style",
            html: "style"
        }]
    };


    // return plugin module
    var pluginModule = {
        name: pluginName
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);
    
    return pluginModule;
});