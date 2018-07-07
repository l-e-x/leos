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
    var transformerStamp = require("transformer/transformer");
    var transformationsConfigUtil = require("specs/util/transformationsConfigUtil");
    var ckEditorFragmentFactory = require("specs/util/ckEditorFragmentFactory");
    var transformationConfigResolverStamp = require("transformer/transformationConfigResolver");
    var aknNumberedParagraph = require("plugins/aknNumberedParagraph/aknNumberedParagraphPlugin");
    var aknOrderedList = require("plugins/aknOrderedList/aknOrderedListPlugin");
    var aknUnorderedList = require("plugins/aknUnorderedList/aknUnorderedListPlugin");
    var configNormalizerStampToTest = require("transformer/configNormalizer");

    var configs = transformationsConfigUtil.configs;
    var aknParagraphNormalizedConfig = configNormalizerStampToTest().getNormalizedConfig({
        rawConfig : aknNumberedParagraph.transformationConfig
    });
    var aknOrderedListNormalizedConfig = configNormalizerStampToTest().getNormalizedConfig({
        rawConfig : aknOrderedList.transformationConfig
    });
    
    var aknUnorderedListNormalizedConfig = configNormalizerStampToTest().getNormalizedConfig({
        rawConfig : aknUnorderedList.transformationConfig
    });

    var allConfigs = [ configs.aknArticle.normalizedConfig, configs.aknAlinea.normalizedConfig, configs.aknAuthorialNote.normalizedConfig,
            configs.aknHtmlItalic.normalizedConfig, configs.aknHtmlUnderline.normalizedConfig, configs.aknHtmlBold.normalizedConfig,
            configs.aknArticle.normalizedConfig, configs.aknHtmlAnchor.normalizedConfig, aknParagraphNormalizedConfig, aknOrderedListNormalizedConfig, aknUnorderedListNormalizedConfig ];

    function performTransformation(aknFragmentInHtml, htmlFragmentInHtml, configs, direction) {
        var inputFragmentInHtml;
        var outputFragmentInHtml;
        if (direction === "to") {
            inputFragmentInHtml = aknFragmentInHtml;
            outputFragmentInHtml = htmlFragmentInHtml;
        } else {
            inputFragmentInHtml = htmlFragmentInHtml;
            outputFragmentInHtml = aknFragmentInHtml;
        }

        var fragmentToBeTransformed = ckEditorFragmentFactory.getCkFragmentForHtml(inputFragmentInHtml);
        var transformationConfigResolver = transformationConfigResolverStamp();
        transformationConfigResolver.init({
            transformationConfigs : configs
        });

        var transformer = transformerStamp();
        transformer.transform({
            transformationConfigResolver : transformationConfigResolver,
            direction : direction,
            fragment : fragmentToBeTransformed
        });
        var transformedFragmentInHtml = ckEditorFragmentFactory.getHtmlForCkFragment(fragmentToBeTransformed);
        return {
            htmlInput : outputFragmentInHtml,
            htmlOutput : transformedFragmentInHtml
        };

    }

    function expectFragmentToBeTransformedCorrectlyForDirection(aknFragmentInHtml, htmlFragmentInHtml, configs, direction) {
        var result = performTransformation(aknFragmentInHtml, htmlFragmentInHtml, configs, direction);
        it("Transform akn: " + aknFragmentInHtml + " and html: " + htmlFragmentInHtml + " with direction: " + direction, function() {
            expect(result.htmlOutput).toEqual(result.htmlInput);

        });

    }
    ;

    function expectFragmentNotToBeTransformedCorrectlyForDirection(aknFragmentInHtml, htmlFragmentInHtml, configs, direction) {
        var result = performTransformation(aknFragmentInHtml, htmlFragmentInHtml, configs, direction);
        expect(result.htmlOutput).not.toEqual(result.htmlInput);
    }
    ;

    function expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, configs) {
        expectFragmentToBeTransformedCorrectlyForDirection(aknFragmentInHtml, htmlFragmentInHtml, configs, "to");
        expectFragmentToBeTransformedCorrectlyForDirection(aknFragmentInHtml, htmlFragmentInHtml, configs, "from");
    }
    ;

    describe(
            "Integration tests for /transformer/transformer",
            function() {

                describe(
                        "Test transformer.transform()",
                        function() {
                            describe(
                                    "Tests single transformations.",
                                    function() {
                                        describe(
                                                "Test akn article.",
                                                function() {
                                                    describe(
                                                            "Test akn article.",
                                                            function() {
                                                                var aknFragmentInHtml = '<article><num editable="false">Article No.</num><heading>Article Heading</heading></article>';
                                                                var htmlFragmentInHtml = '<article><h1 contenteditable="false">Article No.</h1><h2>Article Heading</h2></article>';
                                                                describe("Expects to transform akn article back and forth correctly.", function() {
                                                                    expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                                                });
                                                            });
                                                    describe(
                                                            "Test akn article with unknown child.",
                                                            function() {
                                                                var aknFragmentInHtml = "<article><num>Article No.</num><heading>Article Heading</heading><div>content</div></article>";
                                                                var htmlFragmentInHtml = '<article><h1 contenteditable="false">Article No.</h1><h2>Article Heading</h2></article>';
                                                                describe("Expects that unknown element of akn article will be skipped.", function() {
                                                                    expectFragmentToBeTransformedCorrectlyForDirection(aknFragmentInHtml, htmlFragmentInHtml,
                                                                            allConfigs, "to");
                                                                });
                                                            });
                                                });

                                        describe("Test akn alinea.", function() {
                                            var aknFragmentInHtml = "<alinea><content><mp>text content1</mp></content></alinea>";
                                            var htmlFragmentInHtml = '<p data-akn-name="alinea">text content1</p>';
                                            describe("Expects to transform akn alinea back and forth correctly.", function() {
                                                expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                            });
                                        });

                                        // at the moment we are not testing authorial note cause we decided to rethink implementation and than add missing tests

                                        describe("Test akn ordered list.", function() {
                                            describe("Expects to transform akn ordered list back and forth correctly.", function() {
                                                var aknFragmentInHtml = "<list><point><num>1.</num><content><mp>content 1</mp></content></point></list>";
                                                var htmlFragmentInHtml = '<ol data-akn-name="aknOrderedList"><li data-akn-num="1.">content 1</li></ol>';
                                                expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                            });
                                            describe("Expects to transform akn ordered list with alinea back and forth correctly.", function() {
                                                var aknFragmentInHtml = "<list><point><num>1.</num><alinea><content><mp>content 1</mp></content></alinea><list><point><num>11.</num><content><mp>nested content 1</mp></content></point></list></point></list>";
                                                var htmlFragmentInHtml = '<ol data-akn-name="aknOrderedList"><li data-akn-num="1."><p>content 1</p><ol data-akn-name="aknOrderedList"><li data-akn-num="11.">nested content 1</li></ol></li></ol>';
                                                expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                            });
                                        });

                                        describe("Test akn unordered list.", function() {
                                            describe("Expects to transform akn unordered list back and forth correctly.", function() {
                                                var aknFragmentInHtml = "<list><indent><num>1.</num><content><mp>content 1</mp></content></indent></list>";
                                                var htmlFragmentInHtml = '<ul data-akn-name="aknUnorderedList"><li data-akn-num="1.">content 1</li></ul>';
                                                expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                            });
                                            describe("Expects to transform akn unordered list with alinea back and forth correctly.", function() {
                                                var aknFragmentInHtml = "<list><indent><num>1.</num><alinea><content><mp>content 1</mp></content></alinea><list><indent><num>11.</num><content><mp>nested content 1</mp></content></indent></list></indent></list>";
                                                var htmlFragmentInHtml = '<ul data-akn-name="aknUnorderedList"><li data-akn-num="1."><p>content 1</p><ul data-akn-name="aknUnorderedList"><li data-akn-num="11.">nested content 1</li></ul></li></ul>';
                                                expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                            });
                                        });

                                        describe("Test akn html underline.", function() {
                                            var aknFragmentInHtml = "<u>text content1</u>";
                                            var htmlFragmentInHtml = '<u>text content1</u>';
                                            describe("Expects to transform akn html underline back and forth correctly.", function() {
                                                expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                            });
                                        });

                                        describe("Test akn html italic.", function() {
                                            var aknFragmentInHtml = "<i>text content1</i>";
                                            var htmlFragmentInHtml = '<em>text content1</em>';
                                            describe("Expects to transform akn html italic back and forth correctly.", function() {
                                                expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                            });
                                        });

                                        describe("Test akn html bold.", function() {
                                            var aknFragmentInHtml = "<b>text content1</b>";
                                            var htmlFragmentInHtml = '<strong>text content1</strong>';
                                            describe("Expects to transform akn html bold back and forth correctly.", function() {
                                                expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                            });
                                        });

                                        describe("Test akn html anchor.", function() {
                                            var aknFragmentInHtml = '<a href="link/ffdsa">test content</a>';
                                            var htmlFragmentInHtml = '<a href="link/ffdsa">test content</a>';
                                            describe("Expects to transform akn html anchor back and forth correctly.", function() {
                                                expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                            });
                                        });

                                    });

                            describe(
                                    "Test transformations for multiple elements",
                                    function() {
                                        describe(
                                                "Test mixture of:  akn paragraph, akn alinea, akn authorial note, akn ordered list, akn unordered list, akn html bold, akn html italic and akn html anchor .",
                                                function() {
                                                    var aknFragmentInHtml = '<paragraph id="art_n1__para_1"><num>1.</num><subparagraph><content><mp>Member States<i><b> shall bring i</b></i>nto force the laws...</mp></content></subparagraph><subparagraph><content><mp>When Member States adopt those provisions...</mp></content></subparagraph><list><point><num>(a)</num><alinea><content><mp>fdasfdsafsdafdsafsdafds</mp></content></alinea><list><point><num>(a)</num><content><mp>fdsafsdafdsa</mp></content></point></list></point></list></paragraph>';
                                                    var htmlFragmentInHtml = '<li id="art_n1__para_1" data-akn-name="aknNumberedParagraph" data-akn-num="1."><p>Member States<em><strong> shall bring i</strong></em>nto force the laws...</p><p>When Member States adopt those provisions...</p><ol data-akn-name="aknOrderedList"><li data-akn-num="(a)"><p>fdasfdsafsdafdsafsdafds</p><ol data-akn-name="aknOrderedList"><li data-akn-num="(a)">fdsafsdafdsa</li></ol></li></ol></li>';
                                                    describe("Expects to transform mix of elements back and forth correctly.", function() {
                                                        expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                                    });
                                                });

                                        describe(
                                                "Test mixture of: akn paragraph, akn subparagraph",
                                                function() {
                                                    var aknFragmentInHtml = '<paragraph><num>1</num><subparagraph><content><mp>Sub-Paragraph 1</mp></content></subparagraph><subparagraph><content><mp>Sub-Paragraph 2</mp></content></subparagraph></paragraph>';
                                                    var htmlFragmentInHtml = '<li data-akn-name="aknNumberedParagraph" data-akn-num="1"><p>Sub-Paragraph 1</p><p>Sub-Paragraph 2</p></li>';
                                                    describe("Expects that transformation will transform paragraph with two sub-paragraph back and forth correctly.",
                                                            function() {
                                                                expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                                            });
                                                });

                                        describe(
                                                "Test mixture of: akn paragraph, akn subparagraph, akn bold, akn italic",
                                                function() {
                                                    var aknFragmentInHtml = '<paragraph><num>1</num><subparagraph><content><mp><b>Sub-Paragraph 1</b></mp></content></subparagraph><subparagraph><content><mp><i>Sub-Paragraph 2</i></mp></content></subparagraph></paragraph>';
                                                    var htmlFragmentInHtml = '<li data-akn-name="aknNumberedParagraph" data-akn-num="1"><p><strong>Sub-Paragraph 1</strong></p><p><em>Sub-Paragraph 2</em></p></li>';
                                                    describe(
                                                            "Expects that transformation will transform paragraph with two sub-paragraph with bold,italic back and forth correctly.",
                                                            function() {
                                                                expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                                            });
                                                });

                                        describe(
                                                "Test mixture of: akn paragraph, akn subparagraph",
                                                function() {
                                                    var aknFragmentInHtml = '<paragraph><num>1</num><content><mp>Sub-Paragraph 1</mp></content></paragraph>';
                                                    var htmlFragmentInHtml = '<li data-akn-name="aknNumberedParagraph" data-akn-num="1"><p>Sub-Paragraph 1</p></li>';
                                                    describe(
                                                            "Expects that transformation will transform <li><p>text<p><li> to be transformed to single paragraph on from side.",
                                                            function() {
                                                                expectFragmentToBeTransformedCorrectlyForDirection(aknFragmentInHtml, htmlFragmentInHtml,
                                                                        allConfigs, "from");
                                                            });
                                                });

                                    });

                        });

            });

});