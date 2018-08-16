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
define(function leosNonEditableEmptyWidgetModule(require) {
    "use strict";

    var widgetName = "leosNonEditableEmptyWidget";

    var elementName = "span";

    var elementClass = "non-editable-empty";

    var elementText = "\u00A0";

    var emptyElementTemplate = '<${elementName} class="${elementClass}">${elementText}</${elementName}>';

    var css = "styles/leosNonEditableEmptyWidget.css";

    var widgetDefinition = {
        inline: false,

        draggable: false,

        defaults: {
            deletable: false
        },

        template: emptyElementTemplate,

        requiredContent: "${elementName}(${elementClass})",

        upcast: function upcast(element) {
           return element.name === elementName && element.hasClass(elementClass);
        }
    };

    // return widget module
    return {
        name: widgetName,
        definition: widgetDefinition,
        elementName: elementName,
        elementClass: elementClass,
        elementText: elementText,
        css: css
    };
});