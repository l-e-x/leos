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
package eu.europa.ec.leos.services.content.processor;

import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.domain.document.Content;
import eu.europa.ec.leos.domain.document.Content.Source;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Bill;
import eu.europa.ec.leos.domain.document.LeosMetadata.BillMetadata;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.test.support.LeosTest;
import io.atlassian.fugue.Option;
import okio.ByteString;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.Mockito.*;

public class ElementProcessorImplTest extends LeosTest {

    @InjectMocks
    private ElementProcessorImpl elementServiceImpl = new ElementProcessorImpl();

    @Mock
    private XmlContentProcessor xmlContentProcessor;

    // TODO test getArticleTemplate

    @Test
    public void testGetArticle() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        BillMetadata billMetadata = new BillMetadata("", "REGULATION", "", "");
        Map<String, LeosAuthority> collaborators = new HashMap<String, LeosAuthority>();
        collaborators.put("login", LeosAuthority.OWNER);

        byte[] byteContent = new byte[]{1, 2, 3};

        when(source.getByteString()).thenReturn(ByteString.of(byteContent));
        when(content.getSource()).thenReturn(source);

        XmlDocument.Bill document = new XmlDocument.Bill("1", "Legaltext", "login", Instant.now(), "login", Instant.now(), 
                "", "Version 1.0", "", true, true, "BL-000.xml", "fr", "title", collaborators, Option.some(content), Option.some(billMetadata));

        String articleTag = "article";
        String articleId = "7474";
        String articleContent = "article content";

        when(xmlContentProcessor.getElementByNameAndId(byteContent, articleTag, articleId)).thenReturn(articleContent);

        String article = elementServiceImpl.getElement(document, articleTag, articleId);

        assertThat(article, is(articleContent));
    }

    @Test
    public void updateArticle() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        BillMetadata billMetadata = new BillMetadata("", "REGULATION", "", "");
        Map<String, LeosAuthority> collaborators = new HashMap<String, LeosAuthority>();
        collaborators.put("login", LeosAuthority.OWNER);

        String docId = "555";
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};

        when(source.getByteString()).thenReturn(ByteString.of(originalByteContent));
        when(content.getSource()).thenReturn(source);

        XmlDocument.Bill originalDocument = new XmlDocument.Bill(docId, "Legaltext", "login", Instant.now(), "login", Instant.now(), 
                "", "Version 1.0", "", true, true, "BL-000.xml", "fr", "title", collaborators, Option.some(content), Option.some(billMetadata));

        String articleTag = "article";
        String articleId = "486";
        String newArticleText = "new article text";

        when(xmlContentProcessor.replaceElementByTagNameAndId(originalByteContent, newArticleText, articleTag, articleId)).thenReturn(updatedByteContent);

        byte[] result = elementServiceImpl.updateElement(originalDocument, newArticleText, articleTag, articleId);

        assertThat(result, is(updatedByteContent));
    }

    @Test
    public void saveArticle_when_articleNull_should_DeleteArticle() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        BillMetadata billMetadata = new BillMetadata("", "REGULATION", "", "");
        Map<String, LeosAuthority> collaborators = new HashMap<String, LeosAuthority>();
        collaborators.put("login", LeosAuthority.OWNER);

        String docId = "555";
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};

        when(source.getByteString()).thenReturn(ByteString.of(originalByteContent));
        when(content.getSource()).thenReturn(source);

        XmlDocument.Bill originalDocument = new XmlDocument.Bill(docId, "Legaltext", "login", Instant.now(), "login", Instant.now(), 
                "", "Version 1.0", "", true, true, "BL-000.xml", "fr", "title", collaborators, Option.some(content), Option.some(billMetadata));

        String articleTag = "article";
        String articleId = "486";

        when(xmlContentProcessor.replaceElementByTagNameAndId(originalByteContent, null, articleTag, articleId)).thenReturn(updatedByteContent);

        byte[] result = elementServiceImpl.updateElement(originalDocument, null, articleTag, articleId);

        assertThat(result, is(updatedByteContent));
    }

    @Test
    public void testGetCitations() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        BillMetadata billMetadata = new BillMetadata("", "REGULATION", "", "");
        Map<String, LeosAuthority> collaborators = new HashMap<String, LeosAuthority>();
        collaborators.put("login", LeosAuthority.OWNER);

        byte[] byteContent = new byte[]{1, 2, 3, 4};
        String tagName = "citations";
        String tagId = "cits";
        String contentString = "citations content";

        when(source.getByteString()).thenReturn(ByteString.of(byteContent));
        when(content.getSource()).thenReturn(source);

        XmlDocument.Bill document = new XmlDocument.Bill("1", "Legaltext", "login", Instant.now(), "login", Instant.now(), 
                "", "Version 1.0", "", true, true, "BL-000.xml", "fr", "title", collaborators, Option.some(content), Option.some(billMetadata));

        when(xmlContentProcessor.getElementByNameAndId(byteContent, tagName, tagId)).thenReturn(contentString);

        // DO THE ACTUAL CALL
        String citations = elementServiceImpl.getElement(document, tagName, tagId);

        assertThat(citations, is(contentString));
        verify(xmlContentProcessor).getElementByNameAndId(byteContent, tagName, tagId);
        verifyNoMoreInteractions(xmlContentProcessor);

    }

    @Test
    public void testUpdateCitations() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        BillMetadata billMetadata = new BillMetadata("", "REGULATION", "", "");
        Map<String, LeosAuthority> collaborators = new HashMap<String, LeosAuthority>();
        collaborators.put("login", LeosAuthority.OWNER);

        String docId = "555";
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};

        when(source.getByteString()).thenReturn(ByteString.of(originalByteContent));
        when(content.getSource()).thenReturn(source);

        XmlDocument.Bill originalDocument = new XmlDocument.Bill(docId, "Legaltext", "login", Instant.now(), "login", Instant.now(), 
                "", "Version 1.0", "", true, true, "BL-000.xml", "fr", "title", collaborators, Option.some(content), Option.some(billMetadata));

        String tagName = "citations";
        String tagId = "cits";
        String updtedCitations = "Updated citations content";

        when(xmlContentProcessor.replaceElementByTagNameAndId(originalByteContent, updtedCitations, tagName, tagId)).thenReturn(updatedByteContent);

        // DO THE ACTUAL CALL
        byte[] result = elementServiceImpl.updateElement(originalDocument, updtedCitations, tagName, tagId);

        assertThat(result, is(updatedByteContent));
        verify(xmlContentProcessor).replaceElementByTagNameAndId(originalByteContent, updtedCitations, tagName, tagId);
        verifyNoMoreInteractions(xmlContentProcessor);

    }

    @Test
    public void testGetRecitals() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        BillMetadata billMetadata = new BillMetadata("", "REGULATION", "", "");
        Map<String, LeosAuthority> collaborators = new HashMap<String, LeosAuthority>();
        collaborators.put("login", LeosAuthority.OWNER);

        byte[] byteContent = new byte[]{1, 2, 3, 4};
        String tagName = "recitals";
        String tagId = "recs";
        String contentString = "recitals content";

        when(source.getByteString()).thenReturn(ByteString.of(byteContent));
        when(content.getSource()).thenReturn(source);

        XmlDocument.Bill document = new XmlDocument.Bill("1", "Legaltext", "login", Instant.now(), "login", Instant.now(), 
                "", "Version 1.0", "", true, true, "BL-000.xml", "fr", "title", collaborators, Option.some(content), Option.some(billMetadata));

        when(xmlContentProcessor.getElementByNameAndId(byteContent, tagName, tagId)).thenReturn(contentString);

        // DO THE ACTUAL CALL
        String recitals = elementServiceImpl.getElement(document, tagName, tagId);

        assertThat(recitals, is(contentString));
        verify(xmlContentProcessor).getElementByNameAndId(byteContent, tagName, tagId);
        verifyNoMoreInteractions(xmlContentProcessor);
    }

    @Test
    public void testUpdateRecitals() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        BillMetadata billMetadata = new BillMetadata("", "REGULATION", "", "");
        Map<String, LeosAuthority> collaborators = new HashMap<String, LeosAuthority>();
        collaborators.put("login", LeosAuthority.OWNER);

        String docId = "555";
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};

        when(source.getByteString()).thenReturn(ByteString.of(originalByteContent));
        when(content.getSource()).thenReturn(source);

        XmlDocument.Bill originalDocument = new XmlDocument.Bill(docId, "Legaltext", "login", Instant.now(), "login", Instant.now(), 
                "", "Version 1.0", "", true, true, "BL-000.xml", "fr", "title", collaborators, Option.some(content), Option.some(billMetadata));

        String tagName = "recitals";
        String tagId = "recs";
        String updtedRecitals = "Updated recitals content";

        when(xmlContentProcessor.replaceElementByTagNameAndId(originalByteContent, updtedRecitals, tagName, tagId)).thenReturn(updatedByteContent);

        // DO THE ACTUAL CALL
        byte[] result = elementServiceImpl.updateElement(originalDocument, updtedRecitals, tagName, tagId);

        assertThat(result, is(updatedByteContent));
        verify(xmlContentProcessor).replaceElementByTagNameAndId(originalByteContent, updtedRecitals, tagName, tagId);
        verifyNoMoreInteractions(xmlContentProcessor);

    }

    @Test
    public void deleteElement() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        BillMetadata billMetadata = new BillMetadata("", "REGULATION", "", "");
        Map<String, LeosAuthority> collaborators = new HashMap<String, LeosAuthority>();
        collaborators.put("login", LeosAuthority.OWNER);

        String docId = "555";
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};
        byte[] renumberdContent = new byte[]{14, 15, 16};

        when(source.getByteString()).thenReturn(ByteString.of(originalByteContent));
        when(content.getSource()).thenReturn(source);

        XmlDocument.Bill originalDocument = new XmlDocument.Bill(docId, "Legaltext", "login", Instant.now(), "login", Instant.now(), 
                "", "Version 1.0", "", true, true, "BL-000.xml", "fr", "title", collaborators, Option.some(content), Option.some(billMetadata));

        String elementTag = "article";
        String elementId = "486";

        when(xmlContentProcessor.deleteElementByTagNameAndId(argThat(is(originalByteContent)), argThat(is(elementTag)),
                argThat(is(elementId)))).thenReturn(updatedByteContent);

        // DO THE ACTUAL CALL
        byte[] result = elementServiceImpl.deleteElement(originalDocument, elementId, elementTag);

        assertThat(result, is(updatedByteContent));
    }
}
