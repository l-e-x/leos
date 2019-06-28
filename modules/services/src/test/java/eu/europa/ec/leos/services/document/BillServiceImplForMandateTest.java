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
package eu.europa.ec.leos.services.document;


import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.repository.LeosRepository;
import eu.europa.ec.leos.repository.document.BillRepository;
import eu.europa.ec.leos.repository.document.BillRepositoryImpl;
import eu.europa.ec.leos.repository.store.PackageRepository;
import eu.europa.ec.leos.services.content.processor.AttachmentProcessor;
import eu.europa.ec.leos.services.document.util.DocumentVOProvider;
import eu.europa.ec.leos.services.support.xml.NumberProcessor;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper;
import eu.europa.ec.leos.services.support.xml.XmlNodeProcessor;
import eu.europa.ec.leos.services.validation.ValidationService;
import io.atlassian.fugue.Option;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class BillServiceImplForMandateTest {
    
    @Mock PackageRepository packageRepository;
    @Mock XmlNodeProcessor xmlNodeProcessor;
    @Mock XmlContentProcessor xmlContentProcessor;
    @Mock XmlNodeConfigHelper xmlNodeConfigHelper;
    @Mock AttachmentProcessor attachmentProcessor;
    @Mock ValidationService validationService;
    @Mock DocumentVOProvider documentVOProvider;
    @Mock NumberProcessor numberingProcessor;
    @Mock LeosRepository leosRepository ;
    
    private BillRepository billRepository;
    private BillService billService;
    
    @Before
   	public void onSetUp(){
        MockitoAnnotations.initMocks(this); //without this you will get NPE
        billRepository =  new BillRepositoryImpl(leosRepository);
        billService = new BillServiceImplForMandate(billRepository,packageRepository, xmlNodeProcessor, xmlContentProcessor, xmlNodeConfigHelper
            ,attachmentProcessor, validationService, documentVOProvider, numberingProcessor);
   	}
    
    private User getTestUser() {
   		String user1FirstName = "jane";
   		String user1LastName = "demo";
   		String user1Login = "jane";
   		String user1Mail = "jane@test.com";
   		String entity = "Entity";
   		List<String> roles = new ArrayList<String>();
   		roles.add("ADMIN");
   		
   		User user1 = new User(1l, user1Login, user1LastName + " " + user1FirstName, entity, user1Mail, roles);
   		return user1;
   	}
   	
    @Test
    public void test_saveTableOfContent_shouldbe_calling_correctNumberOfProcessors() {
        // Given
        Content content = mock(Content.class);
        Content.Source source = mock(Content.Source.class);
        final byte[] byteContent = new byte[]{1, 2, 3};
        final BillMetadata billMetadata = new BillMetadata("", "REGULATION", "", "SJ-023", "EN", "", "bill-id", "");
        final Bill bill = new Bill("1", "Legaltext", "login", Instant.now(), "login", Instant.now(),
                "", "Version 1.0", "", true, true, "title",
                Collections.emptyMap(), Arrays.asList(""), Option.some(content), Option.some(billMetadata));

        when(source.getBytes()).thenReturn(byteContent);
        when(content.getSource()).thenReturn(source);
        when(xmlContentProcessor.createDocumentContentWithNewTocList(any(), any(), any(), any())).thenReturn(byteContent);
        when(numberingProcessor.renumberArticles(any(), any())).thenReturn(byteContent);
        when(numberingProcessor.renumberRecitals(any())).thenReturn(byteContent);
        when(xmlContentProcessor.doXMLPostProcessing(any())).thenReturn(byteContent);
        
        //When
        billService.saveTableOfContent(bill, Arrays.asList(), "test", getTestUser());
        
        // Then
        verify(xmlContentProcessor, times(1)).createDocumentContentWithNewTocList(any(), any(), any(), any());
        verify(numberingProcessor, times(1)).renumberArticles(any(), any());
        verify(numberingProcessor, times(1)).renumberRecitals(any());
        verify(xmlContentProcessor, times(1)).doXMLPostProcessing(any());
        
        verifyNoMoreInteractions(xmlContentProcessor, numberingProcessor);
    }
   
}
