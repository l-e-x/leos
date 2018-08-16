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
define(function listItemNumberModule(require) {
    "use strict";

    var UTILS = require("core/leosUtils");

    var DATA_AKN_NUM = "data-akn-num";
    var ORDER_LIST_ELEMENT = "ol";
    var defaultList = [];

    var sequenceMap = [
        {
            type: "alpha",
            inDefault: true,
            format: "(x)",
            name: 'Alphabets',
            generator: function generateSequenceForAlpha(list, item, idx) {
                return this.format.replace('x', generateAlpha(idx));
            }
        }, {
            type: "roman",
            inDefault: true,
            format: "(x)",
            name: 'Roman',
            generator: function generateSequenceForRoman(list, item, idx) {
                return this.format.replace('x', romanize(idx + 1));
            }
        }, {
            type: "indent",
            inDefault: true,
            format: "-",
            name: 'IndentDash',
            generator: function generateSequenceForIndent(list, item, idx) {
                return this.format.replace('x', '-');
            }
        }, {
            type: "arabic",
            inDefault: false,
            format: "(x)",
            name: 'Arabic',
            generator: function generateSequenceForArabic(list, item, idx) {
                return this.format.replace('x', idx + 1);
            }
        }
    ];

    var paragraphSequence = {
        type: "paragraph",
        inDefault: false,
        format: "x.",
        initValue: /\d+?\./,
        name: 'Paragraph',
        generator: function (list, item, idx) {
            return this.format.replace('x', idx + 1);
        }
    };

    function _getSequences(seqName) {
        if (seqName && seqName === 'Paragraph') {
            return paragraphSequence;
        }
        else if (seqName) {
            return sequenceMap.find(function(el){return el.name === seqName});
        }
        else {//return all sequences
            return sequenceMap;
        }
    }

    /*
     * For given num param return roman number literal
     */
    function romanize(num) {
        var digits = String(+num).split(""),
            key = ["", "c", "cc", "ccc", "cd", "d", "dc", "dcc", "dccc", "cm", "", "x", "xx", "xxx", "xl", "l", "lx", "lxx",
                "lxxx", "xc", "", "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix"], roman = "", i = 3;
        while (i--) {
            roman = (key[+digits.pop() + (i * 10)] || "") + roman;
        }
        return Array(+digits.join("") + 1).join("M") + roman;
    }

    /*
     * Returns the array containing literals for alpha points
     */
    function generateAlpha(sequenceNumber) {
        var startOfAlfaNumerical = 97;
        var endOfAlfaNumerical = 123;
        var sequenceBase = endOfAlfaNumerical - startOfAlfaNumerical;
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
        return sequence.join("");
    }

    /*
     * To identify the sequence first point in the list is used
     */
    function identifySequence(listItems, currentNestingLevel) {
        //Strip .,) and (.
        //To differentiate between 1ab, ab -> remove everything except first char
        //ignore whatever is not matching like  #

        //If a digit is found , it is arabic
        //if - is found it is IndentDash
        //Now check for existence for any char other than roman, if it does then it is alpha
        //if only roman numerals exist then it is roman
        //if none is found then we take default

        var nums = [];
        for (var i = 0; i < listItems.length; i++) {
            var num = listItems[i].getAttribute(DATA_AKN_NUM);
            num = num ? num.replace(/[\s\(\),\.]+?/, '') : '';
            num = num.length > 1 && num.charAt(0) === '-' ? num.substring(1) : num; //for -2ab
            nums.push(num.length > 0 ? num.charAt(0) : num);
        }
        nums = nums.filter(function (val) { return !val.match(/#/); });

        var sequence;
        for (var idx=0; idx< nums.length; idx++ ){
            var value = nums[idx];
            if (value.match(/\d/)) {
                sequence =  _getSequences('Arabic');
                break;
            } else if (value === '-') {
                sequence = _getSequences('IndentDash');
                break;
            } else if (value.match(/(?=[^ivx])([a-z])+?/)) {
                sequence = _getSequences('Alphabets');
                break;
            } else if (value.match(/[ivxcdlm]+?/)) {
                sequence = _getSequences('Roman');
                break;
            }
        }

        return sequence ? sequence: getSequenceFromDefaultList(currentNestingLevel);
    }

    function _initialize(editor) {
        _initializeDefaultList(editor);
    }

    /*
     * initialize default sequence list by sequence map
     */
    function _initializeDefaultList(editor) {
        var sequences = _getSequences();
        for (var i = 0; i < sequences.length; i++) {
            if (sequences[i].inDefault) {
                defaultList[i] = sequences[i];
            }
        }
    }

    /*
     * Get sequence type from default sequence list
     */
    function getSequenceFromDefaultList(currentNestingLevel) {
        var sequenceType = '';
        if (currentNestingLevel > defaultList.length || !defaultList[currentNestingLevel - 1]) {
            sequenceType = _getSequences('IndentDash');
            defaultList[currentNestingLevel - 1] = sequenceType;
        } else {
            sequenceType = defaultList[currentNestingLevel - 1];
        }
        return sequenceType;
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

    /*
     * Called to update numbering, changed from indent plugin or context menu
     */
    function _updateNumbers(orderedLists, seqNum) {
        for (var ii = 0; ii < orderedLists.length; ii++) {
            var orderedList = orderedLists[ii];
            var currentNestingLevel = getNestingLevelForOl(orderedList);
            var listItems = orderedList.children;
            var sequence = '';
            if (typeof seqNum === 'undefined' || !seqNum) {
                sequence = identifySequence(listItems, currentNestingLevel);
            } else {
                sequence = seqNum;
            }
            (UTILS.getElementOrigin(orderedList))
                ? _doMandateNum(orderedList, sequence)
                : _doProposalNum(orderedList, sequence);
        }
    }

    function _doProposalNum(orderedList, sequence) {
        var listItems = orderedList.children;
        for (var jj = 0; jj < listItems.length; jj++) {
            sequence && listItems[jj].setAttribute(DATA_AKN_NUM, sequence.generator(orderedList, listItems[jj], jj));
        }
    }

    function _doMandateNum(orderedList, sequence) {
        var listItems = orderedList.children;
        for (var jj = 0; jj < listItems.length; jj++) {
            if (!UTILS.getElementOrigin(listItems[jj])) {
                sequence && listItems[jj].setAttribute(DATA_AKN_NUM, sequence.format.replace('x', '#'));
            }
        }
    }

    return {
        init: _initialize,
        getSequences: _getSequences,
        updateNumbers: _updateNumbers
    };
});