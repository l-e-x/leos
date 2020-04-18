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
define(function leosElementSplitHandlerPluginModule(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var pluginTools = require("plugins/pluginTools");
    var $ = require("jquery");

    var pluginName = "leosElementSplitHandler";
    var config = {
        characterData: false,
        attributes: false,
        childList: true,
        subtree: false
    };

    var pluginDefinition = {
        init: function init(editor) {
            editor.on("receiveData", _startObservingChanges);
        }
    };

    function _startObservingChanges(event) {
        var editor = event.editor;
        if (editor.editable && editor.editable().getChildren && editor.editable().getChildren().count() > 0) {
            _addMutationObserver(_getRootElement(editor).$)
        }
    }

    function _addMutationObserver(rootElement) {
        if (rootElement && !rootElement.mutationObserver) {
            rootElement.mutationObserver = new MutationObserver(_processMutations);
            rootElement.mutationObserver.observe(rootElement, config);
        }
    }

    function _processMutations(mutationsList) {
        var rootElement = null;
        var insertedElement = false;
        $.each(mutationsList, function(i, mutation) {
            if (mutation.type === 'childList' && mutation.addedNodes.length) {
                rootElement = $(mutation.target);
                var node = mutation.addedNodes[0];
                if (node.nodeType == 1) { // Element node
                    insertedElement = true;
                    return false; // Breaks JQuery loop
                }
            }
        });
        if (insertedElement) { // A new element (table, etc.) was inserted
            var rootParElements = rootElement.find("> p");
            var rootTableElements = rootElement.find("> table");
            var rootNoTableParElements = rootElement.find("> *:not(table,p)");
            var rootElementText = rootElement.clone().children().remove().end().text();
            if (rootTableElements.length > 1 || rootParElements.length > 1 || (rootElementText !== '' && rootTableElements.length)
                    || (rootNoTableParElements.length && rootTableElements.length)) {
                var emptyParElements = rootElement.find("> p:emptyTrim");
                if (emptyParElements) {
                    emptyParElements.each(function() {
                        this.innerText = "Text...";
                    });
                }
                var editor = CKEDITOR.currentInstance;
                editor.fire("save", {
                    data: editor.getData()
                });
            }
        } else if (rootElement != null) { // Element not inserted
            var rootElementTables = rootElement.find("> table");
            if (rootElementTables.length) { // If table exists then text has to be removed
                rootElement.contents().filter(function() {
                    return (this.nodeType == 3);
                }).remove();
            }
        }
    }

    function _getRootElement(editor) {
        var jqEditor = $(editor.editable().$);
        return new CKEDITOR.dom.node(jqEditor.find("li")[0]);
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name: pluginName
    };

    return pluginModule;
});