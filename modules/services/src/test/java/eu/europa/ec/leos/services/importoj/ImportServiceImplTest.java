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
package eu.europa.ec.leos.services.importoj;

import eu.europa.ec.leos.integration.ExternalDocumentProvider;
import eu.europa.ec.leos.test.support.LeosTest;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.stereotype.Service;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@Service
public class ImportServiceImplTest extends LeosTest {

    @Mock
    private ExternalDocumentProvider externalDocumentProvider;

    @InjectMocks
    private ImportServiceImpl importServiceImpl;

    @Test
    public void test_getFormexDocument() {
        String type = "dir";
        int year = 2016;
        int number = 97;
        String result = "test";
        when(externalDocumentProvider.getFormexDocument(type, year, number)).thenReturn(result);
        String document = importServiceImpl.getFormexDocument(type, year, number);
        assertNotNull(document);
    }
}
