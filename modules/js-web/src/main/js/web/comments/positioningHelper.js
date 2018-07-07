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
define(function commentPositionHelperModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var leosCore = require("js/leosCore");

    // utilities
    var UTILS = leosCore.utils;
    var CONTAINER_SELECTOR = "#leos-doc-content";
    var POPUP_SELECTOR = '[refersTo="~leosComment"], [refersTo="~leosSuggestion"]';

    function _setupPositioningHandler(connector) {
        //Register a resize listener for the repositioning of legal text pane
        connector.addResizeListener(UTILS.getParentElement(connector), _handleResize.bind(undefined, connector));
    }

    function _teardownPositioningHandler(connector) {
        //skeleton
    }

    function _handleResize(connector) {
        var commentsVisible = connector.getState().commentsVisible;
        if(commentsVisible) {
            _getAllPopups().each(function(index, element) {
                _rePosition($(element), true);
            });
        }
    }

    function _rePosition($element, _resize){
        //MOVE POPOVERS TO RIGHT
        _alignHorizontally($element);

        if(!_resize) {
            _alignVertically($element);
        }
    }

    function _alignHorizontally($currentElement) {
        var $articleWrappers = $(".leos-wrap[data-wrapped-type='article']");

        var commentLeftOffset = $articleWrappers.width() + $articleWrappers.position().left + $(CONTAINER_SELECTOR).scrollLeft();

        _getAllPopups().each(function (index, prevElement) {
            var $prevElement = $(prevElement);

            if ($currentElement.is($prevElement)) {
                return false;   //break;
            } else if (_isOverlap($currentElement, $prevElement)) {
                commentLeftOffset = commentLeftOffset + 20;
            }
        });

        if ($currentElement.data("bs.popover")) {
            var $currentPopover = $currentElement.data("bs.popover").tip();
            $currentPopover.css('left', commentLeftOffset);
        }
    }

    function _isOverlap($currentElement, $prevElement) {
        return ($prevElement.offset().top === $currentElement.offset().top);  //top is compared to check if comments are in same line
    }

    function _alignVertically($currentElement) {
        var $currentPopover = $currentElement.data("bs.popover").tip();
        var commentTopPosition = _parse($currentPopover.css('top'));
        $currentPopover.css('top', commentTopPosition + ($currentPopover[0].offsetHeight/2 - 16)); //16 is a hard-coded value to align the top of comment with the element
    }

    //returns the comments elements
    function _getAllPopups() {
        return $(POPUP_SELECTOR);
    }

    function _parse(position) {
        position = position.slice(0, position.length - 2);
        position = parseInt(position, 10);
        return position;
    }

    return {
        setup: _setupPositioningHandler,
        teardown: _teardownPositioningHandler,
        rePosition: _rePosition
    };
});