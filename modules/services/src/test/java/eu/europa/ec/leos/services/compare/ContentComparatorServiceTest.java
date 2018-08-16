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
package eu.europa.ec.leos.services.compare;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mockito.InjectMocks;

import eu.europa.ec.leos.test.support.LeosTest;

public class ContentComparatorServiceTest extends LeosTest {

    @InjectMocks
    private ContentComparatorService contentComparatorService = new XMLContentComparatorServiceImpl();

    @Test
    public void test_img_diff_attributes_values() {
        String oldContent = "<aknp><img id=\"img1\" src=\"src1\"></img></aknp>";
        String newContent = "<aknp><img id=\"img2\" src=\"src2\"></img></aknp>";
        String expectedResult = "<aknp><img class=\"leos-content-removed\" id=\"img1\" src=\"src1\"></img><img class=\"leos-content-new\" id=\"img2\" src=\"src2\"></img></aknp>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_two_columns_img_diff_attributes_values_() {
        String oldContent = "<aknp><img id=\"img1\" src=\"src1\"></img></aknp>";
        String newContent = "<aknp><img id=\"img2\" src=\"src2\"></img></aknp>";
        String expectedLeftResult = "<aknp><img class=\"leos-content-removed\" id=\"img1\" src=\"src1\"></img><img class=\"leos-marker-content-added\" id=\"img2\" src=\"src2\"></img></aknp>";
        String expectedRightResult = "<aknp><img class=\"leos-marker-content-removed\" id=\"img1\" src=\"src1\"></img><img class=\"leos-content-new\" id=\"img2\" src=\"src2\"></img></aknp>";
        String[]  result = contentComparatorService.twoColumnsCompareHtmlContents(oldContent, newContent);
        assertEquals(expectedLeftResult, result[0]);
        assertEquals(expectedRightResult, result[1]);

    }

    @Test
    public void test_img_diff_attributes() {
        String oldContent = "<doc><aknp id=\"1\"> test test3     test4<img id=\"img1\" src=\"src1\"></img> test2 </aknp></doc>";
        String newContent = "<doc><aknp id=\"1\"> test test3     test4<img style=\"style2\" id=\"img1\" src=\"src2\"></img> test2 </aknp></doc>";
        String expectedResult = "<doc><aknp id=\"1\"> test test3     test4<span class=\"leos-content-removed\"><img id=\"img1\" src=\"src1\"></img></span>" +
                "<span class=\"leos-content-new\"><img style=\"style2\" id=\"img1\" src=\"src2\"></img></span>" +
                " test2 </aknp></doc>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_text_diff_more_blanks_img_diff_attributes() {
        String oldContent = "<doc><aknp id=\"1\">test test3     test4<img id=\"img1\" src=\"src1\"></img> test2 </aknp></doc>";
        String newContent = "<doc><aknp id=\"1\"> test test3     test4<img style=\"style2\" id=\"img1\" src=\"src2\"></img> test2 </aknp></doc>";
        String expectedResult = "<doc><aknp id=\"1\"><span class=\"leos-content-removed\">test</span><span class=\"leos-content-new\"> test</span> test3     test4" +
                "<span class=\"leos-content-removed\"><img id=\"img1\" src=\"src1\"></img></span>" +
                "<span class=\"leos-content-new\"><img style=\"style2\" id=\"img1\" src=\"src2\"></img></span>" +
                " test2 </aknp></doc>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_text_diff_less_blanks_img_diff_attributes() {
        String oldContent = "<doc><aknp id=\"1\">test test3     test4<img id=\"img1\" src=\"src1\"></img> test2 </aknp></doc>";
        String newContent = "<doc><aknp id=\"1\">test test3 test4<img style=\"style2\" id=\"img1\" src=\"src2\"></img> test2 </aknp></doc>";
        String expectedResult = "<doc><aknp id=\"1\">test test3<span class=\"leos-content-removed\"> </span><span class=\"leos-content-removed\"> </span><span class=\"leos-content-removed\"> </span>" +
                "<span class=\"leos-content-removed\"> </span> test4<span class=\"leos-content-removed\"><img id=\"img1\" src=\"src1\"></img></span>" +
                "<span class=\"leos-content-new\"><img style=\"style2\" id=\"img1\" src=\"src2\"></img></span> test2 </aknp></doc>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_text_diff_last_blank() {
        String oldContent = "<doc><aknp id=\"1\"> test <p>test2</p> <p><b>test2</b></p> </aknp></doc>";
        String newContent = "<doc><aknp id=\"1\"> test test3 <p>test2</p> <p><b>test2</b></p></aknp></doc>";
        String expectedResult = "<doc><aknp id=\"1\"> test<span class=\"leos-content-new\"> test3</span> <p>test2</p> <p><b>test2</b></p><span class=\"leos-content-removed\"> </span></aknp></doc>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_text_inside_two_elements() {
        String oldContent = "<doc><aknp id=\"1\"> test <p>test2</p> <p><b>test2</b></p> </aknp></doc>";
        String newContent = "<doc><aknp id=\"1\"> test test3 <p>test2</p> <p><b>test2</b></p> </aknp></doc>";
        String expectedResult = "<doc><aknp id=\"1\"> test<span class=\"leos-content-new\"> test3</span> <p>test2</p> <p><b>test2</b></p> </aknp></doc>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_text_same_inside_b() {
        String oldContent = "<doc><aknp id=\"1\"> test test2 </aknp></doc>";
        String newContent = "<doc><aknp id=\"1\"> test <b>test2</b> </aknp></doc>";
        String expectedResult = "<doc><aknp id=\"1\"> test<span class=\"leos-content-removed\"> test2</span> " +
                "<span class=\"leos-content-new\"><b><span class=\"leos-content-new\">test2</span></b></span>" +
                "<span class=\"leos-content-new\"> </span></aknp></doc>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);

    }

    @Test
    public void test_text_diff_inside_b() {
        String oldContent = "<doc><aknp id=\"1\"> test <b>test2</b> </aknp></doc>";
        String newContent = "<doc><aknp id=\"1\"> test <b>test3</b> </aknp></doc>";
        String expectedResult = "<doc><aknp id=\"1\"> test <b><span class=\"leos-content-removed\">test2</span><span class=\"leos-content-new\">test3</span></b> </aknp></doc>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);

    }

    @Test
    public void test_two_clumns_text_diff_inside_b() {
        String oldContent = "<doc><aknp id=\"1\"> test <b>test2</b> </aknp></doc>";
        String newContent = "<doc><aknp id=\"1\"> test <b>test3</b> </aknp></doc>";
        String expectedLeftResult =  "<doc><aknp id=\"1\"><span class=\"leos-content-modified\"><input type=\"hidden\" name=\"modification_0\"/> test <b><span class=\"leos-content-removed\">test2</span></b> </span></aknp></doc>";
        String expectedRightResult = "<doc><aknp id=\"1\"><span class=\"leos-content-modified\"><input type=\"hidden\" name=\"modification_0\"/> test <b><span class=\"leos-content-new\">test3</span></b> </span></aknp></doc>";
        String[]  result = contentComparatorService.twoColumnsCompareHtmlContents(oldContent, newContent);
        assertEquals(expectedLeftResult, result[0]);
        assertEquals(expectedRightResult, result[1]);

    }

    @Test
    public void test_text_new_elements_diff_blanks_with_ids() {
        String oldContent = "<doc><aknp id=\"1\">text0 <b id=\"333\">text2 text3 <i>text4</i> text5 text6 text7 text8</b> test9 <em> test10 and text 11 </em></aknp></doc>";
        String newContent = "<doc><aknp id=\"1\">text0 <li> mine is </li> <b id=\"333\">text2 text2-1 text3 <i>text4</i> text5 text6 text7 text8</b> test9 <em> test10 and text 11 </em></aknp></doc>";
        String expectedResult = "<doc><aknp id=\"1\">text0 <span class=\"leos-content-new\"><li><span class=\"leos-content-new\"> mine</span><span class=\"leos-content-new\"> is</span>" +
                "<span class=\"leos-content-new\"> </span></li></span>" +
                "<span class=\"leos-content-new\"> </span>" +
                "<b id=\"333\">text2<span class=\"leos-content-new\"> text2-1</span> text3 <i>text4</i> text5 text6 text7 text8</b>" +
                " test9 <em> test10 and text 11 </em></aknp></doc>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);

    }

    @Test
    public void test_text_new_elements_diff_blanks_without_ids() {
        String oldContent = "<doc><aknp id=\"1\">text0 <b>text2 text3 <i>text4</i> text5 text6 text7 text8</b> test9 <em> test10 and text 11 </em></aknp></doc>";
        String newContent = "<doc><aknp id=\"1\">text0 <li> mine is </li> <b>text2 text2-1 text3 <i>text4</i> text5 text6 text7 text8</b> test9 <em> test10 and text 11 </em></aknp></doc>";
        String expectedResult = "<doc><aknp id=\"1\">text0 <span class=\"leos-content-new\"><li><span class=\"leos-content-new\"> mine</span><span class=\"leos-content-new\"> is</span><span class=\"leos-content-new\"> </span></li></span>" +
                "<span class=\"leos-content-new\"> </span>" +
                "<b>text2<span class=\"leos-content-new\"> text2-1</span> text3 <i>text4</i> text5 text6 text7 text8</b> test9 <em> test10 and text 11 </em></aknp></doc>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);

    }

    @Test
    public void test_text_diff_elements_img_diff_attributes() {
        String oldContent = "<doc><aknp id=\"1\">this is a normal text like authors will do: my name is <b>Demo One</b> called <i>demo</i> with picture <img id=\"img1\" src=\"src1\"></img></aknp></doc>";
        String newContent = "<doc><aknp id=\"1\">this is a normal text: my name is <b>Demo First One</b> called <i>demoone</i> with picture <img id=\"img1\" src=\"src2\"></img></aknp></doc>";

        String expectedResult = "<doc><aknp id=\"1\">" +
                "this is a normal" +
                "<span class=\"leos-content-removed\"> text</span><span class=\"leos-content-new\"> text:</span>" +
                "<span class=\"leos-content-removed\"> like</span><span class=\"leos-content-removed\"> authors</span><span class=\"leos-content-removed\"> will</span><span class=\"leos-content-removed\"> do:</span>" +
                " my name is <b>Demo<span class=\"leos-content-new\"> First</span> One</b>" +
                " called <i><span class=\"leos-content-removed\">demo</span><span class=\"leos-content-new\">demoone</span></i> with picture " +
                "<span class=\"leos-content-removed\"><img id=\"img1\" src=\"src1\"></img></span>" +
                "<span class=\"leos-content-new\"><img id=\"img1\" src=\"src2\"></img></span>" +
                "</aknp></doc>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);

    }

    @Test
    public void test_text_diff_elements_values_blanks() {
        String oldContent = "<doc><aknp id=\"1\">text       <b id=\"akn_K57is1\">second </b>third</aknp></doc>";
        String newContent = "<doc><aknp id=\"1\">text     <b id=\"akn_K57is1\">second</b>third          new text</aknp></doc>";

        String expectedResult = "<doc><aknp id=\"1\">text     <span class=\"leos-content-removed\"> </span>" +
                "<span class=\"leos-content-removed\"> </span>" +
                "<b id=\"akn_K57is1\">second<span class=\"leos-content-removed\"> </span></b>" +
                "third<span class=\"leos-content-new\"> </span><span class=\"leos-content-new\"> </span><span class=\"leos-content-new\"> </span>" +
                "<span class=\"leos-content-new\"> </span><span class=\"leos-content-new\"> </span><span class=\"leos-content-new\"> </span>" +
                "<span class=\"leos-content-new\"> </span><span class=\"leos-content-new\"> </span>" +
                "<span class=\"leos-content-new\"> </span><span class=\"leos-content-new\"> new</span>" +
                "<span class=\"leos-content-new\"> text</span></aknp></doc>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);

    }

    @Test
    public void test_authorialnote_diff_tooltip() {
        String oldContent = "<doc><aknp id=\"1\">ref: <authorialnote id=\"akn_0sOWaY\" data-tooltip=\"old ref\">1</authorialnote></aknp></doc>";
        String newContent = "<doc><aknp id=\"1\">ref: <authorialnote id=\"akn_0sOWaY\" data-tooltip=\"new ref\">1</authorialnote></aknp></doc>";

        String expectedResult = "<doc><aknp id=\"1\">ref: <span class=\"leos-content-removed\"><authorialnote id=\"akn_0sOWaY\" data-tooltip=\"old ref\">1</authorialnote></span>" +
                "<span class=\"leos-content-new\"><authorialnote id=\"akn_0sOWaY\" data-tooltip=\"new ref\">1</authorialnote></span></aknp></doc>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_authorialnote_diff_value() {
        String oldContent = "<doc><aknp id=\"1\">ref: <authorialnote id=\"akn_0sOWaY\" data-tooltip=\"old ref\">1</authorialnote></aknp></doc>";
        String newContent = "<doc><aknp id=\"1\">ref: <authorialnote id=\"akn_0sOWaY\" data-tooltip=\"old ref\">2</authorialnote></aknp></doc>";

        String expectedResult = "<doc><aknp id=\"1\">ref: <span class=\"leos-content-removed\"><authorialnote id=\"akn_0sOWaY\" data-tooltip=\"old ref\">1</authorialnote></span>" +
                "<span class=\"leos-content-new\"><authorialnote id=\"akn_0sOWaY\" data-tooltip=\"old ref\">2</authorialnote></span></aknp></doc>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_authorialnote_diff_tooltip_value() {
        String oldContent = "<doc><aknp id=\"1\">ref: <authorialnote id=\"akn_0sOWaY\" data-tooltip=\"old ref\">1</authorialnote></aknp></doc>";
        String newContent = "<doc><aknp id=\"1\">ref: <authorialnote id=\"akn_0sOWaY\" data-tooltip=\"new ref\">2</authorialnote></aknp></doc>";

        String expectedResult = "<doc><aknp id=\"1\">ref: <span class=\"leos-content-removed\"><authorialnote id=\"akn_0sOWaY\" data-tooltip=\"old ref\">1</authorialnote></span>" +
                "<span class=\"leos-content-new\"><authorialnote id=\"akn_0sOWaY\" data-tooltip=\"new ref\">2</authorialnote></span></aknp></doc>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_formula_diff_values() {
        String oldContent = "<doc><aknp id=\"1\">formula: <inline id=\"art_260_Fd76DH_yJ8bBu\" name=\"math-tex\">\\(x = {-b \\pm \\sqrt{b^2-4ac} \\over 2a}\\)</inline></aknp></doc>";
        String newContent = "<doc><aknp id=\"1\">formula: <inline id=\"art_260_Fd76DH_yJ8bBu\" name=\"math-tex\">\\(x = {-b \\pm \\sqrt{b^2-4ac} \\over 299999a}\\)</inline></aknp></doc>";

        String expectedResult = "<doc><aknp id=\"1\">formula: <span class=\"leos-content-removed\"><inline id=\"art_260_Fd76DH_yJ8bBu\" name=\"math-tex\">" +
                "\\(x = {-b \\pm \\sqrt{b^2-4ac} \\over 2a}\\)" +
                "</inline></span>" +
                "<span class=\"leos-content-new\"><inline id=\"art_260_Fd76DH_yJ8bBu\" name=\"math-tex\">" +
                "\\(x = {-b \\pm \\sqrt{b^2-4ac} \\over 299999a}\\)" +
                "</inline></span>" +
                "</aknp></doc>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);
    }

    /**
     * TABLE TEST
     * */

    @Test
    public void test_table_diff_row_added() {
        String oldContent = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td><p>11</p></td></tr><tr><td><p>2</p></td><td><p>2</p></td></tr></table><p> </p></blockContainer>";
        String newContent = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td><p>11</p></td></tr><tr><td><p>3</p></td><td><p>3</p></td></tr><tr><td><p>2</p></td><td><p>2</p></td></tr></table><p> </p></blockContainer>";
        String expectedResult = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td><p>11</p></td></tr><tr class=\"leos-content-new\"><td><p>3</p></td><td><p>3</p></td></tr><tr><td><p>2</p></td><td><p>2</p></td></tr></table><p> </p></blockContainer>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_table_diff_row_removed() {
        String oldContent = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td><p>11</p></td></tr><tr><td><p>2</p></td><td><p>2</p></td></tr></table><p> </p></blockContainer>";
        String newContent = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td><p>11</p></td></tr></table><p> </p></blockContainer>";
        String expectedResult = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td><p>11</p></td></tr><tr class=\"leos-content-removed\"><td><p>2</p></td><td><p>2</p></td></tr></table><p> </p></blockContainer>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_table_diff_colum_added() {
        String oldContent = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td><p>11</p></td></tr><tr><td><p>2</p></td><td><p>22</p></td></tr></table></blockContainer>";
        String newContent = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td><p>11</p></td><td><p>111</p></td></tr><tr><td><p>2</p></td><td><p>22</p></td><td><p>222</p></td></tr></table></blockContainer>";
        String expectedResult = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td><p>11</p></td><td class=\"leos-content-new\"><p>111</p></td></tr><tr><td><p>2</p></td><td><p>22</p></td><td class=\"leos-content-new\"><p>222</p></td></tr></table></blockContainer>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_table_diff_colum_removed() {
        String oldContent = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td><p>11</p></td><td><p>111</p></td></tr><tr><td><p>2</p></td><td><p>22</p></td><td><p>222</p></td></tr></table></blockContainer>";
        String newContent = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td></tr><tr><td><p>2</p></td></tr></table></blockContainer>";
        String expectedResult = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td class=\"leos-content-removed\"><p>11</p></td><td class=\"leos-content-removed\"><p>111</p></td></tr><tr><td><p>2</p></td><td class=\"leos-content-removed\"><p>22</p></td><td class=\"leos-content-removed\"><p>222</p></td></tr></table></blockContainer>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_table_diff_colum_removed_keep_class() {
        String oldContent = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td class=\"myClass\"><p>11</p></td><td style=\"myStyle\"><p>111</p></td></tr><tr><td><p>2</p></td><td id=\"bHnJUIo\" width=\"100px\" style=\"myStyle2\"><p>22</p></td><td id=\"aKnDUIo\" class=\"myClass2\" width=\"100px\"><p>222</p></td></tr></table></blockContainer>";
        String newContent = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td></tr><tr><td><p>2</p></td></tr></table></blockContainer>";
        String expectedResult = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td class=\"leos-content-removed myClass\"><p>11</p></td><td class=\"leos-content-removed\" style=\"myStyle\"><p>111</p></td></tr><tr><td><p>2</p></td><td class=\"leos-content-removed\" id=\"bHnJUIo\" width=\"100px\" style=\"myStyle2\"><p>22</p></td><td id=\"aKnDUIo\" class=\"leos-content-removed myClass2\" width=\"100px\"><p>222</p></td></tr></table></blockContainer>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_table_diff_cell_merged() {
        String oldContent = "<blockContainer GUID=\"body__blockcontainer_1\" leos:editable=\"true\" leos:deletable=\"true\"><table GUID=\"akn_RTIV96\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr GUID=\"akn_kdDevm\"><td GUID=\"akn_JkRPbo\"><p GUID=\"akn_XI6WoD\">1</p></td><td GUID=\"akn_M841Gy\"><p GUID=\"akn_BS0LCE\">2</p></td><td GUID=\"akn_wunh8X\"><p GUID=\"akn_2tp9sa\">3</p></td></tr><tr GUID=\"akn_nufwWM\"><td GUID=\"akn_YozCcq\"><p GUID=\"akn_bfJUBW\">11</p></td><td rowspan=\"1\" GUID=\"akn_3NQOhC\"><p GUID=\"akn_v1cSZo\">22</p></td><td rowspan=\"1\" GUID=\"akn_icJYkP\"><p GUID=\"akn_2jJv0R\">33</p></td></tr><tr GUID=\"akn_5D0exg\"><td GUID=\"akn_8HxAlj\"><p GUID=\"akn_1zKf0X\">111</p></td><td GUID=\"akn_WBDOnj\"><p GUID=\"akn_wJKHTj\">222</p></td><td GUID=\"akn_aihqio\"><p GUID=\"akn_fkayML\">333</p></td></tr></table></blockContainer>";
        String newContent = "<blockContainer GUID=\"body__blockcontainer_1\" leos:editable=\"true\" leos:deletable=\"true\"><table GUID=\"akn_RTIV96\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr GUID=\"akn_kdDevm\"><td GUID=\"akn_JkRPbo\"><p GUID=\"akn_XI6WoD\">1</p></td><td GUID=\"akn_M841Gy\"><p GUID=\"akn_BS0LCE\">2</p></td><td GUID=\"akn_wunh8X\"><p GUID=\"akn_2tp9sa\">3</p></td></tr><tr GUID=\"akn_nufwWM\"><td GUID=\"akn_YozCcq\"><p GUID=\"akn_bfJUBW\">11</p></td><td rowspan=\"1\" colspan=\"2\" GUID=\"akn_3NQOhC\"><p GUID=\"akn_v1cSZo\">22</p><p GUID=\"akn_2jJv0R\">33</p></td></tr><tr GUID=\"akn_5D0exg\"><td GUID=\"akn_8HxAlj\"><p GUID=\"akn_1zKf0X\">111</p></td><td GUID=\"akn_WBDOnj\"><p GUID=\"akn_wJKHTj\">222</p></td><td GUID=\"akn_aihqio\"><p GUID=\"akn_fkayML\">333</p></td></tr></table></blockContainer>";
        String expectedResult = "<blockContainer GUID=\"body__blockcontainer_1\" leos:editable=\"true\" leos:deletable=\"true\"><table GUID=\"akn_RTIV96\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr GUID=\"akn_kdDevm\"><td GUID=\"akn_JkRPbo\"><p GUID=\"akn_XI6WoD\">1</p></td><td GUID=\"akn_M841Gy\"><p GUID=\"akn_BS0LCE\">2</p></td><td GUID=\"akn_wunh8X\"><p GUID=\"akn_2tp9sa\">3</p></td></tr><tr GUID=\"akn_nufwWM\"><td GUID=\"akn_YozCcq\"><p GUID=\"akn_bfJUBW\">11</p></td><td rowspan=\"1\" colspan=\"2\" GUID=\"akn_3NQOhC\"><p GUID=\"akn_v1cSZo\">22</p><p class=\"leos-content-new\" GUID=\"akn_2jJv0R\">33</p></td><td class=\"leos-content-removed\" rowspan=\"1\" GUID=\"akn_icJYkP\"><p GUID=\"akn_2jJv0R\">33</p></td></tr><tr GUID=\"akn_5D0exg\"><td GUID=\"akn_8HxAlj\"><p GUID=\"akn_1zKf0X\">111</p></td><td GUID=\"akn_WBDOnj\"><p GUID=\"akn_wJKHTj\">222</p></td><td GUID=\"akn_aihqio\"><p GUID=\"akn_fkayML\">333</p></td></tr></table></blockContainer>";
        String result = contentComparatorService.compareHtmlContents(oldContent, newContent);
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_two_columns_table_diff_row_added() {
        String oldContent = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td><p>11</p></td></tr><tr><td><p>2</p></td><td><p>2</p></td></tr></table><p> </p></blockContainer>";
        String newContent = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td><p>11</p></td></tr><tr><td><p>3</p></td><td><p>3</p></td></tr><tr><td><p>2</p></td><td><p>2</p></td></tr></table><p> </p></blockContainer>";
        String expectedLeftResult =  "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td><p>11</p></td></tr><tr class=\"leos-marker-content-added\"><td><p>3</p></td><td><p>3</p></td></tr><tr><td><p>2</p></td><td><p>2</p></td></tr></table><p> </p></blockContainer>";
        String expectedRightResult = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td><p>11</p></td></tr><tr class=\"leos-content-new\"><td><p>3</p></td><td><p>3</p></td></tr><tr><td><p>2</p></td><td><p>2</p></td></tr></table><p> </p></blockContainer>";
        String[]  result = contentComparatorService.twoColumnsCompareHtmlContents(oldContent, newContent);
        assertEquals(expectedLeftResult, result[0]);
        assertEquals(expectedRightResult, result[1]);
    }

    @Test
    public void test_two_columns_table_diff_colum_removed_keep_class() {
        String oldContent = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td class=\"myClass\"><p>11</p></td><td style=\"myStyle\"><p>111</p></td></tr><tr><td><p>2</p></td><td id=\"bHnJUIo\" width=\"100px\" style=\"myStyle2\"><p>22</p></td><td id=\"aKnDUIo\" class=\"myClass2\" width=\"100px\"><p>222</p></td></tr></table></blockContainer>";
        String newContent = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td></tr><tr><td><p>2</p></td></tr></table></blockContainer>";
        String expectedLeftResult =  "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td class=\"leos-content-removed myClass\"><p>11</p></td><td class=\"leos-content-removed\" style=\"myStyle\"><p>111</p></td></tr><tr><td><p>2</p></td><td class=\"leos-content-removed\" id=\"bHnJUIo\" width=\"100px\" style=\"myStyle2\"><p>22</p></td><td id=\"aKnDUIo\" class=\"leos-content-removed myClass2\" width=\"100px\"><p>222</p></td></tr></table></blockContainer>";
        String expectedRightResult = "<blockContainer GUID=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\"><tr><td><p>1</p></td><td class=\"leos-marker-content-removed myClass\"><p>11</p></td><td class=\"leos-marker-content-removed\" style=\"myStyle\"><p>111</p></td></tr><tr><td><p>2</p></td><td class=\"leos-marker-content-removed\" id=\"bHnJUIo\" width=\"100px\" style=\"myStyle2\"><p>22</p></td><td id=\"aKnDUIo\" class=\"leos-marker-content-removed myClass2\" width=\"100px\"><p>222</p></td></tr></table></blockContainer>";
        String[]  result = contentComparatorService.twoColumnsCompareHtmlContents(oldContent, newContent);
        assertEquals(expectedLeftResult, result[0]);
        assertEquals(expectedRightResult, result[1]);
    }
}
