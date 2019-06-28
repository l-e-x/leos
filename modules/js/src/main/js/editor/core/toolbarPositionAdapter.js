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
define(function toolbarPositionAdapterModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var $ = require("jquery");
    var CKEDITOR = require("promise!ckEditor");
    var UTILS = require("core/leosUtils");

    var repositionHandler;
    
    function _setupToolbarPositionAdapter(connector) {
        log.debug("Setting up toolbarPosition adapter...");
        CKEDITOR.on("instanceReady", _attachToolbarPositionRouter, undefined, connector);
        CKEDITOR.on("instanceDestroyed", _detachToolbarPositionRouter, undefined, connector);
    }

    function _attachToolbarPositionRouter(event) {
        log.debug("Attaching toolbarPosition router...");
        var editor = event.editor;
        var connector = event.listenerData;
        var rootElement = UTILS.getParentElement(connector);
        repositionHandler = _repositionEditor.bind(undefined, editor);
        $(rootElement).on("scroll.editor." + editor.id, repositionHandler);
        connector.addResizeListener(rootElement, repositionHandler);
    }
    
    function _repositionEditor(editor) {
        editor.fire("reposition");
    }
    
    function _detachToolbarPositionRouter(event) {
        log.debug("Detaching toolbarPosition router...");
        var editor = event.editor;
        var connector = event.listenerData;
        var rootElement = UTILS.getParentElement(connector);
        if(rootElement) {
            $(rootElement).off("scroll." + editor.id);
            connector.removeResizeListener(rootElement, repositionHandler);
        }
    }

    function _teardownToolbarPositionAdapter(connector) {
        log.debug("Tearing down toolbarPosition adapter...");
        var rootElement = UTILS.getParentElement(connector);
        CKEDITOR.removeListener("instanceReady", _attachToolbarPositionRouter);
        CKEDITOR.removeListener("instanceDestroyed", _detachToolbarPositionRouter);
        $(rootElement).off("scroll.editor");
    }

    return {
        setup: _setupToolbarPositionAdapter,
        teardown: _teardownToolbarPositionAdapter
    };
});
