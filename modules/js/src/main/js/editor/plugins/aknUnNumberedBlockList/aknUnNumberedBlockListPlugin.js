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
define(function aknUnNumberedBlockListPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var $ = require('jquery');
    var blockListTransformerStamp = require("plugins/leosBlockListTransformer/blockListTransformer");
    
    var pluginName = "aknUnNumberedBlockList";
    
    var LOG = require("logger");

    var pluginDefinition = {
        init : function init(editor) {

            //Go through ul elements to set "data-akn-name" attribute and through ascendant li elements to set "data-akn-num" attributes.
            editor.on("change", function(event) {
                if (event.editor.checkDirty()) {
                    event.editor.fire( 'lockSnapshot' );
                    var jqEditor = $(event.editor.editable().$);

                    var uls = jqEditor.find("ul");
                    for (var i=0; i<uls.length; i++) {
                        uls[i].setAttribute("data-akn-name","UnNumberedBlockList");
                        var listItems = uls[i].children;
                        for (var jj = 0; jj < listItems.length; jj++) {
                            listItems[jj].setAttribute("data-akn-num","•");
                            listItems[jj].removeAttribute("data-akn-name");
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
          akn : [BLOCKLIST,'[leos:listtype=UnNumbered]'].join(""),
          html : 'ul',
          attr : [{
              akn : "GUID",
              html : "id"
          },{
              akn : "leos:origin",
              html : "data-origin"
          },{
              html : "data-akn-name=UnNumberedBlockList"
          },{
          	akn : "leos:listtype=UnNumbered",
          }],
          sub : [{
              akn : "item",
              html : "ul/li",
              attr : [ {
                  akn : "GUID",
                  html : "id"
              }, {
                  akn : "leos:origin",
                  html : "data-origin"
              }],
              sub : [{
                  akn : "num",
                  html : "ul/li",
                  attr : [ {
                      akn : "GUID",
                      html : "data-akn-num-id"
                  }, {
                      akn : "leos:origin",
                      html : "data-num-origin"
                  }],
                  sub: {
                      akn: "text",
                      html: "ul/li[data-akn-num]"
                  }
              },{
                  akn : "mp",
                  html : "ul/li",
                  attr : [ {
                      akn : "GUID",
                      html : "data-akn-mp-id"
                  }, {
                      akn : "leos:origin",
                      html : "data-mp-origin"
                  } ],
                  sub : {
                      akn: "text",
                      html: "ul/li/text"
                  }
              }]
          }]
        },
        rootElementsForAkn : [ BLOCKLIST.toLowerCase(), "item" ],
        rootElementsForHtml : [ "ul", "li" ]
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