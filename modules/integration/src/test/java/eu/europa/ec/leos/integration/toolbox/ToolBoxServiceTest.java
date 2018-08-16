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
package eu.europa.ec.leos.integration.toolbox;

import eu.europa.ec.leos.integration.toolbox.jaxb.beans.ToolboxManifest;
import eu.europa.ec.leos.test.support.LeosTest;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ToolBoxServiceTest extends LeosTest {
    private static Logger LOG = LoggerFactory.getLogger(ToolBoxServiceTest.class);

    @InjectMocks
    private ToolBoxServiceImpl toolBoxServiceImpl;

    private final String expectedMimeTypeContent = "test mime";
    private final String expectedLeosPackageName = "job.zip";


    @Before
    public void init() {
        ReflectionTestUtils.setField(toolBoxServiceImpl, "mimeTypeContent", expectedMimeTypeContent);
    }

    @Test
    public void test_createZipWsPayload() throws IOException, JAXBException{
        File legisWritePackage = null;
        File legisWritePackageInput = null;

        byte[] resultBytes;
        try {
            legisWritePackage = new File(expectedLeosPackageName);
            FileOutputStream fileOutputStream = new FileOutputStream(legisWritePackage);
            fileOutputStream.write(new byte[]{'t','e','s','t'});
            fileOutputStream.close();
            legisWritePackageInput = toolBoxServiceImpl.createZipWsPayload(legisWritePackage, "packageTest.zip");
            resultBytes = FileUtils.readFileToByteArray(legisWritePackageInput);  
        }
        finally {
            if (legisWritePackageInput != null && legisWritePackageInput.exists()) {
                legisWritePackageInput.delete();
            }
            if (legisWritePackage != null && legisWritePackage.exists()) {
                legisWritePackage.delete();
            }
        }

        assertThat(resultBytes, notNullValue());

        ZipInputStream zis = null;
        ZipEntry entry = null;
        try {
            zis = new ZipInputStream(new ByteArrayInputStream(resultBytes));
            while ((entry = zis.getNextEntry()) != null) {
                byte[] content = extractEntry(zis);
                switch (entry.getName()) {
                    case "manifest.xml":
                        ToolboxManifest toolboxManifest;
                        try {
                            JAXBContext jaxbContext = JAXBContext.newInstance(ToolboxManifest.class);

                            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                            ByteArrayInputStream input = new ByteArrayInputStream(content);
                            toolboxManifest = (ToolboxManifest) jaxbUnmarshaller.unmarshal(input);
                        } catch (JAXBException e) {
                            fail("Manifest file not valid");
                            return;
                        }
                        LOG.debug("Content of manifest: {}", new String(content));
                        
                        assertEquals(expectedLeosPackageName, toolboxManifest.getFiles().getFile().getValue());
                        assertEquals(toolboxManifest.getFiles().getFile().getId(), toolboxManifest.getActions().getAction().getActionData().getData().getValue());
                        break;
                    case "mimetype":
                        LOG.debug("Content of mimetype: {}", new String(content));
                        assertEquals(new String(content), expectedMimeTypeContent);
                        break;
                    case expectedLeosPackageName:
                        assertEquals("test", new String(content));
                        LOG.debug("Content of LegisWrite Package: {}", new String(content));
                        break;
                    default:
                        LOG.error("Entry not valid in result zip file");;
                        fail("Entry not valid in result zip file");
                        break;
                }
            }
        }
        finally {
            if (zis!=null) {
                zis.close();
            }
        }
    }

    private static byte[] extractEntry(ZipInputStream zipFile) throws IOException 
    { 
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int data = 0;
        while( ( data = zipFile.read() ) != - 1 )
        {
            output.write(data);
        }

        output.close();
        return output.toByteArray();
    }
}
