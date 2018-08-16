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
define(function aknNumberedParagraphPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var CKEDITOR = require("promise!ckEditor");
    var $ = require('jquery');
    var leosHierarchicalElementTransformerStamp = require("plugins/leosHierarchicalElementTransformer/hierarchicalElementTransformer");
    var leosKeyHandler = require("plugins/leosKeyHandler/leosKeyHandler");
    var renumberModule = require("plugins/leosNumber/listItemNumberModule");

    var LOG = require("logger");
    var ENTER_KEY = 13;

    var pluginName = "aknNumberedParagraph";
    var DATA_AKN_NAME = "data-akn-name";
    var DATA_AKN_NUM = "data-akn-num";
    var DATA_AKN_NUM_ID = "data-akn-num-id";
    var LIST_FROM_MATCH = /^(ul|ol)$/;
    var HTML_SUB_PARAGRAPH = "p";
    var HTML_PARAGRAPH = "li";

    var CMD_NAME = "aknNumberedParagraph";
    var NUMBERED = CKEDITOR.TRISTATE_ON, UNNUMBERED = CKEDITOR.TRISTATE_OFF;
    var PARA_MODE = NUMBERED;
    var PARA_SELECTOR = "*[data-akn-name='aknNumberedParagraph']";

    var pluginDefinition = {
        icons: pluginName.toLowerCase(),
        init : function init(editor) {
            
            editor.ui.addButton(pluginName, {
                label: 'Paragraph mode',
                command: CMD_NAME,
                toolbar: 'paragraphmode'
            });
            
            var paraCommand = editor.addCommand(CMD_NAME, {
                exec: function(editor) {
                   _changeParaMode(this, editor);
                }
            });
            
            leosKeyHandler.on({
                editor : editor,
                eventType : 'key',
                key : ENTER_KEY,
                action : _onEnterKey
            });

            editor.on("change", function(event) {
                var jqEditor = $(event.editor.editable().$);
                var article = jqEditor.find("*[data-akn-name='article']");
                if (article.length !== 0) {
                    var olElementFromArticle = article.find(">ol");
                    if (olElementFromArticle.length === 0) {
                        //TODO
                        article.append("<ol><li data-akn-num='1.'><br></li></ol>");
                        event.editor.getSelection().selectElement(new CKEDITOR.dom.node(article.find(">ol>li>br")[0]));
                    }
                }
            });

            editor.on("change", resetDataAknNameForOrderedList, null, null, 0);
            editor.on("change", resetNumbering, null, null, 1);
            editor.on("focus", _setCurrentParaMode, null, paraCommand);
            editor.on("dataReady", _setCurrentParaMode, null, paraCommand);
            //Removing the unwanted bogus element inserted from ckeditor's enterkey plugin to restore the list structure
            editor.on('afterCommandExec', function(e) {
                if (e.data.name === 'enter') {
                    var element = e.editor.getSelection().getStartElement();
                    if (element && element.hasAttribute(DATA_AKN_NAME) && element.getAttribute(DATA_AKN_NAME) === CMD_NAME) {
                        if (element.getPrevious() && element.getPrevious().getBogus())
                            element.getPrevious().getBogus().remove();
                    }
                }
            });
        }
    };
    
    function _onEnterKey(context) {
        LOG.debug("ENTER event intercepted: ", context.event);
        var selection = context.event.editor.getSelection();
        var startElement = leosKeyHandler.getSelectedElement(selection);

        // If we are in the first level paragraph and content is empty, it should be stopped
        // If content is not empty but the cursor is at the first character, it should be stopped as well to avoid empty paragraphs
        if ((leosKeyHandler.isContentEmptyTextNode(startElement) && isFirstLevelLiSelected(context))
                || (!leosKeyHandler.isContentEmptyTextNode(startElement) && context.firstRange.startOffset == 0)) {
            context.event.cancel();
        }
        // Specific case when enter is pressed in an empty 'p' embedded in a paragraph, cursor is moved at the end of the 'p' element
        // , then CKEDITOR 'enterkey' plugin will not remove the entire paragraph.
        // Or if the cursor position is at the end of paragraph, then list structure will be restored
        else if (_isEnterAllowedInThisContext(startElement) || _isPostionAtEndofParagraph(startElement, selection) ) {
            var rangeToSelect = context.event.editor.createRange();
            rangeToSelect.setStartAfter(startElement);
            rangeToSelect.setEndAfter(startElement);
            rangeToSelect.select();
        }
        // Else handled by 'enterkey' CKEDITOR plugin
    }

    //Enter is allowed when cursor is in an empty subparagraph
    function _isEnterAllowedInThisContext(startElement) {
        var currentElement = startElement.$;

        if (leosKeyHandler.isContentEmptyTextNode(startElement)) {
            do {
                if (currentElement.parentNode) {
                    var elementName = currentElement.nodeName.toLowerCase();
                    var parentElement = currentElement.parentNode;
                    var parentName = parentElement.nodeName.toLowerCase();
                    if ((elementName === HTML_SUB_PARAGRAPH) && (parentName === HTML_PARAGRAPH) && PARA_MODE == NUMBERED) {
                        return true;
                    }
                }
            } while (currentElement = currentElement.parentNode);
        }
        return false;
    };

    // Checks if cursor position is at the end of paragraph
    function _isPostionAtEndofParagraph(startElement, selection) {
        var parentElement = startElement.getParent();
        if (parentElement && parentElement.hasAttribute(DATA_AKN_NAME) && parentElement.getAttribute(DATA_AKN_NAME) === CMD_NAME
                && selection.getRanges()[0].checkEndOfBlock()) {
            return true;
        }
    }

    //This method sets the current paragraph mode (Numbered/Un-numbered) to the command state.
    function _setCurrentParaMode(event) {
        var cmd = event.listenerData;
        var jqEditor = $(event.editor.editable().$);
        var paragraphs = jqEditor.find(PARA_SELECTOR);
        if(paragraphs.length > 0) {
           PARA_MODE = paragraphs[0].getAttribute(DATA_AKN_NUM) != null ? NUMBERED : UNNUMBERED;
        }
        cmd.setState(PARA_MODE);
    }
    
    //This method toggle the existing paragraph mode (Numbered -> Un-numbered & vice-versa) based on user input.
    function _changeParaMode(cmd, editor) {
        PARA_MODE = cmd.state === NUMBERED ? UNNUMBERED : NUMBERED;
        cmd.setState(PARA_MODE);
        if (PARA_MODE === UNNUMBERED) {
            transformSubparagraphs(editor);
        }
        editor.fire("change");
    }

    /*
     * Resets the numbering of the points depending on nesting level
     */
    function resetNumbering(event) {
        event.editor.fire('lockSnapshot');
        var jqEditor = $(event.editor.editable().$);
        var paragraphs = jqEditor.find(PARA_SELECTOR);
        if (paragraphs.length > 0) {
            if(PARA_MODE === NUMBERED) {
                renumberModule.updateNumbers([paragraphs[0].parentElement], renumberModule.getSequences('Paragraph'));
            } else {
                for (var ii = 0; ii < paragraphs.length; ii++) {
                    paragraphs[ii].removeAttribute(DATA_AKN_NUM);
                    paragraphs[ii].removeAttribute(DATA_AKN_NUM_ID);
                }
            }
        }
        event.editor.fire('unlockSnapshot');
    }

     var getClosestLiElement = function getClosestLiElement(element) {
        var liElement = element.getAscendant('li', true);
        return liElement;
    };

    var isFirstLevelLi = function isFirstLevelLi(liElement) {
        return !liElement.getAscendant('li');
    };

    var isFirstLevelOl = function isFirstLevelOl(olElement) {
        return !olElement.getAscendant('ol');
    };

    var isFirstLevelLiSelected = function isFirstLevelLiSelected(context) {
        var liElement = getClosestLiElement(context.firstRange.startContainer);
        if (liElement && isFirstLevelLi(liElement)) {
            return true;
        }
        return false;
    };

    function getClosestOlAncestor(selection) {
        var commonElementAncestor = selection.getCommonAncestor();
        commonElementAncestor = commonElementAncestor && commonElementAncestor.getAscendant("ol", true);
        return commonElementAncestor;
    }

    // This method transforms subparagraphs into paragraphs when included in unnumbered paragraphs: ol/li/p to ol/li
    var transformSubparagraphs = function transformSubparagraphs(editor) {
        // transforms subparagraphs to paragraphs 
        editor.fire('lockSnapshot');
        var firstLevelOlElt = _getFirstLevelOlElement(editor);
        if (firstLevelOlElt) {
            var paragraphNodes = firstLevelOlElt.getChildren();
            for (var paragraphNodeIndex=0; paragraphNodeIndex < paragraphNodes.count(); paragraphNodeIndex++) {
                var paragraphNode = paragraphNodes.getItem(paragraphNodeIndex);
                var currentParagraphNodeToBeDeleted = false;
                if (_getElementName(paragraphNode) == HTML_PARAGRAPH) {
                    var childNodes = paragraphNode.getChildren();
                    var currentParagraphNodeIndex = paragraphNodeIndex;

                    for (var childNodeIndex=0; childNodeIndex < childNodes.count(); childNodeIndex++) {
                        var currentNode = childNodes.getItem(childNodeIndex);
                        var nextNode = currentNode.hasNext() ? currentNode.getNext() : null;
                        // Empty text nodes should be removed from children
                        if ((currentNode.$.nodeType == Node.TEXT_NODE) && (currentNode.$.textContent == '')) {
                            currentNode.remove();childNodeIndex--;
                        }
                        // Default behavior: when this is a subparagraph converts it to a paragraph
                        else if ((_getElementName(currentNode) === HTML_SUB_PARAGRAPH) && (!LIST_FROM_MATCH.test(_getElementName(nextNode)))) {
                            currentNode.renameNode(HTML_PARAGRAPH);
                            currentNode.setAttribute("data-akn-name", "aknNumberedParagraph");
                            if (childNodeIndex>0) {
                                currentNode.insertAfter(paragraphNode);childNodeIndex--;paragraphNodeIndex++;
                            }
                            //If this is the first child it should be inserted before
                            else {
                                //if current paragraph contains only sub paragraphs, the paragraph should be removed afterwards
                                if (childNodes.count() == 1) {
                                    currentParagraphNodeToBeDeleted = true;
                                }
                                currentNode.insertBefore(paragraphNode);childNodeIndex--;paragraphNodeIndex++;
                            }
                        }
                        // All other cases (except empty texts)
                        else if (!leosKeyHandler.isContentEmptyTextNode(currentNode)) {
                            if (LIST_FROM_MATCH.test(_getElementName(nextNode))) {
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
    
        function _getFirstLevelOlElement(editor) {
            var jqEditor = $(editor.editable().$);
            var article = jqEditor.find("*[data-akn-name='article']");
            var baseElementList = article.find(">ol");
            if (baseElementList) {
                return new CKEDITOR.dom.node(baseElementList[0]);
            }
            return null;
        }

        // Checks if an element is a text element or an inline element
        function _isTextOrInlineElement(element) {
            var elementName = _getElementName(element);
            return ((elementName === 'text') || CKEDITOR.dtd.$inline.hasOwnProperty(elementName));
        }

        function _getElementName(element) {
            var elementName = null;
            if (element instanceof CKEDITOR.dom.element) {
                elementName = element.getName();
            } else if (element instanceof CKEDITOR.dom.text) {
                elementName = "text";
            } else {
                elementName = "unknown";
            }
            return elementName;
        }
    }

    function resetDataAknNameForOrderedList(event) {
        event.editor.fire('lockSnapshot');
        var closestOlAncestor = getClosestOlAncestor(event.editor.getSelection());
        if (closestOlAncestor) {
            var firstLevelLi = true;
            closestOlAncestor.forEach && closestOlAncestor.forEach(function(currentNode) {
                if (firstLevelLi && currentNode.getAscendant("li")) {
                    firstLevelLi = false;
                }
                var currentNodeName = currentNode && currentNode.getName && currentNode.getName();
                if (currentNodeName === "ol" && firstLevelLi) {
                    currentNode.removeAttribute("data-akn-name");
                }
                if (currentNodeName === "li" && firstLevelLi) {
                    currentNode.setAttribute("data-akn-name", "aknNumberedParagraph");
                    // returning false so the iterator won't go to its children
                    return false;
                }
            });
        }
        event.editor.fire('unlockSnapshot');
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);
    
    var leosHierarchicalElementTransformer = leosHierarchicalElementTransformerStamp({
        firstLevelConfig: {
            akn: 'paragraph',
            html: 'li',
            attr: [{
                akn: "leos:editable",
                html: "contenteditable"
            }, {
                akn: "GUID",
                html: "id"
            }, {
                akn : "leos:origin",
                html : "data-origin"
            }, {
                html: "data-akn-name=aknNumberedParagraph"
            }]
        },
        rootElementsForFrom: ["paragraph"],
        contentWrapperForFrom: "subparagraph",
        rootElementsForTo: ["li"]
    });
    
    var transformationConfig = leosHierarchicalElementTransformer.getTransformationConfig();
    
    // return plugin module
    var pluginModule = {
        name : pluginName,
        transformSubparagraphs: transformSubparagraphs,
        transformationConfig : transformationConfig
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    return pluginModule;
});