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
define(function aknOrderedListPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var cachedSequenceForFirstLevel;
    var cachedSequenceForSecondLevel;
    var cachedSequenceForThirdLevel;
    var pluginName = "aknOrderedList";
    var cssPath = "css/aknOrderedList.css";
    var leosHierarchicalElementTransformerStamp = require("plugins/leosHierarchicalElementTransformer/hierarchicalElementTransformer");
 
    var DATA_AKN_NUM = "data-akn-num";

    var pluginDefinition = {
        init : function init(editor) {
            editor.addContentsCss(pluginTools.getResourceUrl(pluginName, cssPath));
            editor.on("change", resetDataAknNameForOrderedList, null, null, 0);
            editor.on("change", resetNumbering, null, null, 1);
        }
    };

    /*
     * For given num param return roman number literal
     */
    function romanize(num) {
        var digits = String(+num).split(""), key = [ "", "c", "cc", "ccc", "cd", "d", "dc", "dcc", "dccc", "cm", "", "x", "xx", "xxx", "xl", "l", "lx", "lxx",
                "lxxx", "xc", "", "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix" ], roman = "", i = 3;
        while (i--) {
            roman = (key[+digits.pop() + (i * 10)] || "") + roman;
        }
        return Array(+digits.join("") + 1).join("M") + roman;
    }

    /*
     * Returns the array containing literals for first level points
     */
    function generateSequenceForFirstLevel(sequenceLength) {
        var initialPrefix = "";
        var startOfAlfaNumerical = 97;
        var endOfAlfaNumerical = 123;
        var sequenceBase = endOfAlfaNumerical - startOfAlfaNumerical;
        var wholeSequence = [];
        for (var ii = 0; ii < sequenceLength; ii++) {
            wholeSequence.push(getNextSequenceLiteral(ii));
        }

        function getNextSequenceLiteral(sequenceNumber) {
            var currentSequenceBase = 0;
            var sequence = [];
            while (true) {
                var currentLetter;
                if (currentSequenceBase > 0) {
                    var currentBaseCount = parseInt(sequenceNumber / (Math.pow(sequenceBase, currentSequenceBase)));
                    if (currentBaseCount === 0) {
                        break;
                    }
                    currentLetter = String.fromCharCode(currentBaseCount + startOfAlfaNumerical - 1);
                }

                if (currentSequenceBase === 0) {
                    currentLetter = String.fromCharCode((sequenceNumber % sequenceBase) + startOfAlfaNumerical);
                }
                currentSequenceBase++;
                sequence.unshift(currentLetter);
            }
            sequence.unshift("(");
            sequence.push(")");
            return sequence.join("");
        }
        return wholeSequence;
    }

    /*
     * Returns the array containing literals for second level points
     */
    function generateSequenceForSecondLevel(sequenceLength) {
        var wholeSequence = [];
        var romanNum;
        for (var ii = 0; ii < sequenceLength; ii++) {
            romanNum = "(" + romanize(ii + 1) + ")";
            wholeSequence.push(romanNum);
        }
        return wholeSequence;
    }

    /*
     * Returns the array containing literals for third level points
     */
    function generateSequenceForThirdLevel(sequenceLength) {
        var wholeSequence = [];
        var arabicNum;
        for (var ii = 0; ii < sequenceLength; ii++) {
            arabicNum = "(" + (ii + 1) + ")";
            wholeSequence.push(arabicNum);
        }
        return wholeSequence;
    }

    /*
     * Returns the array containing literals for given sequence length and nesting level of the points
     */
    function generateSequence(sequenceLength, nestingLevel) {
        nestingLevel = (nestingLevel - 1) % 3;
        if (nestingLevel === 0) {
            if (!cachedSequenceForFirstLevel || (cachedSequenceForFirstLevel && cachedSequenceForFirstLevel.length < sequenceLength)) {
                cachedSequenceForFirstLevel = generateSequenceForFirstLevel(sequenceLength * 2);
            }
            return cachedSequenceForFirstLevel;
        } else if (nestingLevel === 1) {
            if (!cachedSequenceForSecondLevel || (cachedSequenceForSecondLevel && cachedSequenceForSecondLevel.length < sequenceLength)) {
                cachedSequenceForSecondLevel = generateSequenceForSecondLevel(sequenceLength * 2);
            }
            return cachedSequenceForSecondLevel
        } else if (nestingLevel === 2) {
            if (!cachedSequenceForThirdLevel || (cachedSequenceForThirdLevel && cachedSequenceForThirdLevel.length < sequenceLength)) {
                cachedSequenceForThirdLevel = generateSequenceForThirdLevel(sequenceLength * 2);
            }
            return cachedSequenceForThirdLevel
        }
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
        for (var ii = 0; ii < orderedLists.length; ii++) {
            var orderedList = orderedLists[ii];
            var currentNestingLevel = getNestingLevelForOl(orderedList);
            var listItems = orderedList.children;
            var sequence = generateSequence(listItems.length, currentNestingLevel);
            for (var jj = 0; jj < listItems.length; jj++) {
                sequence && listItems[jj].setAttribute(DATA_AKN_NUM, sequence[jj]);
            }
        }
        event.editor.fire('unlockSnapshot');

    }

    /*
     * Returns the nesting level for given ol element
     */
    function getNestingLevelForOl(olElement) {
        var nestingLevel = -1;
        var currentOl = new CKEDITOR.dom.node(olElement);
        while (currentOl) {
            currentOl = currentOl.getAscendant('ol');
            nestingLevel++;
        }
        return nestingLevel;
    }

    function resetDataAknNameForOrderedList(event) {
        event.editor.fire('lockSnapshot');
        var jqEditor = $(event.editor.editable().$);
        var orderedLists = jqEditor.find("ol");
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
                akn : "id",
                html : "id"
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