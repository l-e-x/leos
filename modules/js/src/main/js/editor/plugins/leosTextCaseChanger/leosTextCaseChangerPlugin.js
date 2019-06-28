/*
 * Copyright 2019 European Commission
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
define(function leosTextTransformerPluginModule(require) {

    'use strict';

    var CKEDITOR = require("promise!ckEditor");
    var pluginTools = require("plugins/pluginTools");
    var leosCommandStateHandler = require("plugins/leosCommandStateHandler/leosCommandStateHandler");
    var pluginName = "leosTextCaseChanger";


    var pluginDefinition = {
        init: function(editor) {
            var textCase = 0;
            var lastStartOffset;
            var lastText;
            var lastId;
            var currRange;
            var isLastSelection;
            var operations = [toUpperCase, toLowerCase, changeToCapitalCase];
            //editor.setKeystroke(CKEDITOR.SHIFT + 114, 'transformTextSwitch');
            
            editor.on('selectionChange', _onSelectionChange);

            // transformTextSwitch command to be used with button
            editor.addCommand('transformTextSwitch', {
                exec: function() {

                    var selection = editor.getSelection();
                    if (selection && selection.getSelectedText() && selection.getSelectedText().length > 0) {

                        checkLastSelection(selection);
                        selection.lock();
                        changeTextCase(selection, operations[textCase]);
                        selection.unlock(true);
                    }
                }
            });

            //checks the node level and call the text converter
            function changeTextCase(selection, converter) {

                var ranges = selection.getRanges();
                ranges.forEach(function(range) {
                    var rangeValues = setRangeAttributes(range);
                    var node;
                    while (node = rangeValues.walker.next()) {
                        if (node.type === CKEDITOR.NODE_TEXT && node.getText()) {
                            var text = node.getText();
                            //start node
                            if (rangeValues.startNodeId === node.getUniqueId()) {
                                var selectionPosition = selectionPositionForFirstNode(rangeValues, range, text);
                                node.setText(
                                    text.substring(0, selectionPosition.from) + converter.call(null, text.substring(selectionPosition.from, selectionPosition.to)) +
                                    text.substring(selectionPosition.to));
                            }
                            //end node
                            else if (rangeValues.endNodeId === node.getUniqueId()) {
                                if (range.endContainer.type === CKEDITOR.NODE_TEXT) { //checks if selection is ending in Text node, if yes node text till endOffset will be changed
                                    node.setText(
                                        converter.call(null, text.substring(0, range.endOffset)) +
                                        text.substring(range.endOffset));
                                } else { //if the selection is ending in element node, then endoffset will be 1 or number of element nodes between last selection node and endContainer
                                    node.setText(converter.call(null, text));
                                }
                            }
                            // nodes in between, complete text will be selected
                            else {
                                node.setText(converter.call(null, text));
                            }
                        }
                    }
                });
            }

            //returns the current range attributes
            function setRangeAttributes(range) {
                var rangeAttributes = {
                    walker: new CKEDITOR.dom.walker(range),
                    startNodeId: range.getBoundaryNodes().startNode.getUniqueId(),
                    endNodeId: range.getBoundaryNodes().endNode.getUniqueId(),
                };
                return rangeAttributes;

            }

            //returns the from and to position of selected text in first node
            function selectionPositionForFirstNode(rangeValues, range, text) {
                var to;
                var from;
                //setting the starting position of text selection in node
                if (range.startContainer.type !== CKEDITOR.NODE_TEXT) {
                    from = 0;
                } else {
                    from = range.startOffset;
                }

                //setting the last position of text selection in node
                if (rangeValues.startNodeId === rangeValues.endNodeId && range.endContainer.type === CKEDITOR.NODE_TEXT) { //and both are text
                    to = range.endOffset;
                } else {
                    to = text.length;
                }
                return {
                    from: from,
                    to: to
                };
            }

            //if there is a last selection checks if the selection is same, if yes reset the text converter counter
            function checkLastSelection(selection) {
                currRange = selection.getRanges()[0];
                if(currRange){
                if (isLastSelection) {
                    if (!(lastStartOffset === currRange.startOffset &&
                            lastText.toLocaleUpperCase() === selection.getSelectedText().toLocaleUpperCase() && lastId === selection.getStartElement().getId())) {
                        textCase = 0;
                    } else {
                        if (textCase < operations.length - 1) {
                            textCase++;
                        } else {
                            textCase = 0;
                        }
                    }
                }
                saveLastSelection(selection);
                }
            }

            //saves the last selection everytime command is executed
            function saveLastSelection(selection) {
                lastStartOffset = currRange.startOffset;
                lastText = selection.getSelectedText();
                lastId = selection.getStartElement().getId();
                isLastSelection = true;

            }

            //changes the case to capital case
            function changeToCapitalCase(text) {
                return text.replace(/\w\S*/g, function(text) {
                    return text.charAt(0).toLocaleUpperCase() + text.substring(1).toLocaleLowerCase();
                });
            }

            function toUpperCase(text) {
                return text.toLocaleUpperCase();
            }

            function toLowerCase(text) {
                return text.toLocaleLowerCase();
            }

            editor.ui.addButton('TransformTextSwitcher', {
                label: 'Change Text Case',
                command: 'transformTextSwitch',
                icon: this.path + 'images/transformSwitcher.png',
                toolbar: 'basicstyles'
            });

        }

    };
    
    function _onSelectionChange(event) {
        leosCommandStateHandler.changeCommandState(event, 'transformTextSwitch');       
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name: pluginName
    };

    return pluginModule;

});