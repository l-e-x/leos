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
    var SPAN = "span";
    var SCAYT_MISSPELL_WORD_CLASS = "scayt-misspell-word";
    var SCAYT_GRAMM_PROBLEM_CLASS = "gramm-problem";
    var ORDERED_LIST_SELECTOR = "ol[data-akn-name='aknOrderedList']";
    var ENTER_KEY = 13;
    var TAB_KEY = 9;
    var config = { attributes: false, childList: true, subtree: true };

    var pluginDefinition = {
        lang: 'en',
        init: function init(editor) {
            numberModule.init(editor);
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
            if (leosPluginUtils.getElementName(parent) === SPAN &&
                    (parent.hasClass(SCAYT_MISSPELL_WORD_CLASS) || parent.hasClass(SCAYT_GRAMM_PROBLEM_CLASS))) {
                parent = parent.getParent();
            }
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
    
    /**
     * Add an Observer to all ordered lists (OL) present in the editor, considering as a separate list even the nested ones.
     */
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
    
    /**
     * MutationObserver callback, called after a change has been done inside an OL element.
     * @param mutationsList, array with all nodes affected by the change.
     *
     * Overall logic:
     * 1. Get actual mutations from the OL list.
     * 2. Add intro (HTML_SUB_POINT) to the OLs which to not contains an intro
     * 3. Transform singe sub-points into single points
     */
    function _processMutations(mutationsList) {
        var mutations = _getMutations(mutationsList);
       _addIntroBeforeEachList(mutations.listsWithoutIntro);
       _popSingleSubPoints(mutations.singleSubPoints);
    }
    
    /**
     * Pop out children of <p> node.
     * Before calling:
     * <p>
     *      Text node
     *      <br/>
     * </p>
     *
     * After calling with "singleSubPoints" <p>]:
     *      Text node
     *      <br/>
     *
     * @param singleSubPoints, list of <p> to be changed
     */
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
    
    /**
     * Create a new <p> in the editor and add it before the <ol> for each element of "listsWithoutIntro"
     *
     * Before calling:
     * <li>
     *      Text node
     *      <ol>
     *          <li>
     *          <li>
     *      </ol>
     * </li>
     *
     * After calling with "listsWithoutIntro" <ol>:
     * <li>
     *      <p>Text node<p>
     *      <ol>
     *          <li>
     *          <li>
     *      </ol>
     * </li>
     *
     * @param listsWithoutIntro, list of OLs to be changed
     */
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
    
    /**
     * Dom structure before calling:
     * <li>
     *  text node
     *  <br>
     *  another text node
     *  <p> </p>
     * <li>
     *
     * Dom structure after calling with "element" <p>:
     * <li>
     *  <p>
     *      text node
     *      <br>
     *      another text node
     *  </p>
     * <li>
     * @param element in the dom to be changed, <p>
     */
    function _appendAllPreviousTextNodes(element) {
        var reference = element;
        var spansToRemove = [];
        while (reference.hasPrevious()) {
            var previousElement = reference.getPrevious();
            var previousElementName = leosPluginUtils.getElementName(previousElement);
            if (previousElementName === SPAN && (previousElement.hasClass(SCAYT_MISSPELL_WORD_CLASS) || previousElement.hasClass(SCAYT_GRAMM_PROBLEM_CLASS))) {
                previousElement.getChildren().toArray().forEach(function (child) {
                    element.append(child, true);
                });
                reference = reference.getPrevious();
                spansToRemove.push(previousElement);
            } else if (previousElementName === TEXT || previousElementName === BOGUS || previousElementName === SPAN) {
                element.append(previousElement, true);
            } else {
                break;
            }
        }
        spansToRemove.forEach(function (child) {
            child.remove()
        });
    }
    
    /**
     * Get all mutations to be applied.
     * Mutations are returned as a structure {listsWithoutIntro: array, singleSubPoints: array}
     *
     * @param mutationsList, array with all nodes affected by the change.
     * @returns listsWithoutIntro: OLs without an intro (not a <p> as previous sibling),
     *          singleSubPoints: single SubPoints which will be converted later into Points
     */
    function _getMutations(mutationsList){
        var listsWithoutIntro = []; // OLs without an intro
        var isListPushed = {};      // already processed OLs
        var singleSubPoints = [];   // single SubPoints
        var isSubPointPushed = {};  // already processed SubPoints
        for(var i = 0; i < mutationsList.length; i++){
            _pushMutations(mutationsList[i].target, listsWithoutIntro, isListPushed, singleSubPoints, isSubPointPushed);
        }
        return {listsWithoutIntro: listsWithoutIntro,
                singleSubPoints: singleSubPoints};
    }
    
    /**
     * Build mutations for the actual "node" and all his children.
     *
     * @param node, element inside the OL impacted by the change
     * @param listsWithoutIntro, OLs without an intro (not a <p> as previous sibling),
     * @param isListPushed, OLs already processed
     * @param singleSubPoints, single SubPoints which will be converted later into Points
     * @param isSubPointPushed, SubPoints already processed
     */
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
    
    /**
     * Add "child" element into "listsWithoutIntro" if is not a correct OL structure.
     * Correct structure:
     * <li>
     *  <p> </p> (or Text node, or span tag)  //TODO consider avoiding anything rather than p, and normalize in a second moment with _appendAllPreviousTextNodes()
     *  <ol>
     *      <li></li>
     *      <li></li>
     *  </ol>
     * </li>
     *
     * @param child, OL to be processed
     * @param listsWithoutIntro, array where to add the OL in case is not a correct structure
     * @param isListPushed, array with already processed OLs
     */
    function _pushListsWithoutIntro(child, listsWithoutIntro, isListPushed){
        var hasNoIntro = leosPluginUtils.getElementName(child) === ORDER_LIST_ELEMENT
            && (!child.previousSibling || leosPluginUtils.getElementName(child.previousSibling) !== HTML_SUB_POINT);
        if(hasNoIntro && isListPushed[child] !== 1){
            isListPushed[child] = 1;
            listsWithoutIntro.push(child);
        }
    }
    
    /**
     * Add "child" element into "singleSubPoints" if is the only element inside a <li> node.
     * Example: Add <p> to "singleSubPoints" if the structure is as below:
     * <li>
     *     <p> </p>
     * </li>
     *
     * @param node, parent <li>
     * @param child, element <p>
     * @param singleSubPoints, single SubPoints which will be converted later into Points
     * @param isSubPointPushed, SubPoints already processed
     */
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

    function elementTagIndexProvider(element) {
        return leosPluginUtils.calculateListLevel(element) >= leosPluginUtils.MAX_LIST_LEVEL ? 1 : 0;
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
        rootElementsForFrom : [ "list", { elementTags : ["point", "indent"], elementTagIndexProvider : elementTagIndexProvider }],
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