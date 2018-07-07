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
define(function testTransformerModule(require) {
    "use strict";
    var STAMPIT = require("stampit");
    var transformerStampToTest = require("transformer/transformer");
    var ckEditorFragmentFactory = require("specs/util/ckEditorFragmentFactory");

    describe("Unit Tests for: /transformer/transformer/transform().", function() {

        var fragmentInHtml;
        var fragment;
        var transformationConfigs;
        var direction;
        var transformationConfigResolver = {};

        beforeEach(function() {
            fragmentInHtml = "<div></div>";
            fragment = ckEditorFragmentFactory.getCkFragmentForHtml(fragmentInHtml);
            transformationConfigs = [];
            direction = "whatever";

        });

        it("Required param: 'fragment' not provided.", function() {
            expect(function() {
                var transformerToTest = transformerStampToTest();
                transformerToTest.transform({
                    transformationConfigResolver: transformationConfigResolver,
                    direction: direction,
                    fragment: undefined
                });
            }).toThrowError("Param with name: 'fragment' is required, please provide it.");

        });

        it("Required param: 'direction' not provided.", function() {
            expect(function() {
                var transformerToTest = transformerStampToTest();
                transformerToTest.transform({
                    transformationConfigResolver: transformationConfigResolver,
                    direction: undefined,
                    fragment: fragment
                });
            }).toThrowError("Param with name: 'direction' is required, please provide it.");

        });

        it("Required param: 'transformationConfigResolver' not provided.", function() {
            expect(function() {
                var transformerToTest = transformerStampToTest();
                transformerToTest.transform({
                    transformationConfigResolver: undefined,
                    direction: direction,
                    fragment: fragment
                });
            }).toThrowError("Param with name: 'transformationConfigResolver' is required, please provide it.");
        });

        it("Check if transformation is replacing fragments children.", function() {
            var productStub = {};
            var transformationsCount = 0;
            var fragmentTransformerMock = {
                getTransformedElement: function getTransformedElement(params) {
                    transformationsCount++;
                    if (fragment === params.fragment) {
                        return null;
                    }
                    return productStub;
                }
            };
            var elementReplaceCount = 0;
            var elementMock = {
                replaceWith: function(element) {
                    if (productStub === element) {
                        elementReplaceCount++;
                    }
                }

            };
            fragment.children[0] = elementMock;
            var transformerStubStamp = STAMPIT().compose(transformerStampToTest, (STAMPIT().methods({
                _getFragmentTransformer: function _getFragmentTransformer() {
                    return fragmentTransformerMock;
                }

            })));
            var transformerToTest = transformerStubStamp();
            transformerToTest.transform({
                transformationConfigResolver: transformationConfigResolver,
                direction: direction,
                fragment: fragment
            });
            expect(elementReplaceCount).toEqual(1);
            expect(transformationsCount).toEqual(2);

        });

    });

});