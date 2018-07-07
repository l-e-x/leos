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
define(function sideCommentHelperModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var leosCore = require("js/leosCore");

    // utilities
    var UTILS = leosCore.utils;

    function _setupSideCommentHelper(connector) {
        //skeleton
    }

    function _getComments(connector, sectionSelector) {
        log.debug("Creating comments and sections...");
        var commentObjects =[];

        $(UTILS.getParentElement(connector))
            .find("[refersTo='~leosComment'], [refersTo='~leosSuggestion']")
            .each(function createCommentObject( index, commentElement ){
                var sectionId = $(commentElement).parents(sectionSelector).attr("id");
                commentObjects.push(new Comment(commentElement, sectionId));
                });
        commentObjects.sort(_timestampComparator);
        var sections = commentObjects.reduce(_categorizeCommentsBySection, []);
        return sections;
    }


    function _categorizeCommentsBySection(sections, comment) {
        var section = sections.find(_isMatchingSection, comment);
        if(!section) {
            section = new Section(comment.enclosingElementId);
            sections.push(section);
        }
        section.comments.push(comment);
        return sections;

        function _isMatchingSection(currentSection, index, sections){
            return currentSection.sectionId === this.enclosingElementId;
        }
    }

    function _timestampComparator(commentA, commentB) {
        if (commentA.timestamp < commentB.timestamp) {
            return -1;
        }
        else if (commentA.timestamp > commentB.timestamp) {
            return 1;
        }
        // timestamps must be equal
        return 0;
    }

    //Object Section
    function Comment(commentElement, sectionId){
        var $comment = $(commentElement);
        this.id = $comment.attr("id");
        this.enclosingElementId = sectionId;
        this.comment =  $comment.get(0).innerHTML.replace(/ id="/gi, ' original-id="');//replacing all Ids //TODO sanitize
        this.authorName = $comment.attr("leos:userName");
        this.authorId = $comment.attr("leos:userId");
        this.timestamp = $comment.attr("leos:dateTime");
        this.dg = $comment.attr("leos:dg")? $comment.attr("leos:dg").split('.')[0] :'' ;
        this.refersTo = $comment.attr("refersTo");
    }

    //Object Section
    function Section(elementId){
        this.sectionId = elementId;
        this.comments = [];
    }

    function _teardownSideCommentHelper(connector) {
        //skeleton
    }

    return {
        setup: _setupSideCommentHelper,
        teardown: _teardownSideCommentHelper,
        getComments: _getComments
    };

});