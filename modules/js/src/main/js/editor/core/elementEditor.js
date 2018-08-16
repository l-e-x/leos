/*
 * Copyright 2018 European Commission
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
define(function elementEditorModule(require) {
    "use strict";

    // load module dependencies
    var _ = require("lodash");
    var $ = require("jquery");
    var log = require("logger");
    var CKEDITOR = require("promise!ckEditor");
    var UTILS = require("core/leosUtils");

    // configuration of the profiles (element tag name + document type name separated by '_' all lower case +
    // instance context separated by '_' all lower case (defaults to blank = ec))
    var PROFILES_CFG = {
        clause_bill: "inlineAknClause",
        citations_bill: "inlineAknCitations",
        recitals_bill: "inlineAknRecitals",
        article_bill: "inlineAknArticle",
        division_annex: "inlineAknAnnexDivision",
        blockcontainer_memorandum: "inlineAknEMBlockContainer",
        
        article_bill_council: "inlineAknArticleMandate",
    };

    function _setupElementEditor(connector) {
        log.debug("Setting up element editor...");
        // disable automatic editor creation
        CKEDITOR.disableAutoInline = true;
        // register client side API
        connector.editElement = _editElement;
        connector.refreshElement = _refreshElement;
        connector.receiveElement = _receiveElement;
        connector.receiveToc = _receiveToc;
        connector.receiveRefLabel = _receiveRefLabel;
    }

    function _editElement(elementId, elementType, elementFragment, docType, instanceType) {
        log.debug("Initializing element editor...");
        let connector = this;

        let rootElement = UTILS.getParentElement(connector);
        _createEditorPlaceholder(rootElement, elementId);
        let placeholder = _getEditorPlaceholder(rootElement, elementId);
        if (!placeholder) {
            throw new Error("Editing element wrapper not found!");
        }

        // restrict scope to the extended target
        _scopeEvents(placeholder);

        docType = docType.toLowerCase();
        let profileId = _getEditorProfileId(docType, elementType, instanceType);

        if (profileId) {
            // load the specific profile and initialize the editor
            require(["profiles/" + profileId],
                _initEditor.bind(undefined, connector, elementId, elementType, elementFragment, placeholder, docType, instanceType));
        } else {
            throw new Error("Unknown element editor profile!");
        }
    }

    function _getEditorProfileId(docType, elementType, instanceType) {
        docType = "_".concat(docType);
        instanceType = (instanceType && instanceType === "COUNCIL") ? "_".concat(instanceType.toLowerCase()) : ""; //default instanceType is commission and left as blank
        let propertyName = elementType.toLowerCase() + docType;    // elementType should not be used as lower case in further calls because
        let propertyNameWithInstance = propertyName + instanceType;                                                                 // as for BlockContainer tag, it needs the capitals to be matched. 
        return PROFILES_CFG[propertyNameWithInstance] ? PROFILES_CFG[propertyNameWithInstance] : PROFILES_CFG[propertyName];
    }
    
    function _initEditor(connector, elementId, elementType, elementFragment, placeholder, docType, instanceType, profile) {
        log.debug("Initializing element editor with %s profile...", profile.name);
        // retrieve the current user
        var user = connector.getState().user;
        user['roles'] = connector.getState().roles;

        if (connector.editorChannel) {
            connector.editorChannel.publish('editor.open', {elementId: elementId});
        }

        // set content editable to start the editor in edit mode
        placeholder.contentEditable = true;
        // clear HTML content because fresh XML will be loaded
        placeholder.innerHTML = null;

        var config = _getConfig(user, profile.config);

        // create editor instance
        var editor = CKEDITOR.inline(placeholder, config);

        if (editor) {
            // store LEOS data in editor
            editor.LEOS = {
                profile: profile,
                type: docType,
                instanceType: instanceType,
                user: user
            };
            // register editor event callbacks
            editor.on("close", _destroyEditor.bind(undefined, connector, elementId));
            editor.on("save", _saveElement.bind(undefined, connector, elementId, elementType));
            editor.on("requestElement", _requestElement.bind(undefined, connector));
            editor.on("requestToc", _requestToc.bind(undefined, connector));
            editor.on("requestRefLabel", _requestRefLabel.bind(undefined, connector));

            // load XML fragment in editor
            var options = {
                internal: true,
                callback: function() {
                    var editor = this;
                    editor.fire("receiveData", elementFragment);
                    placeholder.style.height = ''; //reset the height to let editor grow
                }
            };
            editor.setData(elementFragment, options);
        } else {
            throw new Error("Unable to initialize the element editor!");
        }
    }

    function _createEditorPlaceholder(rootElement, elementId) {
        let element;
        if (rootElement) {
            element = rootElement.querySelector(`#${elementId}`);
        } else {
            element = document.querySelector(`#${elementId}`);
        }

        $(element).wrap(function () {
            return `<div class="leos-placeholder" data-wrapped-id='${elementId}' style='height:${_getEditableAreaHeight(elementId)}px'></div>`;
        });
    }

    function _getEditableAreaHeight(elementId) {
        return document.getElementById(elementId).clientHeight;
    }

    function _getEditorPlaceholder(rootElement, elementId) {
        var wrap = null;
        var selector = ".leos-placeholder[data-wrapped-id='" + elementId + "']";
        if (rootElement) {
            wrap = rootElement.querySelector(selector);
        } else {
            wrap = document.querySelector(selector);
        }
        return wrap;
    }

    var _setEventType = function (event) {
        event.hostEventType = "ckEvent";
    };
    
    function _scopeEvents(contentWrap) {
        // Scope all events from editor by a specific type
        for(var key in contentWrap){
            if(key.search('on') === 0) {
                contentWrap.addEventListener(key.slice(2), _setEventType)
            }
        }
    }

    function _unscopeEvents(contentWrap) {
        // Remove scoping on all events by removing listeners
        for(var key in contentWrap){
            if(key.search('on') === 0) {
                contentWrap.removeEventListener(key.slice(2), _setEventType)
            }
        }
    }

    function _getConfig(user, defaultConfig) {
        // clone the profile configuration for this editor instance
        var config = _.cloneDeep(defaultConfig);

        if (user.roles && user.roles.includes("SUPPORT")) {
            config.extraPlugins = [config.extraPlugins, "sourcedialog"].join();
        } else {
            config.removePlugins = [config.removePlugins, "sourcedialog"].join();
        }
        return config;
    }

    function _destroyEditor(connector, elementId, event) {
        log.debug("Destroying element editor...");
        var editor = event.editor;
        // set read-only to prevent changes
        editor.setReadOnly(true);

        var rootElement = UTILS.getParentElement(connector);
        var placeholder = _getEditorPlaceholder(rootElement, elementId);
        _unscopeEvents(placeholder);
        $(placeholder).height(_getEditableAreaHeight(elementId));

        // release the element being edited
        var data = {
            elementId: elementId
        };
        connector.releaseElement(data);
        // destroy editor instance, without updating DOM
        placeholder.innerHTML = null; //used to avoid flickering text
        editor.destroy(true);

        if (connector.editorChannel) {
            connector.editorChannel.publish('editor.close', {elementId: elementId});
        }
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
            elementIds: event.data.selectedNodeIds
        };
        connector.requestToc(data);
    }

    function _receiveToc(tocWrapper) {
        log.debug("Received toc...");
        var editor = CKEDITOR.currentInstance;
        editor.fire("receiveToc", tocWrapper );
    }

    function _requestRefLabel(connector, event) {
        log.debug("Requesting reference labels...");
        connector.requestRefLabel(event.data);
    }

    function _receiveRefLabel(references) {
        log.debug("Received reference labels...");
        var editor = CKEDITOR.currentInstance;
        editor.fire("receiveRefLabel", references);
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
