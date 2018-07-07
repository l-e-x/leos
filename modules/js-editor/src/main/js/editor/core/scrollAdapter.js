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
define(function scrollAdapterModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var $ = require("jquery");
    var CKEDITOR = require("promise!ckEditor");
    var leosCore = require("js/leosCore");

    // utilities
    var UTILS = leosCore.utils;

    function _setupScrollAdapter(connector) {
        log.debug("Setting up scroll adapter...");
        var rootElement = UTILS.getParentElement(connector);
        CKEDITOR.on("instanceReady", _attachScrollRouter, undefined, rootElement);
        CKEDITOR.on("instanceDestroyed", _detachScrollRouter, undefined, rootElement);
    }

    function _attachScrollRouter(event) {
        log.debug("Attaching scroll router...");
        var editor = event.editor;
        var rootElement = event.listenerData;
        $(rootElement).on("scroll.editor." + editor.id, editor, _scrollEditor);
    }

    function _scrollEditor(event) {
        var editor = event.data;
        editor.fire("scroll");
    }

    function _detachScrollRouter(event) {
        log.debug("Detaching scroll router...");
        var editor = event.editor;
        var rootElement = event.listenerData;
        $(rootElement).off("scroll." + editor.id);
    }

    function _teardownScrollAdapter(connector) {
        log.debug("Tearing down scroll adapter...");
        var rootElement = UTILS.getParentElement(connector);
        CKEDITOR.removeListener("instanceReady", _attachScrollRouter);
        CKEDITOR.removeListener("instanceDestroyed", _detachScrollRouter);
        $(rootElement).off("scroll.editor");
    }

    return {
        setup: _setupScrollAdapter,
        teardown: _teardownScrollAdapter
    };
});