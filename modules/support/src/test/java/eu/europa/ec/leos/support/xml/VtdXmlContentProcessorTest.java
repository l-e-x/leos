/**
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
package eu.europa.ec.leos.support.xml;

import eu.europa.ec.leos.test.support.LeosTest;
import eu.europa.ec.leos.vo.CommentVO;
import eu.europa.ec.leos.vo.CommentVO.RefersTo;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.MessageSource;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class VtdXmlContentProcessorTest extends LeosTest {

    @Mock
    private MessageSource messageHelper;

    @InjectMocks
    private XmlContentProcessor xmlContentProcessor = new VtdXmlContentProcessor();

    private byte[] docContent;

    @Before
    public void setup() {
        super.setup();
        String doc = "<part id=\"part11\">"
                +
                "                <num id=\"n1\" class=\"PartNumber\">Part XI</num>"
                +
                "                <heading id=\"h1\" class=\"PartHeading\">FINAL PROVISIONS</heading>"
                +
                "                <article id=\"art485\">"
                +
                "                    <num id=\"n1\" class=\"ArticleNumber\">Article 485</num>"
                +
                "                    <paragraph id=\"art485-par1\">"
                +
                "                        <num  id=\"n2\" class=\"Paragraph(numbered)\">1.</num>"
                +
                "                        <content id=\"c1\" >"
                +
                "                            <p  id=\"p1\" class=\"Paragraph(numbered)\">Subject to paragraph 2, this Regulation<authorialNote marker=\"2\" id=\"a1\"><p id=\"ptest1\">TestNote1</p></authorialNote> shall apply from 1 January 2013.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                    <paragraph id=\"art485-par2\">"
                +
                "                        <num  id=\"n2\" class=\"Paragraph(numbered)\">2.</num>"
                +
                "                        <content id=\"con\">"
                +
                "                            <p  id=\"p2\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015<authorialNote marker=\"4\" id=\"a2\"><p id=\"ptest2\">TestNote2</p></authorialNote>.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                </article>"
                +
                "                <article id=\"art486\">"
                +
                "                    <num  id=\"n3\" class=\"ArticleNumber\">Article 486</num>"
                +
                "                    <alinea id=\"art486-aln1\">"
                +
                "                        <content id=\"c3\">"
                +
                "                            <p id=\"p3\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i id=\"i2\">Official Journal of the European<authorialNote marker=\"8\" id=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
                +
                "                        </content>" +
                "                    </alinea>" +
                "                </article>" +
                "            </part>";

        docContent = doc.getBytes(UTF_8);
    }


    @Test
    public void test_getAncestorsIdsForElementId_should_returnEmptyArray_when_rootElementIdPassed() {
        List<String> ids = xmlContentProcessor.getAncestorsIdsForElementId(
                docContent, "part11");
        assertThat(ids, is(Collections.EMPTY_LIST));
    }

    @Test
    public void test_getAncestorsIdsForElementId_should_returnArrayWithAllAncestorsIds_when_nestedElementPassed() {
        List<String> ids = xmlContentProcessor.getAncestorsIdsForElementId(
                docContent, "p2");
        assertThat(ids,
                is(Arrays.asList("part11", "art485", "art485-par2", "con")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getAncestorsIdsForElementId_should_throwException_when_nonExistedElementPassed()
            throws Exception {
        xmlContentProcessor.getAncestorsIdsForElementId(docContent,
                "notExisted");
    }


    @Test
    public void test_getTagContentByNameAndId_should_returnTagContent_when_tagAndIdFound() throws Exception {
        String tagContent = xmlContentProcessor.getElementByNameAndId(docContent, "alinea", "art486-aln1");
        assertThat(
                tagContent,
                is("<alinea id=\"art486-aln1\">"
                +
                "                        <content id=\"c3\">"
                +
                "                            <p id=\"p3\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i id=\"i2\">Official Journal of the European<authorialNote marker=\"8\" id=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
                +
                "                        </content>" +
                "                    </alinea>"));
    }

    @Test
    public void test_getTagContentByNameAndId_should_returnNull_when_tagAndIdNotFound() throws Exception {
        String tagContent = xmlContentProcessor.getElementByNameAndId(docContent, "alinea", "art486-aln1123456789");
        assertThat(tagContent, is(nullValue()));
    }

    @Test
    public void test_getTagContentByNameAndId_should_returnFirstTag_when_IdNull() throws Exception {
        String tagContent = xmlContentProcessor.getElementByNameAndId(docContent, "alinea", null);
        assertThat(
                tagContent,
                is("<alinea id=\"art486-aln1\">"
                +
                "                        <content id=\"c3\">"
                +
                "                            <p id=\"p3\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i id=\"i2\">Official Journal of the European<authorialNote marker=\"8\" id=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
                +
                "                        </content>" +
                "                    </alinea>"));
    }

    @Test(expected = RuntimeException.class)
    public void test_getTagContentByNameAndId_should_throwRuntimeException_when_illegalXmlFormat() throws Exception {
        String xml = " <article id=\"art486\">" +
                "                    <num class=\"ArticleNumber\">Article 486</num>";
        String tagContent = xmlContentProcessor.getElementByNameAndId(xml.getBytes(UTF_8), "alinea", "art486-aln1");
        assertThat(tagContent, is(nullValue()));
    }

    @Test
    public void test_replaceElementByTagNameAndId_should_match_returnedTagContent() throws Exception {

        String newContent = "<article id=\"art486\">"
                +
                "                    <num id=\"num1\" class=\"ArticleNumber\">Article 486</num>"
                +
                "                    <alinea id=\"art486-aln1\">"
                +
                "                        <content id=\"c\">"
                +
                "                            <p id=\"p\" class=\"Paragraph(unnumbered)\">This text should appear in the main document after merge<authorialNote id=\"a4\" marker=\"1\"><p id=\"p1\">TestNoteX</p></authorialNote> with the updated Article <i id=\"i1\">Official Journal of the European Union</i>.</p>"
                +
                "                        </content>" +
                "                    </alinea>" +
                "                </article>";

        // DO THE ACTUAL CALL
        byte[] returnedElement = xmlContentProcessor.replaceElementByTagNameAndId(docContent, newContent, "article", "art486");

        String expected = "<part id=\"part11\">"
                +
                "                <num id=\"n1\" class=\"PartNumber\">Part XI</num>"
                +
                "                <heading id=\"h1\" class=\"PartHeading\">FINAL PROVISIONS</heading>"
                +
                "                <article id=\"art485\">"
                +
                "                    <num id=\"n1\" class=\"ArticleNumber\">Article 485</num>"
                +
                "                    <paragraph id=\"art485-par1\">"
                +
                "                        <num  id=\"n2\" class=\"Paragraph(numbered)\">1.</num>"
                +
                "                        <content id=\"c1\" >"
                +
                "                            <p  id=\"p1\" class=\"Paragraph(numbered)\">Subject to paragraph 2, this Regulation<authorialNote marker=\"1\" id=\"a1\"><p id=\"ptest1\">TestNote1</p></authorialNote> shall apply from 1 January 2013.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                    <paragraph id=\"art485-par2\">"
                +
                "                        <num  id=\"n2\" class=\"Paragraph(numbered)\">2.</num>"
                +
                "                        <content id=\"con\">"
                +
                "                            <p  id=\"p2\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015<authorialNote marker=\"2\" id=\"a2\"><p id=\"ptest2\">TestNote2</p></authorialNote>.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                </article>"
                +
                "                <article id=\"art486\">"
                +
                "                    <num id=\"num1\" class=\"ArticleNumber\">Article 486</num>"
                +
                "                    <alinea id=\"art486-aln1\">"
                +
                "                        <content id=\"c\">"
                +
                "                            <p id=\"p\" class=\"Paragraph(unnumbered)\">This text should appear in the main document after merge<authorialNote id=\"a4\" marker=\"3\"><p id=\"p1\">TestNoteX</p></authorialNote> with the updated Article <i id=\"i1\">Official Journal of the European Union</i>.</p>"
                +
                "                        </content>" +
                "                    </alinea>" +
                "                </article>" +
                "            </part>";

        assertThat(returnedElement, is(expected.getBytes(UTF_8)));
    }

    @Test
    public void test_replaceElementByTagName() throws Exception {

        String xml = "<bill><meta><proprietary>test</proprietary></meta></bill>";
        byte[] result = xmlContentProcessor.replaceElementsWithTagName(xml.getBytes(UTF_8), "proprietary", "<proprietary>new</proprietary>");
        String expected = "<bill><meta><proprietary>new</proprietary></meta></bill>";
        assertThat(new String(result), is(expected));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_replaceElementByTagName_ShouldReturnUnchanged_WhenTagNotFound() throws Exception {

        String xml = "<bill><meta><proprietary>test</proprietary></meta></bill>";
        xmlContentProcessor.replaceElementsWithTagName(xml.getBytes(UTF_8), "proprietary2", "<proprietary>new</proprietary>");
    }

    @Test
    public void test_deleteElementByTagNameAndId_should_match_returnedTagContent() throws Exception {

        // DO THE ACTUAL CALL
        byte[] returnedElement = xmlContentProcessor.deleteElementByTagNameAndId(docContent, "article", "art486");
        String expected = "<part id=\"part11\">"
                +
                "                <num id=\"n1\" class=\"PartNumber\">Part XI</num>"
                +
                "                <heading id=\"h1\" class=\"PartHeading\">FINAL PROVISIONS</heading>"
                +
                "                <article id=\"art485\">"
                +
                "                    <num id=\"n1\" class=\"ArticleNumber\">Article 485</num>"
                +
                "                    <paragraph id=\"art485-par1\">"
                +
                "                        <num  id=\"n2\" class=\"Paragraph(numbered)\">1.</num>"
                +
                "                        <content id=\"c1\" >"
                +
                "                            <p  id=\"p1\" class=\"Paragraph(numbered)\">Subject to paragraph 2, this Regulation<authorialNote marker=\"2\" id=\"a1\"><p id=\"ptest1\">TestNote1</p></authorialNote> shall apply from 1 January 2013.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                    <paragraph id=\"art485-par2\">"
                +
                "                        <num  id=\"n2\" class=\"Paragraph(numbered)\">2.</num>"
                +
                "                        <content id=\"con\">"
                +
                "                            <p  id=\"p2\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015<authorialNote marker=\"4\" id=\"a2\"><p id=\"ptest2\">TestNote2</p></authorialNote>.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                </article>"
                +
                "                "+
                "            </part>";

        assertThat(returnedElement, is(expected.getBytes(UTF_8)));
    }

    @Test
    public void test_insertElementByTagNameAndId_when_insert_after() throws Exception {

        String template = "                <article id=\"art435\">" +
                "              <num id=\"n4\">Article #</num>" +
                "              <heading id=\"h4\">Article heading...</heading>" +
                "              <paragraph id=\"art1-par1\">" +
                "                <num id=\"n4p\">1.</num>" +
                "                <content id=\"c4\">" +
                "                  <p id=\"p4\">Text.<authorialNote marker=\"1\" id=\"a1\"><p>TestNote4</p></authorialNote>..</p>" +
                "                </content>" +
                "              </paragraph>" +
                "             </article>";

        // DO THE ACTUAL CALL - Insert After
        byte[] returnedElement = xmlContentProcessor.insertElementByTagNameAndId(docContent, template, "article", "art486", false);

        String expected ="<part id=\"part11\">"
                +
                "                <num id=\"n1\" class=\"PartNumber\">Part XI</num>"
                +
                "                <heading id=\"h1\" class=\"PartHeading\">FINAL PROVISIONS</heading>"
                +
                "                <article id=\"art485\">"
                +
                "                    <num id=\"n1\" class=\"ArticleNumber\">Article 485</num>"
                +
                "                    <paragraph id=\"art485-par1\">"
                +
                "                        <num  id=\"n2\" class=\"Paragraph(numbered)\">1.</num>"
                +
                "                        <content id=\"c1\" >"
                +
                "                            <p  id=\"p1\" class=\"Paragraph(numbered)\">Subject to paragraph 2, this Regulation<authorialNote marker=\"2\" id=\"a1\"><p id=\"ptest1\">TestNote1</p></authorialNote> shall apply from 1 January 2013.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                    <paragraph id=\"art485-par2\">"
                +
                "                        <num  id=\"n2\" class=\"Paragraph(numbered)\">2.</num>"
                +
                "                        <content id=\"con\">"
                +
                "                            <p  id=\"p2\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015<authorialNote marker=\"4\" id=\"a2\"><p id=\"ptest2\">TestNote2</p></authorialNote>.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                </article>"
                +
                "                <article id=\"art486\">"
                +
                "                    <num  id=\"n3\" class=\"ArticleNumber\">Article 486</num>"
                +
                "                    <alinea id=\"art486-aln1\">"
                +
                "                        <content id=\"c3\">"
                +
                "                            <p id=\"p3\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i id=\"i2\">Official Journal of the European<authorialNote marker=\"8\" id=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
                +
                "                        </content>" +
                "                    </alinea>" +
                "                </article>" +
                "                <article id=\"art435\">" +
                "              <num id=\"n4\">Article #</num>" +
                "              <heading id=\"h4\">Article heading...</heading>" +
                "              <paragraph id=\"art1-par1\">" +
                "                <num id=\"n4p\">1.</num>" +
                "                <content id=\"c4\">" +
                "                  <p id=\"p4\">Text.<authorialNote marker=\"1\" id=\"a1\"><p>TestNote4</p></authorialNote>..</p>" +
                "                </content>" +
                "              </paragraph>" +
                "             </article>"+
                "            </part>";;

        assertThat(returnedElement, is(expected.getBytes(UTF_8)));

    }

    @Test
    public void test_insertElementByTagNameAndId_when_insert_before() throws Exception {

        String template = "                <article id=\"art435\">" +
                "              <num id=\"n4\">Article #</num>" +
                "              <heading id=\"h4\">Article heading...</heading>" +
                "              <paragraph id=\"art1-par1\">" +
                "                <num id=\"n4p\">1.</num>" +
                "                <content id=\"c4\">" +
                "                  <p id=\"p4\">Text.<authorialNote marker=\"1\" id=\"a1\"><p>TestNote4</p></authorialNote>..</p>" +
                "                </content>" +
                "              </paragraph>" +
                "             </article>";

        // DO THE ACTUAL CALL - Insert Before
        byte[] returnedElement = xmlContentProcessor.insertElementByTagNameAndId(docContent, template, "article", "art486", true);

        String expected = "<part id=\"part11\">"
                +
                "                <num id=\"n1\" class=\"PartNumber\">Part XI</num>"
                +
                "                <heading id=\"h1\" class=\"PartHeading\">FINAL PROVISIONS</heading>"
                +
                "                <article id=\"art485\">"
                +
                "                    <num id=\"n1\" class=\"ArticleNumber\">Article 485</num>"
                +
                "                    <paragraph id=\"art485-par1\">"
                +
                "                        <num  id=\"n2\" class=\"Paragraph(numbered)\">1.</num>"
                +
                "                        <content id=\"c1\" >"
                +
                "                            <p  id=\"p1\" class=\"Paragraph(numbered)\">Subject to paragraph 2, this Regulation<authorialNote marker=\"2\" id=\"a1\"><p id=\"ptest1\">TestNote1</p></authorialNote> shall apply from 1 January 2013.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                    <paragraph id=\"art485-par2\">"
                +
                "                        <num  id=\"n2\" class=\"Paragraph(numbered)\">2.</num>"
                +
                "                        <content id=\"con\">"
                +
                "                            <p  id=\"p2\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015<authorialNote marker=\"4\" id=\"a2\"><p id=\"ptest2\">TestNote2</p></authorialNote>.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                </article>"
                +
                "                                <article id=\"art435\">" +
                "              <num id=\"n4\">Article #</num>" +
                "              <heading id=\"h4\">Article heading...</heading>" +
                "              <paragraph id=\"art1-par1\">" +
                "                <num id=\"n4p\">1.</num>" +
                "                <content id=\"c4\">" +
                "                  <p id=\"p4\">Text.<authorialNote marker=\"1\" id=\"a1\"><p>TestNote4</p></authorialNote>..</p>" +
                "                </content>" +
                "              </paragraph>" +
                "             </article>" +
                "<article id=\"art486\">"
                +
                "                    <num  id=\"n3\" class=\"ArticleNumber\">Article 486</num>"
                +
                "                    <alinea id=\"art486-aln1\">"
                +
                "                        <content id=\"c3\">"
                +
                "                            <p id=\"p3\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i id=\"i2\">Official Journal of the European<authorialNote marker=\"8\" id=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
                +
                "                        </content>" +
                "                    </alinea>" +
                "                </article>" +
                "            </part>"
                ;

        assertThat(returnedElement, is(expected.getBytes(UTF_8)));
    }

    @Test
    public void test_buildTableOfContentItemVOList_should_returnEmptyList_when_noTableOfContentFound() throws Exception {
        List<TableOfContentItemVO> tableOfContentItemVOList = xmlContentProcessor.buildTableOfContent(docContent);
        assertThat(tableOfContentItemVOList, is(notNullValue()));
        assertThat(tableOfContentItemVOList.size(), is(0));
    }

    @Test
    public void test_buildTableOfContentItemVOList_should_returnReturnCorrectContent_when_expectedFormat() throws Exception {

        byte[] fileContent = getFileContent("/akn_toc-test.xml");
        List<TableOfContentItemVO> tableOfContentItemVOList = xmlContentProcessor.buildTableOfContent(fileContent);
        assertThat(tableOfContentItemVOList, is(notNullValue()));
        assertThat(tableOfContentItemVOList.size(), is(4));

        // do a huge number of asserts..
        assertThat(tableOfContentItemVOList.get(0), is(new TableOfContentItemVO(TableOfContentItemVO.Type.PREFACE, null, null,
                "on [...] preface and half bold text", null, 7, 6)));
        assertThat(tableOfContentItemVOList.get(1), is(new TableOfContentItemVO(TableOfContentItemVO.Type.PREAMBLE, null, null, null,
                null, null, 15)));

        // build the body as second expected root item
        TableOfContentItemVO body = new TableOfContentItemVO(TableOfContentItemVO.Type.BODY, null, null, null, null, null, 18);
        TableOfContentItemVO part1 = new TableOfContentItemVO(TableOfContentItemVO.Type.PART, "part1", null, "part-head", null, 22, 19);
        body.addChildItem(part1);
        TableOfContentItemVO title = new TableOfContentItemVO(TableOfContentItemVO.Type.TITLE, "title1", "on [...] preface and half bold text", "title-head",
                29, 27, 24);
        part1.addChildItem(title);
        TableOfContentItemVO chapter = new TableOfContentItemVO(TableOfContentItemVO.Type.CHAPTER, "chap1", "chapter-num", "chapter-head", 40, 42, 37);
        title.addChildItem(chapter);
        TableOfContentItemVO section = new TableOfContentItemVO(TableOfContentItemVO.Type.SECTION, "sect1", "section-num", "section-head", 47, 49, 44);
        chapter.addChildItem(section);
        TableOfContentItemVO art = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art2", "Title I", "title", 70, 72, 66);
        chapter.addChildItem(art);

        TableOfContentItemVO subsection1 = new TableOfContentItemVO(TableOfContentItemVO.Type.SUBSECTION, "subsec1", "Subsection 1", "subsection-head",
                54, 56, 51);
        TableOfContentItemVO subsection2 = new TableOfContentItemVO(TableOfContentItemVO.Type.SUBSECTION, "subsec2", null, null, null, null, 62);
        section.addChildItem(subsection1);
        section.addChildItem(subsection2);
        TableOfContentItemVO artInSubs = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art1", null, null, null, null, 58);
        subsection1.addChildItem(artInSubs);

        assertThat(tableOfContentItemVOList.get(2), is(body));
        assertThat(tableOfContentItemVOList.get(3), is(new TableOfContentItemVO(TableOfContentItemVO.Type.CONCLUSIONS, null, null, null, null, null, 78)));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_boldTagInHeading() throws Exception {
        String xml = "<akomaNtoso><bill><body>    "
                + "<article id=\"art486\">" +
                "<num>  Article 486</num>" +
                "   <heading><content><p>1ste article</p></content></heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article id=\"art487\">" +
                "<num><content><p>Article 487</p></content></num>" +
                "<heading><content><p>2de article</p></content></heading>" +
                "<alinea id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article id=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th <b>test</b> article</heading>" +
                "<alinea id=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO art1 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art486", "Article 486",
                "<content><p>1ste article</p></content>", null, null, 4);
        TableOfContentItemVO art3 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art488", "Article 488 moved", "3th article became 2the",
                null, null, 32);
        TableOfContentItemVO art2 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art487", "<content><p>Article 487 moved</p></content>",
                "<content><p>2de article became 3the</p></content>", null, null, 17);

        articleVOs.add(art1);
        articleVOs.add(art3);
        articleVOs.add(art2);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(TableOfContentItemVO.Type.BODY, "body", null, null, null, null, 3);
        bodyVO.addAllChildItems(articleVOs);

        tableOfContentItemVOList.add(bodyVO);


        byte[] result = xmlContentProcessor.createDocumentContentWithNewTocList(tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill><body>"
                + "<article id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading><content><p>1ste article</p></content></heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article id=\"art488\">" +
                "<num>Article 488 moved</num>" +
                "<heading>3th article became 2the</heading>" +
                "<alinea id=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article id=\"art487\">" +
                "<num><content><p>Article 487 moved</p></content></num>" +
                "<heading><content><p>2de article became 3the</p></content></heading>" +
                "<alinea id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_oldContainedutf8() {
        String xml = "<akomaNtoso><bill><body>"
                + "<article id=\"art486\">" +
                "<num>Article 486 <placeholder>[…]</placeholder></num>" +
                "<heading><content><p>1ste article</p></content></heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";
        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO art1 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art486", "Article 486 <placeholder>[…]</placeholder>",
                "<content><p>1ste article</p></content>", null, null, 4);

        articleVOs.add(art1);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(TableOfContentItemVO.Type.BODY, "body", null, null, null, null, 3);
        bodyVO.addAllChildItems(articleVOs);
        tableOfContentItemVOList.add(bodyVO);


        byte[] result = xmlContentProcessor.createDocumentContentWithNewTocList(tableOfContentItemVOList, xml.getBytes(UTF_8));

        assertThat(new String(result, UTF_8), is(xml));

    }

    // amounts and L &lt; K
    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_oldContainedEscapedXML() {
        String xml = "<akomaNtoso><bill><body>"
                + "<article id=\"art486\">" +
                "<num>Article 486 <placeholder>[…]</placeholder></num>" +
                "<heading><content><p>1ste article</p></content></heading>" +
                "<alinea id=\"art486-aln1\">bla amounts and L &lt; K bla</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();

        TableOfContentItemVO art1 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art486", "Article 486 <placeholder>[…]</placeholder>",
                "<content><p>1ste article</p></content>", null, null, 4);

        articleVOs.add(art1);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(TableOfContentItemVO.Type.BODY, "body", null, null, null, null, 3);
        bodyVO.addAllChildItems(articleVOs);
        tableOfContentItemVOList.add(bodyVO);

        byte[] result = xmlContentProcessor.createDocumentContentWithNewTocList(tableOfContentItemVOList, xml.getBytes(UTF_8));

        assertThat(new String(result, UTF_8), is(xml));

    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_2newAreAddedAtSameOffset() throws Exception {
        String xml = "<akomaNtoso><bill><body>"
                + "<article id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading><content><p>1ste article</p></content></heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"

                + "<article id=\"art489\">" +
                "<num>Article 489</num>" +
                "<heading>4th article</heading>" +
                "<alinea id=\"art488-aln1\">bla bla 4</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO art1 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art486", "Article 486", "1ste article", null, null,
                4);
        TableOfContentItemVO art2 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, null, "Article 487 added", "2de article added", null, null,
                null);
        TableOfContentItemVO art3 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, null, "Article 488 added", "3th article added", null, null,
                null);
        TableOfContentItemVO art4 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art489", "Article 489", "4th article", null, null,
                17);

        articleVOs.add(art1);
        articleVOs.add(art2);
        articleVOs.add(art3);
        articleVOs.add(art4);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(TableOfContentItemVO.Type.BODY, "body", null, null, null, null, 3);
        bodyVO.addAllChildItems(articleVOs);
        tableOfContentItemVOList.add(bodyVO);

        byte[] result = xmlContentProcessor.createDocumentContentWithNewTocList(tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill><body>"
                + "<article id=\"art486\">"
                +
                "<num>Article 486</num>"
                +
                "<heading>1ste article</heading>"
                +
                "<alinea id=\"art486-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "            <article id=\".+\">              <num>Article 487 added</num>              "
                +
                "<heading>2de article added</heading>              <paragraph id=\".+-par1\">                <num>1.</num>"
                +
                "                <content>                  <p>Text...</p>                </content>              </paragraph>            </article>"
                + "            <article id=\".+\">              <num>Article 488 added</num>              "
                +
                "<heading>3th article added</heading>              <paragraph id=\".+-par1\">                <num>1.</num>"
                +
                "                <content>                  <p>Text...</p>                </content>              </paragraph>            </article>"
                + "<article id=\"art489\">" +
                "<num>Article 489</num>" +
                "<heading>4th article</heading>" +
                "<alinea id=\"art488-aln1\">bla bla 4</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";
        Pattern pattern = Pattern.compile(expected);
        Matcher matcher = pattern.matcher(new String(result));

        assertThat(expected + " should be found in " + new String(result), matcher.find(), is(true));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_articlesAreRemoved() throws Exception {
        String xml = "<akomaNtoso><bill><preface id =\"1\"><p>preface</p></preface>"
                + "<body><article id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article id=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article id=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th article</heading>" +
                "<alinea id=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();

        TableOfContentItemVO pref = new TableOfContentItemVO(TableOfContentItemVO.Type.PREFACE, "1", null, null, null, null, 3);
        TableOfContentItemVO art2 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art487", "Article 487", "2de article became 1the", null,
                null, 20);
        TableOfContentItemVO art3 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art488", "Article 488", "3th article became 2the", null,
                null, 31);
        tableOfContentItemVOList.add(pref);
        articleVOs.add(art2);
        articleVOs.add(art3);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(TableOfContentItemVO.Type.BODY, "body", null, null, null, null, 8);
        bodyVO.addAllChildItems(articleVOs);
        tableOfContentItemVOList.add(bodyVO);

        byte[] result = xmlContentProcessor.createDocumentContentWithNewTocList(tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill><preface id =\"1\"><p>preface</p></preface>"
                + "<body><article id=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article became 1the</heading>" +
                "<alinea id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article id=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th article became 2the</heading>" +
                "<alinea id=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_numAndHeadingAreAdded() throws Exception {
        String xml = "<akomaNtoso><bill><body>"
                + "<section id=\"sect1\">"
                + "</section>"
                + "</body></bill></akomaNtoso>";
        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();

        TableOfContentItemVO sec1 = new TableOfContentItemVO(TableOfContentItemVO.Type.SECTION, "sect1", "Section 1", "Paragraphs", null, null, 4);

        articleVOs.add(sec1);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(TableOfContentItemVO.Type.BODY, "body", null, null, null, null, 3);
        bodyVO.addAllChildItems(articleVOs);
        tableOfContentItemVOList.add(bodyVO);

        byte[] result = xmlContentProcessor.createDocumentContentWithNewTocList(tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill><body>"
                + "<section id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "</section>"
                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_articlesMovedFromSection() throws Exception {
        String xml = "<akomaNtoso><bill><body>"
                + "<section id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading >Paragraphs</heading>"
                + "<article id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article id=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "<section id=\"sect2\">"
                + "<num>Section 2</num>"
                + "<heading >Paragraphs</heading>"
                + "<article id=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th article</heading>" +
                "<alinea id=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(TableOfContentItemVO.Type.BODY, "body", null, null, null, null, 3);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(TableOfContentItemVO.Type.SECTION, "sect1", "Section 1", "Paragraphs", null, null, 4);
        TableOfContentItemVO art1 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art486", "Article 486", "1ste article", null, null,
                11);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(TableOfContentItemVO.Type.SECTION, "sect2", "Section 2", "Paragraphs", null, null, 33);
        TableOfContentItemVO art2 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art487", "Article 487", "2de article", null, null,
                22);
        TableOfContentItemVO art3 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art488", "Article 488", "3th article", null, null,
                40);

        bodyVO.addChildItem(sec1);
        sec1.addChildItem(art1);
        bodyVO.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = xmlContentProcessor.createDocumentContentWithNewTocList(tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill><body>"
                + "<section id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "<section id=\"sect2\">"
                + "<num>Section 2</num>"
                + "<heading>Paragraphs</heading>"
                + "<article id=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article id=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th article</heading>" +
                "<alinea id=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_allArticlesRemovedFromSection() throws Exception {
        String xml = "<akomaNtoso><bill>"
                + "<body><section id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article id=\"art487\">" +
                "<num class=\"ArticleNumber\">Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "<section id=\"sect2\">"
                + "<num>Section 2</num>"
                + "<heading>Paragraphs</heading>"
                + "<article id=\"art488\">" +
                "<num class=\"ArticleNumber\">Article 488</num>" +
                "<heading>3th article</heading>" +
                "<alinea id=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(TableOfContentItemVO.Type.BODY, "body", null, null, null, null, 3);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(TableOfContentItemVO.Type.SECTION, "sect1", "Section 1", "Paragraphs", null, null, 4);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(TableOfContentItemVO.Type.SECTION, "sect2", "Section 2", "Paragraphs", null, null, 35);
        TableOfContentItemVO art2 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art487", "Article 487", "2de article",
                null, null, 22);
        TableOfContentItemVO art3 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art488", "Article 488", "3th article",
                null, null, 42);

        bodyVO.addChildItem(sec1);
        bodyVO.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = xmlContentProcessor.createDocumentContentWithNewTocList(tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill><body>"
                + "<section id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "</section>"
                + "<section id=\"sect2\">"
                + "<num>Section 2</num>"
                + "<heading>Paragraphs</heading>"
                + "<article id=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article id=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th article</heading>" +
                "<alinea id=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_sectionAdded() throws Exception {
        String xml = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso><bill><body>"
                + "<section id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article id=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(TableOfContentItemVO.Type.BODY, "body", null, null, null, null, 4);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(TableOfContentItemVO.Type.SECTION, "sect1", "Section 1", "Paragraphs", null, null, 5);
        TableOfContentItemVO art1 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art486", "Article 486", "1ste article", null, null,
                12);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(TableOfContentItemVO.Type.SECTION, null, "Section 2", null, null, null, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art487", "Article 487", "2de article",
                null, null, 23);
        TableOfContentItemVO art3 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, null, "Article 488", "3th article", null, null, null);

        bodyVO.addChildItem(sec1);
        sec1.addChildItem(art1);
        bodyVO.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = xmlContentProcessor.createDocumentContentWithNewTocList(tableOfContentItemVOList, xml.getBytes());

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso><bill><body>"
                + "<section id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article id=\"art486\">"
                +
                "<num>Article 486</num>"
                +
                "<heading>1ste article</heading>"
                +
                "<alinea id=\"art486-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "</section>"
                + "<section id=\".+\">"
                + "<num>Section 2</num>"
                + "<article id=\"art487\">"
                +
                "<num>Article 487</num>"
                +
                "<heading>2de article</heading>"
                +
                "<alinea id=\"art487-aln1\">bla bla</alinea>"
                +
                "</article>"
                +
                "            <article id=\".+\">              <num>Article 488</num>              "
                +
                "<heading>3th article</heading>              <paragraph id=\".+-par1\">                <num>1.</num>"
                +
                "                <content>                  <p>Text...</p>                </content>              </paragraph>            </article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        Pattern pattern = Pattern.compile(expected);
        Matcher matcher = pattern.matcher(new String(result));

        assertThat(expected + " should be found in " + new String(result), matcher.find(), is(true));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_sectionAddedWithHeaderAndNumberTagsPreserved() throws Exception {
        String xml = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso><bill><body>"
                + "<section id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article id=\"art486\">" +
                "<num class=\"numClass\">Article 486</num>" +
                "<heading class=\"hdgClass\">1ste article</heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article id=\"art487\">" +
                "<num class=\"ArticleNumber\">Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(TableOfContentItemVO.Type.BODY, "body", null, null, null, null, 4);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(TableOfContentItemVO.Type.SECTION, "sect1", "Section 1", "Paragraphs", 8, 10, 5);
        TableOfContentItemVO art1 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art486", "Article 486", "1ste article", 15, 19,
                12);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(TableOfContentItemVO.Type.SECTION, null, "Section 2", null, null, null, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art487", "Article 487", "2de article",
                30, 34, 27);
        TableOfContentItemVO art3 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, null, "Article 488", "3th article", null, null, null);

        bodyVO.addChildItem(sec1);
        sec1.addChildItem(art1);
        bodyVO.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = xmlContentProcessor.createDocumentContentWithNewTocList(tableOfContentItemVOList, xml.getBytes());

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso><bill><body>"
                + "<section id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article id=\"art486\">"
                +
                "<num class=\"numClass\">Article 486</num>"
                +
                "<heading class=\"hdgClass\">1ste article</heading>"
                +
                "<alinea id=\"art486-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "</section>"
                + "<section id=\".+\">"
                + "<num>Section 2</num>"
                + "<article id=\"art487\">"
                +
                "<num class=\"ArticleNumber\">Article 487</num>"
                +
                "<heading>2de article</heading>"
                +
                "<alinea id=\"art487-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "            <article id=\".+\">              <num>Article 488</num>              "
                +
                "<heading>3th article</heading>              <paragraph id=\".+-par1\">                <num>1.</num>"
                +
                "                <content>                  <p>Text...</p>                </content>              </paragraph>            </article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";
        Pattern pattern = Pattern.compile(expected);
        Matcher matcher = pattern.matcher(new String(result));

        assertThat(expected + " should be found in " + new String(result), matcher.find(), is(true));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_articleAddedAndHcontainerAtTheEnd() throws Exception {
        String xml = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"
                + "<akomaNtoso>" +
                "<bill><body>"
                + "<article id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<hcontainer><content><p>test</p></content>"
                + "</hcontainer>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(TableOfContentItemVO.Type.BODY, "body", null, null, null, null, 4);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO artNew = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, null, "Article 485", "0ste article", null, null,
                null);
        TableOfContentItemVO art1 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art486", "Article 486", "1ste article", null, null,
                5);

        bodyVO.addChildItem(artNew);
        bodyVO.addChildItem(art1);

        byte[] result = xmlContentProcessor.createDocumentContentWithNewTocList(tableOfContentItemVOList, xml.getBytes());

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso><bill><body>"

                + "            <article id=\".+\">              <num>Article 485</num>              "
                +
                "<heading>0ste article</heading>              <paragraph id=\".+-par1\">                <num>1.</num>"
                +
                "                <content>                  <p>Text...</p>                </content>              </paragraph>            </article>"
                + "<article id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<hcontainer><content><p>test</p></content>"
                + "</hcontainer>"
                + "</body></bill></akomaNtoso>";
        Pattern pattern = Pattern.compile(expected);
        Matcher matcher = pattern.matcher(new String(result));

        assertThat(expected + " should be found in " + new String(result), matcher.find(), is(true));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_sectionMoved() throws Exception {
        String xml = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso><bill><body>"
                + "<section id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "<section id=\"Section 2\">"
                + "<num>Section 2</num>"
                + "<article id=\"art487\">" +
                "<num class=\"ArticleNumber\">Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(TableOfContentItemVO.Type.BODY, "body", null, null, null, null, 4);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(TableOfContentItemVO.Type.SECTION, "sect1", "Section 1", "Paragraphs", null, null, 5);
        TableOfContentItemVO art1 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art486", "Article 486", "1ste article", null, null,
                12);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(TableOfContentItemVO.Type.SECTION, "Section 2", "Section 2", null, null, null, 23);
        TableOfContentItemVO art2 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art487", "Article 487", "2de article",
                null, null, 28);

        bodyVO.addChildItem(sec1);
        sec1.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec1.addChildItem(art1);

        byte[] result = xmlContentProcessor.createDocumentContentWithNewTocList(tableOfContentItemVOList, xml.getBytes());

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso><bill><body>"
                + "<section id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<section id=\"Section 2\">"
                + "<num>Section 2</num>"
                + "<article id=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "<article id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"

                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_sectionHasNoNumOrHeading() throws Exception {
        String xml = "<akomaNtoso><bill><body>"
                + "<section id=\"sect1\">"
                + "<article id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article id=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(TableOfContentItemVO.Type.BODY, "body", null, null, null, null, 3);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(TableOfContentItemVO.Type.SECTION, "sect1", null, null, null, null,4 );
        TableOfContentItemVO art1 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art486", "Article 486", "1ste article", null, null,
                7);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(TableOfContentItemVO.Type.SECTION, null, "Section 2", "Paragraphs", null, null, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art487", "Article 487", "2de article", null, null,
                18);
        TableOfContentItemVO art3 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, null, "Article 488", "3th article", null, null, null);

        bodyVO.addChildItem(sec1);
        sec1.addChildItem(art1);
        bodyVO.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = xmlContentProcessor.createDocumentContentWithNewTocList(tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill><body>"
                + "<section id=\"sect1\">"
                + "<article id=\"art486\">"
                +
                "<num>Article 486</num>"
                +
                "<heading>1ste article</heading>"
                +
                "<alinea id=\"art486-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "</section>"
                + "<section id=\".+\">"
                + "<num>Section 2</num>"
                + "<heading>Paragraphs</heading>"
                + "<article id=\"art487\">"
                +
                "<num>Article 487</num>"
                +
                "<heading>2de article</heading>"
                +
                "<alinea id=\"art487-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "            <article id=\".+\">              <num>Article 488</num>              "
                +
                "<heading>3th article</heading>              <paragraph id=\".+-par1\">                <num>1.</num>"
                +
                "                <content>                  <p>Text...</p>                </content>              </paragraph>            </article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";
        Pattern pattern = Pattern.compile(expected);
        Matcher matcher = pattern.matcher(new String(result));

        assertThat(expected + " should be found in " + new String(result), matcher.find(), is(true));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_sectionAddedin3levelBill() throws Exception {
        String xml = "<akomaNtoso><bill><body>"
                + "<part id=\"part1\">"
                + "<num>Part 1</num>"
                + "<heading>part1</heading>"
                + "<section id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article id=\"art487\">" +
                "<num class=\"ArticleNumber\">Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</part>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(TableOfContentItemVO.Type.BODY, "body", null, null, null, null, 3);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO part1 = new TableOfContentItemVO(TableOfContentItemVO.Type.PART, "part1", "Part 1", "part1", null, null, 4);
        TableOfContentItemVO sec1 = new TableOfContentItemVO(TableOfContentItemVO.Type.SECTION, "sect1", "Section 1", "Paragraphs", null, null, 11);
        TableOfContentItemVO art1 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art486", "Article 486", "1ste article", null, null,
                18);
        TableOfContentItemVO part2 = new TableOfContentItemVO(TableOfContentItemVO.Type.PART, null, "Part 2", "part2", null, null, null);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(TableOfContentItemVO.Type.SECTION, null, "Section 2", "Paragraphs", null, null, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, "art487", "Article 487", "2de article",
                null, null, 29);
        TableOfContentItemVO art3 = new TableOfContentItemVO(TableOfContentItemVO.Type.ARTICLE, null, "Article 488", "3th article", null, null, null);

        bodyVO.addChildItem(part1);
        part1.addChildItem(sec1);
        sec1.addChildItem(art1);
        bodyVO.addChildItem(part2);
        part2.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = xmlContentProcessor.createDocumentContentWithNewTocList(tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill><body>"
                + "<part id=\"part1\">"
                + "<num>Part 1</num>"
                + "<heading>part1</heading>"
                + "<section id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article id=\"art486\">"
                +
                "<num>Article 486</num>"
                +
                "<heading>1ste article</heading>"
                +
                "<alinea id=\"art486-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "</section>"
                + "</part>"
                + "<part id=\".+\">"
                + "<num>Part 2</num>"
                + "<heading>part2</heading>"
                + "<section id=\".+\">"
                + "<num>Section 2</num>"
                + "<heading>Paragraphs</heading>"
                + "<article id=\"art487\">"
                +
                "<num>Article 487</num>"
                +
                "<heading>2de article</heading>"
                +
                "<alinea id=\"art487-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "            <article id=\".+\">              <num>Article 488</num>              "
                +
                "<heading>3th article</heading>              <paragraph id=\".+-par1\">                <num>1.</num>"
                +
                "                <content>                  <p>Text...</p>                </content>              </paragraph>            </article>"
                + "</section>"
                + "</part>"
                + "</body></bill></akomaNtoso>";
        Pattern pattern = Pattern.compile(expected);
        Matcher matcher = pattern.matcher(new String(result));

        assertThat(expected + " should be found in " + new String(result), matcher.find(), is(true));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_when_chapterHasNoChildrenAndBodyHasHcontainer() {
        String xml = "<akomaNtoso><bill>" +
                "<body id =\"body1\">" +
                "<part id=\"part1\">" +
                "<num>Part I</num>" +
                "<heading>LEOS (Proof-Of-Concept)</heading>" +
                "<title id=\"titl1\">" +
                "<num>Title I</num>" +
                "<heading>Example Document</heading>" +
                "<chapter id=\"chap1\">" +
                "</chapter>" +
                "</title>" +
                "</part>" +
                "<hcontainer name=\"application\" id=\"app1\">" +
                "<content id=\"con\">" +
                "<p class=\"DirectApplication\">This Regulation shall be binding in its entirety and directly applicable in all Member States.</p>" +
                "<p class=\"DirectApplication\">(A possible extension of the direct application.)</p>" +
                "</content>" +
                "</hcontainer>" +
                "</body>" +
                "</bill>" +
                "</akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO body1 = new TableOfContentItemVO(TableOfContentItemVO.Type.BODY, "body1", null, null, null, null, 3);
        TableOfContentItemVO part1 = new TableOfContentItemVO(TableOfContentItemVO.Type.PART, "part1", "Part I", "LEOS (Proof-Of-Concept)", null,
                null, 6);
        TableOfContentItemVO title1 = new TableOfContentItemVO(TableOfContentItemVO.Type.TITLE, "titl1", "Title I", "Example Document", null, null,
                13);
        TableOfContentItemVO ch1 = new TableOfContentItemVO(TableOfContentItemVO.Type.CHAPTER, "chap1", null, null, null, null, 20);

        tableOfContentItemVOList.add(body1);
        body1.addChildItem(part1);
        part1.addChildItem(title1);
        title1.addChildItem(ch1);

        byte[] result = xmlContentProcessor.createDocumentContentWithNewTocList(tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill>" +
                "<body id =\"body1\">" +
                "<part id=\"part1\">" +
                "<num>Part I</num>" +
                "<heading>LEOS (Proof-Of-Concept)</heading>" +
                "<title id=\"titl1\">" +
                "<num>Title I</num>" +
                "<heading>Example Document</heading>" +
                "<chapter id=\"chap1\">" +
                "</chapter>" +
                "</title>" +
                "</part>" +
                "<hcontainer name=\"application\" id=\"app1\">" +
                "<content id=\"con\">" +
                "<p class=\"DirectApplication\">This Regulation shall be binding in its entirety and directly applicable in all Member States.</p>" +
                "<p class=\"DirectApplication\">(A possible extension of the direct application.)</p>" +
                "</content>" +
                "</hcontainer>" +
                "</body>" +
                "</bill>" +
                "</akomaNtoso>";

        assertThat(new String(result), is(expected));

    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_NoBillFound() throws Exception {
        String xml = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"
                + "<akomaNtoso>" +
                "<blabla>"
                + "<article id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<hcontainer><content><p>test</p></content>"
                + "</hcontainer>"
                + "</blabla></akomaNtoso>";

        byte[] result = xmlContentProcessor.createDocumentContentWithNewTocList(Collections.<TableOfContentItemVO>emptyList(), xml.getBytes());

        assertThat(new String(result), is(xml));
    }

    @Test
    public void test_renumberArticle() {
        String xml = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"
                + "<akomaNtoso id=\"akn\">" +
                "<article id=\"art486\">" +
                "<num id=\"aknum\">Article 486</num>" +
                "<heading id=\"aknhead\">1st article</heading>" +
                "</article>" +
                "<article id=\"art486\">" +
                "<num id=\"aknnum2\"></num>" +
                "<heading id=\"aknhead2\">2th articl<authorialNote marker=\"101\" id=\"a1\"><p id=\"p1\">TestNote1</p></authorialNote>e</heading>" +
                "</article>" +
                "<article id=\"art486\">" +
                "<heading id=\"aknhead3\">3th article<authorialNote marker=\"90\" id=\"a2\"><p id=\"p2\">TestNote2</p></authorialNote></heading>" +
                "</article>" +
                "<article id=\"art486\">" +
                "</article>" +
                "</akomaNtoso>";

        when(messageHelper.getMessage("legaltext.article.num", new Object[]{1L}, Locale.FRENCH)).thenReturn("Article 1");
        when(messageHelper.getMessage("legaltext.article.num", new Object[]{2L}, Locale.FRENCH)).thenReturn("Article 2");
        when(messageHelper.getMessage("legaltext.article.num", new Object[]{3L}, Locale.FRENCH)).thenReturn("Article 3");
        when(messageHelper.getMessage("legaltext.article.num", new Object[]{4L}, Locale.FRENCH)).thenReturn("Article 4");

        byte[] result = xmlContentProcessor.renumberArticles(xml.getBytes(), "fr");

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"
                + "<akomaNtoso id=\"akn\">" +
                "<article id=\"art486\">" +
                "<num>Article 1</num>" +
                "<heading id=\"aknhead\">1st article</heading>" +
                "</article>" +
                "<article id=\"art486\">" +
                "<num>Article 2</num>" +
                "<heading id=\"aknhead2\">2th articl<authorialNote marker=\"1\" id=\"a1\"><p id=\"p1\">TestNote1</p></authorialNote>e</heading>" +
                "</article>" +
                "<article id=\"art486\">" +
                "<num>Article 3</num>" +
                "<heading id=\"aknhead3\">3th article<authorialNote marker=\"2\" id=\"a2\"><p id=\"p2\">TestNote2</p></authorialNote></heading>" +
                "</article>" +
                "<article id=\"art486\">" +
                "<num>Article 4</num>" +
                "</article>" +
                "</akomaNtoso>";

        assertThat(new String(result).replaceAll("<num(\\s)*?id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    private byte[] getFileContent(String fileName) throws IOException {
        InputStream inputStream = this.getClass().getResource(fileName).openStream();

        byte[] content = new byte[inputStream.available()];
        inputStream.read(content);

        inputStream.close();

        return content;
    }

    @Test
    public void test_injectIdShouldNotInjectIdForSkippedNodes() {
        String xml = "<akomaNtoso>xxx</akomaNtoso>";
        byte[] result = xmlContentProcessor.injectTagIdsinXML(xml.getBytes());

        Pattern pattern = Pattern.compile("id=");
        Matcher matcher = pattern.matcher(new String(result));

        assertThat("id= " + " should not be found in " + new String(result), matcher.find(), is(false));
    }

    @Test
    public void test_injectIdShouldInjectId() {
        String xml = "<p>xxx</p>";
        byte[] result = xmlContentProcessor.injectTagIdsinXML(xml.getBytes());

        Pattern pattern = Pattern.compile("id=");
        Matcher matcher = pattern.matcher(new String(result));

        assertThat("id= " + " should be found in " + new String(result), matcher.find(), is(true));
    }

    @Test
    public void test_injectIdMethodShould_notInjectNewID() {
        String xml = "<p id=\"PQR\">xxx</p>";
        String expected = "<p id=\"PQR\">xxx</p>";
        byte[] result = xmlContentProcessor.injectTagIdsinXML(xml.getBytes());

        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_updateReferedAttributes(){
        String xml      = "<xyz>" +
                "<p id=\"PQR\" refersTo=\"~ABCD\">xxx</p>" +//shd be updated
                "<p id=\"PQR2\" refersTo=\"~ABCD1\">xxx</p>" +
                "<b><p id=\"LMN\" refersTo=\"~ABCD\">xxx</p></b>" +//shd be updated
                "<p id=\"PQR3\" refersTo=\"ABCD\">xxx</p>" +
                "<p id=\"PQR4\">xxx</p>" +
                "</xyz>";
        String expected = "<xyz>" +
                "<p id=\"PQR\" refersTo=\"~ABCD\">newValue</p>" +
                "<p id=\"PQR2\" refersTo=\"~ABCD1\">xxx</p>" +
                "<b><p id=\"LMN\" refersTo=\"~ABCD\">newValue</p></b>" +
                "<p id=\"PQR3\" refersTo=\"ABCD\">xxx</p>" +
                "<p id=\"PQR4\">xxx</p>" +
                "</xyz>";
        HashMap hm= new HashMap<String, String>();
        hm.put("ABCD", "newValue");
        byte[] result = xmlContentProcessor.updateReferedAttributes(xml.getBytes(),hm);
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_insertCommentInElement_element_found_addInStart() throws Exception{
        boolean start=true;
        String commentText = "<popup id=\"xyz\" refersTo=\"~leosComment\" leos:userId=\"user1\" leos:userName=\"User One\" leos:dateTime=\"2015-05-29T11:30:00Z\"><p id=\"pq\">This is a comment...</p></popup>";
        String xml = "<abc id=\"y\"><p id=\"PQR0\">xxx0</p><p id=\"XYZ\">xxx1</p><x id=\"y11\"><p id=\"PQR1\">xxx2</p></x><p id=\"PQR2\">xxx3</p></abc>";
        String expected = "<abc id=\"y\"><p id=\"PQR0\">xxx0</p><p id=\"XYZ\">xxx1</p><x id=\"y11\"><p id=\"PQR1\">"+commentText+"xxx2</p></x><p id=\"PQR2\">xxx3</p></abc>";
        byte[] result = xmlContentProcessor.insertCommentInElement(xml.getBytes(UTF_8),"PQR1",commentText, start);

        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_insertCommentInElement_element_found_addInEnd() throws Exception{
        boolean start=false;
        String commentText = "<popup id=\"xyz\" refersTo=\"~leosComment\" leos:userId=\"user1\" leos:userName=\"User One\" leos:dateTime=\"2015-05-29T11:30:00Z\"><p id=\"pq\">This is a comment...</p></popup>";
        String xml = "<abc id=\"y\"><p id=\"PQR0\">xxx0</p><p id=\"XYZ\">xxx1</p><x id=\"y2\"><p id=\"PQR1\">xxx2</p></x><p id=\"PQR2\">xxx3</p></abc>";
        String expected = "<abc id=\"y\"><p id=\"PQR0\">xxx0</p><p id=\"XYZ\">xxx1</p><x id=\"y2\"><p id=\"PQR1\">xxx2"+commentText+"</p></x><p id=\"PQR2\">xxx3</p></abc>";
        byte[] result = xmlContentProcessor.insertCommentInElement(xml.getBytes(UTF_8),"PQR1",commentText, start);

        assertThat(new String(result), is(expected));
    }

    @Test (expected = RuntimeException.class)
    public void test_insertCommentInElement_element_not_found_same_xml_returned() throws Exception{
        boolean start=false;
        String commentText = "<popup id=\"xyz\" refersTo=\"~leosComment\" leos:userId=\"user1\" leos:userName=\"User One\" leos:dateTime=\"2015-05-29T11:30:00Z\"><p>This is a comment...</p></popup>";
        String xml = "<abc id=\"y\"><p id=\"PQR0\">xxx0</p><p id=\"XYZ\">xxx1</p><x id=\"y1\"><p id=\"PQR1\">xxx2</p></x><p id=\"PQR2\">xxx3</p></abc>";
        byte[] result = xmlContentProcessor.insertCommentInElement(xml.getBytes(UTF_8),"XXX",commentText, start);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_insertCommentInElement_id_null() throws Exception {
        boolean start=false;
        String commentText = "<popup id=\"xyz\" refersTo=\"~leosComment\" leos:userId=\"user1\" leos:userName=\"User One\" leos:dateTime=\"2015-05-29T11:30:00Z\"><p>This is a comment...</p></popup>";
        String xml =        "<abc><p id=\"PQR0\">xxx0</p><p id=\"XYZ\">xxx1</p><x><p id=\"PQR1\">xxx2</p></x><p id=\"PQR2\">xxx3</p></abc>";
        String expected =   "<abc><p id=\"PQR0\">xxx0</p><p id=\"XYZ\">xxx1</p><x><p id=\"PQR1\">xxx2"+commentText+"</p></x><p id=\"PQR2\">xxx3</p></abc>";
        byte[] result = xmlContentProcessor.insertCommentInElement(xml.getBytes(UTF_8),null,commentText, start);

        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_insertCommentInElement_element_found_addSecondSuggestion_InStart() throws Exception {
        boolean start = true;
        String commentText = "<popup id=\"xyz\" refersTo=\"~leosSuggestion\" leos:userId=\"user1\" leos:userName=\"User One\" leos:dateTime=\"2015-05-29T11:30:00Z\"><p id=\"pq\">This is a comment...</p></popup>";
        String comment2Text = "<popup id=\"xyz2\" refersTo=\"~leosSuggestion\" leos:userId=\"user1\" leos:userName=\"User One\" leos:dateTime=\"2015-05-29T11:30:00Z\"><p id=\"pq2\">This is a comment2...</p></popup>";
        String xml = "<abc id=\"y\"><p id=\"PQR0\">xxx0</p><p id=\"XYZ\">xxx1</p><x id=\"y2\"><p id=\"PQR1\">xxx2</p></x><p id=\"PQR2\">xxx3</p></abc>";
        String expected = "<abc id=\"y\"><p id=\"PQR0\">xxx0</p><p id=\"XYZ\">xxx1</p><x id=\"y2\"><p id=\"PQR1\">" + commentText + comment2Text +
                "xxx2</p></x><p id=\"PQR2\">xxx3</p></abc>";
        byte[] result1 = xmlContentProcessor.insertCommentInElement(xml.getBytes(UTF_8), "PQR1", commentText, start);
        byte[] result2 = xmlContentProcessor.insertCommentInElement(result1, "PQR1", comment2Text, start);

        assertThat(new String(result2), is(expected));
    }

    @Test
    public void test_insertCommentInElement_element_found_addSecondCommentInEnd() throws Exception {
        boolean start = false;

        String commentText = "<popup id=\"xyz\" refersTo=\"~leosComment\" leos:userId=\"user1\" leos:userName=\"User One\" leos:dateTime=\"2015-05-29T11:30:00Z\"><p id=\"pq\">This is a comment...</p></popup>";
        String comment2Text = "<popup id=\"xyz2\" refersTo=\"~leosComment\" leos:userId=\"user1\" leos:userName=\"User One\" leos:dateTime=\"2015-05-29T11:30:00Z\"><p id=\"pq2\">This is a comment2...</p></popup>";
        String xml = "<abc id=\"y\"><p id=\"PQR0\">xxx0</p><p id=\"XYZ\">xxx1</p><x id=\"y2\"><p id=\"PQR1\">xxx2</p></x><p id=\"PQR2\">xxx3</p></abc>";
        String expected = "<abc id=\"y\"><p id=\"PQR0\">xxx0</p><p id=\"XYZ\">xxx1</p><x id=\"y2\"><p id=\"PQR1\">xxx2" + commentText + comment2Text +
                "</p></x><p id=\"PQR2\">xxx3</p></abc>";
        byte[] result1 = xmlContentProcessor.insertCommentInElement(xml.getBytes(UTF_8), "PQR1", commentText, start);
        byte[] result2 = xmlContentProcessor.insertCommentInElement(result1, "PQR1", comment2Text, start);

        assertThat(new String(result2), is(expected));
    }

    @Mock
    XmlCommentProcessor xmlCommentProcessor;

    @Test
    public void test_getAllComments_singleComment() throws ParseException {
        //setup
        CommentVO commentVOExpected = new CommentVO("xyz", "kk", "This is a comment...","User One", "user1", "DIGIT.B2.001",
                 new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-29T11:30:00Z"), RefersTo.LEOS_COMMENT);

        String xml = "<meta id=\"ElementId\"><xx id = \"kk\">" +
                "<popup id=\"xyz\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user1\"" +
                " leos:userName=\"User One\"" + " leos:dg=\"DIGIT.B2.001\"" +
                " leos:dateTime=\"2015-05-29T11:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</popup></xx>"+
                "</meta>";
        String parentTag = "<xx id = \"kk\">" +
                "<popup id=\"xyz\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user1\"" +
                " leos:userName=\"User One\"" +  " leos:dg=\"DIGIT.B2.001\"" +
                " leos:dateTime=\"2015-05-29T11:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</popup></xx>";

        when(xmlCommentProcessor.fromXML(parentTag)).thenReturn(new ArrayList<CommentVO>(Arrays.asList(commentVOExpected)));

        //make the actual call
        List<CommentVO> results = xmlContentProcessor.getAllComments(xml.getBytes(UTF_8));
        CommentVO result=results.get(0);
        //verify
        assertThat(results.size(), equalTo(1));
        assertThat(result.getId(), equalTo(commentVOExpected.getId()));
        assertThat(result.getEnclosingElementId(), equalTo(commentVOExpected.getEnclosingElementId()));
        assertThat(result.getComment(), equalTo(commentVOExpected.getComment()));
        assertThat(result.getAuthorId(), equalTo(commentVOExpected.getAuthorId()));
        assertThat(result.getTimestamp(), equalTo(commentVOExpected.getTimestamp()));
        assertThat(result.getAuthorName(), equalTo(commentVOExpected.getAuthorName()));

    }

    @Test
    public void test_getAllComments_NoComment() throws ParseException {

        // setup
        String xml = "<meta id=\"ElementId\">" +
                "<temp id=\"xyz\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user1\"" +
                " leos:userName=\"User One\"" +  " leos:dg=\"DIGIT.B2.001\"" +
                " leos:dateTime=\"2015-05-29T11:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</temp>" +
                "</meta>";


        //make the actual call
        List<CommentVO> results = xmlContentProcessor.getAllComments(xml.getBytes(UTF_8));

        // verify
        assertThat(results.size(), equalTo(0));
    }
    // care: ElementId is top element ID.Inner wrappers are not considered, So XML snippet passed will be considered one block
    @Test
    public void test_getAllComments_comment_present_deep() throws ParseException {
        // setup
        CommentVO commentVOExpected = new CommentVO("xyz", "ElementId", "This is a comment...", "User One", "user1","DIGIT.B2.001",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-29T11:30:00Z"), RefersTo.LEOS_COMMENT);

        String xml = "<meta id=\"ElementId\"><x id=\"xx\">" +
                "<popup id=\"xyz\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user1\"" +
                " leos:userName=\"User One\"" +  " leos:dg=\"DIGIT.B2.001\"" +
                " >" + // dont check date as it is auto generated
                "<p>This is a comment...</p>" +
                "</popup></x>" +
                "</meta>";
        String parentTag = "<x id=\"xx\">" +
                "<popup id=\"xyz\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user1\"" +
                " leos:userName=\"User One\"" +  " leos:dg=\"DIGIT.B2.001\"" +
                " >" + // dont check date as it is auto generated
                "<p>This is a comment...</p>" +
                "</popup></x>";
        when(xmlCommentProcessor.fromXML(parentTag)).thenReturn(new ArrayList<CommentVO>(Arrays.asList(commentVOExpected)));

        // actual test
        List<CommentVO> results = xmlContentProcessor.getAllComments(xml.getBytes(UTF_8));

        // verify
        CommentVO result = results.get(0);
        assertThat(results.size(), equalTo(1));
        assertThat(result.getId(), equalTo(commentVOExpected.getId()));
        assertThat(result.getEnclosingElementId(), equalTo(commentVOExpected.getEnclosingElementId()));// care
        assertThat(result.getComment(), equalTo(commentVOExpected.getComment()));
        assertThat(result.getAuthorId(), equalTo(commentVOExpected.getAuthorId()));
        assertThat(result.getAuthorName(), equalTo(commentVOExpected.getAuthorName()));
        assertThat(result.getDg(), equalTo(commentVOExpected.getDg()));
    }

    @Test
    public void test_getAllComments_ForMultipleComment() throws ParseException {

        // setup
        CommentVO commentVOExpected = new CommentVO("xyz", "ElementId", "This is a comment...", "User One", "user1","DIGIT.B2.001",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-29T11:30:00Z"), RefersTo.LEOS_COMMENT);
        // setup
        CommentVO commentVOExpected2 = new CommentVO("xyz2", "ElementId", "This is a comment...2", "User One2", "user2","TRADE.A1",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-30T11:30:00Z"), RefersTo.LEOS_COMMENT);
        // setup
        CommentVO commentVOExpected3 = new CommentVO("xyz3", "ElementId", "This is a comment...3", "User One3", "user3","DIGIT.B2.002",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-06-29T11:30:00Z"), RefersTo.LEOS_COMMENT);


        String xml = "<meta id=\"ElementId\">" +
                "<popup id=\"xyz\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user1\"" +
                " leos:userName=\"User One\"" +  " leos:dg=\"DIGIT.B2.001\""  +
                " leos:dateTime=\"2015-05-29T11:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</popup>" +
                "<popup id=\"xyz2\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user2\"" +
                " leos:userName=\"User One2\"" +  " leos:dg=\"TRADE.A1\""  +
                " leos:dateTime=\"2015-05-30T11:30:00Z\">" +
                "<p>This is a comment...2</p>" +
                "</popup>" +
                "<popup id=\"xyz3\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user3\"" +
                " leos:userName=\"User One3\"" +  " leos:dg=\"DIGIT.B2.002\""  +
                " leos:dateTime=\"2015-06-29T11:30:00Z\">" +
                "<p>This is a comment...3</p>" +
                "</popup>" +
                "</meta>";

        when(xmlCommentProcessor.fromXML(xml)).thenReturn(new ArrayList<CommentVO>(Arrays.asList(commentVOExpected,commentVOExpected2,commentVOExpected3)));
        // actual test
        List<CommentVO> results = xmlContentProcessor.getAllComments(xml.getBytes(UTF_8));

        // verify

        assertThat(results.size(), equalTo(3));
        CommentVO result = results.get(0);
        assertThat(result.getId(), equalTo(commentVOExpected.getId()));
        assertThat(result.getEnclosingElementId(), equalTo(commentVOExpected.getEnclosingElementId()));
        assertThat(result.getComment(), equalTo(commentVOExpected.getComment()));
        assertThat(result.getAuthorId(), equalTo(commentVOExpected.getAuthorId()));
        assertThat(result.getTimestamp(), equalTo(commentVOExpected.getTimestamp()));
        assertThat(result.getAuthorName(), equalTo(commentVOExpected.getAuthorName()));
        result = results.get(1);
        assertThat(result.getId(), equalTo(commentVOExpected2.getId()));
        assertThat(result.getEnclosingElementId(), equalTo(commentVOExpected2.getEnclosingElementId()));
        assertThat(result.getComment(), equalTo(commentVOExpected2.getComment()));
        assertThat(result.getAuthorId(), equalTo(commentVOExpected2.getAuthorId()));
        assertThat(result.getTimestamp(), equalTo(commentVOExpected2.getTimestamp()));
        assertThat(result.getAuthorName(), equalTo(commentVOExpected2.getAuthorName()));
        result = results.get(2);
        assertThat(result.getId(), equalTo(commentVOExpected3.getId()));
        assertThat(result.getEnclosingElementId(), equalTo(commentVOExpected3.getEnclosingElementId()));
        assertThat(result.getComment(), equalTo(commentVOExpected3.getComment()));
        assertThat(result.getAuthorId(), equalTo(commentVOExpected3.getAuthorId()));
        assertThat(result.getTimestamp(), equalTo(commentVOExpected3.getTimestamp()));
        assertThat(result.getAuthorName(), equalTo(commentVOExpected3.getAuthorName()));
    }

    @Test
    public void test_getAllComments_MultipleComment_diffLevel() throws ParseException {

        // setup
        CommentVO commentVOExpected = new CommentVO("xyz", "ElementId", "This is a comment...", "User One", "user1",null,
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-29T16:30:00Z"), RefersTo.LEOS_COMMENT);
        CommentVO commentVOExpected2 = new CommentVO("xyz2", "ElementId", "This is a comment...2", "User One2", "user2","TRADE.A1",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-30T11:30:00Z"), RefersTo.LEOS_COMMENT);
        CommentVO commentVOExpected21 = new CommentVO("xyz2", "ElementId", "This is a comment...2", "User One2", "user2","TRADE.A2",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-30T11:30:00Z"), RefersTo.LEOS_COMMENT);
        CommentVO commentVOExpected3 = new CommentVO("xyz3", "ElementId", "This is a comment...3", "User One3", "user3","TRADE.A3",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-06-29T11:30:00Z"), RefersTo.LEOS_COMMENT);


        String xml = "<ak><meta id=\"ElementId\">" +
                "<popup id=\"xyz\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user1\"" +
                " leos:userName=\"User One\"" +
                " leos:dateTime=\"2015-05-29T16:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</popup></meta>" +
                "<level2 id=\"level2\"><popup id=\"xyz2\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user2\"" +
                " leos:userName=\"User One2\"" + " leos:dg=\"TRADE.A1\"" +
                " leos:dateTime=\"2015-05-30T11:30:00Z\">" +
                "<p>This is a comment...2</p>" +
                "</popup><popup id=\"xyz21\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user21\"" +
                " leos:userName=\"User One21\"" + " leos:dg=\"TRADE.A2\"" +
                " leos:dateTime=\"2015-05-30T11:30:00Z\">" +
                "<p>This is a comment...21</p>" +
                "</popup></level2>" +
                "<level3><level4 id=\"level4\"><popup id=\"xyz3\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user3\"" +
                " leos:userName=\"User One3\"" + " leos:dg=\"TRADE.A3\"" +
                " leos:dateTime=\"2015-06-29T11:30:00Z\">" +
                "<p>This is a comment...3</p>" +
                "</popup></level4></level3></ak>";

        String parent1="<meta id=\"ElementId\">" +
                "<popup id=\"xyz\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user1\"" +
                " leos:userName=\"User One\"" +
                " leos:dateTime=\"2015-05-29T16:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</popup></meta>";
        String parent2 = "<level2 id=\"level2\">" +
                "<popup id=\"xyz2\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user2\"" +
                " leos:userName=\"User One2\"" + " leos:dg=\"TRADE.A1\"" +
                " leos:dateTime=\"2015-05-30T11:30:00Z\">" +
                "<p>This is a comment...2</p>" +
                "</popup><popup id=\"xyz21\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user21\"" +
                " leos:userName=\"User One21\"" +" leos:dg=\"TRADE.A2\"" +
                " leos:dateTime=\"2015-05-30T11:30:00Z\">" +
                "<p>This is a comment...21</p>" +
                "</popup></level2>";
        String parent3="<level4 id=\"level4\"><popup id=\"xyz3\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user3\"" +
                " leos:userName=\"User One3\"" +" leos:dg=\"TRADE.A3\"" +
                " leos:dateTime=\"2015-06-29T11:30:00Z\">" +
                "<p>This is a comment...3</p>" +
                "</popup></level4>";

        when(xmlCommentProcessor.fromXML(parent1)).thenReturn(new ArrayList<CommentVO>(Arrays.asList(commentVOExpected)));
        when(xmlCommentProcessor.fromXML(parent2)).thenReturn(new ArrayList<CommentVO>(Arrays.asList(commentVOExpected2, commentVOExpected21)));
        when(xmlCommentProcessor.fromXML(parent3)).thenReturn(new ArrayList<CommentVO>(Arrays.asList(commentVOExpected3)));


        // actual test
        List<CommentVO> results = xmlContentProcessor.getAllComments(xml.getBytes(UTF_8));

        // verify

        assertThat(results.size(), equalTo(4));
        CommentVO result = results.get(0);
        assertThat(result.getId(), equalTo(commentVOExpected.getId()));
        assertThat(result.getEnclosingElementId(), equalTo(commentVOExpected.getEnclosingElementId()));
        assertThat(result.getComment(), equalTo(commentVOExpected.getComment()));
        assertThat(result.getAuthorId(), equalTo(commentVOExpected.getAuthorId()));
        assertThat(result.getTimestamp(), equalTo(commentVOExpected.getTimestamp()));
        assertThat(result.getAuthorName(), equalTo(commentVOExpected.getAuthorName()));
        assertThat(result.getDg(), equalTo(commentVOExpected.getDg()));

        result = results.get(1);
        assertThat(result.getId(), equalTo(commentVOExpected2.getId()));
        assertThat(result.getEnclosingElementId(), equalTo(commentVOExpected2.getEnclosingElementId()));
        assertThat(result.getComment(), equalTo(commentVOExpected2.getComment()));
        assertThat(result.getAuthorId(), equalTo(commentVOExpected2.getAuthorId()));
        assertThat(result.getTimestamp(), equalTo(commentVOExpected2.getTimestamp()));
        assertThat(result.getAuthorName(), equalTo(commentVOExpected2.getAuthorName()));
        assertThat(result.getDg(), equalTo(commentVOExpected2.getDg()));

        result = results.get(2);
        assertThat(result.getId(), equalTo(commentVOExpected21.getId()));
        assertThat(result.getEnclosingElementId(), equalTo(commentVOExpected21.getEnclosingElementId()));
        assertThat(result.getComment(), equalTo(commentVOExpected21.getComment()));
        assertThat(result.getAuthorId(), equalTo(commentVOExpected21.getAuthorId()));
        assertThat(result.getTimestamp(), equalTo(commentVOExpected21.getTimestamp()));
        assertThat(result.getAuthorName(), equalTo(commentVOExpected21.getAuthorName()));
        result = results.get(3);
        assertThat(result.getId(), equalTo(commentVOExpected3.getId()));
        assertThat(result.getEnclosingElementId(), equalTo(commentVOExpected3.getEnclosingElementId()));
        assertThat(result.getComment(), equalTo(commentVOExpected3.getComment()));
        assertThat(result.getAuthorId(), equalTo(commentVOExpected3.getAuthorId()));
        assertThat(result.getTimestamp(), equalTo(commentVOExpected3.getTimestamp()));
        assertThat(result.getAuthorName(), equalTo(commentVOExpected3.getAuthorName()));
        assertThat(result.getDg(), equalTo(commentVOExpected3.getDg()));
    }

    @Test
    public void test_removeElements_one() throws ParseException {

        // setup
        String xml = "<meta id=\"ElementId\">" +
                "<temp id=\"xyz\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user1\"" +
                " leos:userName=\"User One\"" +  " leos:dg=\"DIGIT.B2.001\"" +
                " leos:dateTime=\"2015-05-29T11:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</temp>" +
                "</meta>";


        //make the actual call
        byte[] result = xmlContentProcessor.removeElements(xml.getBytes(UTF_8), "//*[@refersTo=\"~leosComment\"]");

        // verify
        assertThat(new String(result,UTF_8) , equalTo("<meta id=\"ElementId\"></meta>"));
    }

    public void test_removeElements_multiple() throws ParseException {

        // setup
        String xml = "<meta id=\"ElementId\">" +
                "<temp id=\"xyz\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user1\"" +
                " leos:userName=\"User One\"" +  " leos:dg=\"DIGIT.B2.001\"" +
                " leos:dateTime=\"2015-05-29T11:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</temp>" +
                "<temp id=\"xyz1\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user1\"" +
                " leos:userName=\"User One\"" +  " leos:dg=\"DIGIT.B2.001\"" +
                " leos:dateTime=\"2015-05-29T11:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</temp>"+
                "</meta>";


        //make the actual call
        byte[] result = xmlContentProcessor.removeElements(xml.getBytes(UTF_8), "//*[@refersTo=\"~leosComment\"]");

        // verify
        assertThat(new String(result,UTF_8) , equalTo("<meta id=\"ElementId\"></meta>"));
    }
}
