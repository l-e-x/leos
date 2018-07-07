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
define(function leosDropHandlerPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var $ = require("jquery");
    var LOG = require("logger");
    
    var pluginName = "leosDropHandler";


    var pluginDefinition = {
        init: function init(editor) {
            editor.on("paste", function(evt) {
                var dataObj = evt.data; 
                if(dataObj.method === "drop") {
                    _handleDrop(dataObj);
                }
            });
            
            function _handleDrop(dataObj) {
                var droppedData = dataObj.dataValue;
                var droppedElement = $.parseHTML(droppedData);
                var $widgetWrappers = $(droppedElement).find(".cke_widget_wrapper");
                if($widgetWrappers.length > 0) {
                   dataObj.dataValue =  $widgetWrappers[0].outerHTML;
                }
            }
        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name: pluginName,
    };

    return pluginModule;
});