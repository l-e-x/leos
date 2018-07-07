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
define(function leosInlineCancelDialog(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var LOG = require("logger");
    var pluginTools = require("plugins/pluginTools");

    var dialogDefinition = {
        dialogName : "leosInlineCancelDialog"
    };

    dialogDefinition.initializeDialog = function initializeDialog(editor) {

        var msg = "<span>There are some unsave changes. Are you sure to continue? </span>";

        var dialogDefinition = {
            title : "Confirm cancel editing.",
            minWidth : 400,
            minHeight : 50,
            contents : [ {
                id : 'tab1',
                elements : [ {
                    id : "cancel",
                    type : 'hbox',
                    className : 'crDialogbox',
                    widths : [ '100%' ],
                    height : 50,
                    children : [ {
                        type : 'html',
                        html : msg
                    }

                    ]
                } ]
            } ],
            onOk: function(event) {
                event.sender.hide();
                editor.fire('close');
            }
            

        };
        return dialogDefinition;
    };

    return dialogDefinition;

});
