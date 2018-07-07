/*
 * Copyright 2015 European Commission
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
(function sideCommentsLoader(global) {
    "use strict";
    
    // provides the side-comments location to the require js
    var PATH_PREFIX = '../web/sideComments';
    require.config({
        paths : {
            'sideComments' : PATH_PREFIX,
            'text' : PATH_PREFIX + '/lib/text'
        }
    });
    
      
    var SIDE_COMMENTS_ROOT_ELEMENT = "akomantoso";
    var SIDE_COMMENTS_DATA_ATTR_ID = "side-comments-id";
    var SELECTOR_FOR_COMENTABLE_SECTION = "aknp, heading";

    function isNewSideComments() {
        var sideCommentsId = $(SIDE_COMMENTS_ROOT_ELEMENT).data(SIDE_COMMENTS_DATA_ATTR_ID);
        if (!sideCommentsId) {
            return true;
        }
        return false;
    }

    global.LEOS.setUpSideComments = function setUpSideComments(existingComments, currentUser) {
        require(['sideComments/main'],function(SideComments) {
            if (isNewSideComments()) {
                var commentableSections = $(SELECTOR_FOR_COMENTABLE_SECTION);
                commentableSections.addClass('commentable-section');
                var sideComments = new SideComments(SIDE_COMMENTS_ROOT_ELEMENT, currentUser, existingComments);
                sideComments.on('commentPosted', function(comment) {
                    comment.id = _.uniqueId('akn_');
                    vaadin_addComment(comment.sectionId, comment.id, comment.comment);
                    sideComments.insertComment(comment);
                });
                sideComments.on('commentDeleted', function(comment) {
                    vaadin_deleteComment(comment.sectionId, comment.id);
                    sideComments.removeComment(comment.sectionId, comment.id);
                });
                $(SIDE_COMMENTS_ROOT_ELEMENT).data(SIDE_COMMENTS_DATA_ATTR_ID, _.uniqueId());
            }
        })
    }
    

}(this));