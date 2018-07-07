/*
 * Copyright 2017 European Commission
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
package eu.europa.ec.leos.services.support.xml;

import eu.europa.ec.leos.test.support.LeosTest;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.LegalTextTocItemType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.MessageSource;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
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
    private VtdXmlContentProcessor vtdXmlContentProcessor = new VtdXmlContentProcessor();

    private byte[] docContent;

    @Before
    public void setup() {
        super.setup();
        String doc = "<part GUID=\"part11\">"
                +
                "                <num GUID=\"n1\" class=\"PartNumber\">Part XI</num>"
                +
                "                <heading GUID=\"h1\" class=\"PartHeading\">FINAL PROVISIONS</heading>"
                +
                "                <article GUID=\"art485\">"
                +
                "                    <num GUID=\"n1\" class=\"ArticleNumber\">Article 485</num>"
                +
                "                    <paragraph GUID=\"art485-par1\">"
                +
                "                        <num  GUID=\"n2\" class=\"Paragraph(numbered)\">1.</num>"
                +
                "                        <content GUID=\"c1\" >"
                +
                "                            <p  GUID=\"p1\" class=\"Paragraph(numbered)\">Subject to paragraph 2, this Regulation<authorialNote marker=\"2\" GUID=\"a1\"><p GUID=\"ptest1\">TestNote1</p></authorialNote> shall apply from 1 January 2013.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                    <paragraph GUID=\"art485-par2\">"
                +
                "                        <num  GUID=\"n2\" class=\"Paragraph(numbered)\">2.</num>"
                +
                "                        <content GUID=\"con\">"
                +
                "                            <p  GUID=\"p2\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015<authorialNote marker=\"4\" GUID=\"a2\"><p GUID=\"ptest2\">TestNote2</p></authorialNote>.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                </article>"
                +
                "                <article GUID=\"art486\">"
                +
                "                    <num  GUID=\"n3\" class=\"ArticleNumber\">Article 486</num>"
                +
                "                    <alinea GUID=\"art486-aln1\">"
                +
                "                        <content GUID=\"c3\">"
                +
                "                            <p GUID=\"p3\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i GUID=\"i2\">Official Journal of the European<authorialNote marker=\"8\" GUID=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
                +
                "                        </content>" +
                "                    </alinea>" +
                "                </article>" +
                "            </part>";

        docContent = doc.getBytes(UTF_8);
    }


    @Test
    public void test_getAncestorsIdsForElementId_should_returnEmptyArray_when_rootElementIdPassed() {
        List<String> ids = vtdXmlContentProcessor.getAncestorsIdsForElementId(
                docContent, "part11");
        assertThat(ids, is(Collections.EMPTY_LIST));
    }

    @Test
    public void test_getAncestorsIdsForElementId_should_returnArrayWithAllAncestorsIds_when_nestedElementPassed() {
        List<String> ids = vtdXmlContentProcessor.getAncestorsIdsForElementId(
                docContent, "p2");
        assertThat(ids,
                is(Arrays.asList("part11", "art485", "art485-par2", "con")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getAncestorsIdsForElementId_should_throwException_when_nonExistedElementPassed()
            throws Exception {
        vtdXmlContentProcessor.getAncestorsIdsForElementId(docContent,
                "notExisted");
    }


    @Test
    public void test_getTagContentByNameAndId_should_returnTagContent_when_tagAndIdFound() throws Exception {
        String tagContent = vtdXmlContentProcessor.getElementByNameAndId(docContent, "alinea", "art486-aln1");
        assertThat(
                tagContent,
                is("<alinea GUID=\"art486-aln1\">"
                +
                "                        <content GUID=\"c3\">"
                +
                "                            <p GUID=\"p3\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i GUID=\"i2\">Official Journal of the European<authorialNote marker=\"8\" GUID=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
                +
                "                        </content>" +
                "                    </alinea>"));
    }

    @Test
    public void test_getTagContentByNameAndId_should_returnNull_when_tagAndIdNotFound() throws Exception {
        String tagContent = vtdXmlContentProcessor.getElementByNameAndId(docContent, "alinea", "art486-aln1123456789");
        assertThat(tagContent, is(nullValue()));
    }

    @Test
    public void test_getTagContentByNameAndId_should_returnFirstTag_when_IdNull() throws Exception {
        String tagContent = vtdXmlContentProcessor.getElementByNameAndId(docContent, "alinea", null);
        assertThat(
                tagContent,
                is("<alinea GUID=\"art486-aln1\">"
                +
                "                        <content GUID=\"c3\">"
                +
                "                            <p GUID=\"p3\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i GUID=\"i2\">Official Journal of the European<authorialNote marker=\"8\" GUID=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
                +
                "                        </content>" +
                "                    </alinea>"));
    }

    @Test(expected = RuntimeException.class)
    public void test_getTagContentByNameAndId_should_throwRuntimeException_when_illegalXmlFormat() throws Exception {
        String xml = " <article GUID=\"art486\">" +
                "                    <num class=\"ArticleNumber\">Article 486</num>";
        String tagContent = vtdXmlContentProcessor.getElementByNameAndId(xml.getBytes(UTF_8), "alinea", "art486-aln1");
        assertThat(tagContent, is(nullValue()));
    }

    @Test
    public void test_replaceElementByTagNameAndId_should_match_returnedTagContent() throws Exception {

        String newContent = "<article GUID=\"art486\">"
                +
                "                    <num GUID=\"num1\" class=\"ArticleNumber\">Article 486</num>"
                +
                "                    <alinea GUID=\"art486-aln1\">"
                +
                "                        <content GUID=\"c\">"
                +
                "                            <p GUID=\"p\" class=\"Paragraph(unnumbered)\">This text should appear in the main document after merge<authorialNote GUID=\"a4\" marker=\"1\"><p GUID=\"p1\">TestNoteX</p></authorialNote> with the updated Article <i GUID=\"i1\">Official Journal of the European Union</i>.</p>"
                +
                "                        </content>" +
                "                    </alinea>" +
                "                </article>";

        // DO THE ACTUAL CALL
        byte[] returnedElement = vtdXmlContentProcessor.replaceElementByTagNameAndId(docContent, newContent, "article", "art486");

        String expected = "<part GUID=\"part11\">"
                +
                "                <num GUID=\"n1\" class=\"PartNumber\">Part XI</num>"
                +
                "                <heading GUID=\"h1\" class=\"PartHeading\">FINAL PROVISIONS</heading>"
                +
                "                <article GUID=\"art485\">"
                +
                "                    <num GUID=\"n1\" class=\"ArticleNumber\">Article 485</num>"
                +
                "                    <paragraph GUID=\"art485-par1\">"
                +
                "                        <num  GUID=\"n2\" class=\"Paragraph(numbered)\">1.</num>"
                +
                "                        <content GUID=\"c1\" >"
                +
                "                            <p  GUID=\"p1\" class=\"Paragraph(numbered)\">Subject to paragraph 2, this Regulation<authorialNote marker=\"1\" GUID=\"a1\"><p GUID=\"ptest1\">TestNote1</p></authorialNote> shall apply from 1 January 2013.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                    <paragraph GUID=\"art485-par2\">"
                +
                "                        <num  GUID=\"n2\" class=\"Paragraph(numbered)\">2.</num>"
                +
                "                        <content GUID=\"con\">"
                +
                "                            <p  GUID=\"p2\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015<authorialNote marker=\"2\" GUID=\"a2\"><p GUID=\"ptest2\">TestNote2</p></authorialNote>.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                </article>"
                +
                "                <article GUID=\"art486\">"
                +
                "                    <num GUID=\"num1\" class=\"ArticleNumber\">Article 486</num>"
                +
                "                    <alinea GUID=\"art486-aln1\">"
                +
                "                        <content GUID=\"c\">"
                +
                "                            <p GUID=\"p\" class=\"Paragraph(unnumbered)\">This text should appear in the main document after merge<authorialNote GUID=\"a4\" marker=\"3\"><p GUID=\"p1\">TestNoteX</p></authorialNote> with the updated Article <i GUID=\"i1\">Official Journal of the European Union</i>.</p>"
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
        byte[] result = vtdXmlContentProcessor.replaceElementsWithTagName(xml.getBytes(UTF_8), "proprietary", "<proprietary>new</proprietary>");
        String expected = "<bill><meta><proprietary>new</proprietary></meta></bill>";
        assertThat(new String(result), is(expected));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_replaceElementByTagName_ShouldReturnUnchanged_WhenTagNotFound() throws Exception {

        String xml = "<bill><meta><proprietary>test</proprietary></meta></bill>";
        vtdXmlContentProcessor.replaceElementsWithTagName(xml.getBytes(UTF_8), "proprietary2", "<proprietary>new</proprietary>");
    }

    @Test
    public void test_deleteElementByTagNameAndId_should_match_returnedTagContent() throws Exception {

        // DO THE ACTUAL CALL
        byte[] returnedElement = vtdXmlContentProcessor.deleteElementByTagNameAndId(docContent, "article", "art486");
        String expected = "<part GUID=\"part11\">"
                +
                "                <num GUID=\"n1\" class=\"PartNumber\">Part XI</num>"
                +
                "                <heading GUID=\"h1\" class=\"PartHeading\">FINAL PROVISIONS</heading>"
                +
                "                <article GUID=\"art485\">"
                +
                "                    <num GUID=\"n1\" class=\"ArticleNumber\">Article 485</num>"
                +
                "                    <paragraph GUID=\"art485-par1\">"
                +
                "                        <num  GUID=\"n2\" class=\"Paragraph(numbered)\">1.</num>"
                +
                "                        <content GUID=\"c1\" >"
                +
                "                            <p  GUID=\"p1\" class=\"Paragraph(numbered)\">Subject to paragraph 2, this Regulation<authorialNote marker=\"2\" GUID=\"a1\"><p GUID=\"ptest1\">TestNote1</p></authorialNote> shall apply from 1 January 2013.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                    <paragraph GUID=\"art485-par2\">"
                +
                "                        <num  GUID=\"n2\" class=\"Paragraph(numbered)\">2.</num>"
                +
                "                        <content GUID=\"con\">"
                +
                "                            <p  GUID=\"p2\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015<authorialNote marker=\"4\" GUID=\"a2\"><p GUID=\"ptest2\">TestNote2</p></authorialNote>.</p>"
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

        String template = "                <article GUID=\"art435\">" +
                "              <num GUID=\"n4\">Article #</num>" +
                "              <heading GUID=\"h4\">Article heading...</heading>" +
                "              <paragraph GUID=\"art1-par1\">" +
                "                <num GUID=\"n4p\">1.</num>" +
                "                <content GUID=\"c4\">" +
                "                  <p GUID=\"p4\">Text.<authorialNote marker=\"1\" GUID=\"a1\"><p>TestNote4</p></authorialNote>..</p>" +
                "                </content>" +
                "              </paragraph>" +
                "             </article>";

        // DO THE ACTUAL CALL - Insert After
        byte[] returnedElement = vtdXmlContentProcessor.insertElementByTagNameAndId(docContent, template, "article", "art486", false);

        String expected ="<part GUID=\"part11\">"
                +
                "                <num GUID=\"n1\" class=\"PartNumber\">Part XI</num>"
                +
                "                <heading GUID=\"h1\" class=\"PartHeading\">FINAL PROVISIONS</heading>"
                +
                "                <article GUID=\"art485\">"
                +
                "                    <num GUID=\"n1\" class=\"ArticleNumber\">Article 485</num>"
                +
                "                    <paragraph GUID=\"art485-par1\">"
                +
                "                        <num  GUID=\"n2\" class=\"Paragraph(numbered)\">1.</num>"
                +
                "                        <content GUID=\"c1\" >"
                +
                "                            <p  GUID=\"p1\" class=\"Paragraph(numbered)\">Subject to paragraph 2, this Regulation<authorialNote marker=\"2\" GUID=\"a1\"><p GUID=\"ptest1\">TestNote1</p></authorialNote> shall apply from 1 January 2013.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                    <paragraph GUID=\"art485-par2\">"
                +
                "                        <num  GUID=\"n2\" class=\"Paragraph(numbered)\">2.</num>"
                +
                "                        <content GUID=\"con\">"
                +
                "                            <p  GUID=\"p2\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015<authorialNote marker=\"4\" GUID=\"a2\"><p GUID=\"ptest2\">TestNote2</p></authorialNote>.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                </article>"
                +
                "                <article GUID=\"art486\">"
                +
                "                    <num  GUID=\"n3\" class=\"ArticleNumber\">Article 486</num>"
                +
                "                    <alinea GUID=\"art486-aln1\">"
                +
                "                        <content GUID=\"c3\">"
                +
                "                            <p GUID=\"p3\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i GUID=\"i2\">Official Journal of the European<authorialNote marker=\"8\" GUID=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
                +
                "                        </content>" +
                "                    </alinea>" +
                "                </article>" +
                "                <article GUID=\"art435\">" +
                "              <num GUID=\"n4\">Article #</num>" +
                "              <heading GUID=\"h4\">Article heading...</heading>" +
                "              <paragraph GUID=\"art1-par1\">" +
                "                <num GUID=\"n4p\">1.</num>" +
                "                <content GUID=\"c4\">" +
                "                  <p GUID=\"p4\">Text.<authorialNote marker=\"1\" GUID=\"a1\"><p>TestNote4</p></authorialNote>..</p>" +
                "                </content>" +
                "              </paragraph>" +
                "             </article>"+
                "            </part>";;

        assertThat(returnedElement, is(expected.getBytes(UTF_8)));

    }

    @Test
    public void test_insertElementByTagNameAndId_when_insert_before() throws Exception {

        String template = "                <article GUID=\"art435\">" +
                "              <num GUID=\"n4\">Article #</num>" +
                "              <heading GUID=\"h4\">Article heading...</heading>" +
                "              <paragraph GUID=\"art1-par1\">" +
                "                <num GUID=\"n4p\">1.</num>" +
                "                <content GUID=\"c4\">" +
                "                  <p GUID=\"p4\">Text.<authorialNote marker=\"1\" GUID=\"a1\"><p>TestNote4</p></authorialNote>..</p>" +
                "                </content>" +
                "              </paragraph>" +
                "             </article>";

        // DO THE ACTUAL CALL - Insert Before
        byte[] returnedElement = vtdXmlContentProcessor.insertElementByTagNameAndId(docContent, template, "article", "art486", true);

        String expected = "<part GUID=\"part11\">"
                +
                "                <num GUID=\"n1\" class=\"PartNumber\">Part XI</num>"
                +
                "                <heading GUID=\"h1\" class=\"PartHeading\">FINAL PROVISIONS</heading>"
                +
                "                <article GUID=\"art485\">"
                +
                "                    <num GUID=\"n1\" class=\"ArticleNumber\">Article 485</num>"
                +
                "                    <paragraph GUID=\"art485-par1\">"
                +
                "                        <num  GUID=\"n2\" class=\"Paragraph(numbered)\">1.</num>"
                +
                "                        <content GUID=\"c1\" >"
                +
                "                            <p  GUID=\"p1\" class=\"Paragraph(numbered)\">Subject to paragraph 2, this Regulation<authorialNote marker=\"2\" GUID=\"a1\"><p GUID=\"ptest1\">TestNote1</p></authorialNote> shall apply from 1 January 2013.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                    <paragraph GUID=\"art485-par2\">"
                +
                "                        <num  GUID=\"n2\" class=\"Paragraph(numbered)\">2.</num>"
                +
                "                        <content GUID=\"con\">"
                +
                "                            <p  GUID=\"p2\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015<authorialNote marker=\"4\" GUID=\"a2\"><p GUID=\"ptest2\">TestNote2</p></authorialNote>.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                </article>"
                +
                "                                <article GUID=\"art435\">" +
                "              <num GUID=\"n4\">Article #</num>" +
                "              <heading GUID=\"h4\">Article heading...</heading>" +
                "              <paragraph GUID=\"art1-par1\">" +
                "                <num GUID=\"n4p\">1.</num>" +
                "                <content GUID=\"c4\">" +
                "                  <p GUID=\"p4\">Text.<authorialNote marker=\"1\" GUID=\"a1\"><p>TestNote4</p></authorialNote>..</p>" +
                "                </content>" +
                "              </paragraph>" +
                "             </article>" +
                "<article GUID=\"art486\">"
                +
                "                    <num  GUID=\"n3\" class=\"ArticleNumber\">Article 486</num>"
                +
                "                    <alinea GUID=\"art486-aln1\">"
                +
                "                        <content GUID=\"c3\">"
                +
                "                            <p GUID=\"p3\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i GUID=\"i2\">Official Journal of the European<authorialNote marker=\"8\" GUID=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
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
        List<TableOfContentItemVO> tableOfContentItemVOList = vtdXmlContentProcessor.buildTableOfContent("bill", LegalTextTocItemType::getTocItemTypeFromName, docContent);
        assertThat(tableOfContentItemVOList, is(notNullValue()));
        assertThat(tableOfContentItemVOList.size(), is(0));
    }

    @Test
    public void test_buildTableOfContentItemVOList_should_returnReturnCorrectContent_when_expectedFormat() throws Exception {

        byte[] fileContent = getFileContent("/akn_toc-test.xml");
        List<TableOfContentItemVO> tableOfContentItemVOList = vtdXmlContentProcessor.buildTableOfContent("bill", LegalTextTocItemType::getTocItemTypeFromName, fileContent);
        assertThat(tableOfContentItemVOList, is(notNullValue()));
        assertThat(tableOfContentItemVOList.size(), is(4));

        // do a huge number of asserts..
        assertThat(tableOfContentItemVOList.get(0), is(new TableOfContentItemVO(LegalTextTocItemType.PREFACE, null, null,
                "on [...] preface and half bold text", null, 7, 6)));
        assertThat(tableOfContentItemVOList.get(1), is(new TableOfContentItemVO(LegalTextTocItemType.PREAMBLE, null, null, null,
                null, null, 15)));

        // build the body as second expected root item
        TableOfContentItemVO body = new TableOfContentItemVO(LegalTextTocItemType.BODY, null, null, null, null, null, 18);
        TableOfContentItemVO part1 = new TableOfContentItemVO(LegalTextTocItemType.PART, "part1", null, "part-head", null, 22, 19);
        body.addChildItem(part1);
        TableOfContentItemVO title = new TableOfContentItemVO(LegalTextTocItemType.TITLE, "title1", "on [...] preface and half bold text", "title-head",
                29, 27, 24);
        part1.addChildItem(title);
        TableOfContentItemVO chapter = new TableOfContentItemVO(LegalTextTocItemType.CHAPTER, "chap1", "chapter-num", "chapter-head", 40, 42, 37);
        title.addChildItem(chapter);
        TableOfContentItemVO section = new TableOfContentItemVO(LegalTextTocItemType.SECTION, "sect1", "section-num", "section-head", 47, 49, 44);
        chapter.addChildItem(section);
        TableOfContentItemVO art = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art2", "Title I", "title", 70, 72, 66);
        chapter.addChildItem(art);

        TableOfContentItemVO subsection1 = new TableOfContentItemVO(LegalTextTocItemType.SUBSECTION, "subsec1", "Subsection 1", "subsection-head",
                54, 56, 51);
        TableOfContentItemVO subsection2 = new TableOfContentItemVO(LegalTextTocItemType.SUBSECTION, "subsec2", null, null, null, null, 62);
        section.addChildItem(subsection1);
        section.addChildItem(subsection2);
        TableOfContentItemVO artInSubs = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art1", null, null, null, null, 58);
        subsection1.addChildItem(artInSubs);

        assertThat(tableOfContentItemVOList.get(2), is(body));
        assertThat(tableOfContentItemVOList.get(3), is(new TableOfContentItemVO(LegalTextTocItemType.CONCLUSIONS, null, null, null, null, null, 78)));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_boldTagInHeading() throws Exception {
        String xml = "<akomaNtoso><bill><body>    "
                + "<article GUID=\"art486\">" +
                "<num>  Article 486</num>" +
                "   <heading><content><p>1ste article</p></content></heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article GUID=\"art487\">" +
                "<num><content><p>Article 487</p></content></num>" +
                "<heading><content><p>2de article</p></content></heading>" +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article GUID=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th <b>test</b> article</heading>" +
                "<alinea GUID=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art486", "Article 486",
                "<content><p>1ste article</p></content>", null, null, 4);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art488", "Article 488 moved", "3th article became 2the",
                null, null, 32);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art487", "<content><p>Article 487 moved</p></content>",
                "<content><p>2de article became 3the</p></content>", null, null, 17);

        articleVOs.add(art1);
        articleVOs.add(art3);
        articleVOs.add(art2);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextTocItemType.BODY, "body", null, null, null, null, 3);
        bodyVO.addAllChildItems(articleVOs);

        tableOfContentItemVOList.add(bodyVO);


        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName,tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill><body>"
                + "<article GUID=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading><content><p>1ste article</p></content></heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article GUID=\"art488\">" +
                "<num>Article 488 moved</num>" +
                "<heading>3th article became 2the</heading>" +
                "<alinea GUID=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article GUID=\"art487\">" +
                "<num><content><p>Article 487 moved</p></content></num>" +
                "<heading><content><p>2de article became 3the</p></content></heading>" +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_oldContainedutf8() {
        String xml = "<akomaNtoso><bill><body>"
                + "<article GUID=\"art486\">" +
                "<num>Article 486 <placeholder>[…]</placeholder></num>" +
                "<heading><content><p>1ste article</p></content></heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";
        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art486", "Article 486 <placeholder>[…]</placeholder>",
                "<content><p>1ste article</p></content>", null, null, 4);

        articleVOs.add(art1);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextTocItemType.BODY, "body", null, null, null, null, 3);
        bodyVO.addAllChildItems(articleVOs);
        tableOfContentItemVOList.add(bodyVO);


        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes(UTF_8));

        assertThat(new String(result, UTF_8), is(xml));

    }

    // amounts and L &lt; K
    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_oldContainedEscapedXML() {
        String xml = "<akomaNtoso><bill><body>"
                + "<article GUID=\"art486\">" +
                "<num>Article 486 <placeholder>[…]</placeholder></num>" +
                "<heading><content><p>1ste article</p></content></heading>" +
                "<alinea GUID=\"art486-aln1\">bla amounts and L &lt; K bla</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();

        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art486", "Article 486 <placeholder>[…]</placeholder>",
                "<content><p>1ste article</p></content>", null, null, 4);

        articleVOs.add(art1);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextTocItemType.BODY, "body", null, null, null, null, 3);
        bodyVO.addAllChildItems(articleVOs);
        tableOfContentItemVOList.add(bodyVO);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName ,tableOfContentItemVOList, xml.getBytes(UTF_8));

        assertThat(new String(result, UTF_8), is(xml));

    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_2newAreAddedAtSameOffset() throws Exception {
        String xml = "<akomaNtoso><bill><body>"
                + "<article GUID=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading><content><p>1ste article</p></content></heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
                "</article>"

                + "<article GUID=\"art489\">" +
                "<num>Article 489</num>" +
                "<heading>4th article</heading>" +
                "<alinea GUID=\"art488-aln1\">bla bla 4</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art486", "Article 486", "1ste article", null, null,
                4);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, null, "Article 487 added", "2de article added", null, null,
                null);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, null, "Article 488 added", "3th article added", null, null,
                null);
        TableOfContentItemVO art4 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art489", "Article 489", "4th article", null, null,
                17);

        articleVOs.add(art1);
        articleVOs.add(art2);
        articleVOs.add(art3);
        articleVOs.add(art4);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextTocItemType.BODY, "body", null, null, null, null, 3);
        bodyVO.addAllChildItems(articleVOs);
        tableOfContentItemVOList.add(bodyVO);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName ,tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill><body>"
                + "<article GUID=\"art486\">"
                +
                "<num>Article 486</num>"
                +
                "<heading>1ste article</heading>"
                +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "            <article GUID=\".+\" leos:editable=\"true\"  leos:deletable=\"true\">              <num>Article 487 added</num>              "
                +
                "<heading>2de article added</heading>              <paragraph GUID=\".+-par1\">                <num>1.</num>"
                +
                "                <content>                  <p>Text...</p>                </content>              </paragraph>            </article>"
                + "            <article GUID=\".+\" leos:editable=\"true\"  leos:deletable=\"true\">              <num>Article 488 added</num>              "
                +
                "<heading>3th article added</heading>              <paragraph GUID=\".+-par1\">                <num>1.</num>"
                +
                "                <content>                  <p>Text...</p>                </content>              </paragraph>            </article>"
                + "<article GUID=\"art489\">" +
                "<num>Article 489</num>" +
                "<heading>4th article</heading>" +
                "<alinea GUID=\"art488-aln1\">bla bla 4</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";
        Pattern pattern = Pattern.compile(expected);
        Matcher matcher = pattern.matcher(new String(result));

        assertThat(expected + " should be found in " + new String(result), matcher.find(), is(true));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_articlesAreRemoved() throws Exception {
        String xml = "<akomaNtoso><bill><preface id =\"1\"><p>preface</p></preface>"
                + "<body><article GUID=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article GUID=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article GUID=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th article</heading>" +
                "<alinea GUID=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();

        TableOfContentItemVO pref = new TableOfContentItemVO(LegalTextTocItemType.PREFACE, "1", null, null, null, null, 3);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art487", "Article 487", "2de article became 1the", null,
                null, 20);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art488", "Article 488", "3th article became 2the", null,
                null, 31);
        tableOfContentItemVOList.add(pref);
        articleVOs.add(art2);
        articleVOs.add(art3);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextTocItemType.BODY, "body", null, null, null, null, 8);
        bodyVO.addAllChildItems(articleVOs);
        tableOfContentItemVOList.add(bodyVO);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill><preface id =\"1\"><p>preface</p></preface>"
                + "<body><article GUID=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article became 1the</heading>" +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article GUID=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th article became 2the</heading>" +
                "<alinea GUID=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_numAndHeadingAreAdded() throws Exception {
        String xml = "<akomaNtoso><bill><body>"
                + "<section GUID=\"sect1\">"
                + "</section>"
                + "</body></bill></akomaNtoso>";
        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();

        TableOfContentItemVO sec1 = new TableOfContentItemVO(LegalTextTocItemType.SECTION, "sect1", "Section 1", "Paragraphs", null, null, 4);

        articleVOs.add(sec1);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextTocItemType.BODY, "body", null, null, null, null, 3);
        bodyVO.addAllChildItems(articleVOs);
        tableOfContentItemVOList.add(bodyVO);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill><body>"
                + "<section GUID=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "</section>"
                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_articlesMovedFromSection() throws Exception {
        String xml = "<akomaNtoso><bill><body>"
                + "<section GUID=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading >Paragraphs</heading>"
                + "<article GUID=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article GUID=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "<section GUID=\"sect2\">"
                + "<num>Section 2</num>"
                + "<heading >Paragraphs</heading>"
                + "<article GUID=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th article</heading>" +
                "<alinea GUID=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextTocItemType.BODY, "body", null, null, null, null, 3);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(LegalTextTocItemType.SECTION, "sect1", "Section 1", "Paragraphs", null, null, 4);
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art486", "Article 486", "1ste article", null, null,
                11);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(LegalTextTocItemType.SECTION, "sect2", "Section 2", "Paragraphs", null, null, 33);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art487", "Article 487", "2de article", null, null,
                22);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art488", "Article 488", "3th article", null, null,
                40);

        bodyVO.addChildItem(sec1);
        sec1.addChildItem(art1);
        bodyVO.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill><body>"
                + "<section GUID=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article GUID=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "<section GUID=\"sect2\">"
                + "<num>Section 2</num>"
                + "<heading>Paragraphs</heading>"
                + "<article GUID=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article GUID=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th article</heading>" +
                "<alinea GUID=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_allArticlesRemovedFromSection() throws Exception {
        String xml = "<akomaNtoso><bill>"
                + "<body><section GUID=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article GUID=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article GUID=\"art487\">" +
                "<num class=\"ArticleNumber\">Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "<section GUID=\"sect2\">"
                + "<num>Section 2</num>"
                + "<heading>Paragraphs</heading>"
                + "<article GUID=\"art488\">" +
                "<num class=\"ArticleNumber\">Article 488</num>" +
                "<heading>3th article</heading>" +
                "<alinea GUID=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextTocItemType.BODY, "body", null, null, null, null, 3);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(LegalTextTocItemType.SECTION, "sect1", "Section 1", "Paragraphs", null, null, 4);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(LegalTextTocItemType.SECTION, "sect2", "Section 2", "Paragraphs", null, null, 35);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art487", "Article 487", "2de article",
                null, null, 22);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art488", "Article 488", "3th article",
                null, null, 42);

        bodyVO.addChildItem(sec1);
        bodyVO.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill><body>"
                + "<section GUID=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "</section>"
                + "<section GUID=\"sect2\">"
                + "<num>Section 2</num>"
                + "<heading>Paragraphs</heading>"
                + "<article GUID=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article GUID=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th article</heading>" +
                "<alinea GUID=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_sectionAdded() throws Exception {
        String xml = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso><bill><body>"
                + "<section GUID=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article GUID=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article GUID=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextTocItemType.BODY, "body", null, null, null, null, 4);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(LegalTextTocItemType.SECTION, "sect1", "Section 1", "Paragraphs", null, null, 5);
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art486", "Article 486", "1ste article", null, null,
                12);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(LegalTextTocItemType.SECTION, null, "Section 2", null, null, null, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art487", "Article 487", "2de article",
                null, null, 23);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, null, "Article 488", "3th article", null, null, null);

        bodyVO.addChildItem(sec1);
        sec1.addChildItem(art1);
        bodyVO.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes());

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso><bill><body>"
                + "<section GUID=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article GUID=\"art486\">"
                +
                "<num>Article 486</num>"
                +
                "<heading>1ste article</heading>"
                +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "</section>"
                + "<section GUID=\".+\">"
                + "<num>Section 2</num>"
                + "<article GUID=\"art487\">"
                +
                "<num>Article 487</num>"
                +
                "<heading>2de article</heading>"
                +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>"
                +
                "</article>"
                +
                "            <article GUID=\".+\" leos:editable=\"true\"  leos:deletable=\"true\">              <num>Article 488</num>              "
                +
                "<heading>3th article</heading>              <paragraph GUID=\".+-par1\">                <num>1.</num>"
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
                + "<section GUID=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article GUID=\"art486\">" +
                "<num class=\"numClass\">Article 486</num>" +
                "<heading class=\"hdgClass\">1ste article</heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article GUID=\"art487\">" +
                "<num class=\"ArticleNumber\">Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextTocItemType.BODY, "body", null, null, null, null, 4);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(LegalTextTocItemType.SECTION, "sect1", "Section 1", "Paragraphs", 8, 10, 5);
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art486", "Article 486", "1ste article", 15, 19,
                12);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(LegalTextTocItemType.SECTION, null, "Section 2", null, null, null, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art487", "Article 487", "2de article",
                30, 34, 27);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, null, "Article 488", "3th article", null, null, null);

        bodyVO.addChildItem(sec1);
        sec1.addChildItem(art1);
        bodyVO.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes());

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso><bill><body>"
                + "<section GUID=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article GUID=\"art486\">"
                +
                "<num class=\"numClass\">Article 486</num>"
                +
                "<heading class=\"hdgClass\">1ste article</heading>"
                +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "</section>"
                + "<section GUID=\".+\">"
                + "<num>Section 2</num>"
                + "<article GUID=\"art487\">"
                +
                "<num class=\"ArticleNumber\">Article 487</num>"
                +
                "<heading>2de article</heading>"
                +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "            <article GUID=\".+\" leos:editable=\"true\"  leos:deletable=\"true\">              <num>Article 488</num>              "
                +
                "<heading>3th article</heading>              <paragraph GUID=\".+-par1\">                <num>1.</num>"
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
                + "<article GUID=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<hcontainer><content><p>test</p></content>"
                + "</hcontainer>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextTocItemType.BODY, "body", null, null, null, null, 4);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO artNew = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, null, "Article 485", "0ste article", null, null,
                null);
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art486", "Article 486", "1ste article", null, null,
                5);

        bodyVO.addChildItem(artNew);
        bodyVO.addChildItem(art1);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes());

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso><bill><body>"

                + "            <article GUID=\".+\" leos:editable=\"true\"  leos:deletable=\"true\">              <num>Article 485</num>              "
                +
                "<heading>0ste article</heading>              <paragraph GUID=\".+-par1\">                <num>1.</num>"
                +
                "                <content>                  <p>Text...</p>                </content>              </paragraph>            </article>"
                + "<article GUID=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
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
                + "<section GUID=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article GUID=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "<section GUID=\"Section 2\">"
                + "<num>Section 2</num>"
                + "<article GUID=\"art487\">" +
                "<num class=\"ArticleNumber\">Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextTocItemType.BODY, "body", null, null, null, null, 4);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(LegalTextTocItemType.SECTION, "sect1", "Section 1", "Paragraphs", null, null, 5);
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art486", "Article 486", "1ste article", null, null,
                12);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(LegalTextTocItemType.SECTION, "Section 2", "Section 2", null, null, null, 23);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art487", "Article 487", "2de article",
                null, null, 28);

        bodyVO.addChildItem(sec1);
        sec1.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec1.addChildItem(art1);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes());

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso><bill><body>"
                + "<section GUID=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<section GUID=\"Section 2\">"
                + "<num>Section 2</num>"
                + "<article GUID=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "<article GUID=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"

                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_sectionHasNoNumOrHeading() throws Exception {
        String xml = "<akomaNtoso><bill><body>"
                + "<section GUID=\"sect1\">"
                + "<article GUID=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article GUID=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextTocItemType.BODY, "body", null, null, null, null, 3);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(LegalTextTocItemType.SECTION, "sect1", null, null, null, null,4 );
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art486", "Article 486", "1ste article", null, null,
                7);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(LegalTextTocItemType.SECTION, null, "Section 2", "Paragraphs", null, null, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art487", "Article 487", "2de article", null, null,
                18);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, null, "Article 488", "3th article", null, null, null);

        bodyVO.addChildItem(sec1);
        sec1.addChildItem(art1);
        bodyVO.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill><body>"
                + "<section GUID=\"sect1\">"
                + "<article GUID=\"art486\">"
                +
                "<num>Article 486</num>"
                +
                "<heading>1ste article</heading>"
                +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "</section>"
                + "<section GUID=\".+\">"
                + "<num>Section 2</num>"
                + "<heading>Paragraphs</heading>"
                + "<article GUID=\"art487\">"
                +
                "<num>Article 487</num>"
                +
                "<heading>2de article</heading>"
                +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "            <article GUID=\".+\" leos:editable=\"true\"  leos:deletable=\"true\">              <num>Article 488</num>              "
                +
                "<heading>3th article</heading>              <paragraph GUID=\".+-par1\">                <num>1.</num>"
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
                + "<part GUID=\"part1\">"
                + "<num>Part 1</num>"
                + "<heading>part1</heading>"
                + "<section GUID=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article GUID=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article GUID=\"art487\">" +
                "<num class=\"ArticleNumber\">Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</part>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextTocItemType.BODY, "body", null, null, null, null, 3);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO part1 = new TableOfContentItemVO(LegalTextTocItemType.PART, "part1", "Part 1", "part1", null, null, 4);
        TableOfContentItemVO sec1 = new TableOfContentItemVO(LegalTextTocItemType.SECTION, "sect1", "Section 1", "Paragraphs", null, null, 11);
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art486", "Article 486", "1ste article", null, null,
                18);
        TableOfContentItemVO part2 = new TableOfContentItemVO(LegalTextTocItemType.PART, null, "Part 2", "part2", null, null, null);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(LegalTextTocItemType.SECTION, null, "Section 2", "Paragraphs", null, null, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, "art487", "Article 487", "2de article",
                null, null, 29);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextTocItemType.ARTICLE, null, "Article 488", "3th article", null, null, null);

        bodyVO.addChildItem(part1);
        part1.addChildItem(sec1);
        sec1.addChildItem(art1);
        bodyVO.addChildItem(part2);
        part2.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill><body>"
                + "<part GUID=\"part1\">"
                + "<num>Part 1</num>"
                + "<heading>part1</heading>"
                + "<section GUID=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article GUID=\"art486\">"
                +
                "<num>Article 486</num>"
                +
                "<heading>1ste article</heading>"
                +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "</section>"
                + "</part>"
                + "<part GUID=\".+\">"
                + "<num>Part 2</num>"
                + "<heading>part2</heading>"
                + "<section GUID=\".+\">"
                + "<num>Section 2</num>"
                + "<heading>Paragraphs</heading>"
                + "<article GUID=\"art487\">"
                +
                "<num>Article 487</num>"
                +
                "<heading>2de article</heading>"
                +
                "<alinea GUID=\"art487-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "            <article GUID=\".+\" leos:editable=\"true\"  leos:deletable=\"true\">              <num>Article 488</num>              "
                +
                "<heading>3th article</heading>              <paragraph GUID=\".+-par1\">                <num>1.</num>"
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
                "<part GUID=\"part1\">" +
                "<num>Part I</num>" +
                "<heading>LEOS (Proof-Of-Concept)</heading>" +
                "<title GUID=\"titl1\">" +
                "<num>Title I</num>" +
                "<heading>Example Document</heading>" +
                "<chapter GUID=\"chap1\">" +
                "</chapter>" +
                "</title>" +
                "</part>" +
                "<hcontainer name=\"application\" GUID=\"app1\">" +
                "<content GUID=\"con\">" +
                "<p class=\"DirectApplication\">This Regulation shall be binding in its entirety and directly applicable in all Member States.</p>" +
                "<p class=\"DirectApplication\">(A possible extension of the direct application.)</p>" +
                "</content>" +
                "</hcontainer>" +
                "</body>" +
                "</bill>" +
                "</akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO body1 = new TableOfContentItemVO(LegalTextTocItemType.BODY, "body1", null, null, null, null, 3);
        TableOfContentItemVO part1 = new TableOfContentItemVO(LegalTextTocItemType.PART, "part1", "Part I", "LEOS (Proof-Of-Concept)", null,
                null, 6);
        TableOfContentItemVO title1 = new TableOfContentItemVO(LegalTextTocItemType.TITLE, "titl1", "Title I", "Example Document", null, null,
                13);
        TableOfContentItemVO ch1 = new TableOfContentItemVO(LegalTextTocItemType.CHAPTER, "chap1", null, null, null, null, 20);

        tableOfContentItemVOList.add(body1);
        body1.addChildItem(part1);
        part1.addChildItem(title1);
        title1.addChildItem(ch1);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes());

        String expected = "<akomaNtoso><bill>" +
                "<body id =\"body1\">" +
                "<part GUID=\"part1\">" +
                "<num>Part I</num>" +
                "<heading>LEOS (Proof-Of-Concept)</heading>" +
                "<title GUID=\"titl1\">" +
                "<num>Title I</num>" +
                "<heading>Example Document</heading>" +
                "<chapter GUID=\"chap1\">" +
                "</chapter>" +
                "</title>" +
                "</part>" +
                "<hcontainer name=\"application\" GUID=\"app1\">" +
                "<content GUID=\"con\">" +
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
                + "<article GUID=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea GUID=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<hcontainer><content><p>test</p></content>"
                + "</hcontainer>"
                + "</blabla></akomaNtoso>";

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName, Collections.<TableOfContentItemVO>emptyList(), xml.getBytes());

        assertThat(new String(result), is(xml));
    }

    @Test
    public void test_renumberArticle() {
        String xml = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"
                + "<akomaNtoso GUID=\"akn\">" +
                "<article GUID=\"art486\">" +
                "<num GUID=\"aknum\">Article 486</num>" +
                "<heading GUID=\"aknhead\">1st article</heading>" +
                "</article>" +
                "<article GUID=\"art486\">" +
                "<num GUID=\"aknnum2\"></num>" +
                "<heading GUID=\"aknhead2\">2th articl<authorialNote marker=\"101\" GUID=\"a1\"><p GUID=\"p1\">TestNote1</p></authorialNote>e</heading>" +
                "</article>" +
                "<article GUID=\"art486\">" +
                "<heading GUID=\"aknhead3\">3th article<authorialNote marker=\"90\" GUID=\"a2\"><p GUID=\"p2\">TestNote2</p></authorialNote></heading>" +
                "</article>" +
                "<article GUID=\"art486\">" +
                "</article>" +
                "</akomaNtoso>";

        when(messageHelper.getMessage("legaltext.article.num", new Object[]{1L}, Locale.FRENCH)).thenReturn("Article 1");
        when(messageHelper.getMessage("legaltext.article.num", new Object[]{2L}, Locale.FRENCH)).thenReturn("Article 2");
        when(messageHelper.getMessage("legaltext.article.num", new Object[]{3L}, Locale.FRENCH)).thenReturn("Article 3");
        when(messageHelper.getMessage("legaltext.article.num", new Object[]{4L}, Locale.FRENCH)).thenReturn("Article 4");

        byte[] result = vtdXmlContentProcessor.renumberArticles(xml.getBytes(), "fr");

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"
                + "<akomaNtoso GUID=\"akn\">" +
                "<article GUID=\"art486\">" +
                "<num>Article 1</num>" +
                "<heading GUID=\"aknhead\">1st article</heading>" +
                "</article>" +
                "<article GUID=\"art486\">" +
                "<num>Article 2</num>" +
                "<heading GUID=\"aknhead2\">2th articl<authorialNote marker=\"1\" GUID=\"a1\"><p GUID=\"p1\">TestNote1</p></authorialNote>e</heading>" +
                "</article>" +
                "<article GUID=\"art486\">" +
                "<num>Article 3</num>" +
                "<heading GUID=\"aknhead3\">3th article<authorialNote marker=\"2\" GUID=\"a2\"><p GUID=\"p2\">TestNote2</p></authorialNote></heading>" +
                "</article>" +
                "<article GUID=\"art486\">" +
                "<num>Article 4</num>" +
                "</article>" +
                "</akomaNtoso>";

        assertThat(new String(result).replaceAll("<num(\\s)*?GUID=\".+?\"(\\s)*?>", "<num>"), is(expected));
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
        byte[] result = vtdXmlContentProcessor.injectTagIdsinXML(xml.getBytes());

        Pattern pattern = Pattern.compile("id=");
        Matcher matcher = pattern.matcher(new String(result));

        assertThat("id= " + " should not be found in " + new String(result), matcher.find(), is(false));
    }

    @Test
    public void test_injectIdShouldInjectId() {
        String xml = "<p>xxx</p>";
        byte[] result = vtdXmlContentProcessor.injectTagIdsinXML(xml.getBytes());

        Pattern pattern = Pattern.compile("GUID=");
        Matcher matcher = pattern.matcher(new String(result));

        assertThat("id= " + " should be found in " + new String(result), matcher.find(), is(true));
    }

    @Test
    public void test_injectIdMethodShould_notInjectNewID() {
        String xml = "<p GUID=\"PQR\">xxx</p>";
        String expected = "<p GUID=\"PQR\">xxx</p>";
        byte[] result = vtdXmlContentProcessor.injectTagIdsinXML(xml.getBytes());

        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_updateReferedAttributes(){
        String xml      = "<xyz>" +
                "<p GUID=\"PQR\" refersTo=\"~ABCD\">xxx</p>" +//shd be updated
                "<p GUID=\"PQR2\" refersTo=\"~ABCD1\">xxx</p>" +
                "<b><p GUID=\"LMN\" refersTo=\"~ABCD\">xxx</p></b>" +//shd be updated
                "<p GUID=\"PQR3\" refersTo=\"ABCD\">xxx</p>" +
                "<p GUID=\"PQR4\">xxx</p>" +
                "</xyz>";
        String expected = "<xyz>" +
                "<p GUID=\"PQR\" refersTo=\"~ABCD\">newValue</p>" +
                "<p GUID=\"PQR2\" refersTo=\"~ABCD1\">xxx</p>" +
                "<b><p GUID=\"LMN\" refersTo=\"~ABCD\">newValue</p></b>" +
                "<p GUID=\"PQR3\" refersTo=\"ABCD\">xxx</p>" +
                "<p GUID=\"PQR4\">xxx</p>" +
                "</xyz>";
        HashMap hm= new HashMap<String, String>();
        hm.put("ABCD", "newValue");
        byte[] result = vtdXmlContentProcessor.updateReferedAttributes(xml.getBytes(),hm);
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_removeElements_one() throws ParseException {

        // setup
        String xml = "<meta GUID=\"ElementId\">" +
                "<temp GUID=\"xyz\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user1\"" +
                " leos:userName=\"User One\"" +  " leos:dg=\"DIGIT.B2.001\"" +
                " leos:dateTime=\"2015-05-29T11:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</temp>" +
                "</meta>";


        //make the actual call
        byte[] result = vtdXmlContentProcessor.removeElements(xml.getBytes(UTF_8), "//*[@refersTo=\"~leosComment\"]");

        // verify
        assertThat(new String(result,UTF_8) , equalTo("<meta GUID=\"ElementId\"></meta>"));
    }

    public void test_removeElements_multiple() throws ParseException {

        // setup
        String xml = "<meta GUID=\"ElementId\">" +
                "<temp GUID=\"xyz\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user1\"" +
                " leos:userName=\"User One\"" +  " leos:dg=\"DIGIT.B2.001\"" +
                " leos:dateTime=\"2015-05-29T11:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</temp>" +
                "<temp GUID=\"xyz1\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user1\"" +
                " leos:userName=\"User One\"" +  " leos:dg=\"DIGIT.B2.001\"" +
                " leos:dateTime=\"2015-05-29T11:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</temp>"+
                "</meta>";


        //make the actual call
        byte[] result = vtdXmlContentProcessor.removeElements(xml.getBytes(UTF_8), "//*[@refersTo=\"~leosComment\"]");

        // verify
        assertThat(new String(result,UTF_8) , equalTo("<meta GUID=\"ElementId\"></meta>"));
    }

    @Test
    public void test_removeElements_withOneParent() throws ParseException {

        // setup
        String xml = "<meta GUID=\"ElementId\">" +
                "<parent>"+
                "<temp GUID=\"xyz\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user1\"" +
                " leos:userName=\"User One\"" +  " leos:dg=\"DIGIT.B2.001\"" +
                " leos:dateTime=\"2015-05-29T11:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</temp>" +
                "</parent>"+
                "</meta>";


        //make the actual call
        byte[] result = vtdXmlContentProcessor.removeElements(xml.getBytes(UTF_8), "//*[@refersTo=\"~leosComment\"]", 1);

        // verify
        assertThat(new String(result,UTF_8) , equalTo("<meta GUID=\"ElementId\"></meta>"));
    }

    @Test
    public void test_removeElements_withMultipleParent() throws ParseException {

        // setup
        String xml = "<meta GUID=\"ElementId\">" +
                "<parent>"+
                "<temp GUID=\"xyz\"" +" refersTo=\"~leosComment\">" +
                    "<p refersTo=\"~leosChild\">This is a comment...</p>" +
                "</temp>" +
                "</parent>"+
                "<parent1>"+
                "<temp GUID=\"xyz1\"" +" refersTo=\"~leosComment\">" +
                    "<p refersTo=\"~leosChild\">This is a comment1...</p>" +
                "</temp>" +
                "</parent1>"+
                "</meta>";


        //make the actual call
        byte[] result = vtdXmlContentProcessor.removeElements(xml.getBytes(UTF_8), "//*[@refersTo=\"~leosChild\"]", 2);

        // verify
        assertThat(new String(result,UTF_8) , equalTo("<meta GUID=\"ElementId\"></meta>"));
    }
    
    @Test
    public void test_getLastElementId() throws ParseException {
        String xPath = "/part/article[last()]";
        String elementId = vtdXmlContentProcessor.getElementIdByPath(docContent, xPath);
        
        //verify
        assertThat(elementId, equalTo("art486"));
    }
}
