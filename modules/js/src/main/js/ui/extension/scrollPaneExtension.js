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
define(function scrollPaneExtensionModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var UTILS = require("core/leosUtils");
    var $ = require("jquery");
    var LODASH = require("lodash");
    
    var DOC_CONTAINER_SELECTOR = ".leos-doc-content";
    var MARKED_CONTAINER_SELECTOR = ".leos-marked-content";

    const NON_SELECTABLE_ELEMENTS = "bill, doc, num, content, authorialNote, inline, ref, span, br, tbody, tr, td, th," +
    		                               "a, b, strong, i, em, hr, marker, text, .leos-actions, .cke_editable, hypothesis-highlight";
    
    function _initSyncScroll(connector) {
        log.debug("Initializing ScrollPane extension...");
        connector.setSyncState = _setSyncState;
        connector.userOriginated = true;
        _registerScrollHandler(connector);
        
        log.debug("Registering ScrollPane extension unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;
    }

    function _setSyncState(enabled) {
        var connector = this;
        if(enabled) {
            _unregisterScrollHandler(connector);
            _registerScrollHandler(connector);
            $(DOC_CONTAINER_SELECTOR).trigger("scroll");
        } else {
            _unregisterScrollHandler(connector);
        }
    }
    
    function _registerScrollHandler(connector) {
        var $docContainer = $(DOC_CONTAINER_SELECTOR);
        var $markedContainer = $(MARKED_CONTAINER_SELECTOR);
        
        var $docContainerElements = $docContainer.find(":not(" + NON_SELECTABLE_ELEMENTS + ")");
        var $markedContainerElements = $markedContainer.find(":not(" + NON_SELECTABLE_ELEMENTS + ")");
        
        var data = {
            $docContainer: $docContainer,
            $markedContainer: $markedContainer
        }
        
        var docContainerScrollHandler = _docContainerscrollHandler.bind(undefined, connector, data);
        var markedContainerScrollHandler = _markedContainerscrollHandler.bind(undefined, connector, data);
        
        $docContainer.on("scroll.syncMarkedContainer",  LODASH.debounce(docContainerScrollHandler, 100));
        $markedContainer.on("scroll.syncDocContainer", LODASH.debounce(markedContainerScrollHandler, 100));
    }
    
    function _unregisterScrollHandler(connector) {
        $(DOC_CONTAINER_SELECTOR).off("scroll.syncMarkedContainer");
        $(MARKED_CONTAINER_SELECTOR).off("scroll.syncDocContainer");
        connector.userOriginated = true;
    }

    function _docContainerscrollHandler(connector, data, event) {
        if (connector.userOriginated) { // FIXME: find a better way to stop handling/firing non-user originated event
            log.debug("scrollHandler Called for - " + event.currentTarget.id);
            var $docContainer = data.$docContainer, $markedContainer = data.$markedContainer;

            var docElement = _getFirstElementInViewPort($docContainer);
            var markedElement = docElement ? document.getElementById("marked-" + docElement.id) : undefined;

            markedElement ? _scrollToPositionOfScrolledElememnt(connector, docElement, markedElement, $docContainer.get(0), $markedContainer.get(0)) : log
                    .warn(docElement ? docElement.id + " :element with id does not exist in marked text container" : "Doc element does not exist");
        } else {
            connector.userOriginated = true;
        }
    }

    function _markedContainerscrollHandler(connector, data, event) {
        if (connector.userOriginated) { // FIXME: find a better way to stop handling/firing non-user originated event
            log.debug("scrollHandler Called for - " + event.currentTarget.id);
            var $docContainer = data.$docContainer, $markedContainer = data.$markedContainer;

            var markedElement = _getFirstElementInViewPort($markedContainer);
            markedElement = markedElement ? _getNonDeletedMarkedElementInViewport(markedElement, $markedContainer) : undefined;
            if (markedElement) {
                var docElementId = markedElement.id.substring(7); // extract the id after 'marked-' prefix
                var docElement = document.getElementById(docElementId);

                docElement ? _scrollToPositionOfScrolledElememnt(connector, markedElement, docElement, $markedContainer.get(0), $docContainer.get(0)) : log
                        .warn(markedElement ? markedElement.id + " :element with id does not exist  or not visible in view port"
                                : "Marked element does not exist");
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
    
    function _getNonDeletedMarkedElementInViewport(markedElement, $markedContainer) {
        if (markedElement && _isRemovedElement(markedElement)) {
            var nextMarkedElement = $(markedElement).nextAll(":not(.leos-content-removed)"); //exclude deleted elements
            markedElement = _checkElementInViewport(nextMarkedElement[0], $markedContainer.get(0)) ? nextMarkedElement[0] : undefined;
        }
        return markedElement;
    }
    
    function _checkElementInViewport(element, container) {
        var elementTop = element.getBoundingClientRect().top,
        containerTop = container.getBoundingClientRect().top;
       
        return elementTop > (containerTop) && elementTop < (containerTop + $(container).height());
    }
    
    function _isRemovedElement(element) {
        return $(element).hasClass("leos-content-removed");
    }
    
    function _scrollToPositionOfScrolledElememnt(connector, scrolledElem, scrolledToElem, scrolledContainer, containerToBeScrolled) {
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
