/*
 * Copyright 2017 European Commission
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
define(function leosCoreModule(require) {
    "use strict";

    // load module dependencies
    require("dateFormat");

    function _getParentElement(connector) {
        var element = null;
        if (connector) {
            var id = connector.getParentId();
            element = connector.getElement(id);
        }
        return element;
    }

    function _getContentWrap(rootElement, elementId) {
        var wrap = null;
        var selector = ".leos-wrap[data-wrapped-id='" + elementId + "'] > .leos-wrap-content";
        if (rootElement) {
            wrap = rootElement.querySelector(selector);
        } else {
            wrap = document.querySelector(selector);
        }
        return wrap;
    }

    function _getCurrentUTCDateAsString() {
        return new Date().format("UTC:yyyy-mm-dd'T'HH:MM:ss'Z'");
    }

    function _getLocalDateFromUTCAsString(utcDate, mask) {
        mask = mask || "yyyy-mm-dd HH:MM:ss";
        return new Date(utcDate).format(mask);
    }

    return {
        getParentElement: _getParentElement,
        getContentWrap: _getContentWrap,
        getCurrentUTCDateAsString: _getCurrentUTCDateAsString,
        getLocalDateFromUTCAsString: _getLocalDateFromUTCAsString
    };
});
