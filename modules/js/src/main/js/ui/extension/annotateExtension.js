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
define(function annotateExtensionModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var UTILS = require("core/leosUtils");
    var rulesEngine = require("./rulesEngine");

    function _initAnnotateApplication(connector) {
        log.debug("Initializing annotate extension...");

        // restrict scope to the extended target
        connector.target = UTILS.getParentElement(connector);

        log.debug("Registering annotate extension unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;

        log.debug("Registering sidebar extension state change listener...");
        connector.onStateChange = _connectorStateChangeListener;

        _configureHostBridge(connector);

        //This will add a script to page and boot the sidebar
        //TODO modify the boots.js to have finer control over initialization and destruction on sidebar
        _addHostConfig(document, connector.getState().annotationContainer,
                                connector.getState().authority,
                                connector.getState().apiHost,
                                connector.getState().oauthClientId);
        _addScript(document, connector.getState().clientUrl);

        var annotationContainerElt = document.querySelector(connector.getState().annotationContainer);
        if (annotationContainerElt) {
            var resizeHandler = _resizeSidebar.bind(undefined, annotationContainerElt);
            connector.addResizeListener(annotationContainerElt, resizeHandler);
        }
    }

    function _configureHostBridge(connector) {
        connector.hostBridge = connector.hostBridge || {};

        connector.receiveUserPermissions = _receiveUserPermissions;
        connector.receiveSecurityToken = _receiveSecurityToken;
        connector.receiveMergeSuggestion = _receiveMergeSuggestion;

        connector.stateChangeHandler = _stateChangeHandler;

        var annotationContainerElt = document.querySelector(connector.getState().annotationContainer);
        if (annotationContainerElt) {
            connector.hostBridge.requestSecurityToken = function () {
                if (connector.requestSecurityToken) {
                    connector.requestSecurityToken();
                }
            };
            connector.hostBridge.requestUserPermissions = function () {
                if (connector.requestUserPermissions) {
                    connector.requestUserPermissions();
                }
            };
            connector.hostBridge.requestMergeSuggestion = function (selector) {
                if (selector) {
                    let container = document.querySelector(`#${selector.elementId}`);
                    if (container) {
                        let stringBeforeHighlight = container.innerHTML.substring(0, selector.startOffset);
                        let elementToProcess = document.createElement(container.nodeName);
                        elementToProcess.innerHTML = stringBeforeHighlight;
                        let context = {startOffset: 0, endOffset: 0};
                        //Running rules to get txt positioning in XML tag
                        rulesEngine.process(suggestionsRules, elementToProcess, selector, context);
                        //update offsets as per text positioning in XML.
                        selector.endOffset = context.startOffset + (selector.endOffset - selector.startOffset); //len +sO
                        selector.startOffset = context.startOffset;
                        if (connector.requestMergeSuggestion) {
                            connector.requestMergeSuggestion(selector);
                        }
                    }
                }
            };
            annotationContainerElt.hostBridge = connector.hostBridge;
        }
    }

    function _resizeSidebar(annotationContainerElt) {
        var event = new Event("annotationSidebarResize");
        if (annotationContainerElt) {
            annotationContainerElt.dispatchEvent(event);
        }
    }

    // handle connector state change on client-side
    function _connectorStateChangeListener() {
        var connector = this;
        log.debug("Side Bar extension state changed...");
        // KLUGE delay execution due to sync issues with target update
        setTimeout(_refresh(document, connector.getState().annotationContainer), 500, connector);
    }

    function _receiveUserPermissions(...userPermissions) {
        var connector = this;
        log.debug("User Permissions received and being sent to annotate..!");
        if (connector.hostBridge && connector.hostBridge.responseUserPermissions && typeof connector.hostBridge.responseUserPermissions === 'function') {
            connector.hostBridge.responseUserPermissions(userPermissions)
        }
    }

    function _receiveSecurityToken(token) {
        var connector = this;
        log.debug("Security token received and being sent to annotate..!");
        if (connector.hostBridge && connector.hostBridge.responseSecurityToken && typeof connector.hostBridge.responseSecurityToken === 'function') {
            connector.hostBridge.responseSecurityToken(token)
        }
    }

    function _receiveMergeSuggestion(result) {
        var connector = this;
        log.debug("Merge suggestion result received and being sent to annotate..!");
        if (connector.hostBridge && connector.hostBridge.responseMergeSuggestion && typeof connector.hostBridge.responseMergeSuggestion === 'function') {
            connector.hostBridge.responseMergeSuggestion(result)
        }
    }

    function _stateChangeHandler(state) {
        var connector = this;
        log.debug("State change event and being propagated to annotate..!");
        if (connector.hostBridge && connector.hostBridge.stateChangeHandler && typeof connector.hostBridge.stateChangeHandler === 'function') {
            connector.hostBridge.stateChangeHandler(state)
        }
    }

    function _refresh(doc, annotationContainer) {
        var event = new Event("annotationRefresh");
        var annotationContainerElt = doc.querySelector(annotationContainer);
        if (annotationContainerElt) {
            annotationContainerElt.dispatchEvent(event);
        }
    }

    function _addScript(doc, url) {
        var script = doc.createElement('script');
        script.addEventListener('error', _onErrorLoad, false);
        script.type = 'text/javascript';
        script.src = url;
        script.async = true;
        doc.head.appendChild(script);

        function _onErrorLoad(event) {
            log.debug('Error occurred while loading script ', event);
        }
    }

    /* this method creates the config which would be used by annotate client when it instantiate
     * below config will significantly modify the way client will behave*/
    function _addHostConfig(doc, annotationContainer, authority, apiHost, oauthClientId) {
        var script = doc.createElement('script');
        script.type = 'application/json';
        script.className = 'js-hypothesis-config';

        script.innerHTML = `{
                                "leosDocumentRootNode": "akomantoso",
                                "annotationContainer": "${annotationContainer}",
                                "ignoredTags": ["div"],
                                "allowedSelectorTags": "a.ref2link-generated",
                                "editableAttribute": "leos\:editable",
                                "notAllowedSuggestSelector": "num:not([leos\\\\:origin='cn']), guidance, heading:contains('Entry into force')",
                                "oauthClientId": "${oauthClientId}",
                                "services": [{
                                    "authority": "${authority}",
                                    "apiUrl": "${apiHost}"
                                }]
                            }`;
        doc.body.appendChild(script);
    }

    // handle connector unregistration on client-side
    function _connectorUnregistrationListener() {
        var connector = this;
        log.debug("Unregistering annotate extension...");

        // Unload sidebar
        _destroyAnnotate(document);
        log.debug("Destruction of annotate application done");

        // clean connector
        connector.target = null;
    }

    function _destroyAnnotate(doc) {
        var appLinkEl = doc.querySelector('link[type="application/annotator+html"][rel="sidebar"]');
        if (appLinkEl != null) {
            var event = new Event('destroy');
            appLinkEl.dispatchEvent(event);
        }
    }

    //Rules
    var suggestionsRules = {
        text: {
            $: function (selector, context) {
                let cleanedText = this.textContent.replace(/&nbsp;/g, " ");
                context.startOffset += cleanedText.length;
            }
        },
        element: {
            'authorialnote': function (selector, context) {  //authorialnote
                let data = this.getAttribute("data-tooltip");
                context.startOffset += data.length;
                context.startOffset -= this.textContent.length;
            }
        }
    };

    return {
        init: _initAnnotateApplication
    };
});
