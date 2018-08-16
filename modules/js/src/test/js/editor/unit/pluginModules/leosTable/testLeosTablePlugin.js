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
define(function testLeosTablePlugin(require) {
    "use strict";
    var leosTablePluginToTest = require("plugins/leosTable/leosTablePlugin");

    describe("Unit tests for plugins/leosTable/leosTablePlugin", function() {
        var transformationConfigForLeosTable = '{"akn":"table","html":"table","attr":[{"akn":"id","html":"id"},{"akn":"border","html":"border"},{"akn":"cellpadding","html":"cellpadding"},{"akn":"cellspacing","html":"cellspacing"},{"akn":"style","html":"style"},{"html":"data-akn-name=leosTable"}],"sub":[{"akn":"tbody","html":"table/tbody","attr":[{"akn":"id","html":"id"}],"sub":[{"akn":"tr","html":"table/tbody/tr","attr":[{"akn":"id","html":"id"}],"sub":[{"akn":"td","html":"table/tbody/tr/td","attr":[{"akn":"rowspan","html":"rowspan"},{"akn":"colspan","html":"colspan"},{"akn":"style","html":"style"},{"akn":"id","html":"id"}],"sub":{"akn":"text","html":"table/tbody/tr/td/text"}}]}]},{"akn":"caption","html":"table/caption","attr":[{"akn":"id","html":"id"}],"sub":{"akn":"text","html":"table/caption/text"}}]}';

        it("Tests if transformation config is valid.", function() {
            pending(); //FIXME: skipping as this test config needs to rewritten
            expect(JSON.stringify(leosTablePluginToTest.transformationConfig)).toEqual(transformationConfigForLeosTable);
        });

    });

});