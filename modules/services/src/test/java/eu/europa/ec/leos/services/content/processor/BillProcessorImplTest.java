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
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.services.content.TemplateStructureService;
import eu.europa.ec.leos.services.support.xml.NumberProcessor;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.toc.StructureContext;
import eu.europa.ec.leos.services.toc.StructureServiceImpl;
import eu.europa.ec.leos.test.support.LeosTest;
import eu.europa.ec.leos.vo.toc.NumberingConfig;
import eu.europa.ec.leos.vo.toc.TocItem;
import io.atlassian.fugue.Option;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import javax.inject.Provider;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.ARTICLE;
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

    @Mock
    private TemplateStructureService templateStructureService;

    @Mock
    private MessageHelper messageHelper;

    @Mock
    private Provider<StructureContext> structureContextProvider;

    @Mock
    private StructureContext structureContext;

    @InjectMocks
    private StructureServiceImpl structureServiceImpl = Mockito.spy(new StructureServiceImpl());

    @InjectMocks
    private BillProcessorImpl billProcessorImpl = new BillProcessorImpl(xmlContentProcessor, numberProcessor, messageHelper, structureContextProvider);
    // TODO test getArticleTemplate
    
    private String docTemplate;
    private List<TocItem> tocItems;
    private List<NumberingConfig> numberingConfigs;

    @Before
    public void setUp(){
        docTemplate = "BL-023";
        byte[] bytesFile = getFileContent("/structure-test.xml");
        when(templateStructureService.getStructure(docTemplate)).thenReturn(bytesFile);
        ReflectionTestUtils.setField(structureServiceImpl, "structureSchema", "toc/schema/structure_1.xsd");

        tocItems = structureServiceImpl.getTocItems(docTemplate);
        numberingConfigs = structureServiceImpl.getNumberingConfigs(docTemplate);

        when(structureContextProvider.get()).thenReturn(structureContext);
        when(structureContext.getTocItems()).thenReturn(tocItems);
        when(structureContext.getNumberingConfigs()).thenReturn(numberingConfigs);
    }

    @Test
    public void insertNewArticle() {
        Content content = mock(Content.class);
        Source source = mock(Source.class);

        BillMetadata billMetadata = new BillMetadata("", "REGULATION", "", "SJ-023", "EN", "BL-023", "bill-id", "", "0.1.0");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");

        String docId = "555";
        boolean before = true;
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};
        byte[] renumberdContent = new byte[]{14, 15, 16};

        when(source.getBytes()).thenReturn(originalByteContent);
        when(content.getSource()).thenReturn(source);
    
        final Bill originalDocument = getMockedBill(content, billMetadata, collaborators, docId);
        final String articleTag = ARTICLE;
        final String articleId = "486";

        when(xmlContentProcessor.insertElementByTagNameAndId(argThat(is(originalByteContent)), argThat(is(any(String.class))), argThat(is(articleTag)),
                argThat(is(articleId)), eq(before))).thenReturn(
                updatedByteContent);
        when(numberProcessor.renumberArticles(updatedByteContent)).thenReturn(renumberdContent);
        when(xmlContentProcessor.doXMLPostProcessing(argThat(is(renumberdContent)))).thenReturn(renumberdContent);
        when(xmlContentProcessor.doXMLPostProcessing(argThat(is(renumberdContent)))).thenReturn(renumberdContent);
        when(messageHelper.getMessage("toc.item.template.article.content.text")).thenReturn("Text...");

        // DO THE ACTUAL CALL
        byte[] result = billProcessorImpl.insertNewElement(originalDocument, articleId, before, articleTag);

        assertThat(result, is(renumberdContent));
    }

    @Test
    public void deleteArticle() {
        Content content = mock(Content.class);
        Source source = mock(Source.class);

        BillMetadata billMetadata = new BillMetadata("", "REGULATION", "", "SJ-023", "EN","BL-023", "bill-id", "", "0.1.0");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");

        String docId = "555";
        byte[] originalByteContent = new byte[]{1, 2, 3};
        byte[] updatedByteContent = new byte[]{4, 5, 6};
        byte[] renumberdContent = new byte[]{14, 15, 16};

        when(source.getBytes()).thenReturn(originalByteContent);
        when(content.getSource()).thenReturn(source);
    
        final Bill originalDocument = getMockedBill(content, billMetadata, collaborators, docId);
        final String articleTag = ARTICLE;
        final String articleId = "486";

        when(xmlContentProcessor.deleteElementByTagNameAndId(argThat(is(originalByteContent)), argThat(is(articleTag)),
                argThat(is(articleId)))).thenReturn(updatedByteContent);
        when(numberProcessor.renumberArticles(argThat(is(updatedByteContent)))).thenReturn(renumberdContent);
        when(xmlContentProcessor.doXMLPostProcessing(argThat(is(renumberdContent)))).thenReturn(renumberdContent);

        // DO THE ACTUAL CALL
        byte[] result = billProcessorImpl.deleteElement(originalDocument, articleId, articleTag, null);

        assertThat(result, is(renumberdContent));
    }
    
    private Bill getMockedBill(Content content, BillMetadata billMetadata, Map<String, String> collaborators, String docId) {
        return new Bill(docId, "Legaltext", "login", Instant.now(), "login", Instant.now(),
                    "", "", "Version 1.0.0", "", VersionType.MAJOR, true, "title", collaborators, Arrays.asList(""), Option.some(content), Option.some(billMetadata));
    }
    
    public byte[] getFileContent(String fileName) {
        try {
            InputStream inputStream = this.getClass().getResource(fileName).openStream();
            byte[] content = new byte[inputStream.available()];
            inputStream.read(content);
            inputStream.close();
            return content;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read bytes from file: " + fileName);
        }
    }
}
