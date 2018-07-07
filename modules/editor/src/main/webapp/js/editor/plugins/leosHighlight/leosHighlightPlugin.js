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
define(function leosHighlightPluginModule(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var leosUserInfo = require("core/leosUserInfo");
    var dateFormat = require("dateFormat");
    var pluginTools = require("plugins/pluginTools");

    var pluginName = "leosHighlight";
    var cssPath = "css/leosHighlight.css";

    var pluginDefinition = {
        init: function init(editor) {
            editor.addContentsCss(pluginTools.getResourceUrl(pluginName, cssPath));

            editor.on("change", setHighlight);
            editor.on("toHtml", setHighlight);
        }
    };
    
    function setHighlight(event) {
        var editor = event.editor;
        var jqEditor = $(editor.editable().$);
        var highlightElements = jqEditor.find("*[class*='leos-highlight-']").each(function(index) {

            var userId = this.getAttribute("leos:userid");
            var userName = this.getAttribute("leos:username");
            var timeStamp = this.getAttribute("leos:datetime");
            if (!userId && !timeStamp) {
               
                timeStamp =  pluginTools.getCurrentUTCDateAsString(); 
                userId = leosUserInfo.getUserLogin(editor);
                userName = leosUserInfo.getUserName(editor);
                
                this.setAttribute("leos:userid", userId);
                this.setAttribute("leos:username", userName);
                this.setAttribute("leos:datetime", timeStamp);
            }
            var localeDateStr = pluginTools.getLocalDateFromUTCAsString(timeStamp);
            var dataHintValue = "[Hightlighted by: " + userName + "(" + userId + ") - " + localeDateStr + "]";
            this.setAttribute("data-hint", dataHintValue);
            this.setAttribute("refersto", "~leoshighlight");
            $(this).addClass("hint--top hint--bounce hint--rounded");
        });
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    var transformationConfig = {
        akn: "span[refersto=~leoshighlight]",
        html: 'span[refersto=~leoshighlight]',
        attr: [ {
           akn: "id",
           html: "id"
        }, {
            akn: "class=leos-highlight-*",
            html: "class=leos-highlight-*"
        }, {
            akn: "refersto=~leoshighlight",
            html: "refersto=~leoshighlight"
        }, {
            akn: "leos:userid",
            html: "leos:userid"
        }, {
            akn: "leos:username",
            html: "leos:username"
        }, {
            akn: "leos:datetime",
            html: "leos:datetime"
        }, {
            html: "data-hint"
        }],
        sub: {
            akn: "text",
            html: "span/text"
        }
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    // return plugin module
    var pluginModule = {
        name: pluginName
    };

    return pluginModule;
});