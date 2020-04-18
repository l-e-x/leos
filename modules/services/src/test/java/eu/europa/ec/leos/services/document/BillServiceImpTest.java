/*
 * Copyright 2020 European Commission
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
package eu.europa.ec.leos.services.document;


import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.user.Entity;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.repository.LeosRepository;
import eu.europa.ec.leos.repository.document.BillRepository;
import eu.europa.ec.leos.repository.document.BillRepositoryImpl;
import eu.europa.ec.leos.repository.store.PackageRepository;
import eu.europa.ec.leos.services.content.TemplateStructureService;
import eu.europa.ec.leos.services.content.processor.AttachmentProcessor;
import eu.europa.ec.leos.services.document.util.DocumentVOProvider;
import eu.europa.ec.leos.services.support.xml.NumberProcessor;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper;
import eu.europa.ec.leos.services.support.xml.XmlNodeProcessor;
import eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper;
import eu.europa.ec.leos.services.toc.StructureContext;
import eu.europa.ec.leos.services.toc.StructureServiceImpl;
import eu.europa.ec.leos.services.validation.ValidationService;
import eu.europa.ec.leos.vo.toc.NumberingConfig;
import eu.europa.ec.leos.vo.toc.TocItem;
import io.atlassian.fugue.Option;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.inject.Provider;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class BillServiceImpTest {

    @Mock PackageRepository packageRepository;
    @Mock XmlNodeProcessor xmlNodeProcessor;
    @Mock XmlContentProcessor xmlContentProcessor;
    @Mock XmlNodeConfigHelper xmlNodeConfigHelper;
    @Mock AttachmentProcessor attachmentProcessor;
    @Mock ValidationService validationService;
    @Mock DocumentVOProvider documentVOProvider;
    @Mock NumberProcessor numberingProcessor;
    @Mock LeosRepository leosRepository ;
    @Mock MessageHelper messageHelper;
    
    private BillRepository billRepository;
    private BillService billService;

    @Mock
    private TemplateStructureService templateStructureService;
    @Mock
    private Provider<StructureContext> structureContextProvider;
    @Mock
    private StructureContext structureContext;
    @Mock
    private XmlTableOfContentHelper xmlTableOfContentHelper;

    @InjectMocks
    private StructureServiceImpl structureServiceImpl = Mockito.spy(new StructureServiceImpl());

    private String docTemplate;
    private List<TocItem> tocItems;
    private List<NumberingConfig> numberingConfigs;

    @Before
   	public void onSetUp(){
        docTemplate = "BL-023";
        MockitoAnnotations.initMocks(this); //without this you will get NPE
        billRepository =  new BillRepositoryImpl(leosRepository);
        billService = new BillServiceImpl(billRepository, packageRepository, xmlNodeProcessor, xmlContentProcessor, xmlNodeConfigHelper
            ,attachmentProcessor, validationService, documentVOProvider, numberingProcessor, messageHelper, xmlTableOfContentHelper);
        byte[] bytesFile = getFileContent("/structure-test.xml");
        when(templateStructureService.getStructure(docTemplate)).thenReturn(bytesFile);
        ReflectionTestUtils.setField(structureServiceImpl, "structureSchema", "toc/schema/structure_1.xsd");

        tocItems = structureServiceImpl.getTocItems(docTemplate);
        numberingConfigs = structureServiceImpl.getNumberingConfigs(docTemplate);

        when(structureContextProvider.get()).thenReturn(structureContext);
        when(structureContext.getTocItems()).thenReturn(tocItems);
        when(structureContext.getNumberingConfigs()).thenReturn(numberingConfigs);
   	}

    private User getTestUser() {
   		String user1FirstName = "jane";
   		String user1LastName = "demo";
   		String user1Login = "jane";
   		String user1Mail = "jane@test.com";
   		List<Entity> entities = new ArrayList<Entity>();
   		entities.add(new Entity("1", "EXT.A1", "Ext"));
   		List<String> roles = new ArrayList<String>();
   		roles.add("ADMIN");

   		User user1 = new User(1l, user1Login, user1LastName + " " + user1FirstName, entities, user1Mail, roles);
   		return user1;
   	}

    @Test
    public void test_saveTableOfContent_shouldbe_calling_correctNumberOfProcessors() {
        // Given
        Content content = mock(Content.class);
        Content.Source source = mock(Content.Source.class);
        final byte[] byteContent = new byte[]{1, 2, 3};
        final BillMetadata billMetadata = new BillMetadata("", "REGULATION", "", "SJ-023", "EN", "BL-023", "bill-id", "", "0.1.0");
        final Bill bill = new Bill("1", "Legaltext", "login", Instant.now(), "login", Instant.now(),
                "", "", "Version 1.0.0", "", VersionType.MAJOR, true, "title",
                Collections.emptyMap(), Arrays.asList(""), Option.some(content), Option.some(billMetadata));

        when(source.getBytes()).thenReturn(byteContent);
        when(content.getSource()).thenReturn(source);
        when(xmlContentProcessor.createDocumentContentWithNewTocList(any(), any(), any())).thenReturn(byteContent);
        when(numberingProcessor.renumberArticles(any())).thenReturn(byteContent);
        when(numberingProcessor.renumberRecitals(any())).thenReturn(byteContent);
        when(xmlContentProcessor.doXMLPostProcessing(any())).thenReturn(byteContent);

        //When
        billService.saveTableOfContent(bill, Collections.emptyList(), "test", getTestUser());

        // Then
        verify(xmlContentProcessor, times(1)).createDocumentContentWithNewTocList(any(), any(), any());
        verify(numberingProcessor, times(1)).renumberArticles(any());
        verify(numberingProcessor, times(1)).renumberRecitals(any());
        verify(xmlContentProcessor, times(1)).doXMLPostProcessing(any());

        verifyNoMoreInteractions(xmlContentProcessor, numberingProcessor);
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
