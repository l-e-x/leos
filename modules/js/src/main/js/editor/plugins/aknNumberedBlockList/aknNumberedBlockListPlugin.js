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
define(function aknNumberedBlockListPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var blockListTransformerStamp = require("plugins/leosBlockListTransformer/blockListTransformer");
    
    var pluginName = "aknNumberedBlockList";
    
    var LOG = require("logger");

    var pluginDefinition = {
        init : function init(editor) {

            //Go through ol elements to set "data-akn-name" attribute and through ascendant li elements to set "data-akn-num" attributes.
            editor.on("change", function(event) {
                if (event.editor.checkDirty()) {
                    event.editor.fire('lockSnapshot');
                    var jqEditor = $(event.editor.editable().$);

                    var ols = jqEditor.find("ol");
                    for (var i = 0; i < ols.length; i++) {
                        ols[i].setAttribute("data-akn-name","NumberedBlockList");
                        var listItems = ols[i].children;
                        for (var jj = 0; jj < listItems.length; jj++) {
                            var numericSequence = jj + 1 + "."; //displayed as (1., 2., 3.) etc.
                            listItems[jj].setAttribute("data-akn-num",numericSequence);
                            listItems[jj].removeAttribute("data-akn-name"); //remove copied attribute from the parent
                            listItems[jj].removeAttribute("style");
                        }
                    }
                    event.editor.fire( 'unlockSnapshot' );
                }
            });
        }
    };
    
    pluginTools.addPlugin(pluginName, pluginDefinition);
    var BLOCKLIST = "blockList";
    
    var blockListTransformer = blockListTransformerStamp({
      blockListTransformationConfig : {
        akn : [BLOCKLIST,'[leos:listtype=Numbered]'].join(""),
        html : 'ol',
        attr : [ {
            akn : "xml:id",
            html : "id"
        }, {
            akn : "leos:origin",
            html : "data-origin"
        }, {
            html : "data-akn-name=NumberedBlockList"
        }, {
        	akn : "leos:listtype=Numbered",
        }],
        sub : [{
            akn : "item",
            html : "ol/li",
            attr : [ {
                akn : "xml:id",
                html : "id"
            }, {
                akn : "leos:origin",
                html : "data-origin"
            }],
            sub : [{
                akn : "num",
                html : "ol/li",
                attr : [ {
                    akn : "xml:id",
                    html : "data-akn-num-id"
                }, {
                    akn : "leos:origin",
                    html : "data-num-origin"
                } ],
                sub: {
                    akn: "text",
                    html: "ol/li[data-akn-num]"
                }
            },{
                akn : "mp",
                html : "ol/li",
                attr : [{
                    akn : "xml:id",
                    html : "data-akn-mp-id"
                }, {
                    akn : "leos:origin",
                    html : "data-mp-origin"
                }],
                sub : {
                    akn: "text",
                    html: "ol/li/text"
                }
            }]
        }]
      },
      rootElementsForAkn : [ BLOCKLIST.toLowerCase(), "item" ],
      rootElementsForHtml : [ "ol", "li" ]
    });

    var transformationConfig = blockListTransformer.getTransformationConfig();
    
    // return plugin module
    var pluginModule = {
        name : pluginName,
        transformationConfig: transformationConfig
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    return pluginModule;
});