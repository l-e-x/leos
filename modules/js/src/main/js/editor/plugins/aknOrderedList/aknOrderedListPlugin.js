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
define(function aknOrderedListPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var $ = require('jquery');
    var pluginName = "aknOrderedList";
    var leosHierarchicalElementTransformerStamp = require("plugins/leosHierarchicalElementTransformer/hierarchicalElementTransformer");
    var numberModule = require("plugins/leosNumber/listItemNumberModule");

    var NUM_GROUP = "pointsNumbering";
    var ORDER_LIST_ELEMENT = "ol";

    var pluginDefinition = {
        lang: 'en',
        init: function init(editor) {
            numberModule.init();
            editor.on("beforeAknIndentList", _resetDataNumOnIndent);
            editor.on("change", resetDataAknNameForOrderedList, null, null, 0);
            editor.on("change", resetNumbering, null, null, 1);
            initializePointsNumberingMenuItem(editor, this.path);
        }
    };

    /*
     * returns the Ordered List of the element
     */
    function getOrderedList(element){
        return element.getAscendant(ORDER_LIST_ELEMENT, true);
    }

    /*
    * initialize commands, menu group and items to Points Numbering MenuItem
    */
    function initializePointsNumberingMenuItem(editor, path) {
        //Adding Menu Groups
        editor.addMenuGroup(NUM_GROUP);

        //Create Menu
        editor.addMenuItems({
            PointsNumbering: {
                icon: path + "icons/pointsnumbering.png",
                label: editor.lang.aknOrderedList.pointsnumbering,
                group: NUM_GROUP,
                order: 1,
                getItems: function () {
                    return {
                        Alphabets: CKEDITOR.TRISTATE_OFF,
                        Roman: CKEDITOR.TRISTATE_OFF,
                        Arabic: CKEDITOR.TRISTATE_OFF,
                        IndentDash: CKEDITOR.TRISTATE_OFF
                    }
                }
            }
        });

        numberModule.getSequences().forEach(function (sequence, idx, arr) {
                editor.addMenuItem(sequence.name, {
                    label: editor.lang.aknOrderedList[sequence.type],
                    group: NUM_GROUP,
                    order: idx + 2,
                    command: sequence.name
                });

                //Numbering commands
                editor.addCommand(sequence.name, {
                    exec: function (editor) {
                        resetPointsNumbering(editor, sequence);
                    }
                });
            });


        // Listener
        if (editor.contextMenu) {
            editor.contextMenu.addListener(function (element, selection) {
                if (isValidOrderedList(element)) {
                    return {
                        PointsNumbering: CKEDITOR.TRISTATE_OFF,
                    };
                }
            });
        }
    }

    /*
  * checks if element is an ordered list
  */
    function isValidOrderedList(element) {
        var orderList = getOrderedList(element);
        return orderList.hasAttribute('data-akn-name');
    }

    /*
    * Set numbering to style, selected from Points Numbering Menu Item
    */
    function resetPointsNumbering(editor, sequence) {
        var orderedList = getOrderedList(editor.elementPath().block || editor.elementPath().blockLimit);
        numberModule.updateNumbers([orderedList.$], sequence);
    }

    //This is removing num and origin() on indent and outdent also
    function _resetDataNumOnIndent(event) {
        var editor = event.editor, range, node;
        var selection = editor.getSelection(),
            ranges = selection && selection.getRanges(),
            iterator = ranges.createIterator();

        while ((range = iterator.getNextRange())) {
            if (range.startContainer) {
                var startNode = range.startContainer.type !== CKEDITOR.NODE_TEXT && range.startContainer.getName() === "li"
                    ? range.startContainer
                    : range.startContainer.getAscendant('li');
                _handleNode(startNode);
            }
            if (range.endContainer) {
                var endNode = range.endContainer.type !== CKEDITOR.NODE_TEXT && range.endContainer.getName() === "li"
                    ? range.endContainer
                    : range.endContainer.getAscendant('li');
                _handleNode(endNode);
            }

            var rangeWalker = new CKEDITOR.dom.walker(range);
            while (node = rangeWalker.next()) {
                _handleNode(node);
            }
        }
    }

    var REGEX_ORIGIN = new RegExp("data-(\\w-?)*origin");
    function _removeOrigin(node) {
        var attrs = node instanceof CKEDITOR.dom.element
            ? node.$.attributes
            : node.attributes;
        for (var idx = 0; idx < attrs.length; idx++) {
            if (attrs[idx].name.match(REGEX_ORIGIN)) {
                node.removeAttribute(attrs[idx].name);
            }
        }
    }

    function _handleNode(node) {
        if (!node || node.type !== CKEDITOR.NODE_ELEMENT){
            return;
        }
        node.removeAttribute('data-akn-num');
        _removeOrigin(node);

        node.getChildren().toArray().forEach(_handleNode);
    }

    /*
     * Resets the numbering of the points depending on nesting level. LEOS-1487: Current implementation simply goes through whole document and renumbers all
     * ordered list items. For above reason this could cause some performance issues if so this implementation should be reconsidered.
     *
     */
    function resetNumbering(event) {
        event.editor.fire('lockSnapshot');
        var jqEditor = $(event.editor.editable().$);
        var orderedLists = jqEditor.find("*[data-akn-name='aknOrderedList']");
        numberModule.updateNumbers(orderedLists);
        event.editor.fire('unlockSnapshot');
    }

    /*
     * Returns the nesting level for given ol element
     */
    function getNestingLevelForOl(olElement) {
        var nestingLevel = -1;
        var currentOl = new CKEDITOR.dom.node(olElement);
        while (currentOl) {
            currentOl = currentOl.getAscendant(ORDER_LIST_ELEMENT);
            nestingLevel++;
        }
        return nestingLevel;
    }

    function resetDataAknNameForOrderedList(event) {
        event.editor.fire('lockSnapshot');
        var jqEditor = $(event.editor.editable().$);
        var orderedLists = jqEditor.find(ORDER_LIST_ELEMENT);
        for (var ii = 0; ii < orderedLists.length; ii++) {
            var orderedList = orderedLists[ii];
            var currentNestingLevel = getNestingLevelForOl(orderedList);
            if (currentNestingLevel > 0) {
                orderedList.setAttribute("data-akn-name", "aknOrderedList");
                var listItems = orderedList.children;
                for (var jj = 0; jj < listItems.length; jj++) {
                    listItems[jj].removeAttribute("data-akn-name");
                }
            }

        }
        event.editor.fire('unlockSnapshot');
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    var leosHierarchicalElementTransformer = leosHierarchicalElementTransformerStamp({
        firstLevelConfig : {
            akn : 'list',
            html : 'ol',
            attr : [ {
                akn : "leos:editable",
                html : "contenteditable"
            }, {
                akn : "GUID",
                html : "id"
            }, {
                akn : "leos:origin",
                html : "data-origin"
            }, {
                html : "data-akn-name=aknOrderedList"
            } ]
        },
        rootElementsForFrom : [ "list", "point" ],
        contentWrapperForFrom : "alinea",
        rootElementsForTo : [ "ol", "li" ]
    });

	var transformationConfig = leosHierarchicalElementTransformer.getTransformationConfig();

    // return plugin module
    var pluginModule = {
        name : pluginName,
        transformationConfig: transformationConfig
    };

    pluginTools.addTransformationConfigForPlugin(leosHierarchicalElementTransformer.getTransformationConfig(), pluginName);

    return pluginModule;
});