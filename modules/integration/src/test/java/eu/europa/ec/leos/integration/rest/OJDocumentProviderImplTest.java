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
package eu.europa.ec.leos.integration.rest;

import eu.europa.ec.leos.test.support.LeosTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestOperations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class OJDocumentProviderImplTest extends LeosTest {

    @Mock
    private RestOperations restTemplate;

    @InjectMocks
    private OJDocumentProviderImpl ojDocumentProviderImpl;

    private String ojUrl = "http://publications.europa.eu";
    private String sparqlUri = "/webapi/rdf/sparql";

    @Before
    public void init() {
        ReflectionTestUtils.setField(ojDocumentProviderImpl, "ojUrl", ojUrl);
        ReflectionTestUtils.setField(ojDocumentProviderImpl, "sparqlUri", sparqlUri);
    }

    @Test
    public void test_getOJFormexDocumentByUrl() {
        String uriDocument = "http://publications.europa.eu/resource/cellar/71ac60f7-c97f-11e5-a4b5-01aa75ed71a1.0006.0";
        String result = "test";
        when(restTemplate.getForObject(uriDocument + "/DOC_2", String.class)).thenReturn(result);
        String documentString = ojDocumentProviderImpl.getOJFormexDocumentByUrl(uriDocument);
        assertNotNull(documentString);
    }

    @Test
    public void test_getDocumentUrl() {
        String expectedUrl = "http://publications.europa.eu/resource/cellar/71ac60f7-c97f-11e5-a4b5-01aa75ed71a1.0006.02";
        String json = new String(
                "  {" +
                        "      \"head\": {" +
                        "        \"vars\": [ \"manifestation\" ]" +
                        "      } ," +
                        "     \"results\": {" +
                        "       \"bindings\": [" +
                        "         {" +
                        "            \"manifestation\": { \"type\": \"uri\" , \"value\": \"http://publications.europa.eu/resource/cellar/71ac60f7-c97f-11e5-a4b5-01aa75ed71a1.0006.02\" }" +
                        "          }" +
                        "        ]" +
                        "       }" +
                        "    }");

        String actualUrl = ojDocumentProviderImpl.getDocumentUrl(json);
        assertEquals(expectedUrl, actualUrl);
    }

    public void test_getDocumentUrl_empty_results() {
        String json = new String(
                "  {" +
                        "      \"head\": {" +
                        "        \"vars\": [ \"manifestation\" ]" +
                        "      } ," +
                        "     \"results\": {" +
                        "       }" +
                        "    }");

        String actualUrl = ojDocumentProviderImpl.getDocumentUrl(json);
        assertNull(actualUrl);
    }
}
