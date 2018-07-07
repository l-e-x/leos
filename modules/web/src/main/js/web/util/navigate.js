/*
 * Copyright 2015 European Commission
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
function nav_navigateToContent(elementId) {
    var element = document.getElementById(elementId);
    if (element) {
        var bgColor = element.style.backgroundColor;
        element.style.backgroundColor = "cornsilk";
        setTimeout(function () {
            element.style.backgroundColor = bgColor;
        }, 500);
        element.scrollIntoView(true);
    }
}