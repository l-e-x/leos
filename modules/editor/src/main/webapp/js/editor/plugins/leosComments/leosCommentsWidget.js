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
define(function leosCommentsWidgetModule(require) {
    "use strict";

    var STAMPIT = require("stampit");
    var leosUserInfo = require("core/leosUserInfo");
    var dateFormat = require("dateFormat");
    var pluginTools = require("plugins/pluginTools");
    var pluginName = "leosComments";

    var commentIcon = "icons/leosComment_color.png";
    commentIcon = pluginTools.getResourceUrl(pluginName, commentIcon);

    var leosCommentsWidgetDefinition = {
        inline: true,
        requires: "leosWidgetPlugin",
        allowedContent: "span[data-hint,data-akn-name](!leoscomments),img[src,commentdata]",
        template: '<span class="leoscomments hint--top hint--bounce hint--rounded" data-akn-name="popup"><img src="" commentdata=""></span>',
        dialog: "insertCommentsDialog",

        init: function init() {
            var commentData = "";
            var imageElement = this.element.findOne("img");
            if (imageElement) {
                commentData = imageElement.getAttribute("commentdata") || "";
            }
            // move commentdata value to data so that the dialog can access it
            this.setData("commentdata", commentData);
        },

        //to set the data on the dialog
        data: function data() {
            if (this.data.commentdata != "") {
                var dataHintValue;
                var userId = this.element.getAttribute("leos:userid");
                var userName = this.element.getAttribute("leos:username");
                var timeStamp = this.element.getAttribute("leos:datetime");
                if (!userId && !timeStamp) {
                    userId = leosUserInfo.getUserLogin(this.editor);
                    userName = leosUserInfo.getUserName(this.editor);
                    timeStamp =  pluginTools.getCurrentUTCDateAsString();  

                    this.element.setAttribute("leos:userid", userId);
                    this.element.setAttribute("leos:username", userName);
                    this.element.setAttribute("leos:datetime", timeStamp);
                }
                var localeDateStr = pluginTools.getLocalDateFromUTCAsString(timeStamp);
                // move comment value to data-hint so element's comment can be updated after changes done by user in dialog
                dataHintValue = this.data.commentdata + "\n------------------------------------\n[Commented by: "+ userName + "(" + userId +")" + " - " + localeDateStr + "]";
                this.element.setAttribute("data-hint", dataHintValue);
                this.element.setAttribute("data-akn-name", "popup");

                var imageElement = this.element.findOne("img");
                if (imageElement) {
                    imageElement.setAttribute("commentdata", this.data.commentdata);
                    //Set image for toHTML side
                    if (!imageElement.getAttribute("src")) {
                        imageElement.setAttribute("src", commentIcon);
                    }
                }
            } else {
                this.element.removeAttribute("data-hint");
                this.element.removeAttribute("data-akn-name");
            }
        },

        upcast: function upcast(element) {
            // Defines which elements will become widgets.
            if (element.hasClass("leoscomments")) {
                return true;
            }
            return false;
        }
    }

    return leosCommentsWidgetDefinition;
});