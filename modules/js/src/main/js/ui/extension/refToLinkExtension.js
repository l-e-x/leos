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
define(function refToLinkExtensionModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var $ = require("jquery");
    var refToLink = require("refToLink");
    var UTILS = require("core/leosUtils");

    function _initRefToLink(connector) {
        log.debug("Initializing refToLink extension...");

        // configure ref2Link
        $.fn.ref2link.options = {tooltipTrigger: 'notooltip'}; //Disabling the tooltip 
        // $.fn.ref2link.setFilter('environment', ['SOLON-PRD', ...]);// enable sets of rules

        log.debug("Registering refToLink extension unregistration listener...");
        connector.onUnregister = _connectorUnregistrationListener;

        log.debug("Registering refToLink extension state change listener...");
        connector.onStateChange = _connectorStateChangeListener;
    }

    // handle connector unregistration on client-side
    function _connectorUnregistrationListener() {
        var connector = this;
        log.debug("Unregistering refToLink extension...");
        $.fn.ref2link.clearCache();
    }

    // handle connector state change on client-side
    function _connectorStateChangeListener() {
        var connector = this;
        log.debug("refToLink extension state changed...");
        // KLUGE delay execution due to sync issues with target update
        setTimeout(_renderLinks, 500, connector);
    }

    function _renderLinks(connector) {
        log.debug("Rendering links...");
        var container = UTILS.getParentElement(connector),
            textNodes = _textNodesUnder(container),
            references = $(container).clone().getReferences();

        //Sort to handle case where two references are in same line Example art 2 directive 2017/11/EC  and directive 2017/11/EC
        references.sort(function (left, right) {
            return right.match.length - left.match.length;
        });

        //1. check for all references in text nodes
        //2. check the reference with Longest match first and if found, store a placeholder
        //3. if cache has something replace placeholders with values
        var cache = {}; //placeholder-reference cache. 
        textNodes.forEach(function (textNode, txtIndex) {
            var newVal = textNode.nodeValue;
            references.forEach(function (ref, refIndex) {
                if (newVal.indexOf(ref.wholeMatch) > -1) {
                    newVal = _injectPlaceholders(newVal, '##R' + refIndex + '##', ref, cache);
                }
            });

            if (Object.keys(cache).length > 0) {
                newVal = _ejectPlaceholders(newVal, cache);
                $(textNode).replaceWith(newVal); //inject in DOM
            }
        });
        
        //helper funcitons
        function _injectPlaceholders(text, placeholder, ref, cache) {
            cache[placeholder] = ref;
            return text.replace(new RegExp($.fn.ref2link.regExpEscape(ref.wholeMatch), 'g'), placeholder);
        }

        function _ejectPlaceholders(text, cache) {
            Object.keys(cache).forEach(function (placeholder) {
                // the new value to replace is coming as an attribute of the array cache[placeholder].views
                var arrViews = cache[placeholder].views;
                Object.keys(arrViews).forEach(function (key) {
                    text = text.replace(new RegExp(placeholder, 'g'), arrViews[key]);
                });
            });
            return text;
        }
    }

    function _textNodesUnder(el) {
        var node, result = [],
            walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT,
                {
                    acceptNode: function (node) {
                        return /^(\s*)(\S+)/.test(node.nodeValue)
                            ? NodeFilter.FILTER_ACCEPT
                            : NodeFilter.FILTER_REJECT;
                    }
                }, false);

        //walk
        while (node = walker.nextNode()) {
            result.push(node);
        }
        return result;
    }

    return {
        init: _initRefToLink
    };
});
