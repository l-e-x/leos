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
define(function testAknNumberedParagraphPlugin(require) {
    "use strict";
    var $ = require('jquery');

    var aknNumberedParagraphPluginToTest =  require("plugins/aknNumberedParagraph/aknNumberedParagraphPlugin");
    var inputHtmlTestSingleTextParagraph = '<div>'
        + '<article id="art_1" data-akn-attr-editable="true" data-akn-attr-deletable="true" data-akn-name="article">'
        +     '<ol id="art_1">'
        +         '<li id="art_1__para_1" data-akn-name="aknNumberedParagraph" data-akn-num-id="art_1_hYLtYF" data-akn-num="1." data-akn-content-id="art_1__para_1__content" data-akn-mp-id="art_1__para_1__content__p">Text...</li>'
        +     '</ol>'
        + '</article>'
        + '</div>';
    var expectedHtmlTestSingleTextParagraph = '<div>'
        + '<article id="art_1" data-akn-attr-editable="true" data-akn-attr-deletable="true" data-akn-name="article">'
        +     '<ol id="art_1">'
        +         '<li id="art_1__para_1" data-akn-name="aknNumberedParagraph" data-akn-num-id="art_1_hYLtYF" data-akn-num="1." data-akn-content-id="art_1__para_1__content" data-akn-mp-id="art_1__para_1__content__p">Text...</li>'
        +     '</ol>'
        + '</article>'
        + '</div>';

    var inputHtmlTestTextAndTableSubparagraph = '<div>'
        + '<article id="art_1" data-akn-attr-editable="true" data-akn-attr-deletable="true" data-akn-name="article">'
        +     '<ol id="art_1">'
        +         '<li id="art_1_nAkKRg" data-akn-name="aknNumberedParagraph" data-akn-num-id="art_1_MjrAir" data-akn-num="2." data-akn-subparagraph-id="art_1_uzGVmI" data-akn-wrapped-content-id="art_1_W6UHAf">'
        +             '<p data-akn-mp-id="art_1_91rVCt">text A</p>'
        +             '<table id="art_1_3MggWZ" border="1" cellpadding="1" cellspacing="1" style="width: 500px;" data-akn-name="leosTable">'
        +                 '<thead></thead>'
        +                 '<tbody>'
        +                     '<tr id="art_1_dP125v">'
        +                         '<td id="art_1_7ZcpAT">'
        +                             '<p id="art_1_mYO9aP" data-akn-name="aknParagraph"><br></p>'
        +                         '</td>'
        +                         '<td id="art_1_CaC46V">'
        +                             '<p id="art_1_c5bQom" data-akn-name="aknParagraph"><br></p>'
        +                         '</td>'
        +                     '</tr>'
        +                '</tbody>'
        +             '</table>'
        +             '<p data-akn-mp-id="art_1_0AnPUf"><br></p>'
        +          '</li>'
        +       '</ol>'
        + '</article>'
        + '</div>';

    var expectedHtmlTestTextAndTableSubparagraph = '<div>'
        + '<article id="art_1" data-akn-attr-editable="true" data-akn-attr-deletable="true" data-akn-name="article">'
        +     '<ol id="art_1">'
        +         '<li data-akn-mp-id="art_1_91rVCt" data-akn-name="aknNumberedParagraph">text A</li>'
        +         '<li id="art_1_nAkKRg" data-akn-name="aknNumberedParagraph" data-akn-num-id="art_1_MjrAir" data-akn-num="2." data-akn-subparagraph-id="art_1_uzGVmI" data-akn-wrapped-content-id="art_1_W6UHAf">'
        +             '<table id="art_1_3MggWZ" border="1" cellpadding="1" cellspacing="1" style="width: 500px;" data-akn-name="leosTable">'
        +                 '<thead>'
        +                 '</thead>'
        +                 '<tbody>'
        +                     '<tr id="art_1_dP125v">'
        +                         '<td id="art_1_7ZcpAT">'
        +                             '<p id="art_1_mYO9aP" data-akn-name="aknParagraph">'
        +                                 '<br>'
        +                             '</p>'
        +                         '</td>'
        +                         '<td id="art_1_CaC46V">'
        +                             '<p id="art_1_c5bQom" data-akn-name="aknParagraph"><br></p></td>'
        +                     '</tr>'
        +                 '</tbody>'
        +             '</table>'
        +         '</li>'
        +         '<li data-akn-mp-id="art_1_0AnPUf" data-akn-name="aknNumberedParagraph"><br></li>'
        +     '</ol>'
        + '</article>'
        + '</div>';

    var inputHtmlTestSingleTableSubparagraph = '<div>'
        + '<article id="art_1" data-akn-attr-editable="true" data-akn-attr-deletable="true" data-akn-name="article">'
        +     '<ol id="art_1">'
        +         '<li id="art_1_Pg0emi" data-akn-name="aknNumberedParagraph" data-akn-num-id="art_1_LywNOo" data-akn-num="3." data-akn-content-id="art_1_bJh9cg">'
        +             '<table id="art_1_BIOJ1i" border="1" cellpadding="1" cellspacing="1" style="width: 500px;" data-akn-name="leosTable">'
        +                 '<thead></thead>'
        +                 '<tbody>'
        +                    '<tr id="art_1_Ko6hMM">'
        +                         '<td id="art_1_1Qf5Ut">'
        +                             '<p id="art_1_JSD3Er" data-akn-name="aknParagraph"><br></p>'
        +                         '</td>'
        +                         '<td id="art_1_v6r2Zl">'
        +                             '<p id="art_1_CaKNYX" data-akn-name="aknParagraph"><br></p>'
        +                         '</td>'
        +                     '</tr>'
        +                 '</tbody>'
        +             '</table>'
        +         '</li>'
        +     '</ol>'
        + '</article>'
        + '</div>';

    var expectedHtmlTestSingleTableSubparagraph = '<div>'
        + '<article id="art_1" data-akn-attr-editable="true" data-akn-attr-deletable="true" data-akn-name="article">'
        +     '<ol id="art_1">'
        +         '<li id="art_1_Pg0emi" data-akn-name="aknNumberedParagraph" data-akn-num-id="art_1_LywNOo" data-akn-num="3." data-akn-content-id="art_1_bJh9cg">'
        +             '<table id="art_1_BIOJ1i" border="1" cellpadding="1" cellspacing="1" style="width: 500px;" data-akn-name="leosTable">'
        +                 '<thead>'
        +                 '</thead>'
        +                 '<tbody>'
        +                     '<tr id="art_1_Ko6hMM">'
        +                         '<td id="art_1_1Qf5Ut">'
        +                             '<p id="art_1_JSD3Er" data-akn-name="aknParagraph"><br></p>'
        +                         '</td>'
        +                         '<td id="art_1_v6r2Zl">'
        +                             '<p id="art_1_CaKNYX" data-akn-name="aknParagraph"><br></p>'
        +                         '</td>'
        +                     '</tr>'
        +                 '</tbody>'
        +             '</table>'
        +         '</li>'
        +     '</ol>'
        + '</article>'
        + '</div>';

    var inputHtmlTestThreeTextSubparagraphs = '<div>'
        + '<article id="art_1" data-akn-attr-editable="true" data-akn-attr-deletable="true" data-akn-name="article">'
        +     '<ol id="art_1">'
        +         '<li id="art_1_eutSMN" data-akn-name="aknNumberedParagraph" data-akn-num-id="art_1_UJcAnc" data-akn-num="4." data-akn-subparagraph-id="art_1_FPpW6o" data-akn-wrapped-content-id="art_1_cXGBh9">'
        +             '<p data-akn-mp-id="art_1_ReWOn8">truc</p>'
        +             '<p data-akn-mp-id="art_1_dWVxbQ">est</p>'
        +             '<p data-akn-mp-id="art_1_hLD1oW">test</p>'
        +         '</li>'
        +     '</ol>'
        + '</article>'
        + '</div>';

    var expectedHtmlTestThreeTextSubparagraphs = '<div>'
        + '<article id="art_1" data-akn-attr-editable="true" data-akn-attr-deletable="true" data-akn-name="article">'
        +     '<ol id="art_1">'
        +         '<li data-akn-mp-id="art_1_ReWOn8" data-akn-name="aknNumberedParagraph">truc</li>'
        +         '<li data-akn-mp-id="art_1_dWVxbQ" data-akn-name="aknNumberedParagraph">est</li>'
        +         '<li data-akn-mp-id="art_1_hLD1oW" data-akn-name="aknNumberedParagraph">test</li>'
        +     '</ol>'
        + '</article>'
        + '</div>';

    var inputHtmlTestTextAndSublistSubparagraphs = '<div>'
        + '<article id="art_1" data-akn-attr-editable="true" data-akn-attr-deletable="true" data-akn-name="article">'
        +     '<ol id="art_1">'
        +         '<li id="art_1_hORc3b" data-akn-name="aknNumberedParagraph" data-akn-num-id="art_1_v8ux6I" data-akn-num="5." data-akn-subparagraph-id="art_1_78u4xU" data-akn-wrapped-content-id="art_1_5i2bhZ">'
        +             '<p data-akn-mp-id="art_1_KhpUqt">test</p>'
        +             '<ol id="art_1_aboXLa" data-akn-name="aknOrderedList">'
        +                 '<li id="art_1_978FnA" data-akn-num-id="art_1_P6sf1C" data-akn-num="a)" data-akn-alinea-id="art_1_jxJO8z" data-akn-wrapped-content-id="art_1_i58XcD">'
        +                     '<p data-akn-mp-id="art_1_MtS3xg">test A</p>'
        +                     '<ol id="art_1_HzdnHy" data-akn-name="aknOrderedList">'
        +                         '<li id="art_1_3u1qlg" data-akn-num-id="art_1_0kcSu9" data-akn-num="(1)" data-akn-content-id="art_1_aHfvTF" data-akn-mp-id="art_1_1wukmk">test B</li>'
        +                         '<li id="art_1_mqFHWd" data-akn-num-id="art_1_JNJw1O" data-akn-num="(2)" data-akn-content-id="art_1_wXZ4CN" data-akn-mp-id="art_1_18WkBz">test C</li>'
        +                     '</ol>'
        +                 '</li>'
        +             '</ol>'
        +         '</li>'
        +     '</ol>'
        + '</article>'
        + '</div>';

    var expectedHtmlTestTextAndSublistSubparagraphs = '<div>'
        + '<article id="art_1" data-akn-attr-editable="true" data-akn-attr-deletable="true" data-akn-name="article">'
        +     '<ol id="art_1">'
        +         '<li id="art_1_hORc3b" data-akn-name="aknNumberedParagraph" data-akn-num-id="art_1_v8ux6I" data-akn-num="5." data-akn-subparagraph-id="art_1_78u4xU" data-akn-wrapped-content-id="art_1_5i2bhZ">'
        +             '<p data-akn-mp-id="art_1_KhpUqt">test</p>'
        +             '<ol id="art_1_aboXLa" data-akn-name="aknOrderedList">'
        +                 '<li id="art_1_978FnA" data-akn-num-id="art_1_P6sf1C" data-akn-num="a)" data-akn-alinea-id="art_1_jxJO8z" data-akn-wrapped-content-id="art_1_i58XcD">'
        +                     '<p data-akn-mp-id="art_1_MtS3xg">test A</p>'
        +                     '<ol id="art_1_HzdnHy" data-akn-name="aknOrderedList">'
        +                         '<li id="art_1_3u1qlg" data-akn-num-id="art_1_0kcSu9" data-akn-num="(1)" data-akn-content-id="art_1_aHfvTF" data-akn-mp-id="art_1_1wukmk">test B</li>'
        +                         '<li id="art_1_mqFHWd" data-akn-num-id="art_1_JNJw1O" data-akn-num="(2)" data-akn-content-id="art_1_wXZ4CN" data-akn-mp-id="art_1_18WkBz">test C</li>'
        +                     '</ol>'
        +                 '</li>'
        +             '</ol>'
        +         '</li>'
        +     '</ol>'
        + '</article>'
        + '</div>';

    var fragmentFromString = function (strHTML) {
        var wrapper= document.createElement('div');
        wrapper.innerHTML= strHTML;
        return wrapper.firstChild;
    }

    var fragmentToString = function (eltHTML) {
        return eltHTML.outerHTML;
    }

    describe(
            "Unit tests for plugins/aknNumberedParagraphPlugin",
            function() {
                describe("Test method that transforms subparagraphs into paragraphs.",
                        function() {
                            it("Test with a single text subparagraph",
                                    function() {
                                        var article = fragmentFromString(inputHtmlTestSingleTextParagraph);

                                        var editor = {
                                            editable: function editable() {
                                                return {
                                                    $: article
                                                };
                                            },
                                            fire : function fire(param) {
                                                return true;
                                            },
                                            createRange : function createRange (param) {
                                                return {
                                                    moveToElementEditablePosition: function moveToElementEditablePosition(node, flag) {
                                                        return true;
                                                    },
                                                    select: function select() {
                                                        return true;
                                                    }
                                                };
                                            }
                                        };

                                        // DO THE ACTUAL CALL
                                        aknNumberedParagraphPluginToTest.transformSubparagraphs(editor);

                                        expect(fragmentToString(editor.editable().$)).toEqual(expectedHtmlTestSingleTextParagraph);
                            });
                            it("Test with text and table subparagraphs",
                                    function() {
                                        var article = fragmentFromString(inputHtmlTestTextAndTableSubparagraph);

                                        var editor = {
                                                editable: function editable() {
                                                    return {
                                                        $: article
                                                    };
                                                },
                                                fire : function fire(param) {
                                                    return true;
                                                },
                                                createRange : function createRange (param) {
                                                    return {
                                                        moveToElementEditablePosition: function moveToElementEditablePosition(node, flag) {
                                                            return true;
                                                        },
                                                        select: function select() {
                                                            return true;
                                                        }
                                                    };
                                                }
                                            };

                                        // DO THE ACTUAL CALL
                                        aknNumberedParagraphPluginToTest.transformSubparagraphs(editor);

                                        expect(fragmentToString(editor.editable().$)).toEqual(expectedHtmlTestTextAndTableSubparagraph);
                            });
                            it("Test with single table subparagraph",
                                    function() {
                                        var article = fragmentFromString(inputHtmlTestSingleTableSubparagraph);

                                        var editor = {
                                                editable: function editable() {
                                                    return {
                                                        $: article
                                                    };
                                                },
                                                fire : function fire(param) {
                                                    return true;
                                                },
                                                createRange : function createRange (param) {
                                                    return {
                                                        moveToElementEditablePosition: function moveToElementEditablePosition(node, flag) {
                                                            return true;
                                                        },
                                                        select: function select() {
                                                            return true;
                                                        }
                                                    };
                                                }
                                            };

                                        // DO THE ACTUAL CALL
                                        aknNumberedParagraphPluginToTest.transformSubparagraphs(editor);

                                        expect(fragmentToString(editor.editable().$)).toEqual(expectedHtmlTestSingleTableSubparagraph);
                            });
                            it("Test with three text subparagraphs",
                                    function() {
                                        var article = fragmentFromString(inputHtmlTestThreeTextSubparagraphs);

                                        var editor = {
                                                editable: function editable() {
                                                    return {
                                                        $: article
                                                    };
                                                },
                                                fire : function fire(param) {
                                                    return true;
                                                },
                                                createRange : function createRange (param) {
                                                    return {
                                                        moveToElementEditablePosition: function moveToElementEditablePosition(node, flag) {
                                                            return true;
                                                        },
                                                        select: function select() {
                                                            return true;
                                                        }
                                                    };
                                                }
                                            };

                                        // DO THE ACTUAL CALL
                                        aknNumberedParagraphPluginToTest.transformSubparagraphs(editor);

                                        expect(fragmentToString(editor.editable().$)).toEqual(expectedHtmlTestThreeTextSubparagraphs);
                            });
                            it("Test with text and sublist subparagraphs",
                                    function() {
                                        var article = fragmentFromString(inputHtmlTestTextAndSublistSubparagraphs);

                                        var editor = {
                                                editable: function editable() {
                                                    return {
                                                        $: article
                                                    };
                                                },
                                                fire : function fire(param) {
                                                    return true;
                                                },
                                                createRange : function createRange (param) {
                                                    return {
                                                        moveToElementEditablePosition: function moveToElementEditablePosition(node, flag) {
                                                            return true;
                                                        },
                                                        select: function select() {
                                                            return true;
                                                        }
                                                    };
                                                }
                                            };

                                        // DO THE ACTUAL CALL
                                        aknNumberedParagraphPluginToTest.transformSubparagraphs(editor);

                                        expect(fragmentToString(editor.editable().$)).toEqual(expectedHtmlTestTextAndSublistSubparagraphs);
                            });
                });
    });
});