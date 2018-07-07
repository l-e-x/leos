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
package eu.europa.ec.leos.support;

import eu.europa.ec.leos.support.xml.XmlCommentProcessorImpl;
import eu.europa.ec.leos.vo.CommentVO;
import eu.europa.ec.leos.vo.CommentVO.RefersTo;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class XmlCommentProcessorImplTest {

    private XmlCommentProcessorImpl xmlCommentProcessorImpl = new XmlCommentProcessorImpl();
    private static SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssX");
    @Test
    public void testCreateXmlFor_SingleComment() throws ParseException {

        // setup
        CommentVO commentVOExpected = new CommentVO("xyz", "ElementId", "<p>This is a comment...</p>", "User One", "user1","AGRI.B1",
                sdf.parse("2015-05-29T16:30:00-0200"), RefersTo.LEOS_COMMENT);

        String xml = "<meta id=\"ElementId\">" +
                "<popup id=\"xyz\"" +
                " refersTo	=\"~leosComment\"" +
                " leos:userid=\"user1\"" +" leos:dg=\"AGRI.B1\"" +
                " leos:username=\"User One\"" +
                " leos:datetime=\"2015-05-29T16:30:00-0200\">" +
                "<p>This is a comment...</p>" +
                "</popup>" +
                "</meta>";

        // actual test
        List<CommentVO> results = xmlCommentProcessorImpl.fromXML(xml);

        // verify
        CommentVO result = results.get(0);
        assertThat(results.size(), equalTo(1));
        assertThat(result.getId(), equalTo(commentVOExpected.getId()));
        assertThat(result.getEnclosingElementId(), equalTo(commentVOExpected.getEnclosingElementId()));
        assertThat(result.getComment(), equalTo(commentVOExpected.getComment()));
        assertThat(result.getAuthorId(), equalTo(commentVOExpected.getAuthorId()));
        assertThat(result.getTimestamp(), equalTo(commentVOExpected.getTimestamp()));
        assertThat(result.getAuthorName(), equalTo(commentVOExpected.getAuthorName()));
        assertThat(result.getDg(), equalTo(commentVOExpected.getDg()));

    }
    @Test
    public void testCreateXmlFor_NoComment() throws ParseException {

        // setup
        String xml = "<meta id=\"ElementId\">" +
                "<temp id=\"xyz\"" +
                " refersTo	=\"~leosComment\"" +
                " leos:userid=\"user1\"" +
                " leos:username=\"User One\"" +" leos:dg=\"AGRI.B1\"" +
                " leos:datetime=\"2015-05-29T11:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</temp>" +
                "</meta>";

        // actual test
        List<CommentVO> results = xmlCommentProcessorImpl.fromXML(xml);

        // verify
        assertThat(results.size(), equalTo(0));
    }
    // care: ElementId in VO is top element ID.Inner wrappers are not considered, 
    @Test
    public void testCreateXmlForComment_comment_present_deep() throws ParseException {
        // setup
        CommentVO commentVOExpected = new CommentVO("xyz", "ElementId", "<p>This is a comment...</p>", "User One", "user1","AGRI.B1",
                sdf.parse("2015-05-29T11:30:00Z"), RefersTo.LEOS_COMMENT);

        String xml = "<meta id=\"ElementId\"><x id=\"xx\">" +
                "<popup id=\"xyz\"" +
                " refersTo	=\"~leosComment\"" +
                " leos:userid=\"user1\"" +
                " leos:username=\"User One\"" +" leos:dg=\"AGRI.B1\"" +
                " >" + // dont check date as it is auto generated
                "<p>This is a comment...</p>" +
                "</popup></x>" +
                "</meta>";
        // actual test
        List<CommentVO> results = xmlCommentProcessorImpl.fromXML(xml);

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
    public void testCreateXmlForMultipleComment() throws ParseException {

        // setup
        CommentVO commentVOExpected = new CommentVO("xyz", "ElementId", "<p>This is a comment...</p>", "User One", "user1","AGRI.B1",
                sdf.parse("2015-05-29T11:30:00Z"), RefersTo.LEOS_COMMENT);
        // setup
        CommentVO commentVOExpected2 = new CommentVO("xyz2", "ElementId", "<p>This is a comment...2</p>", "User One2", "user2","AGRI.B2",
                sdf.parse("2015-05-30T11:30:00Z"), RefersTo.LEOS_COMMENT);
        // setup
        CommentVO commentVOExpected3 = new CommentVO("xyz3", "ElementId", "<p>This is a comment...3</p>", "User One3", "user3",null,
                sdf.parse("2015-06-29T11:30:00Z"), RefersTo.LEOS_COMMENT);

        
        String xml = "<meta id=\"ElementId\">" +
                "<popup id=\"xyz\"" +
                " refersTo	=\"~leosComment\"" +
                " leos:userid=\"user1\"" +
                " leos:username=\"User One\"" +" leos:dg=\"AGRI.B1\"" +
                " leos:datetime=\"2015-05-29T11:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</popup>" +
                "<popup id=\"xyz2\"" +
                " refersTo	=\"~leosComment\"" +
                " leos:userid=\"user2\"" +
                " leos:username=\"User One2\"" +" leos:dg=\"AGRI.B2\"" +
                " leos:datetime=\"2015-05-30T11:30:00Z\">" +
                "<p>This is a comment...2</p>" +
                "</popup>" +
                "<popup id=\"xyz3\"" +
                " refersTo	=\"~leosComment\"" +
                " leos:userid=\"user3\"" +
                " leos:username=\"User One3\"" +
                " leos:datetime=\"2015-06-29T11:30:00Z\">" +
                "<p>This is a comment...3</p>" +
                "</popup>" +
                "</meta>";

        // actual test
        List<CommentVO> results = xmlCommentProcessorImpl.fromXML(xml);

        // verify
        
        assertThat(results.size(), equalTo(3));
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
        assertThat(result.getId(), equalTo(commentVOExpected3.getId()));
        assertThat(result.getEnclosingElementId(), equalTo(commentVOExpected3.getEnclosingElementId()));
        assertThat(result.getComment(), equalTo(commentVOExpected3.getComment()));
        assertThat(result.getAuthorId(), equalTo(commentVOExpected3.getAuthorId()));
        assertThat(result.getTimestamp(), equalTo(commentVOExpected3.getTimestamp()));
        assertThat(result.getAuthorName(), equalTo(commentVOExpected3.getAuthorName()));
        assertThat(result.getDg(), equalTo(commentVOExpected3.getDg()));

    }
    @Test
    public void testCreateXmlFor_MultipleComment_diffLevel() throws ParseException {

        // setup
        CommentVO commentVOExpected = new CommentVO("xyz", "ElementId", "<p>This is a comment...</p>", "User One", "user1","AGRI.B1",
                sdf.parse("2015-05-29T11:30:00Z"), RefersTo.LEOS_COMMENT);
        // setup
        CommentVO commentVOExpected2 = new CommentVO("xyz2", "ElementId", "<p>This is a comment...2</p>", "User One2", "user2","AGRI.B1",
                sdf.parse("2015-05-30T11:30:00Z"), RefersTo.LEOS_COMMENT);
        // setup
        CommentVO commentVOExpected3 = new CommentVO("xyz3", "ElementId", "<p>This is a comment...3</p>", "User One3", "user3","AGRI.B3",
                sdf.parse("2015-06-29T11:30:00Z"), RefersTo.LEOS_COMMENT);

        
        String xml = "<meta id=\"ElementId\">" +
                "<popup id=\"xyz\"" +
                " refersTo	=\"~leosComment\"" +
                " leos:userid=\"user1\"" +
                " leos:username=\"User One\"" +" leos:dg=\"AGRI.B1\"" +
                " leos:datetime=\"2015-05-29T11:30:00Z\">" +
                "<p>This is a comment...</p>" +
                "</popup>" +
                "<level2><popup id=\"xyz2\"" +
                " refersTo	=\"~leosComment\"" +
                " leos:userid=\"user2\"" +
                " leos:username=\"User One2\"" +" leos:dg=\"AGRI.B1\"" +
                " leos:datetime=\"2015-05-30T11:30:00Z\">" +
                "<p>This is a comment...2</p>" +
                "</popup></level2>" +
                "<level3><level4><popup id=\"xyz3\"" +
                " refersTo=\"~leosComment\"" +
                " leos:userid=\"user3\"" +
                " leos:username=\"User One3\"" +" leos:dg=\"AGRI.B13\"" +
                " leos:datetime=\"2015-06-29T11:30:00Z\">" +
                "<p>This is a comment...3</p>" +
                "</popup></level4></level3>" +
                "</meta>";

        // actual test
        List<CommentVO> results = xmlCommentProcessorImpl.fromXML(xml);

        // verify
        
        assertThat(results.size(), equalTo(3));
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
        assertThat(result.getId(), equalTo(commentVOExpected3.getId()));
        assertThat(result.getEnclosingElementId(), equalTo(commentVOExpected3.getEnclosingElementId()));
        assertThat(result.getComment(), equalTo(commentVOExpected3.getComment()));
        assertThat(result.getAuthorId(), equalTo(commentVOExpected3.getAuthorId()));
        assertThat(result.getTimestamp(), equalTo(commentVOExpected3.getTimestamp()));
        assertThat(result.getAuthorName(), equalTo(commentVOExpected3.getAuthorName()));
    }
}
