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
package eu.europa.ec.leos.services.compare;

import eu.europa.ec.leos.test.support.LeosTest;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;

import static eu.europa.ec.leos.services.compare.ContentComparatorService.*;
import static org.junit.Assert.assertEquals;

@Ignore
public class ContentComparatorServiceTest extends LeosTest {

    @InjectMocks
    private ContentComparatorService contentComparatorService = new ProposalXMLContentComparatorServiceImpl();

    @Test
    public void test_img_diff_attributes_values() {
        String oldContent = "<aknp><img id=\"img1\" src=\"src1\"></img></aknp>";
        String newContent = "<aknp><img id=\"img2\" src=\"src2\"></img></aknp>";
        String expectedResult = "<aknp><img class=\"leos-content-removed\" id=\"img1\" src=\"src1\"></img><img class=\"leos-content-new\" id=\"img2\" src=\"src2\"></img></aknp>";
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_two_columns_img_diff_attributes_values_() {
        String oldContent = "<aknp><img id=\"img1\" src=\"src1\"></img></aknp>";
        String newContent = "<aknp><img id=\"img2\" src=\"src2\"></img></aknp>";
        String expectedLeftResult = "<aknp><img class=\"leos-content-removed\" id=\"img1\" src=\"src1\"></img><img class=\"leos-marker-content-added\" id=\"img2\" src=\"src2\"></img></aknp>";
        String expectedRightResult = "<aknp><img class=\"leos-marker-content-removed\" id=\"img1\" src=\"src1\"></img><img class=\"leos-content-new\" id=\"img2\" src=\"src2\"></img></aknp>";
        String[]  result = contentComparatorService.twoColumnsCompareContents(new ContentComparatorContext.Builder(oldContent, newContent).build());
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
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
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
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_text_diff_less_blanks_img_diff_attributes() {
        String oldContent = "<doc><aknp id=\"1\">test test3     test4<img id=\"img1\" src=\"src1\"></img> test2 </aknp></doc>";
        String newContent = "<doc><aknp id=\"1\">test test3 test4<img style=\"style2\" id=\"img1\" src=\"src2\"></img> test2 </aknp></doc>";
        String expectedResult = "<doc><aknp id=\"1\">test test3<span class=\"leos-content-removed\"> </span><span class=\"leos-content-removed\"> </span><span class=\"leos-content-removed\"> </span>" +
                "<span class=\"leos-content-removed\"> </span> test4<span class=\"leos-content-removed\"><img id=\"img1\" src=\"src1\"></img></span>" +
                "<span class=\"leos-content-new\"><img style=\"style2\" id=\"img1\" src=\"src2\"></img></span> test2 </aknp></doc>";
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_text_diff_last_blank() {
        String oldContent = "<doc><aknp id=\"1\"> test <p>test2</p> <p><b>test2</b></p> </aknp></doc>";
        String newContent = "<doc><aknp id=\"1\"> test test3 <p>test2</p> <p><b>test2</b></p></aknp></doc>";
        String expectedResult = "<doc><aknp id=\"1\"> test<span class=\"leos-content-new\"> test3</span> <p>test2</p> <p><b>test2</b></p><span class=\"leos-content-removed\"> </span></aknp></doc>";
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_text_inside_two_elements() {
        String oldContent = "<doc><aknp id=\"1\"> test <p>test2</p> <p><b>test2</b></p> </aknp></doc>";
        String newContent = "<doc><aknp id=\"1\"> test test3 <p>test2</p> <p><b>test2</b></p> </aknp></doc>";
        String expectedResult = "<doc><aknp id=\"1\"> test<span class=\"leos-content-new\"> test3</span> <p>test2</p> <p><b>test2</b></p> </aknp></doc>";
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_text_same_inside_b() {
        String oldContent = "<doc><aknp id=\"1\"> test test2 </aknp></doc>";
        String newContent = "<doc><aknp id=\"1\"> test <b>test2</b> </aknp></doc>";
        String expectedResult = "<doc><aknp id=\"1\"> test<span class=\"leos-content-removed\"> test2</span> " +
                "<span class=\"leos-content-new\"><b><span class=\"leos-content-new\">test2</span></b></span>" +
                "<span class=\"leos-content-new\"> </span></aknp></doc>";
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);

    }

    @Test
    public void test_text_diff_inside_b() {
        String oldContent = "<doc><aknp id=\"1\"> test <b>test2</b> </aknp></doc>";
        String newContent = "<doc><aknp id=\"1\"> test <b>test3</b> </aknp></doc>";
        String expectedResult = "<doc><aknp id=\"1\"> test <b><span class=\"leos-content-removed\">test2</span><span class=\"leos-content-new\">test3</span></b> </aknp></doc>";
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);

    }

    @Test
    public void test_two_clumns_text_diff_inside_b() {
        String oldContent = "<doc><aknp id=\"1\"> test <b>test2</b> </aknp></doc>";
        String newContent = "<doc><aknp id=\"1\"> test <b>test3</b> </aknp></doc>";
        String expectedLeftResult =  "<doc><aknp id=\"1\"><span class=\"leos-content-modified\"><input type=\"hidden\" name=\"modification_0\"/> test <b><span class=\"leos-content-removed\">test2</span></b> </span></aknp></doc>";
        String expectedRightResult = "<doc><aknp id=\"1\"><span class=\"leos-content-modified\"><input type=\"hidden\" name=\"modification_0\"/> test <b><span class=\"leos-content-new\">test3</span></b> </span></aknp></doc>";
        String[]  result = contentComparatorService.twoColumnsCompareContents(new ContentComparatorContext.Builder(oldContent, newContent).build());
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
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);

    }

    @Test
    public void test_text_new_elements_diff_blanks_without_ids() {
        String oldContent = "<doc><aknp id=\"1\">text0 <b>text2 text3 <i>text4</i> text5 text6 text7 text8</b> test9 <em> test10 and text 11 </em></aknp></doc>";
        String newContent = "<doc><aknp id=\"1\">text0 <li> mine is </li> <b>text2 text2-1 text3 <i>text4</i> text5 text6 text7 text8</b> test9 <em> test10 and text 11 </em></aknp></doc>";
        String expectedResult = "<doc><aknp id=\"1\">text0 <span class=\"leos-content-new\"><li><span class=\"leos-content-new\"> mine</span><span class=\"leos-content-new\"> is</span><span class=\"leos-content-new\"> </span></li></span>" +
                "<span class=\"leos-content-new\"> </span>" +
                "<b>text2<span class=\"leos-content-new\"> text2-1</span> text3 <i>text4</i> text5 text6 text7 text8</b> test9 <em> test10 and text 11 </em></aknp></doc>";
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
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
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
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
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);

    }

    @Test
    public void test_authorialnote_diff_tooltip() {
        String oldContent = "<doc><aknp id=\"1\">ref: <authorialnote id=\"akn_0sOWaY\" data-tooltip=\"old ref\">1</authorialnote></aknp></doc>";
        String newContent = "<doc><aknp id=\"1\">ref: <authorialnote id=\"akn_0sOWaY\" data-tooltip=\"new ref\">1</authorialnote></aknp></doc>";

        String expectedResult = "<doc><aknp id=\"1\">ref: <span class=\"leos-content-removed\"><authorialnote id=\"akn_0sOWaY\" data-tooltip=\"old ref\">1</authorialnote></span>" +
                "<span class=\"leos-content-new\"><authorialnote id=\"akn_0sOWaY\" data-tooltip=\"new ref\">1</authorialnote></span></aknp></doc>";
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_authorialnote_diff_value() {
        String oldContent = "<doc><aknp id=\"1\">ref: <authorialnote id=\"akn_0sOWaY\" data-tooltip=\"old ref\">1</authorialnote></aknp></doc>";
        String newContent = "<doc><aknp id=\"1\">ref: <authorialnote id=\"akn_0sOWaY\" data-tooltip=\"old ref\">2</authorialnote></aknp></doc>";

        String expectedResult = "<doc><aknp id=\"1\">ref: <span class=\"leos-content-removed\"><authorialnote id=\"akn_0sOWaY\" data-tooltip=\"old ref\">1</authorialnote></span>" +
                "<span class=\"leos-content-new\"><authorialnote id=\"akn_0sOWaY\" data-tooltip=\"old ref\">2</authorialnote></span></aknp></doc>";
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_authorialnote_diff_tooltip_value() {
        String oldContent = "<doc><aknp id=\"1\">ref: <authorialnote id=\"akn_0sOWaY\" data-tooltip=\"old ref\">1</authorialnote></aknp></doc>";
        String newContent = "<doc><aknp id=\"1\">ref: <authorialnote id=\"akn_0sOWaY\" data-tooltip=\"new ref\">2</authorialnote></aknp></doc>";

        String expectedResult = "<doc><aknp id=\"1\">ref: <span class=\"leos-content-removed\"><authorialnote id=\"akn_0sOWaY\" data-tooltip=\"old ref\">1</authorialnote></span>" +
                "<span class=\"leos-content-new\"><authorialnote id=\"akn_0sOWaY\" data-tooltip=\"new ref\">2</authorialnote></span></aknp></doc>";
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
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
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);
    }

    /**
     * TABLE TEST
     * */

    @Test
    public void test_table_diff_row_added() {
        String oldContent = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td><td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\"><aknp id=\"art_1_HajQP1\">22</aknp></td><td id=\"art_1_piXIfk\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_3g180s\"><aknp id=\"art_1_RMx5QI\">32</aknp></td><td id=\"art_1_HbiKIY\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String newContent = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td><td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\"><aknp id=\"art_1_HajQP1\">22</aknp></td><td id=\"art_1_piXIfk\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_YFeoxM\"><td id=\"art_1_Qp0F7z\"><aknp id=\"art_1_yxZtY9\">41</aknp></td><td id=\"art_1_GbEUCa\"><aknp id=\"art_1_nYEEAB\">42</aknp></td><td id=\"art_1_X8NvrM\"><aknp id=\"art_1_seN5sy\">43</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_3g180s\"><aknp id=\"art_1_RMx5QI\">32</aknp></td><td id=\"art_1_HbiKIY\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String expectedResult = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td><td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\"><aknp id=\"art_1_HajQP1\">22</aknp></td><td id=\"art_1_piXIfk\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr class=\"leos-content-new\" id=\"art_1_YFeoxM\"><td id=\"art_1_Qp0F7z\"><aknp id=\"art_1_yxZtY9\">41</aknp></td><td id=\"art_1_GbEUCa\"><aknp id=\"art_1_nYEEAB\">42</aknp></td>"
                +"<td id=\"art_1_X8NvrM\"><aknp id=\"art_1_seN5sy\">43</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_3g180s\"><aknp id=\"art_1_RMx5QI\">32</aknp></td><td id=\"art_1_HbiKIY\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_table_diff_row_removed() {
        String oldContent = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td><td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\"><aknp id=\"art_1_HajQP1\">22</aknp></td><td id=\"art_1_piXIfk\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_3g180s\"><aknp id=\"art_1_RMx5QI\">32</aknp></td><td id=\"art_1_HbiKIY\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String newContent = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td><td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_3g180s\"><aknp id=\"art_1_RMx5QI\">32</aknp></td><td id=\"art_1_HbiKIY\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String expectedResult = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td><td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr class=\"leos-content-removed\" id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\"><aknp id=\"art_1_HajQP1\">22</aknp></td>"
                +"<td id=\"art_1_piXIfk\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_3g180s\"><aknp id=\"art_1_RMx5QI\">32</aknp></td><td id=\"art_1_HbiKIY\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_table_diff_colum_added() {
        String oldContent = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td><td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\"><aknp id=\"art_1_HajQP1\">22</aknp></td><td id=\"art_1_piXIfk\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_3g180s\"><aknp id=\"art_1_RMx5QI\">32</aknp></td><td id=\"art_1_HbiKIY\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String newContent = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td><td id=\"art_1_m3BbYK\"><aknp id=\"art_1_rMSi99\">14</aknp></td>"
                +"<td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\"><aknp id=\"art_1_HajQP1\">22</aknp></td><td id=\"art_1_BQys86\"><aknp id=\"art_1_OHQh4c\">24</aknp></td>"
                +"<td id=\"art_1_piXIfk\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_3g180s\"><aknp id=\"art_1_RMx5QI\">32</aknp></td><td id=\"art_1_S61dc8\"><aknp id=\"art_1_sOCnTs\">34</aknp></td>"
                +"<td id=\"art_1_HbiKIY\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String expectedResult = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td>"
                +"<td class=\"leos-content-new\" id=\"art_1_m3BbYK\"><aknp id=\"art_1_rMSi99\">14</aknp></td><td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\"><aknp id=\"art_1_HajQP1\">22</aknp></td>"
                +"<td class=\"leos-content-new\" id=\"art_1_BQys86\"><aknp id=\"art_1_OHQh4c\">24</aknp></td><td id=\"art_1_piXIfk\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_3g180s\"><aknp id=\"art_1_RMx5QI\">32</aknp></td>"
                +"<td class=\"leos-content-new\" id=\"art_1_S61dc8\"><aknp id=\"art_1_sOCnTs\">34</aknp></td><td id=\"art_1_HbiKIY\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_table_diff_colum_removed() {
        String oldContent = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td><td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\"><aknp id=\"art_1_HajQP1\">22</aknp></td><td id=\"art_1_piXIfk\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_3g180s\"><aknp id=\"art_1_RMx5QI\">32</aknp></td><td id=\"art_1_HbiKIY\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String newContent = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_piXIfk\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_HbiKIY\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String expectedResult = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td class=\"leos-content-removed\" id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td>"
                +"<td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td class=\"leos-content-removed\" id=\"art_1_GPliSK\"><aknp id=\"art_1_HajQP1\">22</aknp></td>"
                +"<td id=\"art_1_piXIfk\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td class=\"leos-content-removed\" id=\"art_1_3g180s\"><aknp id=\"art_1_RMx5QI\">32</aknp></td>"
                +"<td id=\"art_1_HbiKIY\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_table_diff_colum_removed_keep_class() {
        String oldContent = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td><td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\" class=\"myClass\"><aknp id=\"art_1_HajQP1\">22</aknp></td>"
                +"<td id=\"art_1_piXIfk\" style=\"myStyle\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_3g180s\" width=\"100px\" style=\"myStyle2\"><aknp id=\"art_1_RMx5QI\">32</aknp></td>"
                +"<td id=\"art_1_HbiKIY\" class=\"myClass2\" width=\"100px\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String newContent = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td></tr><tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td></tr>"
                +"</table></blockContainer>";
        String expectedResult = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td class=\"leos-content-removed\" id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td>"
                +"<td class=\"leos-content-removed\" id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\" class=\"leos-content-removed myClass\"><aknp id=\"art_1_HajQP1\">22</aknp></td>"
                +"<td class=\"leos-content-removed\" id=\"art_1_piXIfk\" style=\"myStyle\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td class=\"leos-content-removed\" id=\"art_1_3g180s\" width=\"100px\" style=\"myStyle2\"><aknp id=\"art_1_RMx5QI\">32</aknp></td>"
                +"<td id=\"art_1_HbiKIY\" class=\"leos-content-removed myClass2\" width=\"100px\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_table_diff_cell_merged() {
        String oldContent = "<blockContainer id=\"body__blockcontainer_1\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"akn_RTIV96\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"akn_kdDevm\"><td id=\"akn_JkRPbo\"><p id=\"akn_XI6WoD\">1</p></td><td id=\"akn_M841Gy\"><p id=\"akn_BS0LCE\">2</p></td><td id=\"akn_wunh8X\"><p id=\"akn_2tp9sa\">3</p></td></tr>"
                +"<tr id=\"akn_nufwWM\"><td id=\"akn_YozCcq\"><p id=\"akn_bfJUBW\">11</p></td><td rowspan=\"1\" id=\"akn_3NQOhC\"><p id=\"akn_v1cSZo\">22</p></td>"
                +"<td rowspan=\"1\" id=\"akn_icJYkP\"><p id=\"akn_2jJv0R\">33</p></td></tr>"
                +"<tr id=\"akn_5D0exg\"><td id=\"akn_8HxAlj\"><p id=\"akn_1zKf0X\">111</p></td><td id=\"akn_WBDOnj\"><p id=\"akn_wJKHTj\">222</p></td><td id=\"akn_aihqio\"><p id=\"akn_fkayML\">333</p></td></tr>"
                +"</table></blockContainer>";
        String newContent = "<blockContainer id=\"body__blockcontainer_1\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"akn_RTIV96\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"akn_kdDevm\"><td id=\"akn_JkRPbo\"><p id=\"akn_XI6WoD\">1</p></td><td id=\"akn_M841Gy\"><p id=\"akn_BS0LCE\">2</p></td><td id=\"akn_wunh8X\"><p id=\"akn_2tp9sa\">3</p></td></tr>"
                +"<tr id=\"akn_nufwWM\"><td id=\"akn_YozCcq\"><p id=\"akn_bfJUBW\">11</p></td><td rowspan=\"1\" colspan=\"2\" id=\"akn_3NQOhC\"><p id=\"akn_v1cSZo\">22</p><p id=\"akn_2jJv0R\">33</p></td></tr>"
                +"<tr id=\"akn_5D0exg\"><td id=\"akn_8HxAlj\"><p id=\"akn_1zKf0X\">111</p></td><td id=\"akn_WBDOnj\"><p id=\"akn_wJKHTj\">222</p></td><td id=\"akn_aihqio\"><p id=\"akn_fkayML\">333</p></td></tr>"
                +"</table></blockContainer>";
        String expectedResult = "<blockContainer id=\"body__blockcontainer_1\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"akn_RTIV96\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"akn_kdDevm\"><td id=\"akn_JkRPbo\"><p id=\"akn_XI6WoD\">1</p></td><td id=\"akn_M841Gy\"><p id=\"akn_BS0LCE\">2</p></td><td id=\"akn_wunh8X\"><p id=\"akn_2tp9sa\">3</p></td></tr>"
                +"<tr id=\"akn_nufwWM\"><td id=\"akn_YozCcq\"><p id=\"akn_bfJUBW\">11</p></td>"
                +"<td rowspan=\"1\" colspan=\"2\" id=\"akn_3NQOhC\"><p id=\"akn_v1cSZo\">22</p><p class=\"leos-content-new\" id=\"akn_2jJv0R\">33</p></td>"
                +"<td class=\"leos-content-removed\" rowspan=\"1\" id=\"akn_icJYkP\"><p id=\"akn_2jJv0R\">33</p></td></tr>"
                +"<tr id=\"akn_5D0exg\"><td id=\"akn_8HxAlj\"><p id=\"akn_1zKf0X\">111</p></td><td id=\"akn_WBDOnj\"><p id=\"akn_wJKHTj\">222</p></td><td id=\"akn_aihqio\"><p id=\"akn_fkayML\">333</p></td></tr>"
                +"</table></blockContainer>";
        String result = contentComparatorService.compareContents(new ContentComparatorContext.Builder(oldContent, newContent)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
        assertEquals(expectedResult, result);
    }

    @Test
    public void test_two_columns_table_diff_row_added() {
        String oldContent = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td><td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\"><aknp id=\"art_1_HajQP1\">22</aknp></td><td id=\"art_1_piXIfk\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_3g180s\"><aknp id=\"art_1_RMx5QI\">32</aknp></td><td id=\"art_1_HbiKIY\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String newContent = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td><td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\"><aknp id=\"art_1_HajQP1\">22</aknp></td><td id=\"art_1_piXIfk\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_YFeoxM\"><td id=\"art_1_Qp0F7z\"><aknp id=\"art_1_yxZtY9\">41</aknp></td><td id=\"art_1_GbEUCa\"><aknp id=\"art_1_nYEEAB\">42</aknp></td><td id=\"art_1_X8NvrM\"><aknp id=\"art_1_seN5sy\">43</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_3g180s\"><aknp id=\"art_1_RMx5QI\">32</aknp></td><td id=\"art_1_HbiKIY\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String expectedLeftResult = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td><td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\"><aknp id=\"art_1_HajQP1\">22</aknp></td><td id=\"art_1_piXIfk\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr class=\"leos-marker-content-added\" id=\"art_1_YFeoxM\"><td id=\"art_1_Qp0F7z\"><aknp id=\"art_1_yxZtY9\">41</aknp></td>"
                +"<td id=\"art_1_GbEUCa\"><aknp id=\"art_1_nYEEAB\">42</aknp></td><td id=\"art_1_X8NvrM\"><aknp id=\"art_1_seN5sy\">43</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_3g180s\"><aknp id=\"art_1_RMx5QI\">32</aknp></td><td id=\"art_1_HbiKIY\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String expectedRightResult = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td><td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\"><aknp id=\"art_1_HajQP1\">22</aknp></td><td id=\"art_1_piXIfk\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr class=\"leos-content-new\" id=\"art_1_YFeoxM\"><td id=\"art_1_Qp0F7z\"><aknp id=\"art_1_yxZtY9\">41</aknp></td>"
                +"<td id=\"art_1_GbEUCa\"><aknp id=\"art_1_nYEEAB\">42</aknp></td><td id=\"art_1_X8NvrM\"><aknp id=\"art_1_seN5sy\">43</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_3g180s\"><aknp id=\"art_1_RMx5QI\">32</aknp></td><td id=\"art_1_HbiKIY\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String[]  result = contentComparatorService.twoColumnsCompareContents(new ContentComparatorContext.Builder(oldContent, newContent).build());
        assertEquals(expectedLeftResult, result[0]);
        assertEquals(expectedRightResult, result[1]);
    }

    @Test
    public void test_two_columns_table_diff_colum_removed_keep_class() {
        String oldContent = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td><td id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\" class=\"myClass\"><aknp id=\"art_1_HajQP1\">22</aknp></td>"
                +"<td id=\"art_1_piXIfk\" style=\"myStyle\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td id=\"art_1_3g180s\" width=\"100px\" style=\"myStyle2\"><aknp id=\"art_1_RMx5QI\">32</aknp></td>"
                +"<td id=\"art_1_HbiKIY\" class=\"myClass2\" width=\"100px\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String newContent = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td></tr><tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td></tr>"
                +"</table></blockContainer>";
        String expectedLeftResult = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td class=\"leos-content-removed\" id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td>"
                +"<td class=\"leos-content-removed\" id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\" class=\"leos-content-removed myClass\"><aknp id=\"art_1_HajQP1\">22</aknp></td>"
                +"<td class=\"leos-content-removed\" id=\"art_1_piXIfk\" style=\"myStyle\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td class=\"leos-content-removed\" id=\"art_1_3g180s\" width=\"100px\" style=\"myStyle2\"><aknp id=\"art_1_RMx5QI\">32</aknp></td>"
                +"<td id=\"art_1_HbiKIY\" class=\"leos-content-removed myClass2\" width=\"100px\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String expectedRightResult = "<blockContainer id=\"akn_annex_cee4K4\" leos:editable=\"true\" leos:deletable=\"true\"><table id=\"art_1_UHAr9x\" border=\"1\" cellpadding=\"1\" cellspacing=\"1\" style=\"width: 500px;\">"
                +"<tr id=\"art_1_GTOJWM\"><td id=\"art_1_us9lfi\"><aknp id=\"art_1_OqCaT5\">11</aknp></td><td class=\"leos-marker-content-removed\" id=\"art_1_FQTyw7\"><aknp id=\"art_1_VQkemL\">12</aknp></td>"
                +"<td class=\"leos-marker-content-removed\" id=\"art_1_GtCogU\"><aknp id=\"art_1_Hs10Ql\">13</aknp></td></tr>"
                +"<tr id=\"art_1_gB9KqF\"><td id=\"art_1_R0h0H5\"><aknp id=\"art_1_DYWAYz\">21</aknp></td><td id=\"art_1_GPliSK\" class=\"leos-marker-content-removed myClass\"><aknp id=\"art_1_HajQP1\">22</aknp></td>"
                +"<td class=\"leos-marker-content-removed\" id=\"art_1_piXIfk\" style=\"myStyle\"><aknp id=\"art_1_oHumnc\">23</aknp></td></tr>"
                +"<tr id=\"art_1_ALdAhg\"><td id=\"art_1_QIFIXp\"><aknp id=\"art_1_pZ986S\">31</aknp></td><td class=\"leos-marker-content-removed\" id=\"art_1_3g180s\" width=\"100px\" style=\"myStyle2\"><aknp id=\"art_1_RMx5QI\">32</aknp></td>"
                +"<td id=\"art_1_HbiKIY\" class=\"leos-marker-content-removed myClass2\" width=\"100px\"><aknp id=\"art_1_bEp4to\">33</aknp></td></tr>"
                +"</table></blockContainer>";
        String[]  result = contentComparatorService.twoColumnsCompareContents(new ContentComparatorContext.Builder(oldContent, newContent).build());
        assertEquals(expectedLeftResult, result[0]);
        assertEquals(expectedRightResult, result[1]);
    }
}
