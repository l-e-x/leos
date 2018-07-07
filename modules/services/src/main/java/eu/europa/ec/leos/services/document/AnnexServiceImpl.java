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
package eu.europa.ec.leos.services.document;

import com.google.common.base.Stopwatch;
import cool.graph.cuid.Cuid;
import eu.europa.ec.leos.domain.document.Content;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Annex;
import eu.europa.ec.leos.domain.document.LeosMetadata.AnnexMetadata;
import eu.europa.ec.leos.repository.document.AnnexRepository;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper;
import eu.europa.ec.leos.services.support.xml.XmlNodeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper.createValueMap;

@Service
public class AnnexServiceImpl implements AnnexService {

    private static final Logger LOG = LoggerFactory.getLogger(AnnexServiceImpl.class);

    private static final String ANNEX_NAME_PREFIX = "annex_";

    private final AnnexRepository annexRepository;
    private final XmlNodeProcessor xmlNodeProcessor;
    private final XmlContentProcessor xmlContentProcessor;
    private final XmlNodeConfigHelper xmlNodeConfigHelper;

    AnnexServiceImpl(AnnexRepository annexRepository,
                     XmlNodeProcessor xmlNodeProcessor,
                     XmlContentProcessor xmlContentProcessor,
                     XmlNodeConfigHelper xmlNodeConfigHelper) {
        this.annexRepository = annexRepository;
        this.xmlNodeProcessor = xmlNodeProcessor;
        this.xmlContentProcessor = xmlContentProcessor;
        this.xmlNodeConfigHelper = xmlNodeConfigHelper;
    }

    @Override
    public Annex createAnnex(String templateId, String path, AnnexMetadata metadata) {
        LOG.trace("Creating Annex... [templateId={}, path={}, metadata={}]", templateId, path, metadata);
        String name = generateAnnexName();
        Annex annex = annexRepository.createAnnex(templateId, path, name, metadata);
        byte[] updatedBytes = updateDataInXml(annex, metadata);
        return annexRepository.updateAnnex(annex.getId(), metadata, updatedBytes, false,"Metadata updated." );
    }

    @Override
    public void deleteAnnex(Annex annex) {
        LOG.trace("Deleting Annex... [id={}]", annex.getId());
        annexRepository.deleteAnnex(annex.getId());
    }

    @Override
    public Annex findAnnex(String id) {
        LOG.trace("Finding Annex... [it={}]", id);
        return annexRepository.findAnnexById(id, true);
    }

    @Override
    public Annex updateAnnex(String annexId, byte[] updatedAnnexContent, boolean major, String comment) {
        LOG.trace("Updating Annex Xml Content... [id={}]", annexId);
        return annexRepository.updateAnnex(annexId, updatedAnnexContent, major, comment);
    }

    @Override
    public Annex updateAnnex(Annex annex, AnnexMetadata updatedMetadata, boolean major, String comment) {
        LOG.trace("Updating Annex... [id={}, updatedMetadata={}]", annex.getId(), updatedMetadata);
        Stopwatch stopwatch = Stopwatch.createStarted();
        byte[] updatedBytes = updateDataInXml(annex, updatedMetadata); //FIXME: Do we need latest data again??
        annex = annexRepository.updateAnnex(annex.getId(), updatedMetadata, updatedBytes, major, comment);
        LOG.trace("Updated Annex ...({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return annex;
    }

    private byte[] updateDataInXml(Annex annex, AnnexMetadata dataObject) {
        final Content content = annex.getContent().getOrError(() -> "Annex content is required!");
        final byte[] xmlBytes = content.getSource().getByteString().toByteArray();
        byte[] updatedBytes = xmlNodeProcessor.setValuesInXml(xmlBytes, createValueMap(dataObject), xmlNodeConfigHelper.getConfig(annex.getCategory()));
        return xmlContentProcessor.doXMLPostProcessing(updatedBytes);
    }

    private String generateAnnexName() {
        return ANNEX_NAME_PREFIX + Cuid.createCuid();
    }
    
    @Override
    public List<Annex> findVersions(String id) {
        LOG.trace("Finding Annex versions... [id={}]", id);
        return annexRepository.findAnnexVersions(id);
    }

    @Override
    public Annex createVersion(String id, boolean major, String comment) {
        LOG.trace("Creating Annex version... [id={}, major={}, comment={}]", id, major, comment);
        final Annex annex = findAnnex(id);
        final AnnexMetadata metadata = annex.getMetadata().getOrError(() -> "Annex metadata is required!");
        final Content content = annex.getContent().getOrError(() -> "Annex content is required!");
        final byte[] contentBytes = content.getSource().getByteString().toByteArray();
        return annexRepository.updateAnnex(id, metadata, contentBytes, major, comment);
    }
}
