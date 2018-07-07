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
define(function authorialNoteWidgetModule(require) {
    "use strict";

    var CKEDITOR = require("promise!ckEditor");
    var $ = require("jquery");

    var authorialNoteWidgetDefinition = {
        inline: true,
        requires: "leosWidgetPlugin",
        allowedContent: "span[title,marker,data-akn-name](!authorialnote)",
        template: '<span class="authorialnote" title="" marker="default" data-akn-name="aknAuthorialNote"></span>',
        dialog: "authorialNoteDialog",
        init: function() {
            this.on("ready", this._renumberAuthorialNotes.bind(undefined, this.editor));
            this.on("destroy", this._renumberAuthorialNotes.bind(undefined, this.editor));
        },

        data: function data() {
            this.element.setAttribute("title", this.data.fnote);
        },

        upcast: function upcast(element, data) {
            if(element.hasClass("authorialnote")) {
                var title = element.attributes["title"] || "";
                var fnote = CKEDITOR.tools.htmlDecodeAttr(title);
                data["fnote"] = fnote;
                return element;
            }
        },

        downcast: function downcast(element) {
            if(element.hasClass("authorialnote")) {
                var fnote = element.attributes["title"] || "";
                var title = CKEDITOR.tools.htmlEncodeAttr(fnote);
                element.attributes["title"] = title;
                return element;
            }
        },

        _renumberAuthorialNotes: function _renumberAuthorialNotes(editor) {
        	var markerValue = editor.LEOS.lowestMarkerValue;
        	var $editable = $(editor.editable().$);
            var authorialNotes = $editable.find('*[data-akn-name="aknAuthorialNote"]');
            authorialNotes.each(function() {
                    this.innerHTML = markerValue;
                    this.setAttribute("marker", markerValue);
                    markerValue = markerValue + 1;
            });
        }
    };

    return authorialNoteWidgetDefinition;
});