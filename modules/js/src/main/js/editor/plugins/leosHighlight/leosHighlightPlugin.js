/*
 * Copyright 2017 European Commission
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
define(function leosHighlightPluginModule(require) {
    "use strict";

    // load module dependencies
    var $ = require("jquery");
    var UTILS = require("core/leosUtils");
    var pluginTools = require("plugins/pluginTools");

    var pluginName = "leosHighlight";

    var pluginDefinition = {
        init: function init(editor) {
            editor.on("change", setHighlight);
            editor.on("toHtml", setHighlight);
        }
    };
    
    function setHighlight(event) {
        var editor = event.editor;
        var $editable = $(editor.editable().$);
        $editable.find("[class*='leos-highlight-']").each(function() {
            var userId = this.getAttribute("data-hgl-userid");
            var userName = this.getAttribute("data-hgl-username");
            var timeStamp = this.getAttribute("data-hgl-datetime");
            if (!userId && !timeStamp) {
                var user = editor.LEOS && editor.LEOS.user;
                if (user) {
                    this.setAttribute("data-hgl-userid", user.login);
                    this.setAttribute("data-hgl-username", user.name);
                }
                timeStamp =  UTILS.getCurrentUTCDateAsString();
                this.setAttribute("data-hgl-datetime", timeStamp);
                this.setAttribute("refersto", "~leoshighlight");
            }
        });
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    var transformationConfig = {
        akn: "span[refersTo=~leoshighlight]",
        html: 'span[refersto=~leoshighlight]',
        attr: [ {
           akn: "GUID",
           html: "id"
        }, {
            akn: "class=leos-highlight-*",
            html: "class=leos-highlight-*"
        }, {
            akn: "refersTo=~leoshighlight",
            html: "refersto=~leoshighlight"
        }, {
            akn: "leos:userid",
            html: "data-hgl-userid" /*renaming attributes to data-* because overrides property of style removes
                                    all matching attributes from inner elements such as comments.*/
        }, {
            akn: "leos:username",
            html: "data-hgl-username"
        }, {
            akn: "leos:datetime",
            html: "data-hgl-datetime"
        }],
        sub: {
            akn: "text",
            html: "span/text"
        }
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    // return plugin module
    var pluginModule = {
        name: pluginName,
        specificConfig: {
            colorButton_enableMore: false,
            colorButton_colors: 'yellow/FFFF00,green/00FF00,cyan/00FFFF,pink/FF00FF,blue/0000FF,red/FF0000,grey/C0C0C0',
            colorButton_backStyle: {
                element: 'span',
                attributes: {
                    'class': 'leos-highlight-#(color)'
                },
                overrides: [{
                    element: 'span',
                    attributes: {
                        'class': /^leos-highlight/,
                        'refersto': '~leoshighlight',
                        'data-hgl-userId': /[\w]*/,
                        'data-hgl-username': /[\w]*/,
                        'data-hgl-datetime': /[\w]*/
                    }
                }]
            }
        }
    };

    return pluginModule;
});
