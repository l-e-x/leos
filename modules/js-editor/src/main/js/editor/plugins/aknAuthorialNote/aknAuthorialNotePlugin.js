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
define(function aknAuthorialNotePluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var CKEDITOR = require("ckEditor");

    var authorialNoteWidgetDefinition = require("./authorialNoteWidget");

    var pluginName = "aknAuthorialNote";
    var widgetName = "authorialNoteWidget";
    var dialogName = "authorialNoteDialog";
    var cssPath = "css/" + pluginName + ".css";

    var pluginDefinition = {
        requires : "widget,dialog",
        icons : widgetName.toLowerCase(),
        init : function init(editor) {
            editor.addContentsCss(pluginTools.getResourceUrl(pluginName, cssPath));
            pluginTools.addDialog(dialogName, initializeDialog);
            editor.widgets.add(widgetName, authorialNoteWidgetDefinition);
            editor.ui.addButton(widgetName, {
                label : "Create an authorial note",
                command : widgetName,
                toolbar : 'insert,10'
            });
        }
    };

    function initializeDialog(editor) {
        var dialogDefinition = {
            title : "Edit authorial note.",
            minWidth : 400,
            minHeight : 100,
            contents : [ {
                id : "info",
                elements : [ {
                    id : "fnote",
                    type : "text",
                    label : "Enter your text here:",

                    setup : function setup(widget) {
                        // set the dialog value to the value from widget fnote attribute
                        this.setValue(widget.data.fnote);
                    },
                    commit : function commit(widget) {
                        // update fnote value by data introduce by user in dialog
                        widget.setData("fnote", this.getValue());
                    }
                } ]
            } ]
        };
        return dialogDefinition;
    }
    ;

    pluginTools.addPlugin(pluginName, pluginDefinition);

    var transformationConfig = {
        akn : 'authorialNote',
        html : 'span[class=authorialnote]',
        attr : [ {
            html : "class=authorialnote"
        }, {
            akn : "marker",
            html : "marker"
        }, {
            akn : "id",
            html : "id"
        }, {
            html : ["data-akn-name", "aknAuthorialNote"].join("=")
        } ],
        sub : {
            akn : 'mp',
            html : 'span',
            attr : [ {
                akn : "id",
                html : "data-akn-mp-id"
            }],
            sub : [ {
                akn : 'text',
                html : 'span[title]'
            } ]
        }
    };
    ;

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    // return plugin module
    var pluginModule = {
        transformationConfig : transformationConfig,
        name : pluginName
    };
    return pluginModule;
});
