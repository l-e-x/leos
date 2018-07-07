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
define(function authorialNoteWidgetModule(require) {
    "use strict";

    var STAMPIT = require("stampit");

    var authorialNoteWidgetDefinition = {
        inline: true,
        requires: "leosWidgetPlugin",
        allowedContent: "span[title,marker,data-akn-name](!authorialnote)",
        template: '<span class="authorialnote" title="" marker="" data-akn-name="aknAuthorialNote"></span>',
        dialog: "authorialNoteDialog",
        init: function() {
            var title = this.element.getAttribute("title") || "";
            // move title value to fnote so the dialog can access it
            this.setData("fnote", title);
            this.on("ready", this._onRenumberCallback);
            this.on("destroy", this._onRenumberCallback);
        },

        data: function data() {
            // move fnote value to title so element title can be updated after changes done by user in dialog
            this.element.setAttribute("title", this.data.fnote);
        },

        _onRenumberCallback: function _onRenumberCallback(evt) {
            // re-numbering all the widgets
            this._renumberAuthorialNotes(this.editor);
        },

        _renumberAuthorialNotes: function _renumberAuthorialNotes(editor) {
            var jqEditor = $(this.editor.editable().$);
            var authorialNotes = jqEditor.find('*[data-akn-name="aknAuthorialNote"]');
            if (authorialNotes && authorialNotes.length > 0) {
                var markerValue = this._getLowestMarkerValue(authorialNotes);
                var currentMarkerVal = parseInt(this.element.getAttribute("marker"));
                if (!isNaN(currentMarkerVal)) {
                    markerValue = Math.min.apply(Math, [markerValue, currentMarkerVal]);
                }
                for (var index = 0; index < authorialNotes.length; index++) {
                    authorialNotes.get(index).innerHTML = markerValue;
                    authorialNotes.get(index).setAttribute("marker", markerValue);
                    markerValue = markerValue + 1;
                }
            }
        },

        _getLowestMarkerValue: function _getLowestMarkerValue(authorialNotes) {
            var markerArray = [];
            for (var index = 0; index < authorialNotes.length; index++) {
                var markerVal = parseInt(authorialNotes.get(index).getAttribute("marker"));
                if (!isNaN(markerVal)) {
                    markerArray.push(markerVal);
                }
            }
            return markerArray.length > 0 ? Math.min.apply(Math, markerArray) : 1;
        },

        upcast: function upcast(element) {
            // Defines which elements will become widgets.
            if (element.hasClass("authorialnote")) {
                return true;
            }
            return false;
        }

    }

    return authorialNoteWidgetDefinition;
});