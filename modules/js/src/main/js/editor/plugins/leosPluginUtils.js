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
define(function leosPluginUtilsModule(require) {
    "use strict";

    var CKEDITOR = require("promise!ckEditor");
    var TEXT = "text";
    var BOGUS = "br";
    var UNKNOWN = "unknown";
    var ORDER_LIST_ELEMENT = "ol";
    var HTML_POINT = "li";
    var MAX_LIST_LEVEL = 5;

    function _hasTextOrBogusAsNextSibling(element){
        return (element instanceof CKEDITOR.dom.element) && element.hasNext()
            && (_getElementName(element.getNext()) === TEXT || _getElementName(element.getNext()) === BOGUS);
    }

    function _getElementName(element) {
        var elementName = UNKNOWN;
        if (element instanceof CKEDITOR.dom.element) {
            elementName = element.getName();
        } else if (element instanceof CKEDITOR.dom.text) {
            elementName = TEXT;
        } else if(element && element.localName){
            elementName = element.localName;
        }
        return elementName;
    }

    function _setFocus(element, editor){
        if(element){
            var range = editor.createRange();
            range.selectNodeContents(element);
            range.collapse(true);
            range.select();
            range.scrollIntoView();
        }
    }

    /**
     * Calculates the depth level of the selected element inside the list (ol block).
     * It counts how many ol elements are present in the hierarchy.
     */
    function _calculateListLevel(selected) {
        var level = 0;
        var actualEL = selected;
        while (_isListElement(actualEL)) {
            level++;
            actualEL = actualEL.getAscendant(ORDER_LIST_ELEMENT);
        }
        return level;
    }

    /**
     * Returns true if the selected element is child of an li or ol
     */
    function _isListElement(el) {
        if(el && (el.getAscendant(ORDER_LIST_ELEMENT) || el.getAscendant(HTML_POINT))){
            return true;
        } else {
            return false;
        }
    }

    return {
        hasTextOrBogusAsNextSibling: _hasTextOrBogusAsNextSibling,
        getElementName: _getElementName,
        setFocus: _setFocus,
        calculateListLevel: _calculateListLevel,
        MAX_LIST_LEVEL: MAX_LIST_LEVEL
    };
});
