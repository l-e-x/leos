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
define(function leosNonEditablePluginModule(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var LOG = require("logger");
    var pluginTools = require("plugins/pluginTools");
    
    var pluginName = "leosNonEditable";

    var pluginDefinition = {
        icons: pluginName.toLowerCase(),

        init: function init(editor) {
            var command = editor.addCommand('leosNonEditable', commandDefinition);

            editor.ui.addButton && editor.ui.addButton('LeosNonEditable', {
                label: 'Non-Editable',
                command: 'leosNonEditable',
                toolbar: 'others'
            });
        }
    };

    var commandDefinition = {

         exec: function executeCommandDefinition(editor) {
            var selection = editor.getSelection();
            var ranges = selection.getRanges();
            var element = selection.getStartElement();
            var inner = false;
            
            if (element) {
                if (ranges[0] && !ranges[0].collapsed) {
                    // If the range is not collapsed but only single element is selected
                    inner = ranges[0].checkBoundaryOfElement(element, CKEDITOR.END);
                    if (!inner) {
                        inner = ranges[0].getTouchedStartNode().equals(ranges[0].getTouchedEndNode()) ? true : false;
                    }
                }
                if (ranges[0].collapsed || inner) {
                    this.toggleNonEditable(element);
                } else {
                    // If the range is not collapsed we need to find out all the nodes in the selection
                    var enclosedNodes = this.checkAllElementsWithClosingTagInRange(ranges[0]);
                    if (enclosedNodes.length > 0) {
                        var that = this; // keep the context for inner functions
                        enclosedNodes.forEach(function(element) {
                            that.toggleNonEditable(element);
                        })
                    }
                }
            }
        },
        
        toggleNonEditable: function toggleNonEditable(element) {
            var CONTENT_EDITABLE = "contenteditable"
            var contentEditable = element.getAttribute(CONTENT_EDITABLE);
            if (!contentEditable || contentEditable === "" || contentEditable === "true") {
                element.setAttribute(CONTENT_EDITABLE, "false");
            } else {
                element.removeAttribute(CONTENT_EDITABLE);
            }
        },

        checkAllElementsWithClosingTagInRange: function checkAllElementsWithClosingTagInRange(range) {
            var walker = new CKEDITOR.dom.walker(range);
            var isBogus = CKEDITOR.dom.walker.bogus();
            var that = this; // keep the context for inner functions
            var out = [];

            walker.guard = function(node) {
                if (!isBogus(node) && node.type == CKEDITOR.NODE_ELEMENT) {
                    if (that.isUnique(out, node)) { // Walker traverse a node when entering and exiting a node so to avoid duplicate nodes to be added to array
                        out.push(node);
                    }
                }
                return true;
            };
            walker.checkForward();
            return out;
        },
        
        //To avoid duplicates
        isUnique: function isUnique(arr, node) {
            var unique = false;
            if (arr.length > 0) {
                arr.forEach(function(element) {
                    unique = element.getUniqueId() === node.getUniqueId() ? false : true;
                })
            } else {
                unique = true;
            }
            return unique;
        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name: pluginName,
        commandDefinition: commandDefinition
    };

    return pluginModule;
});