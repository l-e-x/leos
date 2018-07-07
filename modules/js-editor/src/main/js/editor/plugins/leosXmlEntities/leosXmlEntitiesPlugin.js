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
define(function leosXmlEntitiesModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var _ = require("lodash");

    var pluginName = "leosXmlEntities";

    // base entities defined by XML specification
    var xmlEntitiesMap = {
        "&lt;": "\u003C",
        "&gt;": "\u003E",
        "&amp;": "\u0026",
        "&quot;": "\u0022",
        "&apos;": "\u0027"
    };

    // extra entities usually encoded by browsers
    var extraEntitiesMap = {
        "&nbsp;": "\u00A0",
        "&shy;": "\u00AD"
    };

    // map all entities to characters
    var entitiesMap = _.assign({}, xmlEntitiesMap, extraEntitiesMap);

    var decodeRegex = new RegExp(_.keys(entitiesMap).join("|"), "g");

    function getChar(entity) {
        return entitiesMap[entity];
    }

    // map characters to XML entities
    var charactersMap = _.invert(xmlEntitiesMap);

    var encodeRegex = new RegExp(_.keys(charactersMap).join("|"), "g");

    function getEntity(char) {
        return charactersMap[char];
    }

    var pluginDefinition = {
        afterInit: function(editor) {
            var dataProcessor = editor.dataProcessor;
            var htmlFilter = dataProcessor && dataProcessor.htmlFilter;
            if (htmlFilter) {
                htmlFilter.addRules({
                    text: function(text) {
                        return text.replace(decodeRegex, getChar).replace(encodeRegex, getEntity);
                    }
                }, {
                    applyToAll: true,
                    excludeNestedEditable: true
                });
            }
        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    return {
        name: pluginName
    };
});