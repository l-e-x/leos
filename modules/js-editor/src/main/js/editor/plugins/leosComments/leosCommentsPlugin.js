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
define(function leosCommentsPluginModule(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var pluginTools = require("plugins/pluginTools");

    var dialogName = "leosCommentsDialog";
    var widgetName = "leosCommentsWidget";
    var pluginName = "leosComments";
    var cssPath = "css/" + pluginName + ".css";

    var leosCommentsWidgetDefinition = require("./leosCommentsWidget");

    var pluginDefinition = {
        icons: 'leosComment',
        requires: "widget,dialog",
        init: function(editor) {
            editor.addContentsCss(pluginTools.getResourceUrl(pluginName, cssPath));
            pluginTools.addDialog(dialogName, initializeDialog);
            editor.widgets.add(widgetName, leosCommentsWidgetDefinition);
            addButton('LeosComment', 'back', 'Insert Comments', 20, widgetName);

            function addButton(name, type, title, order, cmdName) {
                editor.ui.add(name, CKEDITOR.UI_BUTTON, {
                    label: title,
                    title: title,
                    toolbar: 'insert,' + order,
                    command: cmdName
                });
            }
        }
    };

    function initializeDialog(editor) {
        var dialogDefinition = {
            title: "Insert Custom Comments",
            minWidth: 400,
            minHeight: 100,
            contents: [{
                id: "info",
                elements: [{
                    id: "comment",
                    type: "text",
                    label: "Enter your comments here:",
                    setup: function setup(widget) {
                        // set the dialog value to the value from widget comment attribute
                        this.setValue(widget.data.commentText);
                    },
                    commit: function commit(widget) {
                        // update comment value by data introduce by user in dialog
                        widget.setData("commentText", this.getValue());
                    }
                }]
            }],
            //LEOS - 1963
            onOk: function(event) {
                var selection = this._.editor.getSelection();
                selection.getRanges().forEach(
                    function loopOverRange(currentRange) {
                        currentRange.collapse(true);//take cursor to start of selected range
                    });
            }
        };
        return dialogDefinition;
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    var transformationConfig = {
        akn: "popup",
        html: 'span[data-akn-name=popup]',
        attr: [{
            akn: "id",
            html: "id"
        }, {
            akn: "refersto",
            html: "refersto"
        }, {
            html: "data-akn-name=popup"
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
            akn: "leos:dg",
            html: "leos:dg"
        }],
        sub: {
            akn: "mp",
            html: "span/span",
            attr: [{
                akn: "id",
                html: "data-akn-mp-id"
            },{
                html:"class=comment-indicator"
            }],
            sub: [{
                akn: "text",
                html: "span/span/text"
            }]
        }
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    // return plugin module
    var pluginModule = {
        name: pluginName
    };

    return pluginModule;
});