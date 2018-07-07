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
define(function aknNumberedParagraphPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var CKEDITOR = require("promise!ckEditor");
    var leosHierarchicalElementTransformerStamp = require("plugins/leosHierarchicalElementTransformer/hierarchicalElementTransformer");

    var LOG = require("logger");
    var ENTER_KEY = 13;
    var REG_EXP_FOR_UNICODE_ZERO_WIDTH_SPACE_IN_HEX = /\u200B/g;

    var pluginName = "aknNumberedParagraph";
    var cssPath = "css/aknNumberedParagraph.css";
    var DATA_AKN_NUM = "data-akn-num";
    var cachedSequenceForParagraph;

    var pluginDefinition = {
        init : function init(editor) {
            editor.addContentsCss(pluginTools.getResourceUrl(pluginName, cssPath));
            on({
                editor : editor,
                eventType : 'key',
                key : ENTER_KEY,
                predicate : isParagraphSelected,
                action : stop
            });

            editor.on("change", function(event) {
                var jqEditor = $(event.editor.editable().$);
                var article = jqEditor.find("*[data-akn-name='article']");
                if (article.length !== 0) {
                    var olElementFromArticle = article.find(">ol");
                    if (olElementFromArticle.length === 0) {
                        article.append("<ol><li data-akn-num='1.'><br></li></ol>");
                        event.editor.getSelection().selectElement(new CKEDITOR.dom.node(article.find(">ol>li>br")[0]));
                    }
                }
            });

            editor.on("change", resetDataAknNameForOrderedList, null, null, 0);
            editor.on("change", resetNumbering, null, null, 1);

        }
    };

    /*
     * Resets the numbering of the points depending on nesting level
     */
    function resetNumbering(event) {
        event.editor.fire('lockSnapshot');
        var jqEditor = $(event.editor.editable().$);
        var paragraphs = jqEditor.find("*[data-akn-name='aknNumberedParagraph']");
        if (paragraphs.length > 0) {
            var sequence = generateSequenceForParagraph(paragraphs.length);
            for (var ii = 0; ii < paragraphs.length; ii++) {
                paragraphs[ii].setAttribute(DATA_AKN_NUM, sequence[ii]);
            }
        }
        event.editor.fire('unlockSnapshot');
    }

    /*
     * Returns the array containing literals for paragraph
     */
    function generateSequenceForParagraph(sequenceLength) {
        if (!cachedSequenceForParagraph || (cachedSequenceForParagraph && cachedSequenceForParagraph.length < sequenceLength)) {
            cachedSequenceForParagraph = generateSequenceForParagraphHelper(sequenceLength * 2);
        }
        return cachedSequenceForParagraph;
    }

    function generateSequenceForParagraphHelper(sequenceLength) {
        var wholeSequence = [];
        var arabicNum;
        for (var ii = 0; ii < sequenceLength; ii++) {
            arabicNum = ii + 1 + ".";
            wholeSequence.push(arabicNum);
        }
        return wholeSequence;
    }

    /*
     * Returns the nesting level for given ol element
     */
    function getNestingLevelForOl(olElement) {
        var nestingLevel = -1;
        var currentOl = olElement;
        while (currentOl) {
            currentOl = currentOl.getAscendant('ol');
            nestingLevel++;
        }
        return nestingLevel;
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
            var textRepresentationOfLiElement = liElement.getText().trim().replace(REG_EXP_FOR_UNICODE_ZERO_WIDTH_SPACE_IN_HEX, '');
            if (textRepresentationOfLiElement === "" || !context.firstRange.collapsed) {
                return true;
            }
        }
        return false;
    };

    var isParagraphSelected = function isParagraphSelected(context) {
        var liElement;
        if (context.firstRange) {
            return isFirstLevelLiSelected(context);
        }
        return false;

    };

    var stop = function(context) {
        context.event.cancel();
    };

    var on = function onKey(onKeyContext) {
        onKeyContext.editor.on(onKeyContext.eventType, function(event) {
            var selection = onKeyContext.editor.getSelection(), ranges = selection && selection.getRanges();
            if (selection && event.data.keyCode === onKeyContext.key) {
                var firstRange;
                if (ranges && ranges.length > 0) {
                    firstRange = ranges[0];
                }
                var context = {
                    firstRange : firstRange,
                    event : event,
                    selection : selection,
                    editor : onKeyContext.editor
                };
                var predicateResult = onKeyContext.predicate(context);

                if (predicateResult) {
                    LOG.debug("Intercepted on event key with context: ", context);
                    onKeyContext.action(context);
                }
            }

        });
    };

    function getClosestOlAncestor(selection) {
        var commonElementAncestor = selection.getCommonAncestor();
        commonElementAncestor = commonElementAncestor && commonElementAncestor.getAscendant("ol", true);
        return commonElementAncestor;
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
                akn: "id",
                html: "id"
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
        transformationConfig : transformationConfig
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    return pluginModule;
});