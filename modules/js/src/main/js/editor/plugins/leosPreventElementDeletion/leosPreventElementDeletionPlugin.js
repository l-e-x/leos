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
define(function leosPreventElementDeletionPluginModule(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var pluginTools = require("plugins/pluginTools");
    var leosNonEditableEmptyWidget = require("plugins/leosPreventElementDeletion/leosNonEditableEmptyWidget");
    var pluginName = "leosPreventElementDeletion";
    var stylesLoaded = false;

    var pluginDefinition = {
        requires: "widget",

        init: function init(editor) {
            editor.on("toHtml", addPreventElementDeletionWidgetToFirstChild, null, null, 14);
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

    function addPreventElementDeletionWidgetToFirstChild(event) {
        var topEditorElement = event.data.dataValue;
        if(event.editor.config.addPreventElementDeletionWidgetToFirstChild && topEditorElement.children.length > 1) {
            var widgetDiv = topEditorElement.children[0];
            topEditorElement.children[1].add(widgetDiv, 0);
        }
    }

    function addEmptyElement(event) {
        var topEditorElement = event.data.dataValue;
        if(topEditorElement.children.length > 0 && topEditorElement.children[0].type === CKEDITOR.NODE_ELEMENT){
            var text = new CKEDITOR.htmlParser.text(leosNonEditableEmptyWidget.elementText);
            var emptyElement = new CKEDITOR.htmlParser.element(leosNonEditableEmptyWidget.elementName, {class: leosNonEditableEmptyWidget.elementClass});
            emptyElement.add(text);
            topEditorElement.add(emptyElement, 0);
        }
    }

    function removeEmptyElement(event) {
        var topEditorElement = event.data.dataValue;
        if(topEditorElement && topEditorElement.children.length > 0){
            topEditorElement.find(function(child){
                return child.hasClass && child.hasClass(leosNonEditableEmptyWidget.elementClass);
            }, true).forEach(function (emptyElement) {
                emptyElement.remove();
            });
        }
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    return {
        name: pluginName
    };
});