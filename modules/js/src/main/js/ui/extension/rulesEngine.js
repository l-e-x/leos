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
define(function rulesEngineModule(require) {
    "use strict";

    let log = require("logger");

    function _getNodeExplicitType(nodeType) {
        switch (nodeType) {
            case Node.TEXT_NODE:
                return "text";
            case Node.ELEMENT_NODE:
                return "element";
            default:
                return "other";
        }
    }

    function _processElement(engineRules, element, ...args) {
        _processChildren(engineRules, element, ...args);
        _applyRules(engineRules, element, ...args);
    }

    function _processChildren(engineRules, element, ...args) {
        let childNodes = Array.prototype.slice.call(element.childNodes);
        if (childNodes && childNodes.length > 0) {
            childNodes.forEach(function (node) {
                _processElement(engineRules, node, ...args);
            }, this);
        }
    }

    function _applyRules(engineRules, element, ...args) {
        let rules = engineRules[_getNodeExplicitType(element.nodeType)];
        if (rules) {
            for (let rule of Object.keys(rules)) {
                try {
                    if (rule !== '$' && element.matches(rule)) {//using rule name as selector
                        rules[rule].apply(element, args);
                    }
                } catch (e) {
                    log.debug("Error in rule:" + rule.toString() + " - " + e);
                }
            }
            //calling default rule $
            if (rules.$) {
                rules.$.apply(element, args);
            }
        }
    }

    return {

        process: _processElement
    };
});