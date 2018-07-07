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
define(function ckEditorFragmentFactoryModule(require) {
    "use strict";

    var STAMPIT = require("stampit");
    var CKEDITOR = require("promise!ckEditor");

    var ckEditorFragmentFactoryStamp = STAMPIT().methods({
        getCkFragmentForHtml: function getCkFragmentForHtml(html) {
            var ckEditorFragment = CKEDITOR.htmlParser.fragment.fromHtml(html, '', false);
            return ckEditorFragment;
        },

        getHtmlForCkFragment: function getHtmlForCkFragment(ckFragment) {
            var writer = new CKEDITOR.htmlParser.basicWriter();
            ckFragment.writeChildrenHtml(writer);
            var html = writer.getHtml(true);
            return html;
        }
    });

    return ckEditorFragmentFactoryStamp();
});