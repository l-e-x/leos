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

import com.google.common.base.Stopwatch;
import cool.graph.cuid.Cuid;
import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.Annex;
import eu.europa.ec.leos.domain.cmis.metadata.AnnexMetadata;
import eu.europa.ec.leos.domain.common.TocMode;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.action.VersionVO;
import eu.europa.ec.leos.model.annex.AnnexStructureType;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.repository.document.AnnexRepository;
import eu.europa.ec.leos.services.document.util.DocumentVOProvider;
import eu.europa.ec.leos.services.support.VersionsUtil;
import eu.europa.ec.leos.services.support.xml.NumberProcessor;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper;
import eu.europa.ec.leos.services.support.xml.XmlNodeProcessor;
import eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper;
import eu.europa.ec.leos.services.validation.ValidationService;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.DOC;
import static eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper.createValueMap;

@Service
public class AnnexServiceImpl implements AnnexService {

    private static final Logger LOG = LoggerFactory.getLogger(AnnexServiceImpl.class);

    private static final String ANNEX_NAME_PREFIX = "annex_";
    private static final String ANNEX_DOC_EXTENSION = ".xml";

    private final AnnexRepository annexRepository;
    private final XmlNodeProcessor xmlNodeProcessor;
    private final XmlContentProcessor xmlContentProcessor;
    private final NumberProcessor numberingProcessor;
    private final XmlNodeConfigHelper xmlNodeConfigHelper;
    private final DocumentVOProvider documentVOProvider;
    private final ValidationService validationService;
    private final MessageHelper messageHelper;

    private final XmlTableOfContentHelper xmlTableOfContentHelper;
    
    @Autowired
    AnnexServiceImpl(AnnexRepository annexRepository, XmlNodeProcessor xmlNodeProcessor,
                     XmlContentProcessor xmlContentProcessor, NumberProcessor numberingProcessor, XmlNodeConfigHelper xmlNodeConfigHelper,
                     ValidationService validationService, DocumentVOProvider documentVOProvider, XmlTableOfContentHelper xmlTableOfContentHelper,
                     MessageHelper messageHelper) {
        this.annexRepository = annexRepository;
        this.xmlNodeProcessor = xmlNodeProcessor;
        this.xmlContentProcessor = xmlContentProcessor;
        this.numberingProcessor = numberingProcessor;
        this.xmlNodeConfigHelper = xmlNodeConfigHelper;
        this.validationService = validationService;
        this.documentVOProvider = documentVOProvider;
        this.messageHelper = messageHelper;
        this.xmlTableOfContentHelper = xmlTableOfContentHelper;
    }

    @Override
    public Annex createAnnex(String templateId, String path, AnnexMetadata metadata, String actionMessage, byte[] content) {
        LOG.trace("Creating Annex... [templateId={}, path={}, metadata={}]", templateId, path, metadata);
        String annexUid = getAnnexUid();
        String name = generateAnnexName(annexUid);
        metadata = metadata.withRef(generateAnnexReference(annexUid));
        Annex annex = annexRepository.createAnnex(templateId, path, name, metadata);
        byte[] updatedBytes = updateDataInXml((content == null) ? getContent(annex) : content, metadata);
        return annexRepository.updateAnnex(annex.getId(), metadata, updatedBytes, VersionType.MINOR, actionMessage);
    }

    @Override
    public Annex createAnnexFromContent(String path, AnnexMetadata metadata, String actionMessage, byte[] content) {
        LOG.trace("Creating Annex From Content... [path={}, metadata={}]", path, metadata);
        String annexUid = getAnnexUid();
        String name = generateAnnexName(annexUid);
        metadata = metadata.withRef(generateAnnexReference(annexUid));
        byte[] updatedBytes = updateDataInXml(content, metadata);
        Annex annex = annexRepository.createAnnexFromContent(path, name, metadata, updatedBytes);
        return annexRepository.updateAnnex(annex.getId(), metadata, updatedBytes, VersionType.MINOR, actionMessage);
    }

    @Override
    public void deleteAnnex(Annex annex) {
        LOG.trace("Deleting Annex... [id={}]", annex.getId());
        annexRepository.deleteAnnex(annex.getId());
    }

    @Override
    public Annex findAnnex(String id) {
        LOG.trace("Finding Annex... [id={}]", id);
        return annexRepository.findAnnexById(id, true);
    }

    @Override
    @Cacheable(value="docVersions", cacheManager = "cacheManager")
    public Annex findAnnexVersion(String id) {
        LOG.trace("Finding Annex version... [it={}]", id);
        return annexRepository.findAnnexById(id, false);
    }

    @Override
    public Annex updateAnnex(Annex annex, byte[] updatedAnnexContent, VersionType versionType, String comment) {
        LOG.trace("Updating Annex Xml Content... [id={}]", annex.getId());
        
        annex = annexRepository.updateAnnex(annex.getId(), updatedAnnexContent, versionType, comment); 
        
        //call validation on document with updated content
        validationService.validateDocumentAsync(documentVOProvider.createDocumentVO(annex, updatedAnnexContent));

        return annex;
    }

    @Override
    public Annex updateAnnex(Annex annex, AnnexMetadata updatedMetadata, VersionType versionType, String comment) {
        LOG.trace("Updating Annex... [id={}, updatedMetadata={}, versionType={}, comment={}]", annex.getId(), updatedMetadata, versionType, comment);
        Stopwatch stopwatch = Stopwatch.createStarted();
        byte[] updatedBytes = updateDataInXml(getContent(annex), updatedMetadata);
        
        annex = annexRepository.updateAnnex(annex.getId(), updatedMetadata, updatedBytes, versionType, comment);
        
        //call validation on document with updated content
        validationService.validateDocumentAsync(documentVOProvider.createDocumentVO(annex, updatedBytes));
        
        LOG.trace("Updated Annex ...({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return annex;
    }

    @Override
    public Annex updateAnnexWithMetadata(Annex annex, byte[] updatedAnnexContent, AnnexMetadata metadata, VersionType versionType, String comment) {
        LOG.trace("Updating Annex... [id={}, updatedMetadata={}, versionType={}, comment={}]", annex.getId(), metadata, versionType, comment);
        Stopwatch stopwatch = Stopwatch.createStarted();
        updatedAnnexContent = updateDataInXml(updatedAnnexContent, metadata);
        
        annex = annexRepository.updateAnnex(annex.getId(), metadata, updatedAnnexContent, versionType, comment);
        
        //call validation on document with updated content
        validationService.validateDocumentAsync(documentVOProvider.createDocumentVO(annex, updatedAnnexContent));
        
        LOG.trace("Updated Annex ...({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return annex;
    }
    
    @Override
    public Annex updateAnnex(String annexId, AnnexMetadata updatedMetadata) {
        LOG.trace("Updating Annex... [id={}, updatedMetadata={}]", annexId, updatedMetadata);
        return annexRepository.updateAnnex(annexId, updatedMetadata);
    }

    @Override
    public Annex updateAnnexWithMilestoneComments(Annex annex, List<String> milestoneComments, VersionType versionType, String comment){
        LOG.trace("Updating Annex... [id={}, milestoneComments={}, versionType={}, comment={}]", annex.getId(), milestoneComments, versionType, comment);
        final byte[] updatedBytes = getContent(annex);
        annex = annexRepository.updateMilestoneComments(annex.getId(), milestoneComments, updatedBytes, versionType, comment);
        return annex;
    }

    @Override
    public Annex updateAnnexWithMilestoneComments(String annexId, List<String> milestoneComments){
        LOG.trace("Updating Annex... [id={}, milestoneComments={}]", annexId, milestoneComments);
        return annexRepository.updateMilestoneComments(annexId, milestoneComments);
    }

    @Override
    public List<Annex> findVersions(String id) {
        LOG.trace("Finding Annex versions... [id={}]", id);
        //LEOS-2813 We have memory issues is we fetch the content of all versions.
        return annexRepository.findAnnexVersions(id,false);
    }

    @Override
    public Annex createVersion(String id, VersionType versionType, String comment) {
        LOG.trace("Creating Annex version... [id={}, versionType={}, comment={}]", id, versionType, comment);
        final Annex annex = findAnnex(id);
        final AnnexMetadata metadata = annex.getMetadata().getOrError(() -> "Annex metadata is required!");
        final Content content = annex.getContent().getOrError(() -> "Annex content is required!");
        final byte[] contentBytes = content.getSource().getBytes();
        return annexRepository.updateAnnex(id, metadata, contentBytes, versionType, comment);
    }
    
    @Override
    public List<TableOfContentItemVO> getTableOfContent(Annex annex, TocMode mode) {
        Validate.notNull(annex, "Annex is required");
        final Content content = annex.getContent().getOrError(() -> "Annex content is required!");
        final byte[] annexContent = content.getSource().getBytes();
        return xmlTableOfContentHelper.buildTableOfContent(DOC, annexContent, mode);
    }
    
    @Override
    public Annex saveTableOfContent(Annex annex, List<TableOfContentItemVO> tocList, AnnexStructureType structureType, String actionMsg, User user) {
        Validate.notNull(annex, "Annex is required");
        Validate.notNull(tocList, "Table of content list is required");
        byte[] newXmlContent;
        
        newXmlContent = xmlContentProcessor.createDocumentContentWithNewTocList(tocList, getContent(annex), user);
        switch(structureType) {
            case ARTICLE:
                newXmlContent = numberingProcessor.renumberArticles(newXmlContent);
                break;
            case LEVEL:
                newXmlContent = numberingProcessor.renumberLevel(newXmlContent);
                break;
        }
        newXmlContent = xmlContentProcessor.doXMLPostProcessing(newXmlContent);

        return updateAnnex(annex, newXmlContent, VersionType.MINOR, actionMsg);
    }

    private byte[] getContent(Annex annex) {
        final Content content = annex.getContent().getOrError(() -> "Annex content is required!");
        return content.getSource().getBytes();
    }

    private byte[] updateDataInXml(final byte[] content, AnnexMetadata dataObject) {
        byte[] updatedBytes = xmlNodeProcessor.setValuesInXml(content, createValueMap(dataObject), xmlNodeConfigHelper.getConfig(dataObject.getCategory()));
        return xmlContentProcessor.doXMLPostProcessing(updatedBytes);
    }

    private String getAnnexUid() {
        return Cuid.createCuid();
    }

    private String generateAnnexReference(String annexUid) {
        return ANNEX_NAME_PREFIX + annexUid;
    }

    private String generateAnnexName(String annexUid) {
        return generateAnnexReference(annexUid) + ANNEX_DOC_EXTENSION;
    }

    @Override
    public Annex findAnnexByRef(String ref) {
        LOG.trace("Finding Annex by ref... [ref=" + ref + "]");
        return annexRepository.findAnnexByRef(ref);
    }
    
    @Override
    public List<VersionVO> getAllVersions(String documentId, String docRef) {
        // TODO temporary call. paginated loading will be implemented in the future Story
        List<Annex> majorVersions = findAllMajors(docRef, 0, 9999);
        LOG.trace("Found {} majorVersions for [id={}]", majorVersions.size(), documentId);
        
        List<VersionVO> majorVersionsVO = VersionsUtil.buildVersionVO(majorVersions, messageHelper);
        return majorVersionsVO;
    }
    
    @Override
    public List<Annex> findAllMinorsForIntermediate(String docRef, String currIntVersion, int startIndex, int maxResults) {
        final String prevIntVersion = calculatePreviousVersion(currIntVersion);
        return annexRepository.findAllMinorsForIntermediate(docRef, currIntVersion, prevIntVersion, startIndex, maxResults);
    }
    
    @Override
    public int findAllMinorsCountForIntermediate(String docRef, String currIntVersion) {
        final String prevVersion = calculatePreviousVersion(currIntVersion);
        return annexRepository.findAllMinorsCountForIntermediate(docRef, currIntVersion, prevVersion);
    }
    
    private String calculatePreviousVersion(String currIntVersion) {
        final String prevVersion;
        String[] str = currIntVersion.split("\\.");
        if (str.length != 2) {
            throw new IllegalArgumentException("CMIS Version number should be in the format x.y");
        } else {
            int curr = Integer.parseInt(str[0]);
            int prev = curr - 1;
            prevVersion = prev + "." + "0";
        }
        return prevVersion;
    }
    
    @Override
    public Integer findAllMajorsCount(String docRef) {
        return annexRepository.findAllMajorsCount(docRef);
    }

    @Override
    public List<Annex> findAllMajors(String docRef, int startIndex, int maxResults) {
        return annexRepository.findAllMajors(docRef, startIndex, maxResults);
    }
    
    @Override
    public List<Annex> findRecentMinorVersions(String documentId, String documentRef, int startIndex, int maxResults) {
        return annexRepository.findRecentMinorVersions(documentId, documentRef, startIndex, maxResults);
    }

    @Override
    public Integer findRecentMinorVersionsCount(String documentId, String documentRef) {
        return annexRepository.findRecentMinorVersionsCount(documentId, documentRef);
    }

    @Override
    public List<String> getAncestorsIdsForElementId(Annex annex, List<String> elementIds) {
        Validate.notNull(annex, "Annex is required");
        Validate.notNull(elementIds, "Element id is required");
        List<String> ancestorIds = new ArrayList<String>();
        byte[] content = getContent(annex);
        for (String elementId : elementIds) {
            ancestorIds.addAll(xmlContentProcessor.getAncestorsIdsForElementId(content, elementId));
        }
        return ancestorIds;
    }
}
