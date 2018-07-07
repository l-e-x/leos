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
define(function sideCommentsExtensionModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var _ = require("lodash");
    var $ = require("jquery");
    var SideComments = require("sideComments");
    var sideCommentHelper = require("./sideCommentHelper");

    // configuration
    var SC_ROOT_SELECTOR = "akomantoso";
    var SC_SECTION_SELECTOR = "heading, aknp";

    // handle extension initialization
    function _initExtension(connector) {
        log.debug("Initializing Side Comments extension...");

        sideCommentHelper.setup(connector);

        log.debug("Registering Side Comments extension state change listener...");
        connector.onStateChange = _connectorStateChangeListener;

        log.debug("Registering Side Comments extension unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;
    }

    // handle connector state change from server-side
    function _connectorStateChangeListener() {
        log.debug("Side Comments extension state changed...");
        var connector = this;
        _destroySideComments(connector);
        _createSideComments(connector);
    }

    // handle connector unregistration from server-side
    function _connectorUnregistrationListener() {
        log.debug("Side Comments extension unregistered...");
        var connector = this;
        sideCommentHelper.teardown(connector);
        _destroySideComments(connector);
    }

    function _markCommentableSections() {
        log.debug("Marking commentable sections...");

        $(SC_SECTION_SELECTOR).filter(_filterComments).each(function markSection() {
            var $section = $(this);
            $section.addClass("commentable-section");
            $section.data("section-id", $section.attr("id"));
        });

        function _filterComments(index, element){
            return $(element).parent("[refersTo='~leosComment'], [refersTo='~leosSuggestion']").length == 0;
        }
    }

    function _createSideComments(connector) {
        if (!connector.sideComments) {
            var state = connector.getState();
            var user = state.user;
            log.debug("Creating Side Comments...");

            var comments = sideCommentHelper.getComments(connector, SC_SECTION_SELECTOR);
            _markCommentableSections();// must be done after comments are fetched from document

            var sideComments = new SideComments(SC_ROOT_SELECTOR, user, comments);
            // attach side comments instance reference to connector
            connector.sideComments = sideComments;

            log.debug("Registering comment post listener...");
            sideComments.on("commentPosted", _onCommentPosted.bind(sideComments, connector));

            log.debug("Registering comment delete listener...");
            sideComments.on("commentDeleted", _onCommentDeleted.bind(sideComments, connector));
        }
    }

    function _destroySideComments(connector) {
        if (connector.sideComments) {
            log.debug("Destroying Side Comments...");
            connector.sideComments.destroy();
            // detach side comments instance reference from connector
            delete connector.sideComments;
        }
    }

    // handle comment posted from client-side
    function _onCommentPosted(connector, comment) {
        var sideComments = this;
        log.debug("Comment posted...");
        comment.id = _.uniqueId("akn_cmt");
        // insert comment at client-side (optimistic)
        sideComments.insertComment(comment);
        // insert comment at server-side (deferred)
        connector.insertComment(comment.sectionId, comment.id, comment.comment);
    }

    // handle comment deleted from client-side
    function _onCommentDeleted(connector, comment) {
        var sideComments = this;
        log.debug("Comment deleted...");
        // remove comment at client-side (optimistic)
        sideComments.removeComment(comment.sectionId, comment.id);
        // delete comment at server-side (deferred)
        connector.deleteComment(comment.sectionId, comment.id);
    }

    return {
        init: _initExtension
    };
});