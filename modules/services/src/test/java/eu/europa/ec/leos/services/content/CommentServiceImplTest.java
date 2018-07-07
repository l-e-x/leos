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
package eu.europa.ec.leos.services.content;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.test.support.LeosTest;
import eu.europa.ec.leos.test.support.model.ModelHelper;
import eu.europa.ec.leos.vo.CommentVO;
import eu.europa.ec.leos.vo.CommentVO.RefersTo;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.booleanThat;
import static org.mockito.Mockito.*;

public class CommentServiceImplTest extends LeosTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private XmlContentProcessor xmlContentProcessor;

    @Mock
    private SecurityContext leosSecurityContext;

    @InjectMocks
    private CommentServiceImpl commentServiceImpl = new CommentServiceImpl();
    
    @Test
    public void test_getAllComments() throws ParseException {

        // setup
        byte[] byteContent = new byte[]{1, 2, 3};
        LeosDocument document = mock(LeosDocument.class);
        CommentVO commentVOExpected1 = new CommentVO("xyz", "ElementId", "This is a comment...", "User One", "user1", "TESTDG",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-29T16:30:00Z"), RefersTo.LEOS_COMMENT);
        CommentVO commentVOExpected2 = new CommentVO("xyz2", "ElementId", "This is a comment...2", "User One2", "user2","TESTDG1",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-30T11:30:00Z"), RefersTo.LEOS_COMMENT);

        when(xmlContentProcessor.getAllComments(byteContent)).thenReturn(new ArrayList<CommentVO>(Arrays.asList(commentVOExpected1, commentVOExpected2)));
        when(document.getContentStream()).thenReturn(new ByteArrayInputStream(byteContent));

        //Actual Call
        List<CommentVO> results  = commentServiceImpl.getAllComments(document);

        assertThat(results.size(), is(2));
        verify(xmlContentProcessor).getAllComments(byteContent);
    }

    @Test
    public void test_saveComment() throws Exception {

        //set up
        String commentId = "555";
        String newContent = "new comment text";
        String docId = "docid";
        
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};

        User user = ModelHelper.buildUser(45L, "login", "name","TestDG.1.2");
        when(leosSecurityContext.getUser()).thenReturn(user);

        LeosDocument originalDocument = mock(LeosDocument.class);
        when(originalDocument.getLeosId()).thenReturn(docId);
        when(originalDocument.getContentStream()).thenReturn(new ByteArrayInputStream(originalByteContent));

        LeosDocument updatedDocument = mock(LeosDocument.class);
        when(updatedDocument.getLeosId()).thenReturn(docId);
        when(updatedDocument.getContentStream()).thenReturn(new ByteArrayInputStream(updatedByteContent));
        
        when(xmlContentProcessor.replaceElementByTagNameAndId(argThat(equalTo(originalByteContent)),argThat(is(any(String.class))),argThat(equalTo( "popup")), argThat(equalTo(commentId)))).thenReturn(updatedByteContent);
        when(documentService.updateDocumentContent(docId, "login", updatedByteContent, "operation.comment.updated")).thenReturn(updatedDocument);

        //make the actual call
        LeosDocument result = commentServiceImpl.updateComment(originalDocument, commentId, newContent);
        
        //verify
        assertThat(result, is(updatedDocument));
        verify(xmlContentProcessor).replaceElementByTagNameAndId(argThat(is(originalByteContent)),argThat(is(any(String.class))), argThat(is("popup")), argThat(is(commentId)));
        verify(documentService).updateDocumentContent(docId, "login", updatedByteContent, "operation.comment.updated");
        verifyNoMoreInteractions( xmlContentProcessor,documentService);
    }

    @Test
    public void test_saveComment_should_Delete() throws Exception {

        //set up
        String commentId = "555";
        String newContent = null;
        String docId = "docid";
        
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};

        User user = ModelHelper.buildUser(45L, "login", "name");
        when(leosSecurityContext.getUser()).thenReturn(user);

        LeosDocument originalDocument = mock(LeosDocument.class);
        when(originalDocument.getLeosId()).thenReturn(docId);
        when(originalDocument.getContentStream()).thenReturn(new ByteArrayInputStream(originalByteContent));

        LeosDocument updatedDocument = mock(LeosDocument.class);
        when(updatedDocument.getLeosId()).thenReturn(docId);
        when(updatedDocument.getContentStream()).thenReturn(new ByteArrayInputStream(updatedByteContent));
        
        when(xmlContentProcessor.replaceElementByTagNameAndId(argThat(equalTo(originalByteContent)),argThat(equalTo(newContent)) ,argThat(equalTo( "popup")), argThat(equalTo(commentId)))).thenReturn(updatedByteContent);
        when(documentService.updateDocumentContent(docId, "login", updatedByteContent, "operation.comment.updated")).thenReturn(updatedDocument);

        //make the actual call
        LeosDocument result = commentServiceImpl.updateComment(originalDocument, commentId, null);

        assertThat(result, is(updatedDocument));
        verify(xmlContentProcessor).replaceElementByTagNameAndId(argThat(is(originalByteContent)),argThat(equalTo(newContent)), argThat(is("popup")), argThat(is(commentId)));
        verify(documentService).updateDocumentContent(docId, "login", updatedByteContent, "operation.comment.updated");
        verifyNoMoreInteractions( xmlContentProcessor,documentService);
    }

    @Test
    public void test_insertNewComment() throws Exception {

        //set up
        String commentId = "555";
        String elementId ="ele";
        String newContent = "new comment text";
        String docId = "docid";
        
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};

        User user = ModelHelper.buildUser(45L, "login", "name","TestDG.1.2");
        when(leosSecurityContext.getUser()).thenReturn(user);

        LeosDocument originalDocument = mock(LeosDocument.class);
        when(originalDocument.getLeosId()).thenReturn(docId);
        when(originalDocument.getContentStream()).thenReturn(new ByteArrayInputStream(originalByteContent));

        LeosDocument updatedDocument = mock(LeosDocument.class);
        when(updatedDocument.getLeosId()).thenReturn(docId);
        when(updatedDocument.getContentStream()).thenReturn(new ByteArrayInputStream(updatedByteContent));
        
        when(xmlContentProcessor.insertCommentInElement(argThat(equalTo(originalByteContent)),argThat(equalTo(elementId)),argThat(is(any(String.class))), booleanThat(is(true)))).thenReturn(updatedByteContent);
        when(documentService.updateDocumentContent(docId, "login", updatedByteContent, "operation.comment.inserted")).thenReturn(updatedDocument);

        //make the actual call
        LeosDocument result = commentServiceImpl.insertNewComment(originalDocument, elementId, commentId, newContent, true);
        
        //verify
        assertThat(result, is(updatedDocument));
        verify(xmlContentProcessor).insertCommentInElement(argThat(equalTo(originalByteContent)),argThat(equalTo(elementId)),argThat(is(any(String.class))), booleanThat(is(true)));
        verify(documentService).updateDocumentContent(docId, "login", updatedByteContent, "operation.comment.inserted");
        verifyNoMoreInteractions( xmlContentProcessor,documentService);
    }

    @Test
    public void test_deleteComment() throws Exception {

        //set up
        String commentId = "555";
        String docId = "docid";
        
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};

        User user = ModelHelper.buildUser(45L, "login", "name");
        when(leosSecurityContext.getUser()).thenReturn(user);

        LeosDocument originalDocument = mock(LeosDocument.class);
        when(originalDocument.getLeosId()).thenReturn(docId);
        when(originalDocument.getContentStream()).thenReturn(new ByteArrayInputStream(originalByteContent));

        LeosDocument updatedDocument = mock(LeosDocument.class);
        when(updatedDocument.getLeosId()).thenReturn(docId);
        when(updatedDocument.getContentStream()).thenReturn(new ByteArrayInputStream(updatedByteContent));
        
        when(xmlContentProcessor.deleteElementByTagNameAndId(originalByteContent, "popup", commentId)).thenReturn(updatedByteContent);
        when(documentService.updateDocumentContent(docId, "login", updatedByteContent, "operation.comment.deleted")).thenReturn(updatedDocument);

        //make the actual call
        LeosDocument result = commentServiceImpl.deleteComment(originalDocument, commentId);
        
        //verify
        assertThat(result, is(updatedDocument));
        verify(xmlContentProcessor).deleteElementByTagNameAndId(originalByteContent, "popup", commentId);
        verify(documentService).updateDocumentContent(docId, "login", updatedByteContent, "operation.comment.deleted");
        verifyNoMoreInteractions( xmlContentProcessor,documentService);
    }
    @Test
    public void testAggregateComments_oneNewComment() throws Exception {
        //setup
        byte[] baseXmlBytes= new byte[]{1, 2, 3};
        byte[] addendedXmlBytes= new byte[] {5, 6, 8};
        byte[] aggregateXmlBytes= new byte[] {9, 10, 11};

        String comment2="<bla>This is a comment...2</bla>";

        CommentVO commentVOExpected1 = new CommentVO("xyz1", "ElementId", "This is a comment...", "User One", "user1", "TESTDG",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-29T16:30:00Z"), RefersTo.LEOS_COMMENT);
        CommentVO commentVOExpected2 = new CommentVO("xyz2", "ElementId", "This is a comment...2", "User One2", "user2","TESTDG1",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-30T11:30:00Z"), RefersTo.LEOS_COMMENT);


        when(xmlContentProcessor.getAllComments(baseXmlBytes)).thenReturn(new ArrayList<CommentVO>(Arrays.asList(commentVOExpected1)));
        when(xmlContentProcessor.getAllComments(addendedXmlBytes)).thenReturn(new ArrayList<CommentVO>(Arrays.asList(commentVOExpected1, commentVOExpected2)));
        when(xmlContentProcessor.getElementByNameAndId(addendedXmlBytes, "popup", commentVOExpected2.getId())).thenReturn(comment2);
        when(xmlContentProcessor.insertCommentInElement(baseXmlBytes, commentVOExpected2.getEnclosingElementId(), comment2, true)).thenReturn(aggregateXmlBytes);

        //actual call
        byte[] result = commentServiceImpl.aggregateComments(baseXmlBytes, addendedXmlBytes);

        //verify
        verify(xmlContentProcessor).getAllComments(baseXmlBytes);
        verify(xmlContentProcessor).getAllComments(addendedXmlBytes);
        verify(xmlContentProcessor).getElementByNameAndId(addendedXmlBytes, "popup", commentVOExpected2.getId());
        verify(xmlContentProcessor).insertCommentInElement(baseXmlBytes, commentVOExpected2.getEnclosingElementId(), comment2, true);
        MatcherAssert.assertThat(result, is(equalTo(aggregateXmlBytes)));
        verifyNoMoreInteractions(xmlContentProcessor);
    }

    @Test
    public void testAggregateComments_multipleNewComment() throws Exception {
        //setup
        byte[] baseXmlBytes= new byte[]{1, 2, 3};
        byte[] addendedXmlBytes= new byte[] {5, 6, 8};
        byte[] aggregateXmlBytes= new byte[] {9, 10, 11};

        String comment2="<bla>This is a comment...2</bla>";

        CommentVO commentVOExpected1 = new CommentVO("xyz1", "ElementId", "This is a comment...", "User One", "user1", "TESTDG",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-29T16:30:00Z"), RefersTo.LEOS_COMMENT);
        CommentVO commentVOExpected2 = new CommentVO("xyz2", "ElementId", "This is a comment...2", "User One2", "user2","TESTDG1",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-30T11:30:00Z"), RefersTo.LEOS_COMMENT);
        CommentVO commentVOExpected3 = new CommentVO("xyz3", "ElementId3", "This is a comment...2", "User One2", "user2","TESTDG1",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-30T11:30:00Z"), RefersTo.LEOS_COMMENT);
        CommentVO commentVOExpected4 = new CommentVO("xyz4", "ElementId4", "This is a comment...2", "User One2", "user2","TESTDG1",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-30T11:30:00Z"), RefersTo.LEOS_COMMENT);

        when(xmlContentProcessor.getAllComments(baseXmlBytes)).thenReturn(new ArrayList<CommentVO>(Arrays.asList(commentVOExpected1)));
        when(xmlContentProcessor.getAllComments(addendedXmlBytes)).thenReturn(new ArrayList<CommentVO>(Arrays.asList(commentVOExpected1, commentVOExpected2,commentVOExpected3,commentVOExpected4)));
        when(xmlContentProcessor.getElementByNameAndId(addendedXmlBytes, "popup", commentVOExpected2.getId())).thenReturn(comment2);
        when(xmlContentProcessor.getElementByNameAndId(addendedXmlBytes, "popup", commentVOExpected3.getId())).thenReturn(comment2);
        when(xmlContentProcessor.getElementByNameAndId(addendedXmlBytes, "popup", commentVOExpected4.getId())).thenReturn(comment2);

        when(xmlContentProcessor.insertCommentInElement(baseXmlBytes, commentVOExpected2.getEnclosingElementId(), comment2, true)).thenReturn(aggregateXmlBytes);
        when(xmlContentProcessor.insertCommentInElement(aggregateXmlBytes, commentVOExpected3.getEnclosingElementId(), comment2, true)).thenReturn(aggregateXmlBytes);
        when(xmlContentProcessor.insertCommentInElement(aggregateXmlBytes, commentVOExpected4.getEnclosingElementId(), comment2, true)).thenReturn(aggregateXmlBytes);

        //actual call
        byte[] result = commentServiceImpl.aggregateComments(baseXmlBytes, addendedXmlBytes);

        //verify
        verify(xmlContentProcessor).getAllComments(baseXmlBytes);
        verify(xmlContentProcessor).getAllComments(addendedXmlBytes);

        verify(xmlContentProcessor).getElementByNameAndId(addendedXmlBytes, "popup", commentVOExpected2.getId());
        verify(xmlContentProcessor).getElementByNameAndId(addendedXmlBytes, "popup", commentVOExpected3.getId());
        verify(xmlContentProcessor).getElementByNameAndId(addendedXmlBytes, "popup", commentVOExpected4.getId());

        verify(xmlContentProcessor).insertCommentInElement(baseXmlBytes, commentVOExpected2.getEnclosingElementId(), comment2, true);
        verify(xmlContentProcessor).insertCommentInElement(aggregateXmlBytes, commentVOExpected3.getEnclosingElementId(), comment2, true);
        verify(xmlContentProcessor).insertCommentInElement(aggregateXmlBytes, commentVOExpected4.getEnclosingElementId(), comment2, true);

        MatcherAssert.assertThat(result, is(equalTo(aggregateXmlBytes)));
        verifyNoMoreInteractions(xmlContentProcessor);
    }

    @Test
    public void testAggregateComments_noNewComment() throws Exception {
        //setup
        byte[] baseXmlBytes= new byte[]{1, 2, 3};
        byte[] addendedXmlBytes= new byte[] {5, 6, 8};

        CommentVO commentVOExpected1 = new CommentVO("xyz1", "ElementId", "This is a comment...", "User One", "user1", "TESTDG",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-29T16:30:00Z"), RefersTo.LEOS_COMMENT);
        CommentVO commentVOExpected2 = new CommentVO("xyz2", "ElementId", "This is a comment...2", "User One2", "user2","TESTDG1",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-30T11:30:00Z"), RefersTo.LEOS_COMMENT);


        when(xmlContentProcessor.getAllComments(baseXmlBytes)).thenReturn(new ArrayList<>(Arrays.asList(commentVOExpected1,commentVOExpected2)));
        when(xmlContentProcessor.getAllComments(addendedXmlBytes)).thenReturn(new ArrayList<>(Arrays.asList(commentVOExpected1, commentVOExpected2)));

        //actual call
        byte[] result = commentServiceImpl.aggregateComments(baseXmlBytes, addendedXmlBytes);

        //verify
        verify(xmlContentProcessor).getAllComments(baseXmlBytes);
        verify(xmlContentProcessor).getAllComments(addendedXmlBytes);

        MatcherAssert.assertThat(result, is(equalTo(baseXmlBytes)));
        verifyNoMoreInteractions(xmlContentProcessor);
    }

    @Test
    public void testAggregateComments_updatedComment() throws Exception {
        //setup
        byte[] baseXmlBytes= new byte[]{1, 2, 3};
        byte[] addendedXmlBytes= new byte[] {5, 6, 8};
        byte[] aggregateXmlBytes_afterDelete= new byte[] {9, 10, 11};
        byte[] aggregateXmlBytes_afterinsert= new byte[] {12, 13, 14};

        String commentUpdated="<bla>This is a comment...2</bla>";

        CommentVO commentVOExpected1 = new CommentVO("xyz1", "ElementId", "This is a comment...", "User One", "user1", "TESTDG",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-29T16:30:00Z"), RefersTo.LEOS_COMMENT);
        CommentVO commentVOExpected1_updated = new CommentVO("xyz1", "ElementId", "This is a comment...updated", "User One", "user1", "TESTDG",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-29T16:30:00Z"), RefersTo.LEOS_COMMENT);
        CommentVO commentVOExpected2 = new CommentVO("xyz2", "ElementId2", "This is a comment...2", "User One2", "user2","TESTDG1",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-30T11:30:00Z"), RefersTo.LEOS_COMMENT);


        when(xmlContentProcessor.getAllComments(baseXmlBytes)).thenReturn(new ArrayList<>(Arrays.asList(commentVOExpected1,commentVOExpected2)));
        when(xmlContentProcessor.getAllComments(addendedXmlBytes)).thenReturn(new ArrayList<>(Arrays.asList(commentVOExpected1_updated, commentVOExpected2)));
        when(xmlContentProcessor.getElementByNameAndId(addendedXmlBytes, "popup", commentVOExpected1_updated.getId())).thenReturn(commentUpdated);
        when(xmlContentProcessor.deleteElementByTagNameAndId(baseXmlBytes, "popup", commentVOExpected1_updated.getId())).thenReturn(aggregateXmlBytes_afterDelete);
        when(xmlContentProcessor.insertCommentInElement(aggregateXmlBytes_afterDelete, commentVOExpected1_updated.getEnclosingElementId(), commentUpdated, true)).thenReturn(aggregateXmlBytes_afterinsert);

        //actual call
        byte[] result = commentServiceImpl.aggregateComments(baseXmlBytes, addendedXmlBytes);

        //verify
        verify(xmlContentProcessor).getAllComments(baseXmlBytes);
        verify(xmlContentProcessor).getAllComments(addendedXmlBytes);
        verify(xmlContentProcessor).getElementByNameAndId(addendedXmlBytes, "popup", commentVOExpected1_updated.getId());
        verify(xmlContentProcessor).deleteElementByTagNameAndId(baseXmlBytes, "popup", commentVOExpected1_updated.getId());
        verify(xmlContentProcessor).insertCommentInElement(aggregateXmlBytes_afterDelete, commentVOExpected1_updated.getEnclosingElementId(), commentUpdated, true);
        MatcherAssert.assertThat(result, is(equalTo(aggregateXmlBytes_afterinsert)));

        verifyNoMoreInteractions(xmlContentProcessor);
    }
}
