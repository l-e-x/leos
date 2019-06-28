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
package eu.europa.ec.leos.services.support.xml;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.europa.ec.leos.i18n.MessageHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.ximpleware.VTDNav;
import com.ximpleware.XMLModifier;

import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.support.xml.ref.Ref;
import eu.europa.ec.leos.test.support.LeosTest;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.LegalTextProposalTocItemType;

public class VtdXmlContentProcessorTest extends LeosTest {

    @Mock
    private ReferenceLabelProcessor referenceLabelProcessor;
    
    @InjectMocks
    private NumberProcessor proposalNumberingProcessor = new ProposalNumberingProcessor();

    @InjectMocks
    private NumberProcessor mandateNumberingProcessor = new MandateNumberingProcessor();
    
    @InjectMocks
    private VtdXmlContentProcessor vtdXmlContentProcessor = new VtdXmlContentProcessorForProposal();
   
    @Mock
   	private MessageHelper messageHelper;
    
    @Spy
    private ElementNumberingHelper elementNumberingHelper;

    private byte[] docContent;

    private User getTestUser() {
        String user1FirstName = "John";
        String user1LastName = "SMITH";
        String user1Login = "smithj";
        String user1Mail = "smithj@test.com";

        String entity = "Entity";

        String userId = "smithj";
        
        List<String> roles= new ArrayList<String>();
        roles.add("ADMIN");
        
        User user1 = new User(1l, user1Login, user1LastName + " " + user1FirstName, entity, user1Mail,roles);
        return user1;
    }

    @Before
    public void setup() {
        super.setup();
        MockitoAnnotations.initMocks(this);
        String doc = "<part xml:id=\"part11\">"
                +
                "                <num xml:id=\"n1\" class=\"PartNumber\">Part XI</num>"
                +
                "                <heading xml:id=\"h1\" class=\"PartHeading\">FINAL PROVISIONS</heading>"
                +
                "                <article xml:id=\"art485\">"
                +
                "                    <num xml:id=\"n1\" class=\"ArticleNumber\">Article 485</num>"
                +
                "                    <paragraph xml:id=\"art485-par1\">"
                +
                "                        <num  xml:id=\"n2\" class=\"Paragraph(numbered)\">1.</num>"
                +
                "                        <content xml:id=\"c1\" >"
                +
                "                            <p  xml:id=\"p1\" class=\"Paragraph(numbered)\">Subject to paragraph 2, this Regulation<authorialNote marker=\"2\" xml:id=\"a1\"><p xml:id=\"ptest1\">TestNote1</p></authorialNote> shall apply from 1 January 2013.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                    <paragraph xml:id=\"art485-par2\">"
                +
                "                        <num  xml:id=\"n2\" class=\"Paragraph(numbered)\">2.</num>"
                +
                "                        <content xml:id=\"con\">"
                +
                "                            <p  xml:id=\"p2\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015<authorialNote marker=\"4\" xml:id=\"a2\"><p xml:id=\"ptest2\">TestNote2</p></authorialNote>.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                </article>"
                +
                "                <article xml:id=\"art486\">"
                +
                "                    <num  xml:id=\"n3\" class=\"ArticleNumber\">Article 486</num>"
                +
                "                    <alinea xml:id=\"art486-aln1\">"
                +
                "                        <content xml:id=\"c3\">"
                +
                "                            <p xml:id=\"p3\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i xml:id=\"i2\">Official Journal of the European<authorialNote marker=\"8\" xml:id=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
                +
                "                        </content>" +
                "                    </alinea>" +
                "                </article>" +
                "            </part>";

        docContent = doc.getBytes(UTF_8);
    }
    
    
    @Test
    public void test_getElementValue_should_return_elementValue_when_xpath_Is_Valid() {
     String elementValue = vtdXmlContentProcessor.getElementValue(docContent, "//heading[1]", true);
     String expected = "FINAL PROVISIONS";
     assertThat(elementValue, is(expected));
     
    }
    
    @Test
    public void test_getElementValue_should_return_null_when_xpath_Is_InValid() {
     String elementValue = vtdXmlContentProcessor.getElementValue(docContent, "//heading_[1]", true);
     assertThat(elementValue, is(nullValue()));
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
                is("<alinea xml:id=\"art486-aln1\">"
                +
                "                        <content xml:id=\"c3\">"
                +
                "                            <p xml:id=\"p3\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i xml:id=\"i2\">Official Journal of the European<authorialNote marker=\"8\" xml:id=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
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
                is("<alinea xml:id=\"art486-aln1\">"
                +
                "                        <content xml:id=\"c3\">"
                +
                "                            <p xml:id=\"p3\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i xml:id=\"i2\">Official Journal of the European<authorialNote marker=\"8\" xml:id=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
                +
                "                        </content>" +
                "                    </alinea>"));
    }

    @Test(expected = RuntimeException.class)
    public void test_getTagContentByNameAndId_should_throwRuntimeException_when_illegalXmlFormat() throws Exception {
        String xml = " <article xml:id=\"art486\">" +
                "                    <num class=\"ArticleNumber\">Article 486</num>";
        String tagContent = vtdXmlContentProcessor.getElementByNameAndId(xml.getBytes(UTF_8), "alinea", "art486-aln1");
        assertThat(tagContent, is(nullValue()));
    }

    @Test
    public void test_replaceElementByTagNameAndId_should_match_returnedTagContent() throws Exception {

        String newContent = "<article xml:id=\"art486\">"
                +
                "                    <num xml:id=\"num1\" class=\"ArticleNumber\">Article 486</num>"
                +
                "                    <alinea xml:id=\"art486-aln1\">"
                +
                "                        <content xml:id=\"c\">"
                +
                "                            <p xml:id=\"p\" class=\"Paragraph(unnumbered)\">This text should appear in the main document after merge<authorialNote xml:id=\"a4\" marker=\"1\"><p xml:id=\"p1\">TestNoteX</p></authorialNote> with the updated Article <i xml:id=\"i1\">Official Journal of the European Union</i>.</p>"
                +
                "                        </content>" +
                "                    </alinea>" +
                "                </article>";

        // DO THE ACTUAL CALL
        byte[] returnedElement = vtdXmlContentProcessor.replaceElementByTagNameAndId(docContent, newContent, "article", "art486");

        String expected = "<part xml:id=\"part11\">"
                +
                "                <num xml:id=\"n1\" class=\"PartNumber\">Part XI</num>"
                +
                "                <heading xml:id=\"h1\" class=\"PartHeading\">FINAL PROVISIONS</heading>"
                +
                "                <article xml:id=\"art485\">"
                +
                "                    <num xml:id=\"n1\" class=\"ArticleNumber\">Article 485</num>"
                +
                "                    <paragraph xml:id=\"art485-par1\">"
                +
                "                        <num  xml:id=\"n2\" class=\"Paragraph(numbered)\">1.</num>"
                +
                "                        <content xml:id=\"c1\" >"
                +
                "                            <p  xml:id=\"p1\" class=\"Paragraph(numbered)\">Subject to paragraph 2, this Regulation<authorialNote marker=\"1\" xml:id=\"a1\"><p xml:id=\"ptest1\">TestNote1</p></authorialNote> shall apply from 1 January 2013.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                    <paragraph xml:id=\"art485-par2\">"
                +
                "                        <num  xml:id=\"n2\" class=\"Paragraph(numbered)\">2.</num>"
                +
                "                        <content xml:id=\"con\">"
                +
                "                            <p  xml:id=\"p2\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015<authorialNote marker=\"2\" xml:id=\"a2\"><p xml:id=\"ptest2\">TestNote2</p></authorialNote>.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                </article>"
                +
                "                <article xml:id=\"art486\">"
                +
                "                    <num xml:id=\"num1\" class=\"ArticleNumber\">Article 486</num>"
                +
                "                    <alinea xml:id=\"art486-aln1\">"
                +
                "                        <content xml:id=\"c\">"
                +
                "                            <p xml:id=\"p\" class=\"Paragraph(unnumbered)\">This text should appear in the main document after merge<authorialNote xml:id=\"a4\" marker=\"3\"><p xml:id=\"p1\">TestNoteX</p></authorialNote> with the updated Article <i xml:id=\"i1\">Official Journal of the European Union</i>.</p>"
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
        String expected = "<part xml:id=\"part11\">"
                +
                "                <num xml:id=\"n1\" class=\"PartNumber\">Part XI</num>"
                +
                "                <heading xml:id=\"h1\" class=\"PartHeading\">FINAL PROVISIONS</heading>"
                +
                "                <article xml:id=\"art485\">"
                +
                "                    <num xml:id=\"n1\" class=\"ArticleNumber\">Article 485</num>"
                +
                "                    <paragraph xml:id=\"art485-par1\">"
                +
                "                        <num  xml:id=\"n2\" class=\"Paragraph(numbered)\">1.</num>"
                +
                "                        <content xml:id=\"c1\" >"
                +
                "                            <p  xml:id=\"p1\" class=\"Paragraph(numbered)\">Subject to paragraph 2, this Regulation<authorialNote marker=\"2\" xml:id=\"a1\"><p xml:id=\"ptest1\">TestNote1</p></authorialNote> shall apply from 1 January 2013.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                    <paragraph xml:id=\"art485-par2\">"
                +
                "                        <num  xml:id=\"n2\" class=\"Paragraph(numbered)\">2.</num>"
                +
                "                        <content xml:id=\"con\">"
                +
                "                            <p  xml:id=\"p2\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015<authorialNote marker=\"4\" xml:id=\"a2\"><p xml:id=\"ptest2\">TestNote2</p></authorialNote>.</p>"
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

        String template = "                <article xml:id=\"art435\">" +
                "              <num xml:id=\"n4\">Article #</num>" +
                "              <heading xml:id=\"h4\">Article heading...</heading>" +
                "              <paragraph xml:id=\"art1-par1\">" +
                "                <num xml:id=\"n4p\">1.</num>" +
                "                <content xml:id=\"c4\">" +
                "                  <p xml:id=\"p4\">Text.<authorialNote marker=\"1\" xml:id=\"a1\"><p>TestNote4</p></authorialNote>..</p>" +
                "                </content>" +
                "              </paragraph>" +
                "             </article>";

        // DO THE ACTUAL CALL - Insert After
        byte[] returnedElement = vtdXmlContentProcessor.insertElementByTagNameAndId(docContent, template, "article", "art486", false);

        String expected ="<part xml:id=\"part11\">"
                +
                "                <num xml:id=\"n1\" class=\"PartNumber\">Part XI</num>"
                +
                "                <heading xml:id=\"h1\" class=\"PartHeading\">FINAL PROVISIONS</heading>"
                +
                "                <article xml:id=\"art485\">"
                +
                "                    <num xml:id=\"n1\" class=\"ArticleNumber\">Article 485</num>"
                +
                "                    <paragraph xml:id=\"art485-par1\">"
                +
                "                        <num  xml:id=\"n2\" class=\"Paragraph(numbered)\">1.</num>"
                +
                "                        <content xml:id=\"c1\" >"
                +
                "                            <p  xml:id=\"p1\" class=\"Paragraph(numbered)\">Subject to paragraph 2, this Regulation<authorialNote marker=\"2\" xml:id=\"a1\"><p xml:id=\"ptest1\">TestNote1</p></authorialNote> shall apply from 1 January 2013.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                    <paragraph xml:id=\"art485-par2\">"
                +
                "                        <num  xml:id=\"n2\" class=\"Paragraph(numbered)\">2.</num>"
                +
                "                        <content xml:id=\"con\">"
                +
                "                            <p  xml:id=\"p2\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015<authorialNote marker=\"4\" xml:id=\"a2\"><p xml:id=\"ptest2\">TestNote2</p></authorialNote>.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                </article>"
                +
                "                <article xml:id=\"art486\">"
                +
                "                    <num  xml:id=\"n3\" class=\"ArticleNumber\">Article 486</num>"
                +
                "                    <alinea xml:id=\"art486-aln1\">"
                +
                "                        <content xml:id=\"c3\">"
                +
                "                            <p xml:id=\"p3\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i xml:id=\"i2\">Official Journal of the European<authorialNote marker=\"8\" xml:id=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
                +
                "                        </content>" +
                "                    </alinea>" +
                "                </article>" +
                "                <article xml:id=\"art435\">" +
                "              <num xml:id=\"n4\">Article #</num>" +
                "              <heading xml:id=\"h4\">Article heading...</heading>" +
                "              <paragraph xml:id=\"art1-par1\">" +
                "                <num xml:id=\"n4p\">1.</num>" +
                "                <content xml:id=\"c4\">" +
                "                  <p xml:id=\"p4\">Text.<authorialNote marker=\"1\" xml:id=\"a1\"><p>TestNote4</p></authorialNote>..</p>" +
                "                </content>" +
                "              </paragraph>" +
                "             </article>"+
                "            </part>";;

        assertThat(returnedElement, is(expected.getBytes(UTF_8)));

    }

    @Test
    public void test_insertElementByTagNameAndId_when_insert_before() throws Exception {

        String template = "                <article xml:id=\"art435\">" +
                "              <num xml:id=\"n4\">Article #</num>" +
                "              <heading xml:id=\"h4\">Article heading...</heading>" +
                "              <paragraph xml:id=\"art1-par1\">" +
                "                <num xml:id=\"n4p\">1.</num>" +
                "                <content xml:id=\"c4\">" +
                "                  <p xml:id=\"p4\">Text.<authorialNote marker=\"1\" xml:id=\"a1\"><p>TestNote4</p></authorialNote>..</p>" +
                "                </content>" +
                "              </paragraph>" +
                "             </article>";

        // DO THE ACTUAL CALL - Insert Before
        byte[] returnedElement = vtdXmlContentProcessor.insertElementByTagNameAndId(docContent, template, "article", "art486", true);

        String expected = "<part xml:id=\"part11\">"
                +
                "                <num xml:id=\"n1\" class=\"PartNumber\">Part XI</num>"
                +
                "                <heading xml:id=\"h1\" class=\"PartHeading\">FINAL PROVISIONS</heading>"
                +
                "                <article xml:id=\"art485\">"
                +
                "                    <num xml:id=\"n1\" class=\"ArticleNumber\">Article 485</num>"
                +
                "                    <paragraph xml:id=\"art485-par1\">"
                +
                "                        <num  xml:id=\"n2\" class=\"Paragraph(numbered)\">1.</num>"
                +
                "                        <content xml:id=\"c1\" >"
                +
                "                            <p  xml:id=\"p1\" class=\"Paragraph(numbered)\">Subject to paragraph 2, this Regulation<authorialNote marker=\"2\" xml:id=\"a1\"><p xml:id=\"ptest1\">TestNote1</p></authorialNote> shall apply from 1 January 2013.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                    <paragraph xml:id=\"art485-par2\">"
                +
                "                        <num  xml:id=\"n2\" class=\"Paragraph(numbered)\">2.</num>"
                +
                "                        <content xml:id=\"con\">"
                +
                "                            <p  xml:id=\"p2\" class=\"Paragraph(numbered)\">Article 436(1) shall apply from 1 January 2015<authorialNote marker=\"4\" xml:id=\"a2\"><p xml:id=\"ptest2\">TestNote2</p></authorialNote>.</p>"
                +
                "                        </content>"
                +
                "                    </paragraph>"
                +
                "                </article>"
                +
                "                                <article xml:id=\"art435\">" +
                "              <num xml:id=\"n4\">Article #</num>" +
                "              <heading xml:id=\"h4\">Article heading...</heading>" +
                "              <paragraph xml:id=\"art1-par1\">" +
                "                <num xml:id=\"n4p\">1.</num>" +
                "                <content xml:id=\"c4\">" +
                "                  <p xml:id=\"p4\">Text.<authorialNote marker=\"1\" xml:id=\"a1\"><p>TestNote4</p></authorialNote>..</p>" +
                "                </content>" +
                "              </paragraph>" +
                "             </article>" +
                "<article xml:id=\"art486\">"
                +
                "                    <num  xml:id=\"n3\" class=\"ArticleNumber\">Article 486</num>"
                +
                "                    <alinea xml:id=\"art486-aln1\">"
                +
                "                        <content xml:id=\"c3\">"
                +
                "                            <p xml:id=\"p3\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i xml:id=\"i2\">Official Journal of the European<authorialNote marker=\"8\" xml:id=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
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
        List<TableOfContentItemVO> tableOfContentItemVOList = vtdXmlContentProcessor.buildTableOfContent("bill", LegalTextProposalTocItemType::getTocItemTypeFromName, docContent, false);
        assertThat(tableOfContentItemVOList, is(notNullValue()));
        assertThat(tableOfContentItemVOList.size(), is(0));
    }

    @Test
    public void test_buildTableOfContentItemVOList_should_returnReturnCorrectContent_when_expectedFormat() throws Exception {

        byte[] fileContent = getFileContent("/akn_toc-test.xml");
        List<TableOfContentItemVO> tableOfContentItemVOList = vtdXmlContentProcessor.buildTableOfContent("bill", LegalTextProposalTocItemType::getTocItemTypeFromName, fileContent, false);
        assertThat(tableOfContentItemVOList, is(notNullValue()));
        assertThat(tableOfContentItemVOList.size(), is(4));

        // do a huge number of asserts..
        assertThat(tableOfContentItemVOList.get(0), is(new TableOfContentItemVO(LegalTextProposalTocItemType.PREFACE, null, null, null, null,
                "on [...] preface and half bold text", null, 72, 71, null)));
        assertThat(tableOfContentItemVOList.get(1), is(new TableOfContentItemVO(LegalTextProposalTocItemType.PREAMBLE, null, null, null, null, null,
                null, null, 80, null)));

        // build the body as second expected root item
        TableOfContentItemVO body = new TableOfContentItemVO(LegalTextProposalTocItemType.BODY, null, null, null, null, null, null, null, 83, null);
        TableOfContentItemVO part1 = new TableOfContentItemVO(LegalTextProposalTocItemType.PART, "part1", null, null, null, "part-head", null, 87, 84, null);
        body.addChildItem(part1);
        TableOfContentItemVO title = new TableOfContentItemVO(LegalTextProposalTocItemType.TITLE, "title1", null, "on [...] preface and half bold text", null, 
                "title-head", 94, 92, 89, null);
        part1.addChildItem(title);
        TableOfContentItemVO chapter = new TableOfContentItemVO(LegalTextProposalTocItemType.CHAPTER, "chap1", null, "chapter-num", null, "chapter-head", 
                40, 107, 102, null);
        title.addChildItem(chapter);
        TableOfContentItemVO section = new TableOfContentItemVO(LegalTextProposalTocItemType.SECTION, "sect1", null, "section-num", null, 
                "section-head", 112, 114, 109, null);
        chapter.addChildItem(section);
        TableOfContentItemVO art = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art2", null, "Title I", null, "title", 120, 122, 116, null);
        chapter.addChildItem(art);
        assertThat(tableOfContentItemVOList.get(2), is(body));
        assertThat(tableOfContentItemVOList.get(3), is(new TableOfContentItemVO(LegalTextProposalTocItemType.CONCLUSIONS, null, null, null, null, null, null, null,
                128, null)));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_oldContainedutf8() {
        String xml = "<akomaNtoso><bill><body>"
                + "<article xml:id=\"art486\">" +
                "<num>Article 486 <placeholder>[…]</placeholder></num>" +
                "<heading><content><p>1ste article</p></content></heading>" +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";
        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art486", null, "Article 486 <placeholder>[…]</placeholder>",
                null, "<content><p>1ste article</p></content>", null, null, 4, null);

        articleVOs.add(art1);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextProposalTocItemType.BODY, "body", null, null, null, null, null, null, 3, null);
        bodyVO.addAllChildItems(articleVOs);
        tableOfContentItemVOList.add(bodyVO);


        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextProposalTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes(UTF_8), getTestUser());

        assertThat(new String(result, UTF_8), is(xml));

    }

    // amounts and L &lt; K
    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_oldContainedEscapedXML() {
        String xml = "<akomaNtoso><bill><body>"
                + "<article xml:id=\"art486\">" +
                "<num>Article 486 <placeholder>[…]</placeholder></num>" +
                "<heading><content><p>1ste article</p></content></heading>" +
                "<alinea xml:id=\"art486-aln1\">bla amounts and L &lt; K bla</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();

        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art486", null, "Article 486 <placeholder>[…]</placeholder>", null,
                "<content><p>1ste article</p></content>", null, null, 4, null);

        articleVOs.add(art1);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextProposalTocItemType.BODY, "body", null, null, null, null, null, null, 3, null);
        bodyVO.addAllChildItems(articleVOs);
        tableOfContentItemVOList.add(bodyVO);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextProposalTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes(UTF_8), getTestUser());

        assertThat(new String(result, UTF_8), is(xml));

    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_2newAreAddedAtSameOffset() throws Exception {
        String xml = "<akomaNtoso><bill><body>"
                + "<article xml:id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading><content><p>1ste article</p></content></heading>" +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"

                + "<article xml:id=\"art489\">" +
                "<num>Article 489</num>" +
                "<heading>4th article</heading>" +
                "<alinea xml:id=\"art488-aln1\">bla bla 4</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art486", null, "Article 486", null, "1ste article", null, null,
                4, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, null, null, "487 added", null, "2de article added", null, null,
                null, null);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, null, null, "488 added", null, "3th article added", null, null,
                null, null);
        TableOfContentItemVO art4 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art489", null, " 489", null, "4th article", null, null,
                17, null);

        articleVOs.add(art1);
        articleVOs.add(art2);
        articleVOs.add(art3);
        articleVOs.add(art4);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextProposalTocItemType.BODY, "body", null, null, null, null, null, null, 3, null);
        bodyVO.addAllChildItems(articleVOs);
        tableOfContentItemVOList.add(bodyVO);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextProposalTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes(), getTestUser());

        String expected = "<akomaNtoso><bill><body>"
                + "<article xml:id=\"art486\">"
                +
                "<num>Article 486</num>"
                +
                "<heading>1ste article</heading>"
                +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "            <article xml:id=\".+\" leos:editable=\"true\"  leos:deletable=\"true\">              <num leos:editable=\"false\">Article 487 added</num>              "
                +
                "<heading>2de article added</heading>              <paragraph xml:id=\".+-par1\">                <num>1.</num>"
                +
                "                <content>                  <p>Text...</p>                </content>              </paragraph>            </article>"
                + "            <article xml:id=\".+\" leos:editable=\"true\"  leos:deletable=\"true\">              <num leos:editable=\"false\">Article 488 added</num>              "
                +
                "<heading>3th article added</heading>              <paragraph xml:id=\".+-par1\">                <num>1.</num>"
                +
                "                <content>                  <p>Text...</p>                </content>              </paragraph>            </article>"
                + "<article xml:id=\"art489\">" +
                "<num>Article 489</num>" +
                "<heading>4th article</heading>" +
                "<alinea xml:id=\"art488-aln1\">bla bla 4</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";
        Pattern pattern = Pattern.compile(expected);
        Matcher matcher = pattern.matcher(new String(result));

        assertThat(expected + " should be found in " + new String(result), matcher.find(), is(true));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_articlesAreRemoved() throws Exception {
        String xml = "<akomaNtoso><bill><preface id =\"1\"><p>preface</p></preface>"
                + "<body><article xml:id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article xml:id=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea xml:id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article xml:id=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th article</heading>" +
                "<alinea xml:id=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();

        TableOfContentItemVO pref = new TableOfContentItemVO(LegalTextProposalTocItemType.PREFACE, "1", null, null, null, null, null, null, 3, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art487", null, "Article 487", null, "2de article became 1the", null,
                null, 20, null);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art488", null, "Article 488", null, "3th article became 2the", null,
                null, 31, null);
        tableOfContentItemVOList.add(pref);
        articleVOs.add(art2);
        articleVOs.add(art3);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextProposalTocItemType.BODY, "body", null, null, null, null, null, null, 8, null);
        bodyVO.addAllChildItems(articleVOs);
        tableOfContentItemVOList.add(bodyVO);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextProposalTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes(), getTestUser());

        String expected = "<akomaNtoso><bill><preface id =\"1\"><p>preface</p></preface>"
                + "<body><article xml:id=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article became 1the</heading>" +
                "<alinea xml:id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article xml:id=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th article became 2the</heading>" +
                "<alinea xml:id=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_numAndHeadingAreAdded() throws Exception {
        String xml = "<akomaNtoso><bill><body>"
                + "<section xml:id=\"sect1\">"
                + "</section>"
                + "</body></bill></akomaNtoso>";
        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        List<TableOfContentItemVO> articleVOs = new ArrayList<TableOfContentItemVO>();

        TableOfContentItemVO sec1 = new TableOfContentItemVO(LegalTextProposalTocItemType.SECTION, "sect1", null, "Section 1", null, "Paragraphs", null, null, 4, null);

        articleVOs.add(sec1);

        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextProposalTocItemType.BODY, "body", null, null, null, null, null, null, 3, null);
        bodyVO.addAllChildItems(articleVOs);
        tableOfContentItemVOList.add(bodyVO);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextProposalTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes(), getTestUser());

        String expected = "<akomaNtoso><bill><body>"
                + "<section xml:id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "</section>"
                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_articlesMovedFromSection() throws Exception {
        String xml = "<akomaNtoso><bill><body>"
                + "<section xml:id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading >Paragraphs</heading>"
                + "<article xml:id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article xml:id=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea xml:id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "<section xml:id=\"sect2\">"
                + "<num>Section 2</num>"
                + "<heading >Paragraphs</heading>"
                + "<article xml:id=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th article</heading>" +
                "<alinea xml:id=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextProposalTocItemType.BODY, "body", null, null, null, null, null, null, 3, null);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(LegalTextProposalTocItemType.SECTION, "sect1", null, "Section 1", null, "Paragraphs", null, null, 4, null);
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art486", null, "Article 486", null, "1ste article", null, null,
                11, null);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(LegalTextProposalTocItemType.SECTION, "sect2", null, "Section 2", null, "Paragraphs", null, null, 33, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art487", null, "Article 487", null, "2de article", null, null,
                22, null);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art488", null, "Article 488", null, "3th article", null, null,
                40, null);

        bodyVO.addChildItem(sec1);
        sec1.addChildItem(art1);
        bodyVO.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextProposalTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes(), getTestUser());

        String expected = "<akomaNtoso><bill><body>"
                + "<section xml:id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article xml:id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "<section xml:id=\"sect2\">"
                + "<num>Section 2</num>"
                + "<heading>Paragraphs</heading>"
                + "<article xml:id=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea xml:id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article xml:id=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th article</heading>" +
                "<alinea xml:id=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_allArticlesRemovedFromSection() throws Exception {
        String xml = "<akomaNtoso><bill>"
                + "<body><section xml:id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article xml:id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article xml:id=\"art487\">" +
                "<num class=\"ArticleNumber\">Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea xml:id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "<section xml:id=\"sect2\">"
                + "<num>Section 2</num>"
                + "<heading>Paragraphs</heading>"
                + "<article xml:id=\"art488\">" +
                "<num class=\"ArticleNumber\">Article 488</num>" +
                "<heading>3th article</heading>" +
                "<alinea xml:id=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextProposalTocItemType.BODY, "body", null, null, null, null, null, null, 3, null);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(LegalTextProposalTocItemType.SECTION, "sect1", null, "Section 1", null, "Paragraphs", null, null, 4, null);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(LegalTextProposalTocItemType.SECTION, "sect2", null, "Section 2", null, "Paragraphs", null, null, 35, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art487", null, "Article 487", null, "2de article",
                null, null, 22, null);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art488", null, "Article 488", null, "3th article",
                null, null, 42, null);

        bodyVO.addChildItem(sec1);
        bodyVO.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextProposalTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes(), getTestUser());

        String expected = "<akomaNtoso><bill><body>"
                + "<section xml:id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "</section>"
                + "<section xml:id=\"sect2\">"
                + "<num>Section 2</num>"
                + "<heading>Paragraphs</heading>"
                + "<article xml:id=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea xml:id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article xml:id=\"art488\">" +
                "<num>Article 488</num>" +
                "<heading>3th article</heading>" +
                "<alinea xml:id=\"art488-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_sectionAdded() throws Exception {
        String xml = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso><bill><body>"
                + "<section xml:id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article xml:id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article xml:id=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea xml:id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextProposalTocItemType.BODY, "body", null, null, null, null, null, null, 4, null);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(LegalTextProposalTocItemType.SECTION, "sect1", null, "Section 1", null, "Paragraphs", null, null, 5, null);
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art486", null, "Article 486", null, "1ste article", null, null,
                12, null);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(LegalTextProposalTocItemType.SECTION, null, null, "Section 2", null, null, null, null, null, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art487", null, "Article 487", null, "2de article",
                null, null, 23, null);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, null, null, "488", null, "3th article", null, null, null, null);

        bodyVO.addChildItem(sec1);
        sec1.addChildItem(art1);
        bodyVO.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextProposalTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes(), getTestUser());

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso><bill><body>"
                + "<section xml:id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article xml:id=\"art486\">"
                +
                "<num>Article 486</num>"
                +
                "<heading>1ste article</heading>"
                +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "</section>"
                + "<section xml:id=\".+\">"
                + "<num>Section 2</num>"
                + "<article xml:id=\"art487\">"
                +
                "<num>Article 487</num>"
                +
                "<heading>2de article</heading>"
                +
                "<alinea xml:id=\"art487-aln1\">bla bla</alinea>"
                +
                "</article>"
                +
                "            <article xml:id=\".+\" leos:editable=\"true\"  leos:deletable=\"true\">              <num leos:editable=\"false\">Article 488</num>              "
                +
                "<heading>3th article</heading>              <paragraph xml:id=\".+-par1\">                <num>1.</num>"
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
                + "<section xml:id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article xml:id=\"art486\">" +
                "<num class=\"numClass\">Article 486</num>" +
                "<heading class=\"hdgClass\">1ste article</heading>" +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article xml:id=\"art487\">" +
                "<num class=\"ArticleNumber\">Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea xml:id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextProposalTocItemType.BODY, "body", null, null, null, null, null, null, 4, null);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(LegalTextProposalTocItemType.SECTION, "sect1", null, "Section 1", null, "Paragraphs", 8, 10, 5, null);
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art486", null, "Article 486", null, "1ste article", 15, 19,
                12, null);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(LegalTextProposalTocItemType.SECTION, null, null, "Section 2", null, null, null, null, null, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art487", null, "Article 487", null, "2de article",
                30, 34, 27, null);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, null, null, "488", null, "3th article", null, null, null, null);

        bodyVO.addChildItem(sec1);
        sec1.addChildItem(art1);
        bodyVO.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextProposalTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes(), getTestUser());

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso><bill><body>"
                + "<section xml:id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article xml:id=\"art486\">"
                +
                "<num class=\"numClass\">Article 486</num>"
                +
                "<heading class=\"hdgClass\">1ste article</heading>"
                +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "</section>"
                + "<section xml:id=\".+\">"
                + "<num>Section 2</num>"
                + "<article xml:id=\"art487\">"
                +
                "<num class=\"ArticleNumber\">Article 487</num>"
                +
                "<heading>2de article</heading>"
                +
                "<alinea xml:id=\"art487-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "            <article xml:id=\".+\" leos:editable=\"true\"  leos:deletable=\"true\">              <num leos:editable=\"false\">Article 488</num>              "
                +
                "<heading>3th article</heading>              <paragraph xml:id=\".+-par1\">                <num>1.</num>"
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
                + "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">" +
                "<bill><body>"
                + "<article xml:id=\"art486\"> <num leos:editable=\"false\">Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<hcontainer><content><p>test</p></content>"
                + "</hcontainer>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextProposalTocItemType.BODY, "body", null, null, null, null, null, null, 8, null);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO artNew = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, null, null, "485", null, "0ste article", null, null, null, null);
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art486", null, "Article 486", null, "1ste article", null,
                null, 9, null);

        bodyVO.addChildItem(artNew);
        bodyVO.addChildItem(art1);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextProposalTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes(), getTestUser());

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\"><bill><body>"

                + "            <article xml:id=\".+\" leos:editable=\"true\"  leos:deletable=\"true\">              <num leos:editable=\"false\">Article 485</num>              "
                +
                "<heading>0ste article</heading>              <paragraph xml:id=\".+-par1\">                <num>1.</num>"
                +
                "                <content>                  <p>Text...</p>                </content>              </paragraph>            </article>"
                + "<article xml:id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>" +
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
                + "<section xml:id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article xml:id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "<section xml:id=\"Section 2\">"
                + "<num>Section 2</num>"
                + "<article xml:id=\"art487\">" +
                "<num class=\"ArticleNumber\">Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea xml:id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextProposalTocItemType.BODY, "body", null, null, null, null, null, null, 4, null);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(LegalTextProposalTocItemType.SECTION, "sect1", null, "Section 1", null, "Paragraphs", null, null, 5, null);
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art486", null, "Article 486", null, "1ste article", null, null,
                12, null);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(LegalTextProposalTocItemType.SECTION, "Section 2", null, "Section 2", null, null, null, null, 23, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art487", null, "Article 487", null, "2de article",
                null, null, 28, null);

        bodyVO.addChildItem(sec1);
        sec1.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec1.addChildItem(art1);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextProposalTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes(), getTestUser());

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.--><akomaNtoso><bill><body>"
                + "<section xml:id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<section xml:id=\"Section 2\">"
                + "<num>Section 2</num>"
                + "<article xml:id=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea xml:id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "<article xml:id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"

                + "</body></bill></akomaNtoso>";
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_mergeTableOfContentIntoDocument_should_returnUpdatedByteArray_when_sectionHasNoNumOrHeading() throws Exception {
        String xml = "<akomaNtoso><bill><body>"
                + "<section xml:id=\"sect1\">"
                + "<article xml:id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article xml:id=\"art487\">" +
                "<num>Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea xml:id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextProposalTocItemType.BODY, "body", null, null, null, null, null, null, 3, null);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO sec1 = new TableOfContentItemVO(LegalTextProposalTocItemType.SECTION, "sect1", null, null, null, null, null, null,4, null);
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art486", null, "Article 486", null, "1ste article", null, null,
                7, null);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(LegalTextProposalTocItemType.SECTION, null, null, "Section 2", null, "Paragraphs", null, null, null, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art487", null, "Article 487", null, "2de article", null, null,
                18, null);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, null, null, "488", null, "3th article", null, null, null, null);

        bodyVO.addChildItem(sec1);
        sec1.addChildItem(art1);
        bodyVO.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextProposalTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes(), getTestUser());

        String expected = "<akomaNtoso><bill><body>"
                + "<section xml:id=\"sect1\">"
                + "<article xml:id=\"art486\">"
                +
                "<num>Article 486</num>"
                +
                "<heading>1ste article</heading>"
                +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "</section>"
                + "<section xml:id=\".+\">"
                + "<num>Section 2</num>"
                + "<heading>Paragraphs</heading>"
                + "<article xml:id=\"art487\">"
                +
                "<num>Article 487</num>"
                +
                "<heading>2de article</heading>"
                +
                "<alinea xml:id=\"art487-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "            <article xml:id=\".+\" leos:editable=\"true\"  leos:deletable=\"true\">              <num leos:editable=\"false\">Article 488</num>              "
                +
                "<heading>3th article</heading>              <paragraph xml:id=\".+-par1\">                <num>1.</num>"
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
                + "<part xml:id=\"part1\">"
                + "<num>Part 1</num>"
                + "<heading>part1</heading>"
                + "<section xml:id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article xml:id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<article xml:id=\"art487\">" +
                "<num class=\"ArticleNumber\">Article 487</num>" +
                "<heading>2de article</heading>" +
                "<alinea xml:id=\"art487-aln1\">bla bla</alinea>" +
                "</article>"
                + "</section>"
                + "</part>"
                + "</body></bill></akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO bodyVO = new TableOfContentItemVO(LegalTextProposalTocItemType.BODY, "body", null, null, null, null, null, null, 3, null);
        tableOfContentItemVOList.add(bodyVO);

        TableOfContentItemVO part1 = new TableOfContentItemVO(LegalTextProposalTocItemType.PART, "part1", null, "Part 1", null, "part1", null, null, 4, null);
        TableOfContentItemVO sec1 = new TableOfContentItemVO(LegalTextProposalTocItemType.SECTION, "sect1", null, "Section 1", null, "Paragraphs", null, null, 11, null);
        TableOfContentItemVO art1 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art486", null, "Article 486", null, "1ste article", null, null,
                18, null);
        TableOfContentItemVO part2 = new TableOfContentItemVO(LegalTextProposalTocItemType.PART, null, null, "Part 2", null, "part2", null, null, null, null);
        TableOfContentItemVO sec2 = new TableOfContentItemVO(LegalTextProposalTocItemType.SECTION, null, null, "Section 2", null, "Paragraphs", null, null, null, null);
        TableOfContentItemVO art2 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, "art487", null, "Article 487", null, "2de article",
                null, null, 29, null);
        TableOfContentItemVO art3 = new TableOfContentItemVO(LegalTextProposalTocItemType.ARTICLE, null, null, "488", null, "3th article", null, null, null, null);

        bodyVO.addChildItem(part1);
        part1.addChildItem(sec1);
        sec1.addChildItem(art1);
        bodyVO.addChildItem(part2);
        part2.addChildItem(sec2);
        sec2.addChildItem(art2);
        sec2.addChildItem(art3);

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextProposalTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes(), getTestUser());

        String expected = "<akomaNtoso><bill><body>"
                + "<part xml:id=\"part1\">"
                + "<num>Part 1</num>"
                + "<heading>part1</heading>"
                + "<section xml:id=\"sect1\">"
                + "<num>Section 1</num>"
                + "<heading>Paragraphs</heading>"
                + "<article xml:id=\"art486\">"
                +
                "<num>Article 486</num>"
                +
                "<heading>1ste article</heading>"
                +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "</section>"
                + "</part>"
                + "<part xml:id=\".+\">"
                + "<num>Part 2</num>"
                + "<heading>part2</heading>"
                + "<section xml:id=\".+\">"
                + "<num>Section 2</num>"
                + "<heading>Paragraphs</heading>"
                + "<article xml:id=\"art487\">"
                +
                "<num>Article 487</num>"
                +
                "<heading>2de article</heading>"
                +
                "<alinea xml:id=\"art487-aln1\">bla bla</alinea>"
                +
                "</article>"
                + "            <article xml:id=\".+\" leos:editable=\"true\"  leos:deletable=\"true\">              <num leos:editable=\"false\">Article 488</num>              "
                +
                "<heading>3th article</heading>              <paragraph xml:id=\".+-par1\">                <num>1.</num>"
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
                "<part xml:id=\"part1\">" +
                "<num>Part I</num>" +
                "<heading>LEOS (Proof-Of-Concept)</heading>" +
                "<title xml:id=\"titl1\">" +
                "<num>Title I</num>" +
                "<heading>Example Document</heading>" +
                "<chapter xml:id=\"chap1\">" +
                "</chapter>" +
                "</title>" +
                "</part>" +
                "<hcontainer name=\"application\" xml:id=\"app1\">" +
                "<content xml:id=\"con\">" +
                "<p class=\"DirectApplication\">This Regulation shall be binding in its entirety and directly applicable in all Member States.</p>" +
                "<p class=\"DirectApplication\">(A possible extension of the direct application.)</p>" +
                "</content>" +
                "</hcontainer>" +
                "</body>" +
                "</bill>" +
                "</akomaNtoso>";

        List<TableOfContentItemVO> tableOfContentItemVOList = new ArrayList<TableOfContentItemVO>();
        TableOfContentItemVO body1 = new TableOfContentItemVO(LegalTextProposalTocItemType.BODY, "body1", null, null, null, null, null, null, 3, null);
        TableOfContentItemVO part1 = new TableOfContentItemVO(LegalTextProposalTocItemType.PART, "part1", null, "Part I", null, "LEOS (Proof-Of-Concept)", null,
                null, 6, null);
        TableOfContentItemVO title1 = new TableOfContentItemVO(LegalTextProposalTocItemType.TITLE, "titl1", null, "Title I", null, "Example Document", null, null,
                13, null);
        TableOfContentItemVO ch1 = new TableOfContentItemVO(LegalTextProposalTocItemType.CHAPTER, "chap1", null, null, null, null, null, null, 20, null);

        tableOfContentItemVOList.add(body1);
        body1.addChildItem(part1);
        part1.addChildItem(title1);
        title1.addChildItem(ch1);
        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextProposalTocItemType::getTocItemTypeFromName, tableOfContentItemVOList, xml.getBytes(), getTestUser());

        String expected = "<akomaNtoso><bill>" +
                "<body id =\"body1\">" +
                "<part xml:id=\"part1\">" +
                "<num>Part I</num>" +
                "<heading>LEOS (Proof-Of-Concept)</heading>" +
                "<title xml:id=\"titl1\">" +
                "<num>Title I</num>" +
                "<heading>Example Document</heading>" +
                "<chapter xml:id=\"chap1\">" +
                "</chapter>" +
                "</title>" +
                "</part>" +
                "<hcontainer name=\"application\" xml:id=\"app1\">" +
                "<content xml:id=\"con\">" +
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
                + "<article xml:id=\"art486\">" +
                "<num>Article 486</num>" +
                "<heading>1ste article</heading>" +
                "<alinea xml:id=\"art486-aln1\">bla bla</alinea>" +
                "</article>"
                + "<hcontainer><content><p>test</p></content>"
                + "</hcontainer>"
                + "</blabla></akomaNtoso>";

        byte[] result = vtdXmlContentProcessor.createDocumentContentWithNewTocList(LegalTextProposalTocItemType::getTocItemTypeFromName, Collections.<TableOfContentItemVO>emptyList(), xml.getBytes(), getTestUser());

        assertThat(new String(result), is(xml));
    }

    @Test
    public void test_renumberArticle() {
        String xml = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"
                + "<akomaNtoso xml:id=\"akn\">" +
                "<article xml:id=\"art486\">" +
                "<num xml:id=\"aknum\">Article 486</num>" +
                "<heading xml:id=\"aknhead\">1st article</heading>" +
                "</article>" +
                "<article xml:id=\"art486\">" +
                "<num xml:id=\"aknnum2\"></num>" +
                "<heading xml:id=\"aknhead2\">2th articl<authorialNote marker=\"101\" xml:id=\"a1\"><p xml:id=\"p1\">TestNote1</p></authorialNote>e</heading>" +
                "</article>" +
                "<article xml:id=\"art486\">" +
                "<heading xml:id=\"aknhead3\">3th article<authorialNote marker=\"90\" xml:id=\"a2\"><p xml:id=\"p2\">TestNote2</p></authorialNote></heading>" +
                "</article>" +
                "<article xml:id=\"art486\">" +
                "</article>" +
                "</akomaNtoso>";
        
        when(messageHelper.getMessage("legaltext.article.num", new Object[]{1L})).thenReturn("Article 1");
        when(messageHelper.getMessage("legaltext.article.num", new Object[]{2L})).thenReturn("Article 2");
        when(messageHelper.getMessage("legaltext.article.num", new Object[]{3L})).thenReturn("Article 3");
        when(messageHelper.getMessage("legaltext.article.num", new Object[]{4L})).thenReturn("Article 4");

        byte[] result = proposalNumberingProcessor.renumberArticles(xml.getBytes(), "fr");
        result = vtdXmlContentProcessor.doXMLPostProcessing(result);
        

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"
                + "<akomaNtoso xml:id=\"akn\">" +
                "<article xml:id=\"art486\">" +
                "<num>Article 1</num>" +
                "<heading xml:id=\"aknhead\">1st article</heading>" +
                "</article>" +
                "<article xml:id=\"art486\">" +
                "<num>Article 2</num>" +
                "<heading xml:id=\"aknhead2\">2th articl<authorialNote marker=\"1\" xml:id=\"a1\"><p xml:id=\"p1\">TestNote1</p></authorialNote>e</heading>" +
                "</article>" +
                "<article xml:id=\"art486\">" +
                "<num>Article 3</num>" +
                "<heading xml:id=\"aknhead3\">3th article<authorialNote marker=\"2\" xml:id=\"a2\"><p xml:id=\"p2\">TestNote2</p></authorialNote></heading>" +
                "</article>" +
                "<article xml:id=\"art486\">" +
                "<num>Article 4</num>" +
                "</article>" +
                "</akomaNtoso>";

        assertThat(new String(result).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_manualRenumberingArticle() {
        String xml = "<!--This AkomaNtoso document was created via a LegisWrite export.-->" +
                "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\" xml:id=\"akn\">" +
                "<body leos:origin=\"ec\" xml:id=\"body\">" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num leos:origin=\"cn\">Article #</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num leos:origin=\"cn\">Article #</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num  leos:origin=\"cn\">Article #</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"ec\">" +
                "<num  leos:origin=\"ec\">Article 3</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num  leos:origin=\"cn\">Article #</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"ec\">" +
                "<num  leos:origin=\"ec\">Article 5</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num  leos:origin=\"cn\">Article #</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num leos:origin=\"cn\">Article #</num>" +
                "</article>" +
                "</body>" +
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes(), "fr");

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"
                + "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\" xml:id=\"akn\">" +
                "<body leos:origin=\"ec\" xml:id=\"body\">" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num leos:origin=\"cn\">Article -3</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num leos:origin=\"cn\">Article -2</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num  leos:origin=\"cn\">Article -1</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"ec\">" +
                "<num  leos:origin=\"ec\">Article 3</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num  leos:origin=\"cn\">Article 3a</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"ec\">" +
                "<num  leos:origin=\"ec\">Article 5</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num  leos:origin=\"cn\">Article 5a</num>" +
                "</article>" +
                "<article xml:id=\"art486\" leos:origin=\"cn\">" +
                "<num leos:origin=\"cn\">Article 5b</num>" +
                "</article>" +
                "</body>" +
                "</akomaNtoso>";

        assertThat(new String(result).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_manualRenumberingRecitals() {
        String xml = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"+
                "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\" xml:id=\"akn\">" +
                "<recitals xml:id=\"recs\" leos:editable=\"false\" leos:origin=\"ec\">"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num xml:id=\"recs_WiyG0V\"  leos:origin=\"cn\">(#)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...-3</p>"+
                "</recital>"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num xml:id=\"recs_WiyG0V\" leos:origin=\"cn\">(#)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...-2</p>"+
                "</recital>"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num xml:id=\"recs_WiyG0V\" leos:origin=\"cn\">(#)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...-1</p>"+
                "</recital>"+
                "<recital xml:id=\"rec_1\" leos:editable=\"true\" leos:origin=\"ec\">"+
                "<num xml:id=\"rec_1__num\" leos:origin=\"ec\">(1)</num>"+
                "<p xml:id=\"rec_1__p\" leos:origin=\"ec\">Regulation (EC) No 91/2003 of the European Parliament and of the Council</p>"+
                "</recital>"+
                "<recital xml:id=\"imp_rec_d1e89_s3AqGd\" leos:editable=\"true\" leos:origin=\"ec\">"+
                "<num xml:id=\"recs_clDXkw\" leos:origin=\"ec\">(2)</num>"+
                "<p xml:id=\"recs_vY7Jn8\" leos:origin=\"ec\">Regulation (EC) No 91/2003 of the European Parliament and of the Council</p>     "+
                "</recital>"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num xml:id=\"recs_WiyG0V\" leos:origin=\"cn\">(#)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...2a</p>"+
                "</recital>"+
                "<recital xml:id=\"imp_rec_d1e89_quAXDO\" leos:editable=\"true\" leos:origin=\"ec\">"+
                "<num xml:id=\"recs_o4jNQj\" leos:origin=\"ec\">(3)</num>"+
                "<p xml:id=\"recs_hAbYkK\" leos:origin=\"ec\">Regulation (EC) No 91/2003 of the European Parliament and of the Council </p>"+
                "</recital>"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num xml:id=\"recs_WiyG0V\"  leos:origin=\"cn\">(#)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...3a</p>"+
                "</recital>"+
                "<recital xml:id=\"recs_Lt8Khg\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num xml:id=\"recs_nRwj01\"  leos:origin=\"cn\">(#)</num>"+
                "<p xml:id=\"recs_m67lb6\">blah blah blah....3b</p>"+
                "</recital>"+
                "</recitals>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberRecitals(xml.getBytes());

        String expected = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"
                +"<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\" xml:id=\"akn\">" +
                "<recitals xml:id=\"recs\" leos:editable=\"false\" leos:origin=\"ec\">"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num>(-3)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...-3</p>"+
                "</recital>"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num>(-2)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...-2</p>"+
                "</recital>"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num>(-1)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...-1</p>"+
                "</recital>"+
                "<recital xml:id=\"rec_1\" leos:editable=\"true\" leos:origin=\"ec\">"+
                "<num>(1)</num>"+
                "<p xml:id=\"rec_1__p\" leos:origin=\"ec\">Regulation (EC) No 91/2003 of the European Parliament and of the Council</p>"+
                "</recital>"+
                "<recital xml:id=\"imp_rec_d1e89_s3AqGd\" leos:editable=\"true\" leos:origin=\"ec\">"+
                "<num>(2)</num>"+
                "<p xml:id=\"recs_vY7Jn8\" leos:origin=\"ec\">Regulation (EC) No 91/2003 of the European Parliament and of the Council</p>     "+
                "</recital>"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num>(2a)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...2a</p>"+
                "</recital>"+
                "<recital xml:id=\"imp_rec_d1e89_quAXDO\" leos:editable=\"true\" leos:origin=\"ec\">"+
                "<num>(3)</num>"+
                "<p xml:id=\"recs_hAbYkK\" leos:origin=\"ec\">Regulation (EC) No 91/2003 of the European Parliament and of the Council </p>"+
                "</recital>"+
                "<recital xml:id=\"recs_jmIjdF\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num>(3a)</num>"+
                "<p xml:id=\"recs_ILXjvV\">blah blah blah...3a</p>"+
                "</recital>"+
                "<recital xml:id=\"recs_Lt8Khg\" leos:editable=\"true\" leos:origin=\"cn\">"+
                "<num>(3b)</num>"+
                "<p xml:id=\"recs_m67lb6\">blah blah blah....3b</p>"+
                "</recital>"+
                "</recitals>"+
                "</akomaNtoso>";

        assertThat(new String(result).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }


    @Test
    public void test_manualRenumberingParagraphs() {


        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"cn\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"cn\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_1\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__a\">aaaa</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_1\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__b\">bbbbb</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para2</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_1\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__c\">cccc</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para3</p>"+
                "</content>"+
                "</paragraph>"+
                "</article>"+
                "<article leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"akn_art_YnSKbj_N7bQVT\">Article 2</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_dqTi7R\">Article heading...</heading>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_x\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">xxxxxxx</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_y\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">yyyyyyyy</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_zrk0Mz\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_akUEgv\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_RmxNQr\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_UqAJE4\">ergregregerg</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_yiJDrd\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_alkc60\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_yR3ne2\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_ayJnjd\">tgtrg</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_y\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">xxxxxxx</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_y\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">aaaaaaaa</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_CsZvQu\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_3nkQqe\">2.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_RmxNQr\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_UqAJE4\">ergregregerg</p>"+
                "</content>"+
                "</subparagraph>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_z\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">xxxxxxx</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes(),"fr");

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"cn\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"cn\" leos:editable=\"false\" xml:id=\"art_1__num\">Article -1</num>"+
                "<heading leos:origin=\"cn\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_1\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__a\">1.</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_1\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__b\">2.</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para2</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_1\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__c\">3.</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para3</p>"+
                "</content>"+
                "</paragraph>"+
                "</article>"+
                "<article leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"akn_art_YnSKbj_N7bQVT\">Article 2</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_dqTi7R\">Article heading...</heading>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_x\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">-2.</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_y\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">-1.</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_zrk0Mz\" >"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_akUEgv\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_RmxNQr\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_UqAJE4\">ergregregerg</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(-b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(-a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(aa)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_yiJDrd\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_alkc60\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_yR3ne2\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_ayJnjd\">tgtrg</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(ba)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(bb)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_y\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">1a.</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_y\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">1b.</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_CsZvQu\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_3nkQqe\">2.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_RmxNQr\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_UqAJE4\">ergregregerg</p>"+
                "</content>"+
                "</subparagraph>"+
                "</paragraph>"+
                "<paragraph leos:origin=\"cn\" xml:id=\"art_1__para_z\">"+
                "<num leos:origin=\"cn\" xml:id=\"art_1__para_1__num\">2a.</num>"+
                "<content leos:origin=\"cn\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"cn\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }


    
    @Test
    public void test_elementNumbering_when_mandate_list_added_to_proposal_paragraph() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes(),"fr");

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }
    
    @Test
    public void test_elementNumbering_when_nested_list_added() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes(),"fr");

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(1)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(2)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }
    
    @Test
    public void test_pointsNumbering_when_point_having_single_level_nested_list() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes(),"fr");

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(1)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(2)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(1)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(2)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_pointsNumbering_when_point_having_multi_level_nested_list_1() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes(),"fr");

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(1)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(i)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(2)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(i)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }
    
    @Test
    public void test_pointsNumbering_when_point_having_multi_level_nested_list_2() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes(),"fr");

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(1)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(2)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(1)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(1)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(i)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(2)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(2)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }
    
    @Test
    public void test_RenumberingPoints_with_nested_lists() {


        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"akn_art_YnSKbj_N7bQVT\">Article 2</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_dqTi7R\">Article heading...</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_zrk0Mz\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_akUEgv\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_RmxNQr\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_UqAJE4\">ergregregerg</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_yiJDrd\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_alkc60\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_yR3ne2\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_ayJnjd\">tgtrg</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes(),"fr");

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"akn_art_YnSKbj_N7bQVT\">Article 2</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_dqTi7R\">Article heading...</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_zrk0Mz\" >"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_akUEgv\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_RmxNQr\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_UqAJE4\">ergregregerg</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(-b)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(-a)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(aa)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_yiJDrd\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_alkc60\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_yR3ne2\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_ayJnjd\">tgtrg</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(ba)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(bb)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">gregtgtghrt</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_pointsNumbering_when_point_added_on_negative_side() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(-a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes(),"fr");

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(-a)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(-a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }
    
    @Test
    public void test_pointsNumbering_when_point_added_between_proposal_point() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(aa)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(ba)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes(),"fr");

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(aa)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(aa)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(ba)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(ba)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_pointsNumbering_when_nested_point_added_to_proposal_point() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(i)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">-</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point-</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">-</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point-</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(i)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes(),"fr");

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(i)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">-</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point-</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">-</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point-</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(i)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(i)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_pointsNumbering_when_nested_point_added_to_mandate_point() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(aa)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(aa)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(aa1)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" leos:affected=\"true\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(ba)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(ba)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(ba1)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes(),"fr");

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<num leos:origin=\"ec\" xml:id=\"art_1__para_1__b\">1.</num>"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(aa)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(aa)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(1)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(aa1)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(b)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\" >"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(ba)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(ba)</p>"+
                "</content>"+
                "<list leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(1)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(ba1)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
    }

    @Test
    public void test_pointsNumbering_when_point_added_to_unnumbered_paragraph() {

        String xml = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" leos:affected=\"true\">"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" leos:affected=\"true\">"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">#</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        byte[] result = mandateNumberingProcessor.renumberArticles(xml.getBytes(),"fr");

        String expected = "<akomaNtoso xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">"+
                "<bill name=\"regulation\">"+
                "<body leos:origin=\"ec\" xml:id=\"body\">"+
                "<article leos:origin=\"ec\" xml:id=\"art_1\" leos:editable=\"false\" leos:deletable=\"false\" >"+
                "<num leos:origin=\"ec\" leos:editable=\"false\" xml:id=\"art_1__num\">Article 1</num>"+
                "<heading leos:origin=\"ec\" xml:id=\"art_1__heading\">Scope</heading>"+
                "<paragraph leos:origin=\"ec\" xml:id=\"art_1__para_1\" >"+
                "<subparagraph leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_oNh4Vl\">"+
                "<content leos:origin=\"ec\" xml:id=\"art_1__para_1__content\">"+
                "<p leos:origin=\"ec\" xml:id=\"art_1__para_1__content__p\">Para1</p>"+
                "</content>"+
                "</subparagraph>"+
                "<list leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_gvmXCy\">"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(a)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_soCjiB\">(b)</num>"+
                "<content leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"ec\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "<point leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_LA9Azw\">"+
                "<num leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_soCjiB\">(ba)</num>"+
                "<content leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_89xvgD\">"+
                "<p leos:origin=\"cn\" xml:id=\"akn_art_YnSKbj_g319GD\">point(a)</p>"+
                "</content>"+
                "</point>"+
                "</list>"+
                "</paragraph>"+
                "</article>"+
                "</body>"+
                "</bill>"+
                "</akomaNtoso>";

        assertThat(new String(result, UTF_8).replaceAll("<num(\\s)*?xml:id=\".+?\"(\\s)*?>", "<num>"), is(expected));
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

        Pattern pattern = Pattern.compile("xml:id=");
        Matcher matcher = pattern.matcher(new String(result));

        assertThat("id= " + " should be found in " + new String(result), matcher.find(), is(true));
    }

    @Test
    public void test_injectIdMethodShould_notInjectNewID() {
        String xml = "<p xml:id=\"PQR\">xxx</p>";
        String expected = "<p xml:id=\"PQR\">xxx</p>";
        byte[] result = vtdXmlContentProcessor.injectTagIdsinXML(xml.getBytes());

        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_updateReferedAttributes(){
        String xml      = "<xyz>" +
                "<p xml:id=\"PQR\" refersTo=\"~ABCD\">xxx</p>" +//shd be updated
                "<p xml:id=\"PQR2\" refersTo=\"~ABCD1\">xxx</p>" +
                "<b><p xml:id=\"LMN\" refersTo=\"~ABCD\">xxx</p></b>" +//shd be updated
                "<p xml:id=\"PQR3\" refersTo=\"ABCD\">xxx</p>" +
                "<p xml:id=\"PQR4\">xxx</p>" +
                "</xyz>";
        String expected = "<xyz>" +
                "<p xml:id=\"PQR\" refersTo=\"~ABCD\">newValue</p>" +
                "<p xml:id=\"PQR2\" refersTo=\"~ABCD1\">xxx</p>" +
                "<b><p xml:id=\"LMN\" refersTo=\"~ABCD\">newValue</p></b>" +
                "<p xml:id=\"PQR3\" refersTo=\"ABCD\">xxx</p>" +
                "<p xml:id=\"PQR4\">xxx</p>" +
                "</xyz>";
        HashMap hm= new HashMap<String, String>();
        hm.put("ABCD", "newValue");
        byte[] result = vtdXmlContentProcessor.updateReferedAttributes(xml.getBytes(),hm);
        assertThat(new String(result), is(expected));
    }

    @Test
    public void test_removeElements_one() throws ParseException {

        // setup
        String xml = "<meta xml:id=\"ElementId\" xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">" +
                "<temp xml:id=\"xyz\"" +
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
        assertThat(new String(result,UTF_8) , equalTo("<meta xml:id=\"ElementId\" xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\"></meta>"));
    }

    @Test
    public void test_removeElements_multiple() throws ParseException {

        // setup
        String xml = "<meta xml:id=\"ElementId\" xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">" +
                "<temp xml:id=\"xyz\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userId=\"user1\"" +
                " leos:userName=\"User One\"" +  " leos:dg=\"DIGIT.B2.001\"" +
                " leos:dateTime=\"2015-05-29T11:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</temp>" +
                "<temp xml:id=\"xyz1\"" +
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
        assertThat(new String(result,UTF_8) , equalTo("<meta xml:id=\"ElementId\" xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\"></meta>"));
    }

    @Test
    public void test_removeElements_withOneParent() throws ParseException {

        // setup
        String xml = "<meta xml:id=\"ElementId\" xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">" +
                "<parent>"+
                "<temp xml:id=\"xyz\"" +
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
        assertThat(new String(result,UTF_8) , equalTo("<meta xml:id=\"ElementId\" xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\"></meta>"));
    }

    @Test
    public void test_removeElements_withMultipleParent() throws ParseException {

        // setup
        String xml = "<meta xml:id=\"ElementId\">" +
                "<parent>"+
                "<temp xml:id=\"xyz\"" +" refersTo=\"~leosComment\">" +
                    "<p refersTo=\"~leosChild\">This is a comment...</p>" +
                "</temp>" +
                "</parent>"+
                "<parent1>"+
                "<temp xml:id=\"xyz1\"" +" refersTo=\"~leosComment\">" +
                    "<p refersTo=\"~leosChild\">This is a comment1...</p>" +
                "</temp>" +
                "</parent1>"+
                "</meta>";


        //make the actual call
        byte[] result = vtdXmlContentProcessor.removeElements(xml.getBytes(UTF_8), "//*[@refersTo=\"~leosChild\"]", 2);

        // verify
        assertThat(new String(result,UTF_8) , equalTo("<meta xml:id=\"ElementId\"></meta>"));
    }
    
    @Test
    public void test_getLastElementId() throws ParseException {
        String xPath = "/part/article[last()]";
        String elementId = vtdXmlContentProcessor.getElementIdByPath(docContent, xPath);
        
        //verify
        assertThat(elementId, equalTo("art486"));
    }

    @Test
    public void test_updateMultiRefs() throws Exception {
        // setup
        String xml = "<root xml:id=\"ElementId\">" +
                "<parent>" +
                "<p> bla bla" +
                "<mref xml:id=\"mref1\">" +
                "Article 1<ref xml:id=\"aid\" href=\"ref1\">(a)</ref> and <ref xml:id=\"bid\" href=\"ref2\">(b)</ref>" +
                "</mref>" +
                "more bla bla" +
                "<mref xml:id=\"mref2\">" +
                "Article 2<ref xml:id=\"aid2\" href=\"ref21\">(a)</ref> and <ref xml:id=\"bid2\" href=\"ref22\">(b)</ref>" +
                "</mref>" +
                " test bla bla</p>" +
                "</parent>" +
                "</root>";

        String expectedXml = "<root xml:id=\"ElementId\">" +
                "<parent>" +
                "<p> bla bla" +
                "<mref xml:id=\"mref1\">" +
                "Article X<ref xml:id=\"aid\" href=\"ref1\">updated ref for test onl</ref> and <ref xml:id=\"bid\" href=\"ref2\">(b)</ref>" +
                "</mref>" +
                "more bla bla" +
                "<mref xml:id=\"mref2\">" +
                "Article Y<ref xml:id=\"aid2\" href=\"ref21\">updated ref for test only</ref> and <ref xml:id=\"bid2\" href=\"ref22\">(b)</ref>" +
                "</mref>" +
                " test bla bla</p>" +
                "</parent>" +
                "</root>";

        VTDNav nav = VTDUtils.setupVTDNav(xml.getBytes(UTF_8), true);
        XMLModifier modifier = new XMLModifier(nav);
        when(referenceLabelProcessor.generateLabel(
                    (List<Ref>) argThat(containsInAnyOrder(new Ref("aid", "ref1"), new Ref("bid", "ref2"))),
                    argThat(any(String.class)),
                    argThat(any(VTDNav.class)))
        ).thenReturn(new Result<String>("Article X<ref xml:id=\"aid\" href=\"ref1\">updated ref for test onl</ref> and <ref xml:id=\"bid\" href=\"ref2\">(b)</ref>", null));

        when(referenceLabelProcessor.generateLabel(
                (List<Ref>) argThat(containsInAnyOrder(new Ref("aid2", "ref21"), new Ref("bid2", "ref22"))), argThat(any(String.class)),
                argThat(any(VTDNav.class))))
                .thenReturn(new Result<String>("Article Y<ref xml:id=\"aid2\" href=\"ref21\">updated ref for test only</ref> and <ref xml:id=\"bid2\" href=\"ref22\">(b)</ref>", null));

        //make the actual call
        vtdXmlContentProcessor.updateMultiRefs(modifier);

        // verify
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        modifier.output(baos);
        String result = baos.toString("UTF-8");
        assertEquals(expectedXml, result);
        verify(referenceLabelProcessor, times(2)).generateLabel(ArgumentMatchers.any(List.class), ArgumentMatchers.any(String.class), ArgumentMatchers.any(VTDNav.class));
    }

    @Test
    public void test_updateMultiRefs_one_ref() throws Exception {
        // setup
        String xml = "<root xmlns:leos=\"ec:leos\">" +
                    "<recitals xml:id=\"recs\" leos:editable=\"false\">" +
                        "<recital xml:id=\"rec_1\" leos:editable=\"true\"><num xml:id=\"rec_1__num\">(1)</num>" +
                            "<p xml:id=\"rec_1__p\">Recital...<mref xml:id=\"mrefid\"><ref xml:id=\"IDE\" href=\"art_1__para_1\">(1)</ref></mref></p>" +
                        "</recital>" +
                        "<recital xml:id=\"rec_2\" leos:editable=\"true\"><num xml:id=\"rec_2__num\">(2)</num>" +
                            "<p xml:id=\"rec_2__p\">Recital...</p>" +
                        "</recital>" +
                    "</recitals>"+
                "</root>";

        String expectedXml = "<root xmlns:leos=\"ec:leos\">" +
                    "<recitals xml:id=\"recs\" leos:editable=\"false\">" +
                        "<recital xml:id=\"rec_1\" leos:editable=\"true\"><num xml:id=\"rec_1__num\">(1)</num>" +
                            "<p xml:id=\"rec_1__p\">Recital...<mref xml:id=\"mrefid\">Article 1<ref xml:id=\"IDE\" href=\"art_1__para_1\">(1)</ref></mref></p>" +
                    "</recital>" +
                    "<recital xml:id=\"rec_2\" leos:editable=\"true\"><num xml:id=\"rec_2__num\">(2)</num>" +
                        "<p xml:id=\"rec_2__p\">Recital...</p>" +
                    "</recital>" +
                    "</recitals>"+
                "</root>";


        VTDNav nav = VTDUtils.setupVTDNav(xml.getBytes(UTF_8), true);
        XMLModifier modifier = new XMLModifier(nav);
        when(referenceLabelProcessor.generateLabel(
                (List<Ref>) argThat(containsInAnyOrder(new Ref("IDE", "art_1__para_1"))), argThat(any(String.class)),
                argThat(any(VTDNav.class))))
                .thenReturn(new Result<String>("Article 1<ref xml:id=\"IDE\" href=\"art_1__para_1\">(1)</ref>", null));

        //make the actual call
        vtdXmlContentProcessor.updateMultiRefs(modifier);

        // verify
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        modifier.output(baos);
        String result = baos.toString("UTF-8");
        assertEquals(expectedXml, result);
        verify(referenceLabelProcessor, times(1)).generateLabel(ArgumentMatchers.any(List.class), ArgumentMatchers.any(String.class), ArgumentMatchers.any(VTDNav.class));
    }

    @Test
    public void test_merge_suggestion_found_text() throws Exception {
        String xmlContent = "<bill>" +
                "<p xml:id=\"ElementId\">This is an example <i xml:id=\"testEltId\">of a replacement</i> text</p>" +
                "</bill>";
        String origText= "a";
        String newText = "the";
        String eltId = "testEltId";
        int start = 3;
        int end = 4;

        String expectedXmlContent = "<bill>" +
                "<p xml:id=\"ElementId\">This is an example <i xml:id=\"testEltId\">of the replacement</i> text</p>" +
                "</bill>";

        byte[] result = vtdXmlContentProcessor.replaceTextInElement(xmlContent.getBytes(), origText, newText, eltId, start, end);

        // verify
        assertEquals(expectedXmlContent, new String(result, UTF_8));
    }

    @Test
    public void test_merge_suggestion_two_tags_found_text() throws Exception {
        String xmlContent = "<bill>" +
                "<p xml:id=\"cit_5__p\">Having regard to the opinion of the Committee of the Regions<authorialNote xml:id=\"authorialnote_2\" marker=\"1\" placement=\"bottom\"><p xml:id=\"authorialNote_2__p\">OJ C [...], [...], p. [...]</p></authorialNote>,</p>" +
                "</bill>";
        String origText= "the Committee of the Regions";
        String newText = "the Committee of the Countries";
        String eltId = "cit_5__p";
        int start = 32;
        int end = 60;

        String expectedXmlContent = "<bill>" +
                "<p xml:id=\"cit_5__p\">Having regard to the opinion of the Committee of the Countries<authorialNote xml:id=\"authorialnote_2\" marker=\"1\" placement=\"bottom\"><p xml:id=\"authorialNote_2__p\">OJ C [...], [...], p. [...]</p></authorialNote>,</p>" +
                "</bill>";

        byte[] result = vtdXmlContentProcessor.replaceTextInElement(xmlContent.getBytes(), origText, newText, eltId, start, end);

        // verify
        assertEquals(expectedXmlContent, new String(result, UTF_8));
    }

    @Test
    public void test_merge_suggestion_two_tags_wrong_id() throws Exception {
        String xmlContent = "<bill>" +
                "<p xml:id=\"cit_5__p\">Having regard to the opinion of the Committee of the Regions<authorialNote xml:id=\"authorialnote_2\" marker=\"1\" placement=\"bottom\"><p xml:id=\"authorialNote_2__p\">OJ C [...], [...], p. [...]</p></authorialNote>,</p>" +
                "</bill>";
        String origText= "the Committee of the Regions";
        String newText = "the Committee of the Countries";
        String eltId = "cit_5__";
        int start = 32;
        int end = 60;

        String expectedXmlContent = "<bill>" +
                "<p xml:id=\"cit_5__p\">Having regard to the opinion of the Committee of the Countries<authorialNote xml:id=\"authorialnote_2\" marker=\"1\" placement=\"bottom\"><p xml:id=\"authorialNote_2__p\">OJ C [...], [...], p. [...]</p></authorialNote>,</p>" +
                "</bill>";

        byte[] result = vtdXmlContentProcessor.replaceTextInElement(xmlContent.getBytes(), origText, newText, eltId, start, end);

        // verify
        assertTrue(result == null);
    }

    @Test
    public void test_merge_suggestion_two_tags_wrong_text() throws Exception {
        String xmlContent = "<bill>" +
                "<p xml:id=\"cit_5__p\">Having regard to the opinion of the Committee of the Regions<authorialNote xml:id=\"authorialnote_2\" marker=\"1\" placement=\"bottom\"><p xml:id=\"authorialNote_2__p\">OJ C [...], [...], p. [...]</p></authorialNote>,</p>" +
                "</bill>";
        String origText= "the Committee of the Region";
        String newText = "the Committee of the Countries";
        String eltId = "cit_5__";
        int start = 32;
        int end = 60;

        String expectedXmlContent = "<bill>" +
                "<p xml:id=\"cit_5__p\">Having regard to the opinion of the Committee of the Countries<authorialNote xml:id=\"authorialnote_2\" marker=\"1\" placement=\"bottom\"><p xml:id=\"authorialNote_2__p\">OJ C [...], [...], p. [...]</p></authorialNote>,</p>" +
                "</bill>";

        byte[] result = vtdXmlContentProcessor.replaceTextInElement(xmlContent.getBytes(), origText, newText, eltId, start, end);

        // verify
        assertTrue(result == null);
    }

    @Test
    public void test_getParentElement_should_returnParentElement_when_childIdFound() throws Exception {
        String[] element = vtdXmlContentProcessor.getParentElement( docContent, "content", "c3");
        assertThat(
                element[2],
                is("<alinea xml:id=\"art486-aln1\">"
                        +
                        "                        <content xml:id=\"c3\">"
                        +
                        "                            <p xml:id=\"p3\" class=\"Paragraph(unnumbered)\">This Regulation shall enter into force on the day following that of its publication in the <i xml:id=\"i2\">Official Journal of the European<authorialNote marker=\"8\" xml:id=\"a3\"><p>TestNote3</p></authorialNote> Union</i>.</p>"
                        +
                        "                        </content>" +
                        "                    </alinea>"));
    }

    @Test
    public void test_getParentElementId_should_returnParentElementId_when_childIdFound() throws Exception {
        String elementId = vtdXmlContentProcessor.getParentElementId( docContent, "content", "c3");
        assertThat(
                elementId,
                is("art486-aln1"));
    }

    @Test
    public void test_getParentElement_should_returnNull_when_childIdNotFound() throws Exception {
    	String[] element = vtdXmlContentProcessor.getParentElement( docContent, "content", "c3333333");
        assertThat(element, is(nullValue()));
    }

    @Test
    public void test_getParentElementId_should_returnNull_when_childIdNotFound() throws Exception {
        String elementId = vtdXmlContentProcessor.getParentElementId( docContent, "content", "c333333");
        assertThat(elementId, is(nullValue()));
    }
}

