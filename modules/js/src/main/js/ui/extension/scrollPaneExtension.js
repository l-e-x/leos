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
define(function scrollPaneExtensionModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var UTILS = require("core/leosUtils");
    var $ = require("jquery");
    var LODASH = require("lodash");
    
    var DOC_CONTAINER_SELECTOR = ".leos-doc-content";
    var CHANGE_PANE_CONTAINER;
    var ID_PREFIX;

    const NON_SELECTABLE_ELEMENTS = "bill, doc, num, content, authorialNote, inline, ref, span, br, tbody, tr, td, th," +
    		                               "a, b, strong, i, em, hr, marker, text, .leos-actions, .cke_editable, hypothesis-highlight";
    
    function _initSyncScroll(connector) {
        log.debug("Initializing ScrollPane extension...");
        connector.setSyncState = _setSyncState;
        connector.userOriginated = true;
        _configStatic(connector);
        log.debug("Registering scroll pane state change listener...");
        connector.onStateChange = _connectorStateChangeListener;
        log.debug("Registering ScrollPane extension unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;
    }

    function _setSyncState(enable) {
        var connector = this;
        if(enable) {
            _unregisterScrollHandler(connector);
            _registerScrollHandler(connector);
            $(DOC_CONTAINER_SELECTOR).trigger("scroll");
        } else {
            _unregisterScrollHandler(connector);
        }
    }

    // handle connector unregistration on client-side
    function _connectorStateChangeListener() {
        var connector = this;
        log.debug("Scroll pane extension state changed...");
        connector.getState(false).enableSync ? _registerScrollHandler(connector) : _unregisterScrollHandler(connector);
    }
    
    function _configStatic(connector) {
        ID_PREFIX = connector.getState(false).idPrefix;
        CHANGE_PANE_CONTAINER = connector.getState(false).containerSelector;
    }

    function _registerScrollHandler(connector) {
        var $docContainer = $(DOC_CONTAINER_SELECTOR);
        var $changePaneContainer = $(CHANGE_PANE_CONTAINER);
        
        var data = {
            $docContainer: $docContainer,
            $changePaneContainer: $changePaneContainer
        }
        
        var docContainerScrollHandler = _docContainerScrollHandler.bind(undefined, connector, data);
        var changePaneContainerScrollHandler = _changePaneContainerScrollHandler.bind(undefined, connector, data);
        
        $docContainer.on("scroll.syncDocContainer",  LODASH.debounce(docContainerScrollHandler, 100));
        $changePaneContainer.on("scroll.syncChangePaneContainer", LODASH.debounce(changePaneContainerScrollHandler, 100));
    }
    
    function _unregisterScrollHandler(connector) {
        $(DOC_CONTAINER_SELECTOR).off("scroll.syncDocContainer");
        $(CHANGE_PANE_CONTAINER).off("scroll.syncChangePaneContainer");
        connector.userOriginated = true;
    }

    function _docContainerScrollHandler(connector, data, event) {
        if (connector.userOriginated) { // FIXME: find a better way to stop handling/firing non-user originated event
            log.debug("scrollHandler Called for - " + event.currentTarget.id);
            var $docContainer = data.$docContainer, $changePaneContainer = data.$changePaneContainer;

            var docElement = _getFirstElementInViewPort($docContainer);
            var changePaneElement = docElement ? document.getElementById(ID_PREFIX + docElement.id) : undefined;

            changePaneElement ? _scrollToPositionOfScrolledElement(connector, docElement, changePaneElement, $docContainer.get(0), $changePaneContainer.get(0)) : log
                    .warn(docElement ? docElement.id + " :element with id does not exist in change pane text container" : "Doc element does not exist");
        } else {
            connector.userOriginated = true;
        }
    }

    function _changePaneContainerScrollHandler(connector, data, event) {
        if (connector.userOriginated) { // FIXME: find a better way to stop handling/firing non-user originated event
            log.debug("scrollHandler Called for - " + event.currentTarget.id);
            var $docContainer = data.$docContainer, $changePaneContainer = data.$changePaneContainer;

            var changePaneElement = _getFirstElementInViewPort($changePaneContainer);
            changePaneElement = changePaneElement ? _getNonDeletedChangePaneElementInViewport(changePaneElement, $changePaneContainer) : undefined;
            if (changePaneElement) {
                var docElementId = changePaneElement.id.substring(ID_PREFIX.length); // extract the id after prefix
                var docElement = document.getElementById(docElementId);

                docElement ? _scrollToPositionOfScrolledElement(connector, changePaneElement, docElement, $changePaneContainer.get(0), $docContainer.get(0)) : log
                        .warn(changePaneElement ? changePaneElement.id + " :element with id does not exist  or not visible in view port"
                                : "Selected element does not exist");
            }
        } else {
            connector.userOriginated = true;
        }
    }
    
    function _getFirstElementInViewPort($scrolledContainer) {
        var elementInViewPort;
        var $childElements = $scrolledContainer.find(":not(" + NON_SELECTABLE_ELEMENTS + ")");
        $childElements.each(function(index, value) {
            if(_checkElementInViewport(this, $scrolledContainer.get(0))) {
               elementInViewPort = this; 
               return false;  
            }
        });
        return elementInViewPort;
    }
    
    function _getNonDeletedChangePaneElementInViewport(changePaneElement, $changePaneContainer) {
        if (changePaneElement && _isRemovedElement(changePaneElement)) {
            var nextChangePaneElement = $(changePaneElement).nextAll(":not(.leos-content-removed)"); //exclude deleted elements
            changePaneElement = _checkElementInViewport(nextChangePaneElement[0], $changePaneContainer.get(0)) ? nextChangePaneElement[0] : undefined;
        }
        return changePaneElement;
    }
    
    function _checkElementInViewport(element, container) {
        var elementTop = element.getBoundingClientRect().top,
        containerTop = container.getBoundingClientRect().top;
       
        return elementTop > (containerTop) && elementTop < (containerTop + $(container).height());
    }
    
    function _isRemovedElement(element) {
        return $(element).hasClass("leos-content-removed");
    }
    
    function _scrollToPositionOfScrolledElement(connector, scrolledElem, scrolledToElem, scrolledContainer, containerToBeScrolled) {
       var scrollTopPos = _getScrolledElementPositionFromTop(scrolledElem, scrolledToElem, scrolledContainer, containerToBeScrolled);
       _scrollPane(connector, containerToBeScrolled.id, scrollTopPos);
    }
    
    function _getScrolledElementPositionFromTop(scrolledElem, scrolledToElem, scrolledContainer, containerToBeScrolled) {
        var currentElementPositionRelativeToScreen = scrolledElem.getBoundingClientRect().top - scrolledContainer.getBoundingClientRect().top;
        var elementPositionToScroll = scrolledToElem.getBoundingClientRect().top - containerToBeScrolled.getBoundingClientRect().top;
        return (containerToBeScrolled.scrollTop + elementPositionToScroll - currentElementPositionRelativeToScreen);
    }
    
    function _scrollPane(connector, paneId, scrollTop) {
        $("#"+paneId).animate({
            scrollTop: scrollTop
        }, 100, "swing", function() {
            //Animation complete
            connector.userOriginated = false;
        });
     }
    
    // handle connector unregistration on client-side
    function _connectorUnregistrationListener() {
        var connector = this;
        log.debug("Unregistering ScrollPane extension...");
        // clean connector
        connector.userOriginated = null;
    }
    
    return {
        init: _initSyncScroll
    };
});
