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
define(function aknArticleHeadingWidgetModule(require) {
    "use strict";

    var widgetName = "aknArticleHeading";

    var widgetCss = "css/aknArticleHeading.css";

    var widgetDefinition = {
        inline: false,

        draggable: false,

        defaults: {
            deletable: false
        },

        template: '<h2 class="akn-article-heading"></h2>',

        editables: {
            heading: {
                selector: ".akn-article-heading",
                allowedContent: "sup; sub"                      // TODO verify what should be the allowed content
            }
        },

        allowedContent: "h2(!akn-article-heading); sup; sub",   // TODO verify what should be the allowed content

        requiredContent: "h2(akn-article-heading)",             // TODO verify what should be the allowed content

        upcast: function upcast(element, data) {
            return ((element.name === "h2") && element.hasClass("akn-article-heading"));
        }
    };

    // return widget module
    var widgetModule = {
        name: widgetName,
        css: widgetCss,
        definition: widgetDefinition
    };

    return widgetModule;
});