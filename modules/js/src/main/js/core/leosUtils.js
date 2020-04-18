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
define(function leosUtilsModule(require) {
    "use strict";

    // load module dependencies
    require("dateFormat");
    var CKEDITOR = require("promise!ckEditor");
    var REGEX_ORIGIN = new RegExp("data-(\\w-?)*origin");
    
    var COUNCIL_INSTANCE = "COUNCIL";
    // configuration

    var PARAGRAPH_POINT_TAG = "LI";
    var SUBPARAGRAPH_SUBPOINT_TAG = "P";
    var TABLE_CELL_TAG = "TD";
    var LINE_BREAK_TAG = "BR";
    var HEADING_TAG = "H2";

    function _getParentElement(connector) {
        var element = null;
        if (connector) {
            var id = connector.getParentId();
            element = connector.getElement(id);
        }
        return element;
    }


    function _getElementOrigin(element) {
        var attrs = element instanceof CKEDITOR.dom.element ? element.$.attributes : element.attributes;
        for (var idx = 0; idx < attrs.length; idx++) {
            if (attrs[idx].name.match(REGEX_ORIGIN)) {
                return attrs[idx].value;
            }
        }
        return null;
    }

    function _registerEmptyTrimSelector(tocItemsList) {
        jQuery.extend(jQuery.expr[':'], {
            emptyTrim: function (el) {
                var elementsToBeChecked = [
                    PARAGRAPH_POINT_TAG,
                    SUBPARAGRAPH_SUBPOINT_TAG
                ];
                if (el.tagName === HEADING_TAG) {
                    var tocItem = _getParentTocItem(el, tocItemsList);
                    if (tocItem && tocItem.itemHeading === "MANDATORY") {
                        elementsToBeChecked.push(HEADING_TAG);
                    }
                }
                if (!elementsToBeChecked.includes(el.tagName)) {
                    return false;
                } else {
                    var trimmedText = $.trim(el.innerText);
                    var prevElement = $(el).prev().text();
                    if(!trimmedText && $.trim(prevElement)){
                        trimmedText = prevElement;
                    }
                    if (!trimmedText) {
                        if ((el.tagName === PARAGRAPH_POINT_TAG || el.tagName === SUBPARAGRAPH_SUBPOINT_TAG) && el.parentElement.tagName === TABLE_CELL_TAG) {
                            return false;
                        } else if (el.children.length === 1 && el.children[0].tagName === LINE_BREAK_TAG) {
                            return true;
                        } else if (el.children.length > 0 && el.children[0].tagName === HEADING_TAG) {
                            return el.children[0].innerText.trim().length === 0;
                        } else {
                            return el.children.length === 0;
                        }
                    }
                    return !trimmedText;
                }
            }
        });
    }

    function _getParentTocItem(el, tocItemsList) {
        var currentElem = el;
        var parentTocItem;
        do {
            parentTocItem = tocItemsList.find(function (elem) {
                return elem.aknTag.toLowerCase() === currentElem.parentElement.tagName.toLowerCase();
            });
            currentElem = currentElem.parentElement;
        } while (!parentTocItem && currentElem.parentElement);
        return parentTocItem;
    }

    return {
        getParentElement: _getParentElement,
        getElementOrigin : _getElementOrigin,
        registerEmptyTrimSelector: _registerEmptyTrimSelector,
        COUNCIL_INSTANCE : COUNCIL_INSTANCE
    };
});
