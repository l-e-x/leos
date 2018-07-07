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
define(function testTransformerModule(require) {
    "use strict";
    var transformerStamp = require("transformer/transformer");
    var transformationsConfigUtil = require("specs/util/transformationsConfigUtil");
    var ckEditorFragmentFactory = require("specs/util/ckEditorFragmentFactory");
    var transformationConfigResolverStamp = require("transformer/transformationConfigResolver");

    var configs = transformationsConfigUtil.configs;
    var allConfigs = [configs.aknArticle.normalizedConfig, configs.aknParagraph.normalizedConfig, configs.aknAlinea.normalizedConfig,
            configs.aknAuthorialNote.normalizedConfig, configs.aknHtmlItalic.normalizedConfig, configs.aknHtmlUnderline.normalizedConfig,
            configs.aknHtmlBold.normalizedConfig, configs.aknArticle.normalizedConfig, configs.aknOrderedList.normalizedConfig,
            configs.aknUnOrderedList.normalizedConfig, configs.aknHtmlAnchor.normalizedConfig];

    var onlyAknParagraphConfig = [configs.aknParagraph.normalizedConfig];
    var aknParagraphAndAknAlineaConfigs = [configs.aknParagraph.normalizedConfig, configs.aknAlinea.normalizedConfig];

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
            transformationConfigs: configs
        });

        var transformer = transformerStamp();
        transformer.transform({
            transformationConfigResolver: transformationConfigResolver,
            direction: direction,
            fragment: fragmentToBeTransformed
        });
        var transformedFragmentInHtml = ckEditorFragmentFactory.getHtmlForCkFragment(fragmentToBeTransformed);
        return {
            htmlInput: outputFragmentInHtml,
            htmlOutput: transformedFragmentInHtml
        };

    }

    function expectFragmentToBeTransformedCorrectlyForDirection(aknFragmentInHtml, htmlFragmentInHtml, configs, direction) {
        var result = performTransformation(aknFragmentInHtml, htmlFragmentInHtml, configs, direction);
        expect(result.htmlOutput).toEqual(result.htmlInput);

    };

    function expectFragmentNotToBeTransformedCorrectlyForDirection(aknFragmentInHtml, htmlFragmentInHtml, configs, direction) {
        var result = performTransformation(aknFragmentInHtml, htmlFragmentInHtml, configs, direction);
        expect(result.htmlOutput).not.toEqual(result.htmlInput);
    };

    function expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, configs) {
        expectFragmentToBeTransformedCorrectlyForDirection(aknFragmentInHtml, htmlFragmentInHtml, configs, "to");
        expectFragmentToBeTransformedCorrectlyForDirection(aknFragmentInHtml, htmlFragmentInHtml, configs, "from");
    };

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
                                                                it("Expects to transform akn article back and forth correctly.", function() {
                                                                    expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                                                });
                                                            });
                                                    describe(
                                                            "Test akn article with unknown child.",
                                                            function() {
                                                                var aknFragmentInHtml = "<article><num>Article No.</num><heading>Article Heading</heading><div>content</div></article>";
                                                                var htmlFragmentInHtml = '<article><h1 contenteditable="false">Article No.</h1><h2>Article Heading</h2></article>';
                                                                it("Expects that unknown element of akn article will be skipped.", function() {
                                                                    expectFragmentToBeTransformedCorrectlyForDirection(aknFragmentInHtml,
                                                                            htmlFragmentInHtml, allConfigs, "to");
                                                                });
                                                            });
                                                });

/*                                        describe("Test akn paragraph.", function() {
                                            describe("Test akn paragraph with data-akn-name", function() {
                                                var aknFragmentInHtml = "<paragraph><num>2</num><content><mp>test content</mp></content></paragraph>";
                                                var htmlFragmentInHtml = '<li data-akn-name="aknNumberedParagraph" data-akn-num="2">test content</li>';
                                                it("Expects to transform akn paragraph back and forth correctly.", function() {
                                                    expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                                });
                                            });

                                            describe("Test akn paragraph without data-akn-name and without conflicting paragraph config.", function() {
                                                var aknFragmentInHtml = "<paragraph><num>2</num><content><mp>test content</mp></content></paragraph>";
                                                var htmlFragmentInHtml = '<li data-akn-name="aknNumberedParagraph" data-akn-num="2">test content</li>';
                                                it("Expects to transform akn paragraph from html to akn properly without data-akn-name.", function() {
                                                    expectFragmentToBeTransformedCorrectlyForDirection(aknFragmentInHtml, htmlFragmentInHtml,
                                                            onlyAknParagraphConfig, "from");
                                                });
                                            });
                                        });
*/
                                        describe("Test akn alinea.", function() {
                                            var aknFragmentInHtml = "<alinea><content><mp>text content1</mp></content></alinea>";
                                            var htmlFragmentInHtml = '<p data-akn-name="alinea">text content1</p>';
                                            it("Expects to transform akn alinea back and forth correctly.", function() {
                                                expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                            });
                                        });

                                        // at the moment we are not testing authorial note cause we decided to rethink implementation and than add missing tests

                                        describe(
                                                "Test akn ordered list.",
                                                function() {
                                                    var aknFragmentInHtml = "<list><point><num>1.</num><content><mp>content 1</mp></content></point><point><num>2.</num><content><mp>content 2</mp></content></point></list>";
                                                    var htmlFragmentInHtml = '<ol><li num="1.">content 1</li><li num="2.">content 2</li></ol>';
                                                    it("Expects to transform akn ordered list back and forth correctly.", function() {
                                                        expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                                    });
                                                });

                                        describe(
                                                "Test akn unordered list.",
                                                function() {
                                                    var aknFragmentInHtml = "<list><indent><num>1.</num><content><mp>content 1</mp></content></indent><indent><num>2.</num><content><mp>content 2</mp></content></indent></list>";
                                                    var htmlFragmentInHtml = '<ul><li num="1.">content 1</li><li num="2.">content 2</li></ul>';
                                                    it("Expects to transform akn unordered list back and forth correctly.", function() {
                                                        expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                                    });
                                                });

                                        describe("Test akn html underline.", function() {
                                            var aknFragmentInHtml = "<u>text content1</u>";
                                            var htmlFragmentInHtml = '<u>text content1</u>';
                                            it("Expects to transform akn html underline back and forth correctly.", function() {
                                                expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                            });
                                        });

                                        describe("Test akn html italic.", function() {
                                            var aknFragmentInHtml = "<i>text content1</i>";
                                            var htmlFragmentInHtml = '<em>text content1</em>';
                                            it("Expects to transform akn html italic back and forth correctly.", function() {
                                                expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                            });
                                        });

                                        describe("Test akn html bold.", function() {
                                            var aknFragmentInHtml = "<b>text content1</b>";
                                            var htmlFragmentInHtml = '<strong>text content1</strong>';
                                            it("Expects to transform akn html bold back and forth correctly.", function() {
                                                expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                            });
                                        });

                                        describe("Test akn html anchor.", function() {
                                            var aknFragmentInHtml = '<a href="link/ffdsa">test content</a>';
                                            var htmlFragmentInHtml = '<a href="link/ffdsa">test content</a>';
                                            it("Expects to transform akn html anchor back and forth correctly.", function() {
                                                expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                            });
                                        });

                                    });

                            describe(
                                    "Test transformations for multiple elements",
                                    function() {
                                        describe(
                                                "Test mixture of: akn article, akn paragraph, akn alinea, akn authorial note, akn ordered list, akn unordered list, akn html bold, akn html italic and akn html anchor .",
                                                function() {
                                                    var aknFragmentInHtml = '<article id="articleId"><num editable="false">Article No.</num><heading>Article Heading</heading><list><point><num>1.</num><content><mp>ord<i><b>ered 1</b></i></mp></content></point><point><num>2.</num><content><mp>ordered 2</mp></content></point></list><list><indent><num>1.</num><content><mp>uno<a href="http://urlInOrdered">rde</a>red 1</mp></content></indent><indent><num>2.</num><content><mp>unordered 2</mp></content></indent></list></article>';
                                                    var htmlFragmentInHtml = '<article id="articleId"><h1 contenteditable="false">Article No.</h1><h2>Article Heading</h2><ol><li num="1.">ord<em><strong>ered 1</strong></em></li><li num="2.">ordered 2</li></ol><ul><li num="1.">uno<a href="http://urlInOrdered">rde</a>red 1</li><li num="2.">unordered 2</li></ul></article>';
                                                    it("Expects to transform mix of elements back and forth correctly.", function() {
                                                        expectFragmentToBeTransformedCorrectly(aknFragmentInHtml, htmlFragmentInHtml, allConfigs);
                                                    });
                                                });

                                        describe(
                                                "Test mixture of: akn paragraph, akn alinea without data-akn-name.",
                                                function() {
                                                    var aknFragmentInHtml = '<paragraph><num>2</num><content><mp>test content</mp></content></paragraph><alinea><content><mp>text content1</mp></content></alinea>';
                                                    var htmlFragmentInHtml = '<p data-akn-num="2">test content</p><p>text content1</p>';
                                                    it(
                                                            "Expects that transformation will fail for 'from' direction when data-akn-name is missing for conflicting elements: paragraph and alinea.",
                                                            function() {
                                                                expectFragmentNotToBeTransformedCorrectlyForDirection(aknFragmentInHtml, htmlFragmentInHtml,
                                                                        aknParagraphAndAknAlineaConfigs, "from");

                                                            });
                                                });

                                    });

                        });

            });

});