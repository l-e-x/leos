/**
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
package eu.europa.ec.leos.services.content;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.test.support.LeosTest;

public class PreambleServiceImplTest extends LeosTest {

    @Mock
    private XmlContentProcessor xmlContentProcessor;

    @Mock
    private DocumentService documentService;
    
    @InjectMocks
    private PreambleServiceImpl preambleServiceImpl = new PreambleServiceImpl();

    @Test
    public void testGetCitations() {

        byte[] byteContent = new byte[]{1, 2, 3, 4};
        String tagName = "citations";
        String tagId = "cits";
        String content = "citations content";
        LeosDocument document = mock(LeosDocument.class);

        when(document.getContentStream()).thenReturn(new ByteArrayInputStream(byteContent));
        when(xmlContentProcessor.getElementByNameAndId(byteContent, tagName, tagId)).thenReturn(content);

        //DO THE ACTUAL CALL
        String citations = preambleServiceImpl.getCitations(document, tagId);

        assertThat(citations, is(content));
        verify(xmlContentProcessor).getElementByNameAndId(byteContent, tagName, tagId);
        verifyNoMoreInteractions( xmlContentProcessor);

    }
    
    @Test
    public void testSaveCitations() {

        String docId = "555";
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};

        LeosDocument originalDocument = mock(LeosDocument.class);
        when(originalDocument.getLeosId()).thenReturn(docId);
        when(originalDocument.getContentStream()).thenReturn(new ByteArrayInputStream(originalByteContent));

        LeosDocument updatedDocument = mock(LeosDocument.class);
        when(updatedDocument.getLeosId()).thenReturn(docId);
        when(updatedDocument.getContentStream()).thenReturn(new ByteArrayInputStream(updatedByteContent));
        
        String tagName = "citations";
        String tagId = "cits";
        String updtedCitations = "Updated citations content";

        when(xmlContentProcessor.replaceElementByTagNameAndId(originalByteContent, updtedCitations, tagName, tagId)).thenReturn(updatedByteContent);
        when(documentService.updateDocumentContent(docId, null, updatedByteContent,"operation.citations.updated")).thenReturn(updatedDocument);
        
        //DO THE ACTUAL CALL
        LeosDocument result = preambleServiceImpl.saveCitations(originalDocument, updtedCitations, tagId);

        assertThat(result, is(updatedDocument));
        verify(documentService).updateDocumentContent(docId, null, updatedByteContent,"operation.citations.updated");
        verify(xmlContentProcessor).replaceElementByTagNameAndId(originalByteContent, updtedCitations, tagName, tagId);
        verifyNoMoreInteractions( xmlContentProcessor,documentService);

    }
    
    @Test
    public void testGetRecitals() {

        byte[] byteContent = new byte[]{1, 2, 3, 4};
        String tagName = "recitals";
        String tagId = "recs";
        String content = "recitals content";
        LeosDocument document = mock(LeosDocument.class);

        when(document.getContentStream()).thenReturn(new ByteArrayInputStream(byteContent));
        when(xmlContentProcessor.getElementByNameAndId(byteContent, tagName, tagId)).thenReturn(content);

        //DO THE ACTUAL CALL
        String recitals = preambleServiceImpl.getRecitals(document, tagId);

        assertThat(recitals, is(content));
        verify(xmlContentProcessor).getElementByNameAndId(byteContent, tagName, tagId);
        verifyNoMoreInteractions( xmlContentProcessor);
    }
    
    @Test
    public void testSaveRecitals() {

        String docId = "555";
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};

        LeosDocument originalDocument = mock(LeosDocument.class);
        when(originalDocument.getLeosId()).thenReturn(docId);
        when(originalDocument.getContentStream()).thenReturn(new ByteArrayInputStream(originalByteContent));

        LeosDocument updatedDocument = mock(LeosDocument.class);
        when(updatedDocument.getLeosId()).thenReturn(docId);
        when(updatedDocument.getContentStream()).thenReturn(new ByteArrayInputStream(updatedByteContent));
        
        String tagName = "recitals";
        String tagId = "recs";
        String updtedRecitals = "Updated recitals content";

        when(xmlContentProcessor.replaceElementByTagNameAndId(originalByteContent, updtedRecitals, tagName, tagId)).thenReturn(updatedByteContent);
        when(documentService.updateDocumentContent(docId, null, updatedByteContent,"operation.recitals.updated")).thenReturn(updatedDocument);
        
        //DO THE ACTUAL CALL
        LeosDocument result = preambleServiceImpl.saveRecitals(originalDocument, updtedRecitals, tagId);

        assertThat(result, is(updatedDocument));
        verify(documentService).updateDocumentContent(docId, null, updatedByteContent,"operation.recitals.updated");
        verify(xmlContentProcessor).replaceElementByTagNameAndId(originalByteContent, updtedRecitals, tagName, tagId);
        verifyNoMoreInteractions( xmlContentProcessor,documentService);

    }
}
