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
define(function collapsibleEllipsisExtensionModule(require) {
    "use strict";

    function _initExtension(connector) {
        _addCollapsibleListener(connector);
        connector.onUnregister = _connectorUnregistrationListener;
        connector.addCollapsibleListener = _addCollapsibleListener;
    }

    function _addCollapsibleListener(connector) {
        if(connector === undefined) {
            connector = this;
        }
        $(".collapsible-text").addClass("ellipsis");
        toggleCollapsibleButton(connector);

        $(document).on("click", ".collapsible-button.expand", function() {
            var collapsibleText = $(this).prev();
            collapsibleText.removeClass("ellipsis");
            collapsibleText.removeClass("has-ellipsis");
            collapsibleText.append('<div class="collapsible-button collapse">' + connector.getState(false).showLess + '</div>');
            $(this).remove();
        });

        $(document).on("click", ".collapsible-button.collapse", function() {
            var collapsibleText = $(this).parent();
            collapsibleText.addClass("ellipsis");
            $(this).remove();
            toggleCollapsibleButton(connector);
        });

        $(".collapsible-text").watch("offsetWidth", function(propName, oldVal, newVal) {
            toggleCollapsibleButton(connector);
        });
    }

    function toggleCollapsibleButton(connector) {
        $(".collapsible-text").each(function() {
            if (isEllipsisActive(this)) {
                $(this).addClass("has-ellipsis");
                if ($(this).next(".expand").length === 0) {
                    $(this).after('<span class="collapsible-button expand">'+ connector.getState(false).showMore +'</span>');
                }
            } else {
                $(this).removeClass("has-ellipsis");
                $(this).next(".expand").remove();
            }
        });
    }

    function isEllipsisActive(element) {
        return element.offsetWidth < element.scrollWidth;
    }

    jQuery.fn.watch = function(id, fn) {
        return this.each(function() {
            var self = this;
            var oldVal = self[id];
            $(self).data(
                "watch_timer",
                setInterval(function() {
                    if (self[id] !== oldVal) {
                        fn.call(self, id, oldVal, self[id]);
                        oldVal = self[id];
                    }
                }, 100)
            );
        });
    };

    jQuery.fn.unwatch = function() {
        return this.each(function() {
            clearInterval($(this).data("watch_timer"));
        });
    };

    function _connectorUnregistrationListener() {
        var connector = this;
        _removeCollapsibleListeners();
    }

    function _removeCollapsibleListeners() {
        $(".collapsible-text").unwatch("offsetWidth");
        $(document).off("click", ".collapsible-button.expand");
        $(document).off("click", ".collapsible-button.collapse");
    }

    return {
        init: _initExtension
    };
});
