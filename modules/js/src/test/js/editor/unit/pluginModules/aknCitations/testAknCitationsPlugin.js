/*
 * Copyright 2019 European Commission
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
define(function testAknCitationsPlugin(require) {
    "use strict";
    var aknCitationsPluginToTest = require("plugins/aknCitations/aknCitationsPlugin");
    describe("Unit tests for plugins/aknCitationsPlugin", function() {
        var transformationConfigForCitations = '{"akn":"citations","html":"div[data-akn-name=citations]","attr":[{"akn":"xml:id","html":"id"},{"akn":"leos:origin","html":"data-origin"},{"akn":"leos:editable","html":"data-akn-attr-editable"},{"html":"data-akn-name=citations"}]}';
        it("Tests if transformation config is valid.", function() {
            expect(JSON.stringify(aknCitationsPluginToTest.transformationConfig)).toEqual(transformationConfigForCitations);

        });

    });

});