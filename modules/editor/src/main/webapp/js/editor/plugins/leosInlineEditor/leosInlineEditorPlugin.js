/*
 * Copyright 2015 European Commission
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
define(function leosInlineEditorPluginModule(require) {
    "use strict";
    /*
     * This plugin should be the first one to be loaded.
     */
    
    
    // load module dependencies
    var pluginTools = require("plugins/pluginTools");

    var pluginName = "leosInlineEditorPlugin";

    var pluginDefinition = {

        init : function init(editor) {
            /*
             * By default ckeditor does not provide way to load css styles in inline mode.
             * This function override and provide way to load css with jquery.
             */
            editor.addContentsCss = function(href) {
                var cssLink = $("<link rel='stylesheet' type='text/css' href='" + href + "'>");
                $("head").append(cssLink);
                //TODO make sure css are not loaded more than one times
            };

        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name : pluginName
    };

    return pluginModule;
});