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
define(function contentScrollerModule(require) {
    "use strict";

    var $ = require("jquery");

    function _scrollTo(element, additionalAction) {
        if (typeof element == 'string') {//if ID is passed get element
            element = document.getElementById(element);
        }

        var $scrollPane = $('.leos-doc-content');
        if (element) {
            $scrollPane.animate(
                {scrollTop: _calculateScrollTopPosition(element, $scrollPane)},
                500, "swing",
                _onScrollCompletion
            );
        }

        function _onScrollCompletion() {
            var bgColor = element.style.backgroundColor;
            element.style.backgroundColor = "cornsilk";
            setTimeout(function () {
                element.style.backgroundColor = bgColor;
            }, 500);
            if (additionalAction) {
                additionalAction(element);
            }
        }

        function _calculateScrollTopPosition(element, $scrollPane) {
            element = _getPositionedElement(element);// this fix is required for the elements which are not displayed

            //get bounding rect returns position corresponding to browser top. So subtracting container top to convert it to reference to container
            var currentElementPositionRelativeToWindow = element.getBoundingClientRect().top - $scrollPane[0].getBoundingClientRect().top;

            var newScrollTopForPane = currentElementPositionRelativeToWindow
                + $scrollPane[0].scrollTop  //adding already scrolled factor to position to make it relative to target pane
                - ($('.cke_inner').length > 0 ? $('.cke_inner').outerHeight() : 76);//76 is to shift the selected content little away from top bar

            return newScrollTopForPane;
        }

        function _getPositionedElement(element) {
            while ($(element).is(':hidden')) {
                element = element.parentElement;
            }
            return element;
        }
    }

    //Exposing this function to used only for cases where modules are not available
    LEOS.scrollTo = _scrollTo;

    return {
        scrollTo : _scrollTo
    };
});
