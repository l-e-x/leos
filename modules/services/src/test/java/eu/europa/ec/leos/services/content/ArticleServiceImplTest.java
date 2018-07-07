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
import eu.europa.ec.leos.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.support.xml.XmlMetaDataProcessor;
import eu.europa.ec.leos.test.support.LeosTest;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

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
}
