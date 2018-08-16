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
package eu.europa.ec.leos.services.export;

import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.integration.toolbox.ExportResource;
import eu.europa.ec.leos.integration.toolbox.jaxb.beans.ImportOptions;
import eu.europa.ec.leos.test.support.LeosTest;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class ExportHelperTest extends LeosTest {
    private static Logger LOG = LoggerFactory.getLogger(ExportHelperTest.class);

    @Mock
    private Configuration freemarkerConfiguration;

    @InjectMocks
    private ExportHelper exportHelperImpl;

    private String templatePdf = "export/pdf.ftl";

    @Before
    public void init() {
        ReflectionTestUtils.setField(exportHelperImpl, "exportTemplatePdf", templatePdf);
    }

    @Test
    public void test_createContentFile() throws Exception {
        final String expectedProposalName = "COMproposal";
        final String expectedLegPackageName = "COMproposal.leg";

        final ExportResource proposal = new ExportResource(LeosCategory.PROPOSAL);
        Map<String, String> tagRefs = new HashMap();
        tagRefs.put("coverPage", "coverPage");
        proposal.setResourceId(expectedProposalName);
        proposal.setComponentsIdsMap(tagRefs);

        ExportResource memorandum = new ExportResource(LeosCategory.MEMORANDUM);
        memorandum.setResourceId("memorandum");
        memorandum.setComponentsIdsMap(tagRefs);
        proposal.addChildResource(memorandum);

        ExportResource bill = new ExportResource(LeosCategory.BILL);
        bill.setResourceId("bill");
        bill.setComponentsIdsMap(tagRefs);
        proposal.addChildResource(bill);

        ByteArrayOutputStream result = null;
        FileOutputStream fileOutputStream = null;
        String resultString;
        try {

            InputStream inputStream = this.getClass().getResource("/eu/europa/ec/leos/freemarker/templates/export/pdf.ftl").openStream();
            Reader targetReader = new InputStreamReader(inputStream);
            final Template t = new Template("pdf.ftl", targetReader, null);

            when(freemarkerConfiguration.getTemplate(templatePdf)).thenReturn(t);
            result = exportHelperImpl.createContentFile(ExportOptions.TO_PDF, proposal);
            resultString = result.toString();
            assertThat(resultString, notNullValue());
            LOG.debug(resultString);

            JAXBContext jaxbContext = JAXBContext.newInstance( ImportOptions.class );
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            StringReader reader = new StringReader(resultString);
            ImportOptions importOptions = (ImportOptions) jaxbUnmarshaller.unmarshal(reader);
            assertEquals(expectedProposalName, importOptions.getImportJob().get(0).getLeos().getResource().getRef());
        }
        catch (Exception e) {
            fail("XML Parsing failed: " + e.getMessage());
        }
        finally {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }

        
    }
}
