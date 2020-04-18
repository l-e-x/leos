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
define(function SoftActionsExtensionModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var $ = require("jquery");
    var UTILS = require("core/leosUtils");
    var contentScroller = require("contentScroller");

    var SOFT_MOVE_TO_SELECTOR = "[leos\\:softmove_to][leos\\:softactionroot='true']";
    var SOFT_MOVE_FROM_SELECTOR = "[leos\\:softmove_from][leos\\:softactionroot='true']";
    var SOFT_MOVE_TO_ATTR = "leos:softmove_to";
    var SOFT_MOVE_FROM_ATTR = "leos:softmove_from";
    var SOFT_MOVE_LABEL_ATTR = "leos:softmove_label";
    var xmlid = "id";
    var NUM = "num";
    var CONTENT = "content";
    var AKNP = "aknp";
    var SOFT_MOVE_PLACEHOLDER_ID_PREFIX = "moved_";
    var DOUBLE_COMPARE_PREFIX = "doubleCompare-";

    var SOFT_MOVE_LABEL_STYLE = "leos-soft-move-label";
    var TRANSPARENT_SOFT_MOVE_LABEL_STYLE = "leos-transparent-soft-move-label";
    var ELEMENTS_WITH_CENTERED_NUMBER = ["article", "section", "chapter", "akntitle", "part"];
    
    var DOUBLE_COMPARE_CLASS = "leos-double-comparison-content";

    function _initSoftActions(connector) {
        log.debug("Initializing Soft Actions extension...");

        // restrict scope to the extended target
        connector.target = UTILS.getParentElement(connector);

        log.debug("Registering Soft Actions unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;

        log.debug("Registering Soft Actions state change listener...");
        connector.onStateChange = _connectorStateChangeListener;
    }

    // direction should be "FROM" or "TO"
    function _displaySoftMoveLabelForDirection(direction, target) {
        var moveItems = $(target).find(eval("SOFT_MOVE_" + direction.toUpperCase() + "_SELECTOR"));
        var parentElement = target;
        moveItems.each(function(i, moveItem) {
            if (moveItem.hasAttribute(SOFT_MOVE_LABEL_ATTR)) {
                var label = moveItem.getAttribute(SOFT_MOVE_LABEL_ATTR);
                var id = moveItem.getAttribute(xmlid);
                id = _resolveId(id, direction, parentElement);
                var nums = $(moveItem).children(NUM);
                var contents = $(moveItem).children(CONTENT);
                if (nums.length > 0) {
                    var num = nums[0];
                    var $num = $(num);
                    if($num.children("." + SOFT_MOVE_LABEL_STYLE).length === 0) {
                        num.appendChild(_createMoveLabel(id, label, parentElement));
                        if (ELEMENTS_WITH_CENTERED_NUMBER.includes($(moveItem)[0].localName)) {
                            // These elements need to have the number displayed centered
                            // so we also need to add a transparent dummy label with the same content before the number
                            // this way the actual number will be still displayed in the center
                            var transparentLabel = document.createElement("span");
                            transparentLabel.setAttribute("class", TRANSPARENT_SOFT_MOVE_LABEL_STYLE);
                            transparentLabel.innerHTML = label;
                            num.insertBefore(transparentLabel, num.childNodes[0]);
                        }
                    }
                } else if (contents.length > 0) {
                    var content = contents[0];
                    var aknp = content.firstChild;
                    if (aknp && aknp.localName === AKNP) {
                        var $aknp = $(aknp);
                        if($aknp.children("." + SOFT_MOVE_LABEL_STYLE).length === 0) {
                            aknp.insertBefore(_createMoveLabel(id, label, parentElement), aknp.childNodes[0]);
                        }
                    }
                } else {
                    var aknp = moveItem.firstChild;
                    if (aknp && aknp.localName === AKNP) {
                        var $aknp = $(aknp);
                        if($aknp.children("." + SOFT_MOVE_LABEL_STYLE).length === 0) {
                            aknp.insertBefore(_createMoveLabel(id, label, parentElement), aknp.childNodes[0]);
                        }
                    }
                }
            }
        }, this);
    }
    
    function _createMoveLabel(id, label, target) {
        var span = document.createElement("span");
        span.setAttribute("class", SOFT_MOVE_LABEL_STYLE);
        span.innerHTML = label;
        span.setAttribute("title", label);
        span.onclick = function(event) {
            _navigateToMovedElement(id, target);
        };
        return span;
    }
    
    function _resolveId(id, direction, parentElement) {
        var id;
        if(parentElement != null && parentElement.className.indexOf(DOUBLE_COMPARE_CLASS) === -1) { //in content pane
           id = (direction === "FROM") ?  SOFT_MOVE_PLACEHOLDER_ID_PREFIX + id : id.replace(SOFT_MOVE_PLACEHOLDER_ID_PREFIX, "");
        } else { // in double comparison
            id = (direction === "FROM") ?  DOUBLE_COMPARE_PREFIX + SOFT_MOVE_PLACEHOLDER_ID_PREFIX + id.replace(DOUBLE_COMPARE_PREFIX, "")
                    : id.replace(SOFT_MOVE_PLACEHOLDER_ID_PREFIX, "");
        }
        return id;
    }
    
    function _displaySoftMoveLabels(target) {
        _displaySoftMoveLabelForDirection("TO", target);
        _displaySoftMoveLabelForDirection("FROM", target);
    }

    function _navigateToMovedElement(id, parentElement) {
        var element = document.getElementById(id);
        contentScroller.scrollTo(element, parentElement, null);
    }

    function _connectorStateChangeListener() {
        var connector = this;
        log.debug("Soft Actions extension state changed...");
        setTimeout(function(){ 
            _displaySoftMoveLabels(connector.target);
        }, 1000);
    }

    // handle connector unregistration on client-side
    function _connectorUnregistrationListener() {
        var connector = this;
        log.debug("Unregistering Soft Actions extension...");
    }

    return {
        init: _initSoftActions
    };
});