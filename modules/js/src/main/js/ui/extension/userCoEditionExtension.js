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
define(function userCoEditionExtensionModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var $ = require("jquery");

    function _init(connector) {
        log.debug("Initializing User CoEdition extension...");
        log.debug("Registering User CoEdition listeners...");
        connector.onUnregister = _connectorUnregistrationListener;
        connector.onStateChange = _connectorStateChangeListener;
    }
    
    function _connectorUnregistrationListener() {
        var connector = this;
        log.debug("Unregistering User CoEdition extension...");
    }
    
    function _connectorStateChangeListener() {
        var connector = this;
        log.debug("User CoEdition extension state changed...");
        // KLUGE delay execution due to sync issues with target update
        setTimeout(_updateUserCoEditionInfo, 1000, connector.getState().coEditionElements);
    }
    
    function _updateUserCoEditionInfo(coEditionElements) {
        log.debug("User CoEdition _updateUserCoEditionInfo invoked...");
    	if (window.MathJax) {
    		window.MathJax.Hub.Queue([_displayUserCoEditionInfo, coEditionElements]);
    	} else {
    		_displayUserCoEditionInfo(coEditionElements);
    	}
    }
    
    function _displayUserCoEditionInfo(coEditionElements) {
        $("div#docContainer akomantoso .leos-user-coedition").remove();
        let $editedElement = _getEditedElement();
        $.each(coEditionElements, function(key, value) {
            let $element = _getElement($editedElement, key);
            if ($element.length) {
                let userCoEdition = $.parseHTML('<div class="leos-user-coedition"><span class="Vaadin-Icons">&#xe80d</span><div>' + value + '</div></div>');
                if (!value.includes("href=\"")) {
                    $(userCoEdition).addClass("leos-user-coedition-self-user");
                }
                $(userCoEdition).css("top", $element.position().top);
                $(userCoEdition).css("left", $element.position().left - 25);
                $element.before(userCoEdition);
            }
        });
        $("div#docContainer akomantoso .leos-user-coedition").mouseenter(function() {
            let posIcon = $(this).find("span").position();
            $(this).find("div").css("left", posIcon.left + 10).fadeIn("fast");
        }).mouseleave(function() {
            $(this).find("div").fadeOut("fast");
        });
    }
    
    function _getEditedElement() {
    	return $("div#docContainer akomantoso div[data-wrapped-id]");
    }
    
    function _getElement($editedElement, key) {
        let $element = ($editedElement.length && $editedElement.attr("data-wrapped-id") == key) ? $editedElement : $("div#docContainer akomantoso #" + key);
        return ($element.length && $element.prev("num").length) ? $element.prev("num") : $element;
    }
    
    return {
        init: _init
    };
    
});