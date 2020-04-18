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
package eu.europa.ec.leos.services.toc;

import eu.europa.ec.leos.services.content.TemplateStructureService;
import eu.europa.ec.leos.services.export.ExportHelperTest;
import eu.europa.ec.leos.test.support.LeosTest;
import eu.europa.ec.leos.vo.toc.TocItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Mockito.when;

public class StructureServiceTest extends LeosTest {

    private static Logger LOG = LoggerFactory.getLogger(ExportHelperTest.class);

    @Mock
    private TemplateStructureService templateStructureService;

    @InjectMocks
    private StructureServiceImpl structureServiceImpl = Mockito.spy(new StructureServiceImpl());

    private String docTemplate;
    
    @Before
    public void init() {
        docTemplate = "BL-023";
        byte[] bytesFile = getFileContent("/structure-test.xml");
        when(templateStructureService.getStructure(docTemplate)).thenReturn(bytesFile);
        ReflectionTestUtils.setField(structureServiceImpl, "structureSchema", "toc/schema/structure_1.xsd");
        
    }

    @Test
    public void test_getName() {
        String structureName = structureServiceImpl.getStructureName(docTemplate);
        assertEquals("structure_01.xml", structureName);
    }

    @Test
    public void test_getDescription() {
        String structureName = structureServiceImpl.getStructureDescription(docTemplate);
        assertEquals("Structure used for test", structureName);
    }

    @Test
    public void test_getVersion() {
        String version = structureServiceImpl.getStructureVersion(docTemplate);
        assertEquals("1", version);
    }

    @Test
    public void test_getBillTocItems() {
        List<TocItem> tocItems = structureServiceImpl.getTocItems(docTemplate);
        assertNotNull(tocItems);
        assertEquals(20, tocItems.size());
    }

    @Test
    public void test_getBillTocRules() {
        Map<TocItem, List<TocItem>> billTocRules = structureServiceImpl.getTocRules(docTemplate);
        assertNotNull(billTocRules);
        assertEquals(13, billTocRules.size());
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
