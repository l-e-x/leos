/*
 * Copyright 2018 European Commission
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
import eu.europa.ec.leos.domain.document.LeosMetadata.BillMetadata;
import eu.europa.ec.leos.services.support.xml.NumberProcessor;
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

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class ArticleProcessorImplTest extends LeosTest {

    @Mock
    private XmlContentProcessor xmlContentProcessor;

    @Mock
    private NumberProcessor articleNumberProcessor;

    @InjectMocks
    private ArticleProcessorImpl articleProcessorImpl = new ArticleProcessorImpl();
    // TODO test getArticleTemplate

    @Test
    public void insertNewArticle() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        BillMetadata billMetadata = new BillMetadata("", "REGULATION", "", "SJ-023", "EN", "", "bill-id");
        Map<String, LeosAuthority> collaborators = new HashMap<String, LeosAuthority>();
        collaborators.put("login", LeosAuthority.OWNER);

        String docId = "555";
        boolean before = true;
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};
        byte[] renumberdContent = new byte[]{14, 15, 16};

        when(source.getByteString()).thenReturn(ByteString.of(originalByteContent));
        when(content.getSource()).thenReturn(source);

        XmlDocument.Bill originalDocument = new XmlDocument.Bill(docId, "Legaltext", "login", Instant.now(), "login", Instant.now(), 
                "", "Version 1.0", "", true, true, "title", collaborators, Option.some(content), Option.some(billMetadata));

        String articleTag = "article";
        String articleId = "486";

        when(xmlContentProcessor.insertElementByTagNameAndId(argThat(is(originalByteContent)), argThat(is(any(String.class))), argThat(is(articleTag)),
                argThat(is(articleId)), eq(before))).thenReturn(
                updatedByteContent);
        when(articleNumberProcessor.renumberArticles(updatedByteContent, "EN")).thenReturn(renumberdContent);
        when(xmlContentProcessor.doXMLPostProcessing(argThat(is(renumberdContent)))).thenReturn(renumberdContent);

        // DO THE ACTUAL CALL
        byte[] result = articleProcessorImpl.insertNewArticle(originalDocument, articleId, before);

        assertThat(result, is(renumberdContent));
    }

    @Test
    public void deleteArticle() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        BillMetadata billMetadata = new BillMetadata("", "REGULATION", "", "SJ-023", "EN","", "bill-id");
        Map<String, LeosAuthority> collaborators = new HashMap<String, LeosAuthority>();
        collaborators.put("login", LeosAuthority.OWNER);

        String docId = "555";
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};
        byte[] renumberdContent = new byte[]{14, 15, 16};

        when(source.getByteString()).thenReturn(ByteString.of(originalByteContent));
        when(content.getSource()).thenReturn(source);

        XmlDocument.Bill originalDocument = new XmlDocument.Bill(docId, "Legaltext", "login", Instant.now(), "login", Instant.now(), 
                "", "Version 1.0", "", true, true, "title", collaborators, Option.some(content), Option.some(billMetadata));

        String articleTag = "article";
        String articleId = "486";

        when(xmlContentProcessor.deleteElementByTagNameAndId(argThat(is(originalByteContent)), argThat(is(articleTag)),
                argThat(is(articleId)))).thenReturn(updatedByteContent);
        when(articleNumberProcessor.renumberArticles(argThat(is(updatedByteContent)), argThat(is("EN")))).thenReturn(renumberdContent);
        when(xmlContentProcessor.doXMLPostProcessing(argThat(is(renumberdContent)))).thenReturn(renumberdContent);

        // DO THE ACTUAL CALL
        byte[] result = articleProcessorImpl.deleteArticle(originalDocument, articleId);

        assertThat(result, is(renumberdContent));
    }
}
