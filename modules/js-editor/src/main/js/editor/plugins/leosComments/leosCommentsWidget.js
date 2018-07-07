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
    var cuid = require("cuid");
    var CKEDITOR = require("promise!ckEditor");
    // utilities
    var UTILS = leosCore.utils;

    var leosCommentsWidgetDefinition = {
        inline: true,
        requires: "leosWidgetPlugin",
        allowedContent: "span[data-akn-name,refersTo,id],span(!comment-indicator)",
        template: '<span data-akn-name="popup" refersTo="~leosComment"><span class="comment-indicator"/></span>',
        dialog: "leosCommentsDialog",

        init: function init() {
            this.on("destroy", this._onCommentUpdate);
            this.on("ready", this._onCommentUpdate);
        },

        //to set the data on the dialog
        data: function data() {
            if (this.data.commentText) {
                // if id is not present...
                // assume that this is a new comment and all data needs to be populated
                if (!this.element.getAttribute("id")) {
                    this.element.setAttribute("id", 'akn_' + cuid.slug());
                    this.element.setAttribute("leos:datetime", UTILS.getCurrentUTCDateAsString());
                    var user = this.editor.LEOS && this.editor.LEOS.user;
                    if (user) {
                        this.element.setAttribute("leos:userid", user.id);
                        this.element.setAttribute("leos:username", user.name);
                        this.element.setAttribute("leos:dg", user.dg||"");
                    }
                }

                this.element.setAttribute("data-akn-name", "popup");

                var innerElement = this.element.findOne("span");
                if (innerElement) {
                    innerElement.setText(this.data.commentText);
                }
            } else {
                this.element.removeAttribute("data-akn-name");
            }
            this._onCommentUpdate();
        },

        _onCommentUpdate: function _onCommentUpdate(event) {
            if (this.ready) {// to block calls before widget data is in DOM
                this.editor.fire("contentChange");
            }
        },

        upcast: function upcast(element, data) {
            if (element.attributes.refersto === "~leosComment") {
                var commentElement = element;
                var commentText = undefined;

                if (!commentElement.isEmpty) {
                    var indicatorElement = commentElement.getFirst(
                        function (child) {
                            return child.hasClass("comment-indicator");
                        });
                    if (indicatorElement && !indicatorElement.isEmpty) {
                        var textElement = indicatorElement.getFirst(
                            function (child) {
                                return child.type == CKEDITOR.NODE_TEXT;
                            }
                        );
                        if (textElement) {
                            commentText = textElement.value;
                        }
                    }
                }

                data["commentText"] = CKEDITOR.tools.htmlDecode(commentText);
                return element;
            }
        }
    };

    return leosCommentsWidgetDefinition;
});