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
define(function leosFixNestedPsPluginModule(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var pluginTools = require("plugins/pluginTools");

    var pluginName = "leosFixNestedPs";

    // configure <mp> tag as block so that it will not be wrapped in <p> tag
    CKEDITOR.dtd.$block['mp'] = 1;

    var pluginDefinition = {
        init: function init(editor) {
            // current regex is not fully compliant with Unicode characters
            // that the XML standard allows for tag and attribute names
            var pTagsRegex = /(<\/?)(?:p)((?:\s+[^>]*)*>)/gi;
            var mpTagsRegex = /(<\/?)(?:mp)((?:\s+[^>]*)*>)/gi;

            editor.on("toHtml", function(event) {
                var xml = event.data.dataValue;
                // replace all <p> tags with <mp> tags
                xml = xml.replace(pTagsRegex, '$1mp$2');
                event.data.dataValue = xml;
            }, null, null, 1);

            editor.on("toDataFormat", function(event) {
                var xml = event.data.dataValue;
                // replace all <mp> tags with <p> tags
                xml = xml.replace(mpTagsRegex, '$1p$2');
                event.data.dataValue = xml;
            }, null, null, 16);
        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // create plugin module
    var pluginModule = {
        name: pluginName
    };

    return pluginModule;
});