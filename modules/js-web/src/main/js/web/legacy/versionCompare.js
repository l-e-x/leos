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
var versionCompare = versionCompare || {};

versionCompare.alignModifiedElements = function() {
    var modifElements = 0;
    var elements = document.getElementsByName('modification_' + modifElements++);
    while(elements.length > 0) {
        var firstModifElement = elements[0].parentNode;
        var secondModifElement = elements[1].parentNode;
        var firstHeight = firstModifElement.clientHeight;
        var secondHeight = secondModifElement.clientHeight;

        if(firstHeight > secondHeight) {
            firstModifElement.style.height = firstHeight + "px";
            secondModifElement.style.height = firstHeight + "px";
        } else if(secondHeight > firstHeight) {
            firstModifElement.style.height = secondHeight + "px";
            secondModifElement.style.height = secondHeight + "px";
        }
        elements = document.getElementsByName('modification_' + modifElements++);
    }
};