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
define(function recitalNumberModule(require) {
    "use strict";

    // load module dependencies
    var UTILS = require("core/leosUtils");

    var DATA_AKN_NUM = "data-akn-num";
    var NUM_ORIGIN = "data-num-origin";
    var LEFT_NUM_DELIMITER = "(";
    var RIGHT_NUM_DELIMITER = ")";

    function _initialize(editor) {
        //DO init
    }

    function _numberRecitals(event) {
        var jqEditor = $(event.editor.editable().$);
        //TODO: Put more restricted check for eg. Check for preamble/data-akn-name='recital'.
        var recitals = jqEditor.find("[data-akn-name='recitals']").first();
        var recitalList = recitals.find("*[data-akn-name='recital']");
        if (recitalList && recitals[0]) {
            UTILS.getElementOrigin(recitals[0])
                ? _doMandateNum(recitalList)
                : _doProposalNum(recitalList);
        }
    }

    function _doProposalNum($recitalList) {
        for (var index = 0; index < $recitalList.length; index++) {
            var num = index + 1;
            $recitalList[index].setAttribute(DATA_AKN_NUM, LEFT_NUM_DELIMITER + num + RIGHT_NUM_DELIMITER);
        }
    }

    function _doMandateNum($recitalList) {
        for (var index = 0; index < $recitalList.length; index++) {
            var recital = $recitalList[index];
            if(!recital.getAttribute(NUM_ORIGIN)) {
                recital.setAttribute(DATA_AKN_NUM, LEFT_NUM_DELIMITER + _getMandateNum($recitalList, recital) + RIGHT_NUM_DELIMITER);
            }
        }
    }

    function _getMandateNum($allSiblings, currElement) {
        return '#';
    }

    return {
        init: _initialize,
        numberRecitals: _numberRecitals
    };
})
;