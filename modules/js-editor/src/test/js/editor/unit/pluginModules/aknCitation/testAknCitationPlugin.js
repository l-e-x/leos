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
define(function testAknCitationPlugin(require) {
    "use strict";
    var aknCitationPluginToTest = require("plugins/aknCitation/aknCitationPlugin");
    
    describe("Unit tests for plugins/aknCitationPlugin", function() {
        var transformationConfigForCitation = '{"akn":"citation","html":"p","attr":[{"akn":"id","html":"id"},{"akn":"leos:editable","html":"contenteditable"},{"html":"data-akn-name=citation"},{"html":"class=citation"}],"sub":{"akn":"mp","html":"p","attr":[{"akn":"id","html":"data-akn-mp-id"}],"sub":{"akn":"text","html":"p/text"}}}';

        it("Tests if transformation config is valid.", function() {
            expect(JSON.stringify(aknCitationPluginToTest.transformationConfig)).toEqual(transformationConfigForCitation);

        });

    });

});