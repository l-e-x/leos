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
define(function leosPreventElementDeletionPluginModule(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var pluginTools = require("plugins/pluginTools");
    var leosNonEditableEmptyWidget = require("plugins/leosPreventElementDeletion/leosNonEditableEmptyWidget")
    var pluginName = "leosPreventElementDeletion";
    var stylesLoaded = false;

    var pluginDefinition = {
        requires: "widget",

        init: function init(editor) {
            editor.on("toHtml", addEmptyElement, null, null, 7);
            editor.on("toDataFormat", removeEmptyElement, null, null, 13);

            editor.widgets.add(leosNonEditableEmptyWidget.name, leosNonEditableEmptyWidget.definition);
            if (!stylesLoaded) {
                CKEDITOR.document.appendStyleSheet( this.path + leosNonEditableEmptyWidget.css );
                stylesLoaded = true;
            }
            editor.addContentsCss(pluginTools.getResourceUrl(pluginName, leosNonEditableEmptyWidget.css));
        }
    };

    function addEmptyElement(event) {
        var topEditorElement = event.data.dataValue;
        if(topEditorElement.children.length > 0){
            var text = new CKEDITOR.htmlParser.text(leosNonEditableEmptyWidget.elementText);
            var emptyElement = new CKEDITOR.htmlParser.element(leosNonEditableEmptyWidget.elementName, {class: leosNonEditableEmptyWidget.elementClass});
            emptyElement.add(text);
            topEditorElement.children[0].add(emptyElement, 0);
        }
    }

    function removeEmptyElement(event) {
        var topEditorElement = event.data.dataValue;
        if(topEditorElement.children.length > 0 && topEditorElement.children[0].children.length > 0){
            var emptyElement = topEditorElement.children[0].getFirst(function(child){
                return child.hasClass(leosNonEditableEmptyWidget.elementClass);
            });
            if(emptyElement){
                emptyElement.remove();
            }
        }
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    return {
        name: pluginName
    };
});