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
define(function leosCoreModule(require) {
    "use strict";

    var dateFormat = require("dateFormat");

    // configuration
    var config = {};

    // channels
    config.channels = {
        locks: {
            name: "locks",
            diagnostics: false
        },
        editor: {
            name: "editor",
            diagnostics: false
        }
    };

    // locks
    config.lockLevels = {
        document: "DOCUMENT_LOCK",
        element: "ELEMENT_LOCK",
        read: "READ_LOCK"
    };

    // messages
    config.messages = {
        action: {
            "lock.document": "Document is locked by ",
            "lock.element": "Element is locked by "
        }
    };

    // utilities
    var utils = {};

    utils.getParentElement = function(connector) {
        var element = null;
        if (connector) {
            var id = connector.getParentId();
            element = connector.getElement(id);
        }
        return element;
    };

    utils.getContentWrap = function(rootElement, elementId) {
        var wrap = null;
        var selector = ".leos-wrap[data-wrapped-id='" + elementId + "'] > .leos-wrap-content";
        if (rootElement) {
            wrap = rootElement.querySelector(selector);
        } else {
            wrap = document.querySelector(selector);
        }
        return wrap;
    };

    utils.getCurrentUTCDateAsString = function() {
        return new Date().format("UTC:yyyy-mm-dd'T'HH:MM:ss'Z'");
    };

    utils.getLocalDateFromUTCAsString = function(utcDate, mask) {
        mask = mask || "yyyy-mm-dd HH:MM:ss";
        return new Date(utcDate).format(mask);
    };

    return {
        config: config,
        utils: utils
    };
});