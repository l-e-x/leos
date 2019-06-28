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
; // jshint ignore:line
define(function aknOrderedListPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var $ = require('jquery');
    var CKEDITOR = require("promise!ckEditor");
    var pluginName = "aknOrderedList";
    var leosHierarchicalElementTransformerStamp = require("plugins/leosHierarchicalElementTransformer/hierarchicalElementTransformer");
    var numberModule = require("plugins/leosNumber/listItemNumberModule");
    var leosKeyHandler = require("plugins/leosKeyHandler/leosKeyHandler");
    var leosPluginUtils = require("plugins/leosPluginUtils");

    var NUM_GROUP = "pointsNumbering";
    var ORDER_LIST_ELEMENT = "ol";
    var HTML_SUB_POINT = "p";
    var HTML_POINT = "li";
    var BOGUS = "br";
    var TEXT = "text";
    var ORDERED_LIST_SELECTOR = "ol[data-akn-name='aknOrderedList']";
    var ENTER_KEY = 13;
    var TAB_KEY = 9;
    var config = { attributes: false, childList: true, subtree: true };

    var pluginDefinition = {
        lang: 'en',
        init: function init(editor) {
            numberModule.init();
            editor.on("beforeAknIndentList", _resetDataNumOnIndent);
            editor.on("change", resetDataAknNameForOrderedList, null, null, 0);
            editor.on("change", resetNumbering, null, null, 1);
            editor.on("change", _startObservingAllLists);
            editor.on("receiveData", _startObservingAllLists);
            editor.on('afterCommandExec', _restoreListStructure, null, null, 1);
            leosKeyHandler.on({
                editor : editor,
                eventType : 'key',
                key : ENTER_KEY,
                action : _onEnterKey
            });
            leosKeyHandler.on({
                editor : editor,
                eventType : 'key',
                key : TAB_KEY,
                action : _onTabKey
            });
        }
    };

    function _onEnterKey(context) {
        var selection = context.event.editor.getSelection();
        var selectedElement = leosKeyHandler.getSelectedElement(selection);

        // If we are in an empty sub-point it should be stopped because enterKey plugin is out-denting the whole point
        if (leosKeyHandler.isContentEmptyTextNode(selectedElement)
            && leosPluginUtils.getElementName(selectedElement) === HTML_SUB_POINT
            && leosPluginUtils.getElementName(selectedElement.getParent()) === HTML_POINT
            && _getAscendantPoint(selectedElement.getParent())) {
            context.event.cancel();
        }
    }

    function _onTabKey(context) {
        var selection = context.event.editor.getSelection();
        var selectedElement = leosKeyHandler.getSelectedElement(selection);

        var actualLevel = leosPluginUtils.calculateListLevel(selectedElement);
        if (actualLevel > leosPluginUtils.MAX_LIST_LEVEL){
            context.event.cancel();
        }
    }

    //Fix ckeditor enterKey plugin's behaviour when enter is pressed at the end of a sub-point and restore the point structure
    function _restoreListStructure(event) {
        if (event.data.name === 'enter') {
            var selectedElement = leosKeyHandler.getSelectedElement(event.editor.getSelection());
            var isSelectedElementEmpty = leosKeyHandler.isContentEmptyTextNode(selectedElement);
            var isOnlyChild = !selectedElement.hasNext() && !selectedElement.hasPrevious();
            var hasTextNext = leosPluginUtils.hasTextOrBogusAsNextSibling(selectedElement);
            var parent = selectedElement.getParent();
            var isParentSubPoint = leosPluginUtils.getElementName(parent) === HTML_SUB_POINT;
            var point = isParentSubPoint ? parent.getParent() : parent;
            var isSelectedElementInsidePoint = (leosPluginUtils.getElementName(point) === HTML_POINT) && _getAscendantPoint(point);
            var isSelectedElementSubPoint = isSelectedElementInsidePoint && leosPluginUtils.getElementName(selectedElement) === HTML_SUB_POINT;

            if(isSelectedElementSubPoint && isSelectedElementEmpty && (hasTextNext || isParentSubPoint)){
                var listParent = point.getParent();
                var listParentHasIntro = listParent.getPrevious(); //if not, it means that enter was pressed in the last sub-point before the list
                selectedElement.insertBefore(listParentHasIntro ? parent : listParent);
                leosPluginUtils.setFocus(selectedElement, event.editor);
                if(isOnlyChild){
                    parent.appendBogus();
                }
            }
        }
    }

    function _startObservingAllLists(event){
        var editor = event.editor;
        if(editor.editable && editor.editable().getChildren && editor.editable().getChildren().count() > 0){
            _addMutationObserverToLists(editor.editable().getChildren().getItem(0).find(ORDERED_LIST_SELECTOR).$)
        }
    }

    function _addMutationObserverToLists(listsNodeList){
        for (var i = 0; i < listsNodeList.length; i++){
            var list = listsNodeList[i];
            if (!list.listMutationObserver){
                list.listMutationObserver = new MutationObserver(_processMutations);
                list.listMutationObserver.observe(list, config);
            }
        }
    }

    function _processMutations(mutationsList) {
        var mutations = _getMutations(mutationsList);
       _addIntroBeforeEachList(mutations.listsWithoutIntro);
       _popSingleSubPoints(mutations.singleSubPoints);
    }

    function _popSingleSubPoints(singleSubPoints){
        singleSubPoints.forEach(function(subPoint){
            if(subPoint.getParent()){
                while(subPoint.getChildCount() > 0){
                    subPoint.getChild(0).insertBefore(subPoint);
                }
                subPoint.remove();
            }
        });
    }

    function _addIntroBeforeEachList(listsWithoutIntro){
        listsWithoutIntro.forEach(function(list){
            if(list.parentNode){
                var intro = new CKEDITOR.dom.element(HTML_SUB_POINT);
                list.parentNode.insertBefore(intro.$, list);
                _appendAllPreviousTextNodes(intro);
                if(intro.getChildCount() === 0){
                    intro.appendBogus();
                }
            }
        });
    }

    function _appendAllPreviousTextNodes(element){
        while(element.hasPrevious()){
            var previousElement = element.getPrevious();
            var previousElementName = leosPluginUtils.getElementName(previousElement);
            if(previousElementName === TEXT || previousElementName === BOGUS){
                element.append(previousElement, true);
            } else {
                break;
            }
        }
    }

    function _getMutations(mutationsList){
        var listsWithoutIntro = [];
        var isListPushed = {};
        var singleSubPoints = [];
        var isSubPointPushed = {};
        for(var i = 0; i < mutationsList.length; i++){
            _pushMutations(mutationsList[i].target, listsWithoutIntro, isListPushed, singleSubPoints, isSubPointPushed);
        }
        return {listsWithoutIntro: listsWithoutIntro,
                singleSubPoints: singleSubPoints};
    }

    function _pushMutations(node, listsWithoutIntro, isListPushed, singleSubPoints, isSubPointPushed){
        for (var i = 0; i < node.childNodes.length; i++){
            var child = node.childNodes[i];
            if(child.childNodes.length > 0){
                _pushMutations(child, listsWithoutIntro, isListPushed, singleSubPoints, isSubPointPushed);
            }
            _pushListsWithoutIntro(child, listsWithoutIntro, isListPushed);
            _pushSingleSubPoints(node, child, singleSubPoints, isSubPointPushed);
        }
        _pushListsWithoutIntro(node, listsWithoutIntro, isListPushed);
    }

    function _pushListsWithoutIntro(child, listsWithoutIntro, isListPushed){
        var hasNoIntro = leosPluginUtils.getElementName(child) === ORDER_LIST_ELEMENT
            && (!child.previousSibling || leosPluginUtils.getElementName(child.previousSibling) !== HTML_SUB_POINT);
        if(hasNoIntro && isListPushed[child] !== 1){
            isListPushed[child] = 1;
            listsWithoutIntro.push(child);
        }
    }

    function _pushSingleSubPoints(node, child, singleSubPoints, isSubPointPushed){
        var isSingleSubPoint = leosPluginUtils.getElementName(node) === HTML_POINT && leosPluginUtils.getElementName(child) === HTML_SUB_POINT
            && !child.previousSibling && !child.nextSibling;
        if(isSingleSubPoint){
            var subPoint = new CKEDITOR.dom.element(child);
            if(_getAscendantPoint(subPoint.getParent()) && isSubPointPushed[subPoint] !== 1){
                isSubPointPushed[subPoint] = 1;
                singleSubPoints.push(subPoint);
            }
        }
    }

    /*
     * returns the Ordered List of the element
     */
    function getOrderedList(element){
        return element.getAscendant(ORDER_LIST_ELEMENT, true);
    }

    function _getAscendantPoint(element) {
        return element.getAscendant(HTML_POINT);
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
        var orderedLists = jqEditor.find(ORDERED_LIST_SELECTOR);
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
                akn : "xml:id",
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