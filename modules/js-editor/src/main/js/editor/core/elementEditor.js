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
define(function elementEditorModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var CKEDITOR = require("promise!ckEditor");
    var leosCore = require("js/leosCore");

    // configuration
    var PROFILES_CFG = {
        citations: "inlineAknCitations",
        recitals: "inlineAknRecitals",
        article: "inlineAknArticle"
    };

    // utilities
    var UTILS = leosCore.utils;

    function _setupElementEditor(connector) {
        log.debug("Setting up element editor...");
        // disable automatic editor creation
        CKEDITOR.disableAutoInline = true;
        // register client side API
        connector.editElement = _editElement;
        connector.refreshElement = _refreshElement;
        connector.receiveElement = _receiveElement;
        connector.receiveToc = _receiveToc;
    }

    function _editElement(elementId, elementType, elementFragment) {
        log.debug("Initializing element editor...");
        var connector = this;
        var profileId = PROFILES_CFG[elementType];
        if (profileId) {
            // load the specific profile and initialize the editor
            require(["profiles/" + profileId],
                _initEditor.bind(undefined, connector, elementId, elementType, elementFragment));
        } else {
            throw new Error("Unknown element editor profile!");
        }
    }

    function _initEditor(connector, elementId, elementType, elementFragment, profile) {
        log.debug("Initializing element editor with %s profile...", profile.name);
        // retrieve the current user
        var user = connector.getState().user;

        // restrict scope to the extended target
        var rootElement = UTILS.getParentElement(connector);
        var contentWrap = UTILS.getContentWrap(rootElement, elementId);
        if (!contentWrap) {
            throw new Error("Editing element wrapper not found!");
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
            // register editor event callbacks
            editor.on("close", _destroyEditor.bind(undefined, connector, elementId));
            editor.on("save", _saveElement.bind(undefined, connector, elementId, elementType));
            editor.on("requestElement", _requestElement.bind(undefined, connector));
            editor.on("requestToc", _requestToc.bind(undefined, connector));

            // load XML fragment in editor
            var options = {
                internal: true,
                callback: function() {
                    var editor = this;
                    editor.fire("receiveData", elementFragment);
                }
            };
            editor.setData(elementFragment, options);
        } else {
            throw new Error("Unable to initialize the element editor!");
        }
    }

    function _destroyEditor(connector, elementId, event) {
        log.debug("Destroying element editor...");
        var editor = event.editor;
        // set read-only to prevent changes
        editor.setReadOnly(true);
        // release the element being edited
        var data = {
            elementId: elementId
        };
        connector.releaseElement(data);
        // destroy editor instance, without updating DOM
        editor.destroy(true);
        // clear LEOS data from editor
        editor.LEOS = null;
    }

    function _saveElement(connector, elementId, elementType, event) {
        log.debug("Saving element...");
        var editor = event.editor;
        // set read-only to prevent changes
        editor.setReadOnly(true);
        // save the element being edited
        var data = {
            elementId: elementId,
            elementType: elementType,
            elementFragment: event.data.data
        };
        connector.saveElement(data);
    }

    function _refreshElement(elementId, elementType, elementFragment) {
        log.debug("Refreshing element editor...");
        var editor = CKEDITOR.currentInstance;
        // set editable to allow changes
        editor.setReadOnly(false);
        // reload XML fragment in editor
        var options = {
            callback: function() {
                var editor = this;
                editor.fire("receiveData", elementFragment);
                // reset dirty state as for unchanged content
                editor.resetDirty();
            }
        };
        editor.setData(elementFragment, options);
    }

    function _requestElement(connector, event) {
        log.debug("Requesting element...");
        var data = {
            elementId: event.data.elementId,
            elementType: event.data.elementType
        };
        connector.requestElement(data);
    }

    function _receiveElement(elementId, elementType, elementFragment) {
        log.debug("Received element...");
        var editor = CKEDITOR.currentInstance;
        var data = {
            elementId :elementId,
            elementType: elementType,
            elementFragment: elementFragment
        };
        editor.fire("receiveElement", data );
    }

    function _requestToc(connector, event) {
        log.debug("Requesting toc...");
        var data = {
            elementId: event.data.selectedNodeId
        };
        connector.requestToc(data);
    }

    function _receiveToc(tocWrapper) {
        log.debug("Received toc...");
        var editor = CKEDITOR.currentInstance;
        editor.fire("receiveToc", tocWrapper );
    }

    function _teardownElementEditor(connector) {
        log.debug("Tearing down element editor...");
        // destroy any editor instances remaining
        for (var name in CKEDITOR.instances) {
            if (CKEDITOR.instances.hasOwnProperty(name)) {
                var editor = CKEDITOR.instances[name];
                if (editor) {
                    log.debug("Destroying editor instance:", name);
                    // destroy editor instance, without updating DOM
                    editor.destroy(true);
                }
            }
        }
    }

    return {
        setup: _setupElementEditor,
        teardown: _teardownElementEditor
    };
});