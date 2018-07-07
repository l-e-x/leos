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
/************************************************************************************************************/
; // jshint ignore:line
define(function legalTextCommentsExtensionModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var $ = require("jquery");
    var bootstrap = require("bootstrap/popover");
    var leosCore = require("js/leosCore");
    var postal = require("postal");
    var commentTemplate = require("text!./leosCommentTemplate.html");

    var CONTAINER_SELECTOR = "#leos-doc-content";
    var POPUP_SELECTOR = '[data-leosComments="popover"]';
    var POPOVER_SELECTOR = '.popover';

    var EDITOR_CHANNEL = leosCore.config.channels.editor.name;
    var UTILS = leosCore.utils;

    function _initLegalTextComments(connector) {
        log.debug("Initializing Legal Text Comments extension...");
        var $content_label = $(CONTAINER_SELECTOR); //legal text document content label
        _createHoverListeners($content_label, connector);
        //Register a resize listener for the repositioning of legal text pane
        connector.addResizeListener($content_label[0], function resizeCallBack() {
            _handleResize(connector);
        });
        
        _subscribe(connector);
        
        connector.onStateChange = _handleComments;
        
        connector.onUnregister = _connectorUnregistrationListener;
    }
    
    function _handleResize(connector) {
        var commentsVisible = connector.getState().commentsVisible;
        if(commentsVisible) {
          _getAllPopups().each(function(index, element) {
            _rePosition($(element), true);
          });
       }
    }

    function _createHoverListeners($eventRoot, connector) {
        $eventRoot.on("mouseover", POPUP_SELECTOR, _commentMouseIn);
        $eventRoot.on("mouseout", POPUP_SELECTOR, _commentMouseOut);

        function _commentMouseIn() {
            var $element = $(this);
            var commentsVisible = connector.getState().commentsVisible;//get latest state from connector
            if( ! commentsVisible) {
                _showComment($element);
            }
            _selectComment($element);
        }

        function _commentMouseOut() {
            var $element = $(this);
            var commentsVisible = connector.getState().commentsVisible;//get latest state from connector
            if ( ! commentsVisible) {
                _hideComment($element);
            }
            _unSelectComment($element);
        }
    }
    
    function _subscribe(connector) {
        var channel = postal.channel(EDITOR_CHANNEL);
        connector.subscription = channel.subscribe("contentChange", _handleComments.bind(connector));
    }

    function _handleComments() {
        var connector = this;
        var commentsVisible = connector.getState().commentsVisible;

        _hideAllComments(); //hides all the existing popover comments.

         _getAllPopups().each(function(index, element) {
            _showHideComment($(element), commentsVisible);
        });
    }
    
    function _showHideComment($element, commentsVisible) {
       if(commentsVisible) {
           _showComment($element);
       } else {
           _hideComment($element);
       }
    }
    
    //returns the comments elements
    function _getAllPopups() {
        return $(POPUP_SELECTOR);
    }
    
    //returns the actual comment popovers
    function _getAllPopovers() {
        return $(POPOVER_SELECTOR);
    }
    
    function _hideAllComments() {
        _getAllPopovers().each(function(index, element) {
            _hideComment($(element));
        });
    }
    
    /* finds the comments object for popup element and if not existing, creates it*/
    function _getOrCreateComment($element){
        var comment = _getComment($element);
        return  (comment ) ? comment : _initializeComment($element);
    }

    function _showComment($element){
        _getOrCreateComment($element).show();
        _rePosition($element);
    }

    function _hideComment($element){
        var comment =_getComment($element)
        if(comment) {
            comment.hide();
        }
    }

    function _selectComment($element){
        _getComment($element).tip().addClass("selected");
    }
    function _unSelectComment($element){
        _getComment($element).tip().removeClass("selected");
    }

    function _getComment($element){
        return  $element.data("bs.popover");
    }

    function _initializeComment($element) {
        var element = $element[0];
        var title = getCommentTitle(element);
        var containtingAkomaNtoso=$element.parents("akomaNtoso");
        var containtingBill=$element.parents("bill");

        var newComment = $element.popover({
            title: title,
            placement: "right",
            trigger: "manual",
            html: true,
            container: containtingBill,
            template:commentTemplate,
            viewport: containtingAkomaNtoso         //calculate the popover position as per the viewport instead of window.
        });

        newComment = newComment.data("bs.popover");
        var newCommentTip = newComment.tip();

        /*SELECTED POPOVER TO TOP*/
        newCommentTip.mouseover(function() {
            newCommentTip.addClass("selected");
          }).mouseout(function() {
            newCommentTip.removeClass("selected");
         });

         newCommentTip.dblclick(function(evt) {
             evt.stopPropagation();
         });

         //Click Handlers for comment navigation buttons (next/prev)
         _prevButtonClickHandler(newCommentTip);
         _nextButtonClickHandler(newCommentTip);
         
        return newComment;
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
    
    function _parse(position) {
        position = position.slice(0, position.length - 2);
        position = parseInt(position, 10);
        return position;
    }
    
    function getCommentTitle(element) {
        var title = '';
        if (element) {
           var userId = element.hasAttribute("leos:userid") ? element.getAttribute("leos:userid") : '';
           var userName = element.hasAttribute("leos:username") ? element.getAttribute("leos:username") : '';
           var timestamp = element.hasAttribute("leos:datetime") ? element.getAttribute("leos:datetime") : new Date();

           var formattedDate = UTILS.getLocalDateFromUTCAsString(timestamp);
           title = getInitials(userName) + " (" + userId + ")" + " - " + formattedDate;
        }
        return title;
    }

    function getInitials(userName) {
        var initials = userName.match(/\b\w/g);
        return initials != null ? (initials.shift() + initials.pop()).toUpperCase() : "";
    }

    function _prevButtonClickHandler(comment) {
        comment.find(".comment-nav-prev").click(function(){
            moveToPrevPopover(this);
        });
    }
    
    function _nextButtonClickHandler(comment) {
        comment.find(".comment-nav-next").click(function(){
            moveToNextPopover(this);
        });
    }
    
    function moveToNextPopover(clickedElement){
        moveTo(1, $(clickedElement).closest('.popover'));
    }

    function moveToPrevPopover(clickedElement){
        moveTo(-1, $(clickedElement).closest('.popover'));
    }

    function moveTo(moveBy, currentPopover) {
        var $currentPopup = currentPopover.data("bs.popover").$element;

        var $popups = _getAllPopups();
        var currentIndex = $popups.index($currentPopup);
        var $targetPopup = $popups.filter(
            function(index) {
                return index == currentIndex + moveBy;
            }
        );

        if ($targetPopup.length > 0) {
            var $targetComment = _getComment($targetPopup);
            nav_navigateToContent($targetComment.tip().attr("id"));
            _selectComment($targetPopup);
            _unSelectComment($currentPopup);
        }
    }
   
    // handle connector unregistration from server-side
    function _connectorUnregistrationListener() {
        log.debug("Legal Text Comment Extension unregistered...");
        var connector = this;
        if(connector.subscription) {
            connector.subscription.unsubscribe();
            connector.subscription = null;
        }
    }
    
    return {
        init: _initLegalTextComments
    };
});

