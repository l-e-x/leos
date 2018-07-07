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
define(function bookmarkHandlerModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var bookmarkHtml = require('text!./bookmarkTemplate.html');
    var CKEDITOR = require("promise!ckEditor");
    var leosCore = require("js/leosCore");
    var WaypointInview = require("waypoint.inview");

    // utilities
    var UTILS = leosCore.utils;
    var BOOKMARK_SELECTOR="#bookmarkContainer";

    function _setupBookmarkHandler(connector) {
        log.debug("Setting up bookmark handler...");
        var rootElement = UTILS.getParentElement(connector);
        CKEDITOR.on("instanceReady", _createBookmark, undefined, rootElement);
        CKEDITOR.on("instanceDestroyed", _destroyBookmark, undefined, rootElement);
    }

    function _createBookmark(event) {
        log.debug("Creating bookmark...");
        var editor = event.editor;
        var rootElement = event.listenerData;
        var element = editor.element.$;
        $(rootElement).append(bookmarkHtml);
        $(BOOKMARK_SELECTOR).on("click.navigation", _navigateToEditor.bind(undefined, editor, element));

        $(BOOKMARK_SELECTOR)[0].waypoint = new Waypoint.Inview({
            element: element,
            context: rootElement,
            enter: function(direction) {
                $(BOOKMARK_SELECTOR).removeClass("displayed");
            },
            exited: function(direction) {
                $(BOOKMARK_SELECTOR).addClass("displayed");
            }
        });
    }

    function _navigateToEditor(editor, element) {
        function _setFocus() {
            this.focus();
        }
        nav_navigateToElement(element, _setFocus.bind(editor));
    }

    function _destroyBookmark(event) {
        log.debug("Destroying bookmark ...");
        var $bookmark = $(BOOKMARK_SELECTOR);
        var waypoint = $bookmark.length >0 ? $bookmark[0].waypoint : undefined;
        if(waypoint){
            waypoint.destroy();
        }
        $bookmark.remove();
    }

    function _teardownBookmarkHandler(connector) {
        log.debug("Tearing down bookmark handler...");
        CKEDITOR.removeListener("instanceReady", _createBookmark);
        CKEDITOR.removeListener("instanceDestroyed", _destroyBookmark);
    }

    return {
        setup: _setupBookmarkHandler,
        teardown: _teardownBookmarkHandler
    };
});