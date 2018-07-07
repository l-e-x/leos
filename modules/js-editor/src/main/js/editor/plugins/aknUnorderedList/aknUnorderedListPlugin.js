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
define(function aknUnorderedListPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var leosHierarchicalElementTransformerStamp = require("plugins/leosHierarchicalElementTransformer/hierarchicalElementTransformer");

    var pluginName = "aknUnorderedList";
    var cssPath = "css/aknUnorderedList.css";

    var DATA_AKN_NUM = "data-akn-num";
    var LIST_ITEM = "li";
    var BULLETED_LIST = "bulletedlist";

    var pluginDefinition = {
        init : function init(editor) {
            editor.addContentsCss(pluginTools.getResourceUrl(pluginName, cssPath));
            editor.on("change", resetDataAknNameForUnOrderedList, null, null, 0);
            editor.on("change", resetIndentsIndicators, null, null, 1);
        }
    };

    function resetDataAknNameForUnOrderedList(event) {
        event.editor.fire( 'lockSnapshot' );
        var jqEditor = $(event.editor.editable().$);
        var unOrderedLists = jqEditor.find("ul");
        for (var ii = 0; ii < unOrderedLists.length; ii++) {
            var unOrderedList = unOrderedLists[ii];
            unOrderedList.setAttribute("data-akn-name", "aknUnorderedList");
            var listItems = unOrderedList.children;
            for (var jj = 0; jj < listItems.length; jj++) {
                listItems[jj].removeAttribute("data-akn-name");
            }

        }
        event.editor.fire( 'unlockSnapshot' );
    }

    /*
     * Resets the numbering of the points depending on nesting level. LEOS-1487: Current implementation simply goes through whole document and renumbers all
     * ordered list items. For above reason this could cause some performance issues if so this implementation should be reconsidered.
     * 
     */
    function resetIndentsIndicators(event) {
        event.editor.fire( 'lockSnapshot' );
        var jqEditor = $(event.editor.editable().$);
        var unOrderedLists = jqEditor.find("*[data-akn-name='aknUnorderedList']");
        for (var ii = 0; ii < unOrderedLists.length; ii++) {
            var listItems = unOrderedLists[ii].children;
            for (var jj = 0; jj < listItems.length; jj++) {
                listItems[jj].setAttribute(DATA_AKN_NUM, "-");
            }
        }
        event.editor.fire( 'unlockSnapshot' );

    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    var leosHierarchicalElementTransformer = leosHierarchicalElementTransformerStamp({
        firstLevelConfig : {
            akn : 'list',
            html : 'ul',
            attr : [ {
                akn : "leos:editable",
                html : "contenteditable"
            }, {
                akn : "id",
                html : "id"
            }, {
                html : "data-akn-name=aknUnorderedList"
            } ]
        },
        rootElementsForFrom : [ "list", "indent" ],
        contentWrapperForFrom : "alinea",
        rootElementsForTo : [ "ul", "li" ]
    });

    var transformationConfig = leosHierarchicalElementTransformer.getTransformationConfig();

    // return plugin module
    var pluginModule = {
        name : pluginName,
        transformationConfig: transformationConfig
    };

    pluginTools.addTransformationConfigForPlugin(leosHierarchicalElementTransformer.getTransformationConfig(), pluginName);

    return pluginModule;
});