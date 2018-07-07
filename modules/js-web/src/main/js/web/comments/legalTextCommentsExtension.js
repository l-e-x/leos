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
    var suggestionTemplate = require("text!./leosSuggestionTemplate.html");
    var positioningHelper = require("./positioningHelper");

    var POPUP_SELECTOR = '[refersTo="~leosComment"], [refersTo="~leosSuggestion"]';
    var POPOVER_SELECTOR = '.popover';

    var EDITOR_CHANNEL = leosCore.config.channels.editor.name;
    var UTILS = leosCore.utils;

    function _initLegalTextComments(connector) {
        log.debug("Initializing Legal Text Comments extension...");
        _createHoverListeners(connector);
        _subscribe(connector);
        positioningHelper.setup(connector);
        connector.onStateChange = _handleComments;
        connector.onUnregister = _connectorUnregistrationListener;
    }

    function _createHoverListeners(connector) {
        var $eventRoot = $(UTILS.getParentElement(connector));
        $eventRoot.on("mouseover", POPUP_SELECTOR, _commentMouseIn);
        $eventRoot.on("mouseout", POPUP_SELECTOR, _commentMouseOut);

        function _commentMouseIn() {
            var $element = $(this);
            var commentsVisible = connector.getState().commentsVisible;//get latest state from connector
            if (!commentsVisible) {
                _showComment($element);
            }
            _selectComment($element);
        }

        function _commentMouseOut() {
            var $element = $(this);
            var commentsVisible = connector.getState().commentsVisible;//get latest state from connector
            if (!commentsVisible) {
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
        _hideAllComments(); //hides all the existing popover comments as popup element might be removed

        if (connector.getState().commentsVisible) {
            _getAllPopups().each(function (index, element) {
                var $element = $(element);
                _showComment($element);
                _setActions($element);
            });
        }
    }

    function _setActions( $element) {
            var display = ($element.parents(".cke_editable").length > 0)
                ? "inherit"
                : "none";
            $element.data("bs.popover").tip()
                .find(".balloon-actions")
                .css("display", display);
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
            _getAllPopovers().each(function (index, element) {
                _hideComment($(element));
            });
        }

        /* finds the comments object for popup element and if not existing, creates it*/
        function _getOrCreateComment($element) {
            var comment = _getComment($element);
            return (comment ) ? comment : _initializeComment($element);
        }

        function _showComment($element) {
            _getOrCreateComment($element).show();
            positioningHelper.rePosition($element);
        }

        function _hideComment($element) {
            var comment = _getComment($element);
            if (comment) {
                comment.hide();
            }
        }

        function _selectComment($element) {
            _getComment($element).tip().addClass("selected");
        }

        function _unSelectComment($element) {
            _getComment($element).tip().removeClass("selected");
        }

        function _getComment($element) {
            return $element.data("bs.popover");
        }

        function _getTemplate($element) {
            return ($element.attr("refersTo") === "~leosSuggestion")
                ? suggestionTemplate
                : commentTemplate;
        }

        function _initializeComment($element) {
            var containtingAkomaNtoso = $element.parents("akomaNtoso");
            var containtingBill = $element.parents("bill");

            var newComment = $element.popover({
                title: _getCommentTitle,
                content: _getContent,
                placement: "right",
                trigger: "manual",
                html: true,
                container: containtingBill,
                template: _getTemplate($element),
                viewport: containtingAkomaNtoso         //calculate the popover position as per the viewport instead of window.
            });

            newComment = newComment.data("bs.popover");
            var newCommentTip = newComment.tip();

            newCommentTip.addClass($element.attr("refersTo"));

            /*SELECTED POPOVER TO TOP*/
            newCommentTip.mouseover(function () {
                newCommentTip.addClass("selected");
            }).mouseout(function () {
                newCommentTip.removeClass("selected");
            });

            newCommentTip.dblclick(function (evt) {
                evt.stopPropagation();
            });

            //Click Handlers for comment navigation buttons (next/prev)
            newCommentTip.on("click.navigation", ".comment-nav-prev", _moveTo.bind(undefined, -1, newCommentTip));
            newCommentTip.on("click.navigation", ".comment-nav-next", _moveTo.bind(undefined, 1, newCommentTip));

            return newComment;
        }

        function _getContent() {
            var element = this;
            return element
                .innerHTML
                .replace(/(\sid=")/gi, ' original-id="')
                .replace(/(<popup(.|\s)*?\/popup>)/gi, '');//remove ids and popups
        }

        function _getCommentTitle() {
            var element = this;
            var title = '';
            if (element) {
                var userId = element.hasAttribute("leos:userid") ? element.getAttribute("leos:userid") : '';
                var userName = element.hasAttribute("leos:username") ? element.getAttribute("leos:username") : '';
                var timestamp = element.hasAttribute("leos:datetime") ? element.getAttribute("leos:datetime") : new Date();

                var formattedDate = UTILS.getLocalDateFromUTCAsString(timestamp, "yyyy-mm-dd HH:MM");
                title = getInitials(userName) + " (" + userId + ")" + " - " + formattedDate;
            }
            return title;
        }

        function getInitials(userName) {
            var initials = userName.match(/\b\w/g);
            return initials != null ? (initials.shift() + initials.pop()).toUpperCase() : "";
        }

        function _moveTo(moveBy, currentPopover) {
            var $currentPopup = currentPopover.data("bs.popover").$element;

            var $popups = _getAllPopups();
            var currentIndex = $popups.index($currentPopup);
            var $targetPopup = $popups.filter(
                function (index) {
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
            positioningHelper.teardown(connector);
            if (connector.subscription) {
                connector.subscription.unsubscribe();
                connector.subscription = null;
            }
        }

        return {
            init: _initLegalTextComments
        };
    }

    )
    ;

