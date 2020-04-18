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
import eu.europa.ec.leos.vo.toc.AknTag;
import eu.europa.ec.leos.vo.toc.NumberingConfig;
import eu.europa.ec.leos.vo.toc.ObjectFactory;
import eu.europa.ec.leos.vo.toc.Structure;
import eu.europa.ec.leos.vo.toc.TocItem;
import eu.europa.ec.leos.vo.toc.TocRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.europa.ec.leos.vo.toc.TocItemUtils.getTocItemByNameOrThrow;

@Service
public class StructureServiceImpl implements StructureService {
    
    protected static final Logger LOG = LoggerFactory.getLogger(StructureServiceImpl.class);
    
    @Value("${leos.templates.structure.schema.path}")
    private String structureSchema;
    
    @Autowired
    private TemplateStructureService templateStructureService;
    
    protected Map<String, TocStructure> tocStructureMap = new HashMap<>();
    
    @Override
    @Cacheable(value="tocStructureList", cacheManager = "cacheManager")
    public List<TocItem> getTocItems(String docTemplate) {
        loadTocStructure(docTemplate);
        return tocStructureMap.get(docTemplate).getTocItems();
    }
    
    @Override
    @Cacheable(value="tocStructureTocRulesMap", cacheManager = "cacheManager")
    public Map<TocItem, List<TocItem>> getTocRules(String docTemplate) {
        loadTocStructure(docTemplate);
        return tocStructureMap.get(docTemplate).getTocRules();
    }
    
    @Override
    @Cacheable(value="tocStructureNumConfList", cacheManager = "cacheManager")
    public List<NumberingConfig> getNumberingConfigs(String docTemplate) {
        loadTocStructure(docTemplate);
        return tocStructureMap.get(docTemplate).getNumberingConfigs();
    }
    
    @Override
    public String getStructureName(String docTemplate) {
        loadTocStructure(docTemplate);
        return tocStructureMap.get(docTemplate).getStructureName();
    }
    
    @Override
    public String getStructureVersion(String docTemplate) {
        loadTocStructure(docTemplate);
        return tocStructureMap.get(docTemplate).getStructureVersion();
    }

    @Override
    public String getStructureDescription(String docTemplate) {
        loadTocStructure(docTemplate);
        return tocStructureMap.get(docTemplate).getStructureDescription();
    }
    
    /**
     * Lazy loading. Load from CMIS only the first time for specific template.
     * The server needs to be restarted in case the cmis xml has been changed.
     */
    private void loadTocStructure(String docTemplate) {
        byte[] structureXmlFile = templateStructureService.getStructure(docTemplate);
        final Structure structure = loadRulesFromFile(structureXmlFile);

        TocStructure tocStructure = new TocStructure();
        tocStructure.setStructureName(structure.getName());
        tocStructure.setStructureVersion(structure.getVersion());
        tocStructure.setStructureDescription(structure.getDescription());
        tocStructure.setNumberingConfigs(structure.getNumberingConfigs().getNumberingConfigs());

        List<TocItem> tocItems = structure.getTocItems().getTocItems();
        tocStructure.setTocItems(tocItems);
        tocStructure.setTocRules(buildProposalTocRules(structure, tocItems));

        tocStructureMap.put(docTemplate, tocStructure); //cache it for the next call
    }
    
    private Structure loadRulesFromFile(byte[] fileBytes) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema tocSchema = sf.newSchema(new StreamSource(loadSchema()));
            jaxbUnmarshaller.setSchema(tocSchema);
            
            Structure structure = (Structure) jaxbUnmarshaller.unmarshal(new ByteArrayInputStream(fileBytes));
            return structure;
        } catch (Exception e) {
            LOG.debug("Error loadRulesFromFile", e);
            throw new IllegalStateException("Error loading xml configurations", e);
        }
    }
    
    private InputStream loadSchema() {
        return StructureServiceImpl.class.getClassLoader().getResourceAsStream(structureSchema);
    }
    
    private Map<TocItem, List<TocItem>> buildProposalTocRules(Structure structure, List<TocItem> tocItems) {
        Map<TocItem, List<TocItem>> tocRules = new HashMap<>();
        for (TocRules.Entry entry : structure.getTocRules().getEntries()) {
            AknTag itemName = entry.getTocItem();
            TocItem tocItem = getTocItemByNameOrThrow(tocItems, itemName.value());
            List<TocItem> list = new ArrayList<>();
            for (AknTag listTocName : entry.getList().getTocItems()) {
                list.add(getTocItemByNameOrThrow(tocItems, listTocName.value()));
            }
            tocRules.put(tocItem, list);
        }
        
        return tocRules;
    }
}
