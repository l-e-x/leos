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
package eu.europa.ec.leos.services.content.processor;


import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.Content.Source;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.services.support.xml.NumberProcessor;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.test.support.LeosTest;
import io.atlassian.fugue.Option;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class BillProcessorImplTest extends LeosTest {

    @Mock
    private XmlContentProcessor xmlContentProcessor;

    @Mock
    private NumberProcessor numberProcessor;

    @InjectMocks
    private BillProcessorImpl billProcessorImpl = new BillProcessorImpl(xmlContentProcessor, numberProcessor);
    // TODO test getArticleTemplate

    @Test
    public void insertNewArticle() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        BillMetadata billMetadata = new BillMetadata("", "REGULATION", "", "SJ-023", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");

        String docId = "555";
        boolean before = true;
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};
        byte[] renumberdContent = new byte[]{14, 15, 16};

        when(source.getBytes()).thenReturn(originalByteContent);
        when(content.getSource()).thenReturn(source);

        Bill originalDocument = new Bill(docId, "Legaltext", "login", Instant.now(), "login", Instant.now(),
                "", "Version 1.0", "", true, true, "title", collaborators, Arrays.asList(""), Option.some(content), Option.some(billMetadata));

        String articleTag = "article";
        String articleId = "486";

        when(xmlContentProcessor.insertElementByTagNameAndId(argThat(is(originalByteContent)), argThat(is(any(String.class))), argThat(is(articleTag)),
                argThat(is(articleId)), eq(before))).thenReturn(
                updatedByteContent);
        when(numberProcessor.renumberArticles(updatedByteContent, "EN")).thenReturn(renumberdContent);
        when(xmlContentProcessor.doXMLPostProcessing(argThat(is(renumberdContent)))).thenReturn(renumberdContent);

        // DO THE ACTUAL CALL
        byte[] result = billProcessorImpl.insertNewElement(originalDocument, articleId, before, articleTag);

        assertThat(result, is(renumberdContent));
    }

    @Test
    public void deleteArticle() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        BillMetadata billMetadata = new BillMetadata("", "REGULATION", "", "SJ-023", "EN","", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");

        String docId = "555";
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};
        byte[] renumberdContent = new byte[]{14, 15, 16};

        when(source.getBytes()).thenReturn(originalByteContent);
        when(content.getSource()).thenReturn(source);

        Bill originalDocument = new Bill(docId, "Legaltext", "login", Instant.now(), "login", Instant.now(),
                "", "Version 1.0", "", true, true, "title", collaborators, Arrays.asList(""), Option.some(content), Option.some(billMetadata));

        String articleTag = "article";
        String articleId = "486";

        when(xmlContentProcessor.deleteElementByTagNameAndId(argThat(is(originalByteContent)), argThat(is(articleTag)),
                argThat(is(articleId)))).thenReturn(updatedByteContent);
        when(numberProcessor.renumberArticles(argThat(is(updatedByteContent)), argThat(is("EN")))).thenReturn(renumberdContent);
        when(xmlContentProcessor.doXMLPostProcessing(argThat(is(renumberdContent)))).thenReturn(renumberdContent);

        // DO THE ACTUAL CALL
        byte[] result = billProcessorImpl.deleteElement(originalDocument, articleId, articleTag, null);

        assertThat(result, is(renumberdContent));
    }
}
