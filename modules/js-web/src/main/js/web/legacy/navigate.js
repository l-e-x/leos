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
/**
 * Navigate from TOC on selection of tree node to the selected section of document
 * @param elementId
 * 
 */
function nav_navigateToContent(elementId, additionalAction) {
    var element = document.getElementById(elementId);
    nav_navigateToElement(element, additionalAction);
}

function nav_navigateToElement(element, additionalAction) {
    if (element) {
        $('.leos-viewdoc-content').animate(
            { scrollTop: _calculateScrollTopPosition(element)},
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
        if(additionalAction){
            additionalAction(element);
        }
    }

    function _calculateScrollTopPosition(element) {
        return  $('.leos-viewdoc-content')[0].scrollTop
            + $(element).position().top
            - ($('.cke_inner').length> 0 ? $('.cke_inner').outerHeight():76);//76 is to center the selected content
    }
}