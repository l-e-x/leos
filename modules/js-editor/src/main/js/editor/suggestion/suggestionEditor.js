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
define(function suggestionEditorModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var CKEDITOR = require("promise!ckEditor");
    var leosCore = require("js/leosCore");
    var profile = require("profiles/inlineSuggestion");
    
    // utilities
    var UTILS = leosCore.utils;

    function _setupSuggestionEditor(connector) {
        log.debug("Setting up suggestion editor...");
        // disable automatic editor creation
        CKEDITOR.disableAutoInline = true;
        // register client side API
        connector.initSuggestionEditor = _initSuggestionEditor;
    }

    function _initSuggestionEditor(elementId, suggestionFragment) {
        log.debug("Initializing suggestion editor...");
        var connector = this;
        
        var editor = _initEditor(connector, elementId, suggestionFragment, profile);
        var suggestionId = _extractSuggestionId(suggestionFragment);
        // register editor event callbacks
        _registerClickListener(connector, elementId, suggestionId, editor);
        _modifyStyles(elementId);
    }
    
    function _initEditor(connector, elementId, elementFragment, profile) {
        log.debug("Initializing suggestion editor with %s profile...", profile.name);
        // retrieve the current user
        var user = connector.getState().user;

        // restrict scope to the extended target
        var rootElement = UTILS.getParentElement(connector);
        var contentWrap = UTILS.getContentWrap(rootElement, elementId);
        if (!contentWrap) {
            throw new Error("Editing suggestion wrapper not found!");
        }

        // set content editable to start the editor in edit mode
        contentWrap.contentEditable = true;
        // clear HTML content because fresh XML will be loaded
        contentWrap.innerHTML = null;

        // create editor instance
        var editor = CKEDITOR.inline(contentWrap, profile.config);

        if (editor) {
            // store LEOS data in editor
            editor.LEOS = {
                profile: profile,
                user: user
            };
            // load XML fragment in editor
            var options = {
                internal: true,
                callback: function() {
                    var editor = this;
                    editor.fire("receiveData", elementFragment);
                }
            };
            editor.setData(elementFragment, options);
            return editor;
        } else {
            throw new Error("Unable to initialize the suggestion editor!");
        }
    }
    
    function _extractSuggestionId(elementFragment) {
        var matches = elementFragment.match(/ id=\"(.+?)\"/);
        if(matches && matches.length > 1) {
            return matches[1];
        }
    }

    function _registerClickListener(connector, elementId, suggestionId, editor) {
        //No need to de-register the listener on destroy since the element itself will be destroyed.
        $('.sugg-actions > .sugg-cancel').on("click.cancel", _destroyEditor.bind(undefined, connector, elementId, editor));
        $('.sugg-actions > .sugg-post').on("click.post", _postSuggestion.bind(undefined, connector, elementId, suggestionId, editor));
    }
    
    function _modifyStyles(elementId) {
        $('#'+elementId).addClass('editor-active');   //On editor ready highlight background of original element
        $('.suggestion-btn').addClass('suggestion-btn-inactive'); //On editor ready hide the suggestion button
    }
    
    function _postSuggestion(connector, elementId, suggestionId, editor, event) {
        log.debug("Saving suggestion...");
        
        // set read-only to prevent changes
        editor.setReadOnly(true);
        // save the element being edited
        var data = {
            elementId: elementId,
            suggestionId: suggestionId,
            suggestionFragment: editor.getData()
        };
        _destroyEditor(connector, elementId, editor);
        connector.saveSuggestion(data);
    }
    
    function _destroyEditor(connector, elementId, editor, event) {
        log.debug("Destroying suggestion editor...");
        // release the element being edited
        var data = {
            elementId: elementId
        };
        _cleanup(connector, elementId);
        // destroy editor instance, without updating DOM
        editor.destroy(true);
        // clear LEOS data from editor
        editor.LEOS = null;
    }

    function _cleanup(connector, elementId) {
        var rootElement = UTILS.getParentElement(connector);
        $('#'+elementId).removeClass('editor-active');
        $('#'+elementId).next().remove(); // remove suggestion wrapper
        $('.suggestion-btn').removeClass('suggestion-btn-inactive'); //On editor destroy display suggestion button
    }

    return {
        setup: _setupSuggestionEditor,
    };
});