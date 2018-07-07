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

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.support.xml.XmlMetaDataProcessor;
import eu.europa.ec.leos.test.support.LeosTest;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import eu.europa.ec.leos.services.content.DocumentService;

import java.io.ByteArrayInputStream;


import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.booleanThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArticleServiceImplTest extends LeosTest {

    @InjectMocks
    private ArticleServiceImpl articleServiceImpl = new ArticleServiceImpl();

    @Mock
    private DocumentService documentService;

    @Mock
    private XmlContentProcessor xmlContentProcessor;

    @Mock
    private XmlMetaDataProcessor xmlMetaDataProcessor;

    // TODO test getArticleTemplate

    @Test
    public void testGetArticle() {

        byte[] byteContent = new byte[]{1, 2, 3};

        LeosDocument document = mock(LeosDocument.class);
        when(document.getContentStream()).thenReturn(new ByteArrayInputStream(byteContent));

        String articleTag = "article";
        String articleId = "7474";
        String articleContent = "article content";

        when(xmlContentProcessor.getElementByNameAndId(byteContent, articleTag, articleId)).thenReturn(articleContent);

        String article = articleServiceImpl.getArticle(document, articleId);

        assertThat(article, is(articleContent));
    }

    @Test
    public void saveArticle() {

        String docId = "555";
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};

        LeosDocument originalDocument = mock(LeosDocument.class);
        when(originalDocument.getLeosId()).thenReturn(docId);
        when(originalDocument.getContentStream()).thenReturn(new ByteArrayInputStream(originalByteContent));

        LeosDocument updatedDocument = mock(LeosDocument.class);
        when(updatedDocument.getLeosId()).thenReturn(docId);
        when(updatedDocument.getContentStream()).thenReturn(new ByteArrayInputStream(updatedByteContent));
        String articleTag = "article";
        String articleId = "486";
        String newArticleText = "new article text";

        when(xmlContentProcessor.replaceElementByTagNameAndId(originalByteContent, newArticleText, articleTag, articleId)).thenReturn(updatedByteContent);
        when(documentService.updateDocumentContent(docId, "sessionID", updatedByteContent, "operation.article.updated")).thenReturn(updatedDocument);

        LeosDocument result = articleServiceImpl.saveArticle(originalDocument, "sessionID", newArticleText, articleId);

        assertThat(result, is(updatedDocument));
    }

    @Test
    public void saveArticle_when_articleNull_should_DeleteArticle() {

        String docId = "555";
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};

        LeosDocument originalDocument = mock(LeosDocument.class);
        when(originalDocument.getLeosId()).thenReturn(docId);
        when(originalDocument.getContentStream()).thenReturn(new ByteArrayInputStream(originalByteContent));

        LeosDocument updatedDocument = mock(LeosDocument.class);
        when(updatedDocument.getLeosId()).thenReturn(docId);
        when(updatedDocument.getContentStream()).thenReturn(new ByteArrayInputStream(updatedByteContent));
        String articleTag = "article";
        String articleId = "486";

        when(xmlContentProcessor.replaceElementByTagNameAndId(originalByteContent, null, articleTag, articleId)).thenReturn(updatedByteContent);
        when(documentService.updateDocumentContent(docId, "sessionID", updatedByteContent ,"operation.article.updated")).thenReturn(updatedDocument);

        LeosDocument result = articleServiceImpl.saveArticle(originalDocument, "sessionID", null, articleId);

        assertThat(result, is(updatedDocument));
    }

    @Test
    public void insertNewArticle() {

        String docId = "555";
        boolean before = true;
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};
        byte[] renumberdContent = new byte[]{14, 15, 16};

        LeosDocument originalDocument = mock(LeosDocument.class);
        when(originalDocument.getLeosId()).thenReturn(docId);
        when(originalDocument.getContentStream()).thenReturn(new ByteArrayInputStream(originalByteContent));
        when(originalDocument.getLanguage()).thenReturn("fr");

        LeosDocument updatedDocument = mock(LeosDocument.class);
        when(updatedDocument.getContentStream()).thenReturn(new ByteArrayInputStream(updatedByteContent));

        String articleTag = "article";
        String articleId = "486";

        when(xmlContentProcessor.insertElementByTagNameAndId(argThat(is(originalByteContent)), argThat(is(any(String.class))), argThat(is(articleTag)),
                argThat(is(articleId)), booleanThat(is(before)))).thenReturn(
                updatedByteContent);
        when(xmlContentProcessor.renumberArticles(updatedByteContent, "fr")).thenReturn(renumberdContent);
        when(documentService.updateDocumentContent(docId, "sessionID", renumberdContent, "operation.article.inserted")).thenReturn(updatedDocument);

        LeosDocument result = articleServiceImpl.insertNewArticle(originalDocument, "sessionID", articleId, before);

        assertThat(result, is(updatedDocument));
    }

    @Test
    public void deleteArticle() {

        String docId = "555";
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};
        byte[] renumberdContent = new byte[]{14, 15, 16};

        LeosDocument originalDocument = mock(LeosDocument.class);
        when(originalDocument.getLeosId()).thenReturn(docId);
        when(originalDocument.getContentStream()).thenReturn(new ByteArrayInputStream(originalByteContent));
        when(originalDocument.getLanguage()).thenReturn("fr");

        LeosDocument updatedDocument = mock(LeosDocument.class);
        when(updatedDocument.getContentStream()).thenReturn(new ByteArrayInputStream(updatedByteContent));

        String articleTag = "article";
        String articleId = "486";

        when(xmlContentProcessor.deleteElementByTagNameAndId(argThat(is(originalByteContent)), argThat(is(articleTag)),
                argThat(is(articleId)))).thenReturn(updatedByteContent);
        when(xmlContentProcessor.renumberArticles(updatedByteContent, "fr")).thenReturn(renumberdContent);
        when(documentService.updateDocumentContent(docId, "sessionID", renumberdContent, "operation.article.deleted")).thenReturn(updatedDocument);

        // DO THE ACTUAL CALL
        LeosDocument result = articleServiceImpl.deleteArticle(originalDocument, "sessionID", articleId);

        assertThat(result, is(updatedDocument));
    }
}
