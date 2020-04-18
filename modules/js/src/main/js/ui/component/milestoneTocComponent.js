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
define(function milestoneTocComponentModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var $ = require("jquery");
    var contentScroller = require("contentScroller");

    // handle slider initialization
    function _init(connector) {
        log.debug("Registering Milestone Toc component unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;

        log.debug("Registering Milestone Toc component state change listener...");
        connector.onStateChange = _connectorStateChangeListener;
    }

    // handle connector unregistration on client-side
    function _connectorUnregistrationListener() {
        var connector = this;
        log.debug("Unregistering Milestone TOC component...");
    }
    
    function buildTree(treeContainer, toc_data) {
        treeContainer.tree({
            data: toc_data,
            autoOpen: true
        });

        treeContainer.on('tree.click', function(e) {
            var selected_node = e.node;
            if (selected_node && selected_node.href) {
                var elementId = selected_node.href;
                elementId = elementId.startsWith("#") ? elementId.substring(1, elementId.length) : elementId;
                LEOS.scrollTo(elementId);
            }
        });
    }
    
    // handle connector state change from server-side
    function _connectorStateChangeListener() {
        var connector = this;
        var treeContainer = $('#treeContainer');
        var state = connector.getState();
        var toc_data = state.tocData;
        toc_data = JSON.parse(toc_data); // convert string data to json object
        buildTree(treeContainer, toc_data); 
    }
    
    return {
        init: _init
    };
});