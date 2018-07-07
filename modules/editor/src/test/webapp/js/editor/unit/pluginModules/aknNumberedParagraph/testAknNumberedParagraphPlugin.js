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
; // jshint ignore:line
define(function aknNumberedParagraphPluginModule(require) {
    "use strict";
    var aknNumburedParagraphModule = require("plugins/aknNumberedParagraph/aknNumberedParagraphPlugin");
    var DATA_AKN_NUM = "data-akn-num";


    describe("Unit tests for: plugins/aknNumberedParagraph/aknNumberedParagraphPlugin", function() {
        var transformationConfigForNumberedParagraph = '{"akn":"paragraph","html":"li","attr":[{"akn":"leos:editable","html":"contenteditable"},{"akn":"id","html":"id"},{"html":"data-akn-name=aknNumberedParagraph"}],"sub":[{"akn":"num","html":"li","attr":[{"akn":"id","html":"data-akn-num-id"}],"sub":{"akn":"text","html":"li[data-akn-num]"}},{"akn":"content","html":"li","attr":[{"akn":"id","html":"data-akn-content-id"}],"sub":{"akn":"mp","html":"li","attr":[{"akn":"id","html":"data-akn-mp-id"}],"sub":{"akn":"text","html":"li/text"}}}]}';

        it("Tests if transformation config is valid.", function() {
            expect(JSON.stringify(aknNumburedParagraphModule.transformationConfig)).toEqual(transformationConfigForNumberedParagraph);

        });
    });

});