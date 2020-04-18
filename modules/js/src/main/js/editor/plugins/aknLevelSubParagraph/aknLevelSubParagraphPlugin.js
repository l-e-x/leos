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
define(function aknLevelSubParagraphPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var CKEDITOR = require("promise!ckEditor");
    var $ = require('jquery');
    var leosHierarchicalElementTransformerStamp = require("plugins/leosHierarchicalElementTransformer/hierarchicalElementTransformer");
    var leosKeyHandler = require("plugins/leosKeyHandler/leosKeyHandler");
    var renumberModule = require("plugins/leosNumber/listItemNumberModule");
    var leosPluginUtils = require("plugins/leosPluginUtils");

    var LOG = require("logger");
    var ENTER_KEY = 13;

    var pluginName = "aknLevelSubParagraph";
    var LIST_FROM_MATCH = /^(ul|ol)$/;
    var HTML_SUB_PARAGRAPH = "p";
    var HTML_PARAGRAPH = "li";
    var config = { attributes: false, childList: true, subtree: true };


    var pluginDefinition = {
        icons: pluginName.toLowerCase(),
        init : function init(editor) {
            renumberModule.init(editor);

            leosKeyHandler.on({
                editor : editor,
                eventType : 'key',
                key : ENTER_KEY,
                action : _onEnterKey
            });
            editor.on("receiveData", _startObservingAllParagraphs);
            editor.on('afterCommandExec', _restoreParagraphStructure, null, null, 0);
        }
    };

    function _onEnterKey(context) {
        LOG.debug("ENTER event intercepted: ", context.event);
        var selection = context.event.editor.getSelection();
        var startElement = leosKeyHandler.getSelectedElement(selection);

        // If we are in the first level paragraph and content is empty, it should be stopped
        // If content is not empty but the cursor is at the first character, it should NOT be stopped. LEOS-2831.
        if (leosKeyHandler.isContentEmptyTextNode(startElement) && isFirstLevelLiSelected(context)) {
            context.event.cancel();
        }
    }

    function _restoreParagraphStructure(event) {
        if (event.data.name === 'enter') {
            var selectedElement = leosKeyHandler.getSelectedElement(event.editor.getSelection());
            var isSelectedElementSubParagraph = leosPluginUtils.getElementName(selectedElement) === HTML_SUB_PARAGRAPH && isFirstLevelLi(getClosestLiElement(selectedElement));
            var isSelectedElementEmpty = leosKeyHandler.isContentEmptyTextNode(selectedElement);
            var isOnlyChild = !selectedElement.hasNext() && !selectedElement.hasPrevious();
            var hasTextNext = leosPluginUtils.hasTextOrBogusAsNextSibling(selectedElement);
            var parent = selectedElement.getParent();
            var isParentSubParagraph = leosPluginUtils.getElementName(parent) === HTML_SUB_PARAGRAPH;

            if(isSelectedElementSubParagraph && isSelectedElementEmpty && (hasTextNext || isParentSubParagraph)){
                selectedElement.insertBefore(parent);
                leosPluginUtils.setFocus(selectedElement, event.editor);
                if(isOnlyChild){
                    parent.appendBogus();
                }
            }
        }
    }

    function _startObservingAllParagraphs(event){
        var editor = event.editor;
        if(editor.editable && editor.editable().getChildren && editor.editable().getChildren().count() > 0){
            _addMutationObserverToParagraphList(_getFirstLevelOlElement(editor).$)
        }
    }

    function _addMutationObserverToParagraphList(paragraphList){
        if (paragraphList && !paragraphList.paragraphMutationObserver){
            paragraphList.paragraphMutationObserver = new MutationObserver(_processMutations);
            paragraphList.paragraphMutationObserver.observe(paragraphList, config);
        }
    }

    function _processMutations(mutationsList) {
        var mutations = _getMutations(mutationsList);
        _popSingleSubParagraphs(mutations.singleSubParagraphs);
    }

    function _popSingleSubParagraphs(singleSubParagraphs){
        singleSubParagraphs.forEach(function(subParagraph){
            if(subParagraph.getParent()){
                while(subParagraph.getChildCount() > 0){
                    subParagraph.getChild(0).insertBefore(subParagraph);
                }
                subParagraph.remove();
            }
        });
    }

    function _getMutations(mutationsList){
        var singleSubParagraphs = [];
        var isSubParagraphPushed = {};
        for(var i = 0; i < mutationsList.length; i++){
            _pushMutations(mutationsList[i].target, singleSubParagraphs, isSubParagraphPushed);
        }
        return {singleSubParagraphs: singleSubParagraphs};
    }

    function _pushMutations(node, singleSubParagraphs, isSubParagraphPushed){
        for (var i = 0; i < node.childNodes.length; i++){
            var child = node.childNodes[i];
            if(child.childNodes.length > 0){
                _pushMutations(child, singleSubParagraphs, isSubParagraphPushed);
            }
            _pushSingleSubParagraphs(node, child, singleSubParagraphs, isSubParagraphPushed);
        }
    }

    function _pushSingleSubParagraphs(node, child, singleSubParagraphs, isSubParagraphPushed){
        var isSingleSubParagraph = leosPluginUtils.getElementName(node) === HTML_PARAGRAPH && leosPluginUtils.getElementName(child) === HTML_SUB_PARAGRAPH
            && !child.previousSibling && !child.nextSibling;
        if(isSingleSubParagraph){
            var subParagraph = new CKEDITOR.dom.element(child);
            if(isFirstLevelLi(subParagraph.getParent()) && isSubParagraphPushed[subParagraph] !== 1){
                isSubParagraphPushed[subParagraph] = 1;
                singleSubParagraphs.push(subParagraph);
            }
        }
    }

    var getClosestLiElement = function getClosestLiElement(element) {
        return element.getAscendant('li', true);
    };

    var isFirstLevelLi = function isFirstLevelLi(liElement) {
        return !liElement.getAscendant('li');
    };


    var isFirstLevelLiSelected = function isFirstLevelLiSelected(context) {
        var liElement = getClosestLiElement(context.firstRange.startContainer);
        return liElement && isFirstLevelLi(liElement);
    };

    var transformSubParagraphs = function transformSubParagraphs(editor) {
        editor.fire('lockSnapshot');
        var firstLevelOlElt = _getFirstLevelOlElement(editor);
        if (firstLevelOlElt) {
            var paragraphNodes = firstLevelOlElt.getChildren();
            for (var paragraphNodeIndex=0; paragraphNodeIndex < paragraphNodes.count(); paragraphNodeIndex++) {
                var paragraphNode = paragraphNodes.getItem(paragraphNodeIndex);
                var currentParagraphNodeToBeDeleted = false;
                if (leosPluginUtils.getElementName(paragraphNode) === HTML_PARAGRAPH) {
                    var childNodes = paragraphNode.getChildren();

                    for (var childNodeIndex=0; childNodeIndex < childNodes.count(); childNodeIndex++) {
                        var currentNode = childNodes.getItem(childNodeIndex);
                        var nextNode = currentNode.hasNext() ? currentNode.getNext() : null;
                        // Empty text nodes should be removed from children
                        if ((currentNode.$.nodeType === Node.TEXT_NODE) && (currentNode.$.textContent === '')) {
                            currentNode.remove();childNodeIndex--;
                        }
                        // Default behavior: when this is a subparagraph converts it to a paragraph
                        else if ((leosPluginUtils.getElementName(currentNode) === HTML_SUB_PARAGRAPH) && (!LIST_FROM_MATCH.test(leosPluginUtils.getElementName(nextNode)))) {
                            currentNode.renameNode(HTML_PARAGRAPH);
                            currentNode.setAttribute("data-akn-name", "aknLevelSubParagraph");
                            if (childNodeIndex>0) {
                                currentNode.insertAfter(paragraphNode);childNodeIndex--;paragraphNodeIndex++;
                            }
                            //If this is the first child it should be inserted before
                            else {
                                //if current paragraph contains only sub paragraphs, the paragraph should be removed afterwards
                                if (childNodes.count() === 1) {
                                    currentParagraphNodeToBeDeleted = true;
                                }
                                currentNode.insertBefore(paragraphNode);childNodeIndex--;paragraphNodeIndex++;
                            }
                        }
                        // All other cases (except empty texts)
                        else if (!leosKeyHandler.isContentEmptyTextNode(currentNode)) {
                            if (LIST_FROM_MATCH.test(leosPluginUtils.getElementName(nextNode))) {
                                paragraphNode.append(nextNode); // If there is a list after, this list is included in the same paragraph
                            }
                        }
                    }
                    if (currentParagraphNodeToBeDeleted) {
                        paragraphNode.remove();paragraphNodeIndex--;
                        // To avoid bug of ticket LEOS-2734: when removing a selectable node, move the cursor to avoid bad positioning of it. Move cursor to the beginning of the paragrpah
                        var nodeToBeSelected = firstLevelOlElt.getFirst();
                        if (nodeToBeSelected) {
                            var rangeToSelect = editor.createRange();
                            rangeToSelect.moveToElementEditablePosition(nodeToBeSelected, false);
                            rangeToSelect.select();
                        }
                    }
                }
            }
        }
        editor.fire('unlockSnapshot');
    };

    function _getFirstLevelOlElement(editor) {
        var jqEditor = $(editor.editable().$);
        var aknLevel = jqEditor.find("*[data-akn-name='aknLevel']");
        var baseElementList = aknLevel.find(">ol");
        if (baseElementList) {
            return new CKEDITOR.dom.node(baseElementList[0]);
        }
        return null;
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    var leosHierarchicalElementTransformer = leosHierarchicalElementTransformerStamp({
        firstLevelConfig: {
            akn: 'subparagraph',
            html: 'li',
            attr: [{
                akn: "leos:editable",
                html: "contenteditable"
            }, {
                akn: "xml:id",
                html: "id"
            }, {
                akn : "leos:origin",
                html : "data-origin"
            }, {
                html: "data-akn-name=aknLevelSubParagraph"
            }]
        },
        rootElementsForFrom: ["level"],
        contentWrapperForFrom: "subparagraph",
        rootElementsForTo: ["li"]
    });

    var transformationConfig = leosHierarchicalElementTransformer.getTransformationConfig();

    // return plugin module
    var pluginModule = {
        name : pluginName,
        transformSubParagraphs: transformSubParagraphs,
        transformationConfig : transformationConfig
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    return pluginModule;
});