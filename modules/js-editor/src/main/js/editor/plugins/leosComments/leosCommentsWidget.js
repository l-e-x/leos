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
define(function leosCommentsWidgetModule(require) {
    "use strict";

    // load module dependencies
    var leosCore = require("js/leosCore");

    // utilities
    var UTILS = leosCore.utils;

    var leosCommentsWidgetDefinition = {
        inline: true,
        requires: "leosWidgetPlugin",
        allowedContent: "span[data-content,data-akn-name, data-leosComments](!leoscomments),span(!comment-indicator)",
        template: '<span class="leoscomments" data-akn-name="popup" data-leosComments="popover"><span class="comment-indicator"/></span>',
        dialog: "leosCommentsDialog",

        init: function init() {
            var commentText = "";
            var innerElement = this.element.findOne("span");
            if (innerElement) {
                commentText = innerElement.getText() || "";
            }
            // move comment data value to data so that the dialog can access it
            this.setData("widgetdata", commentText);
            this.on("destroy", this._onCommentUpdate);
            this.on("ready", this._onCommentUpdate);
        },

        //to set the data on the dialog
        data: function data() {
            if (this.data.widgetdata != "") {
                var userId = this.element.getAttribute("leos:userid");
                var userName = this.element.getAttribute("leos:username");
                var timeStamp = this.element.getAttribute("leos:datetime");
                if (!userId && !timeStamp) {
                    var user = this.editor.LEOS && this.editor.LEOS.user;
                    if (user) {
                        this.element.setAttribute("leos:userid", user.id);
                        this.element.setAttribute("leos:username", user.name);
                    }
                    timeStamp = UTILS.getCurrentUTCDateAsString();
                    this.element.setAttribute("leos:datetime", timeStamp);
                }
                this.element.setAttribute("data-akn-name", "popup");
                var innerElement = this.element.findOne("span");
                if (innerElement) {
                    innerElement.setText(this.data.widgetdata);
                    // for popover box
                    this.element.setAttribute("data-content", this.data.widgetdata);
                }
            } else {
                this.element.removeAttribute("data-akn-name");
            }
            this._onCommentUpdate();
       },
        
        _onCommentUpdate: function _onCommentUpdate(event) {
            if(this.ready) {// to block calls before widget data is in DOM
                this.editor.fire("contentChange");
            }
        },
        
        upcast: function upcast(element) {
            // Defines which elements will become widgets.
            return element.hasClass("leoscomments");
        }
    };

    return leosCommentsWidgetDefinition;
});