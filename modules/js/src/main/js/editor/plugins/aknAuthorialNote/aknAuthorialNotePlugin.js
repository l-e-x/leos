/*
 * Copyright 2018 European Commission
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
define(function aknAuthorialNotePluginModule(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var pluginTools = require("plugins/pluginTools");
    var $ = require('jquery');

    var authorialNoteWidgetDefinition = require("./authorialNoteWidget");
    var leosCommandStateHandler = require("plugins/leosCommandStateHandler/leosCommandStateHandler");

    var pluginName = "aknAuthorialNote";
    var widgetName = "authorialNoteWidget";
    var dialogName = "authorialNoteDialog";
    var changeStateElements = {
        articleHeading : {
            elementName: 'h2',
            selector: '[data-akn-name=aknHeading]'
        }
    };

    var pluginDefinition = {
        requires : "widget,dialog",
        icons : widgetName.toLowerCase(),
        init : function init(editor) {
            pluginTools.addDialog(dialogName, initializeDialog);
            editor.on("receiveData", _handleAuthNotes); //LEOS-2114
            editor.widgets.add(widgetName, authorialNoteWidgetDefinition);
            editor.ui.addButton(widgetName, {
                label : "Insert Footnote",
                command : widgetName,
                toolbar : 'ref,10'
            });
            
            editor.on('selectionChange', _onSelectionChange);
        }
    };
    
    function _onSelectionChange(event) {
        leosCommandStateHandler.changeCommandState(event, widgetName, changeStateElements);
    }

    function _handleAuthNotes(evt) {
    	var editor = evt.editor;
    	var elementFragment = evt.data;  //need to pass updated data after save from server
    	editor.LEOS.lowestMarkerValue = _getLowestMarkerValue($(elementFragment).find('authorialNote'));
    	authorialNoteWidgetDefinition._renumberAuthorialNotes(editor);
    }

    function _getLowestMarkerValue(authorialNotes) {
        var markerArray =  authorialNotes.map(function() {
        	 return this.getAttribute("marker");
        }).get();
        return markerArray.length > 0 ? Math.min.apply(Math, markerArray) : 1;
    }

    function initializeDialog(editor) {
        var dialogDefinition = {
            title : "Edit Footnote",
            minWidth : 400,
            minHeight : 100,
            contents : [ {
                id : "info",
                elements : [ {
                    id : "fnote",
                    type : "textarea",
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
            } ],
            //LEOS - 1963, 2160
            onOk: function (event) {
                var selection = this._.editor.getSelection();
                selection.getRanges().forEach(
                    function loopOverRange(currentRange) {
                        currentRange.collapse(false);//take cursor to end of selected range
                    });
            }

        };
        return dialogDefinition;
    }

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
            akn : "GUID",
            html : "id"
        }, {
            akn : "leos:origin",
            html : "data-origin"
        }, {
            html : ["data-akn-name", "aknAuthorialNote"].join("=")
        } ],
        sub : {
            akn : 'mp',
            html : 'span',
            attr : [ {
                akn : "GUID",
                html : "data-akn-mp-id"
            }, {
                akn : "leos:origin",
                html : "data-mp-origin"
            }],
            sub : [ {
                akn : 'text',
                html : 'span[title]'
            } ]
        }
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    // return plugin module
    var pluginModule = {
        transformationConfig : transformationConfig,
        name : pluginName
    };
    return pluginModule;
});
