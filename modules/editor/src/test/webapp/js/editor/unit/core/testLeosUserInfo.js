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
define(function testLeosUserInfoModule(require) {
    "use strict";
    var leosUserInfo = require("core/leosUserInfo");

    describe("Unit tests for core/leosUserInfo", function() {
        describe("User info not provided", function() {
            var editor = {
                LEOS : {}
            };
            it("Return undefined from getUserLogin when userInfo undefined.", function() {
                expect(leosUserInfo.getUserLogin(editor)).toEqual(undefined);
            });
            it("Return undefined from getUserName when userInfo undefined.", function() {
                expect(leosUserInfo.getUserName(editor)).toEqual(undefined);
            });
        });

        describe("User info provided", function() {
            var editor = {
                LEOS : {
                    userInfo : {
                        userName : "testName",
                        userLogin : "testLogin"
                    }
                }
            };
            it("Return name from getUserLogin when userInfo provided.", function() {
                expect(leosUserInfo.getUserLogin(editor)).toEqual("testLogin");
            });
            it("Return login from getUserName when userInfo provided.", function() {
                expect(leosUserInfo.getUserName(editor)).toEqual("testName");
            });
        });
    });

});