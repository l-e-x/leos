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
package eu.europa.ec.leos.repository.document;

import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.Memorandum;
import eu.europa.ec.leos.domain.cmis.metadata.MemorandumMetadata;
import eu.europa.ec.leos.model.filter.QueryFilter;
import eu.europa.ec.leos.repository.LeosRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Stream;

/**
 * Memorandum Repository implementation.
 *
 * @constructor Creates a specific Memorandum Repository, injected with a generic LEOS Repository.
 */
@Repository
public class MemorandumRepositoryImpl implements MemorandumRepository {

    private static final Logger logger = LoggerFactory.getLogger(MemorandumRepositoryImpl.class);

    private final LeosRepository leosRepository;

    @Autowired
    public MemorandumRepositoryImpl(LeosRepository leosRepository) {
        this.leosRepository = leosRepository;
    }

    @Override
    public Memorandum createMemorandum(String templateId, String path, String name, MemorandumMetadata metadata) {
        logger.debug("Creating Memorandum... [template=" + templateId + ", path=" + path + ", name=" + name + "]");
        return leosRepository.createDocument(templateId, path, name, metadata, Memorandum.class);
    }

    @Override
    public Memorandum createMemorandumFromContent(String path, String name, MemorandumMetadata metadata, byte[] content) {
        logger.debug("Creating Memorandum From Content... [path=" + path + ", name=" + name + "]");
        return leosRepository.createDocumentFromContent(path, name, metadata, Memorandum.class, LeosCategory.MEMORANDUM.name(), content);
    }

    @Override
    public Memorandum updateMemorandum(String id, MemorandumMetadata metadata) {
        logger.debug("Updating Memorandum metadata... [id=" + id + "]");
        return leosRepository.updateDocument(id, metadata, Memorandum.class);
    }

    @Override
    public Memorandum updateMemorandum(String id, byte[] content, VersionType versionType, String comment) {
        logger.debug("Updating Memorandum content... [id=" + id + "]");
        return leosRepository.updateDocument(id, content, versionType, comment, Memorandum.class);
    }

    @Override
    public Memorandum updateMemorandum(String id, MemorandumMetadata metadata, byte[] content, VersionType versionType, String comment) {
        logger.debug("Updating Memorandum metadata and content... [id=" + id + "]");
        return leosRepository.updateDocument(id, metadata, content, versionType, comment, Memorandum.class);
    }

    @Override
    public Memorandum updateMilestoneComments(String id, List<String> milestoneComments, byte[] content, VersionType versionType, String comment) {
        logger.debug("Updating Memorandum milestoneComments... [id=" + id + "]");
        return leosRepository.updateMilestoneComments(id, content, milestoneComments, versionType, comment, Memorandum.class);
    }

    @Override
    public Memorandum updateMilestoneComments(String id, List<String> milestoneComments) {
        logger.debug("Updating Memorandum milestoneComments... [id=" + id + "]");
        return leosRepository.updateMilestoneComments(id, milestoneComments, Memorandum.class);
    }

    @Override
    public Memorandum findMemorandumById(String id, boolean latest) {
        logger.debug("Finding Memorandum by ID... [id=" + id + ", latest=" + latest + "]");
        return leosRepository.findDocumentById(id, Memorandum.class, latest);
    }

    @Override
    public List<Memorandum> findMemorandumVersions(String id, boolean fetchContent) {
        logger.debug("Finding Memorandum versions... [id=" + id + "]");
        return leosRepository.findDocumentVersionsById(id, Memorandum.class, fetchContent);
    }

    @Override
    public Memorandum findMemorandumByRef(String ref) {
        logger.debug("Finding Memorandum by ref... [ref=" + ref + "]");
        return leosRepository.findDocumentByRef(ref, Memorandum.class);
    }
    
    @Override
    public List<Memorandum> findAllMinorsForIntermediate(String docRef, String currIntVersion, String prevIntVersion, int startIndex, int maxResults) {
        logger.debug("Finding Memorandum versions between intermediates...");
        return leosRepository.findAllMinorsForIntermediate(Memorandum.class, docRef, currIntVersion, prevIntVersion, startIndex, maxResults);
    }
    
    @Override
    public int findAllMinorsCountForIntermediate(String docRef, String currIntVersion, String prevIntVersion) {
    	logger.debug("Finding Memorandum minor versions count between intermediates...");
        return leosRepository.findAllMinorsCountForIntermediate(Memorandum.class, docRef, currIntVersion, prevIntVersion);
    }
    
    @Override
    public Integer findAllMajorsCount(String docRef) {
        return leosRepository.findAllMajorsCount(Memorandum.class, docRef);
    }
    
    @Override
    public List<Memorandum> findAllMajors(String docRef, int startIndex, int maxResult) {
        return leosRepository.findAllMajors(Memorandum.class, docRef, startIndex, maxResult);
    }
    
    @Override
    public List<Memorandum> findRecentMinorVersions(String documentId, String documentRef, int startIndex, int maxResults) {
        final Memorandum memorandum = leosRepository.findLatestMajorVersionById(Memorandum.class, documentId);
        return leosRepository.findRecentMinorVersions(Memorandum.class, documentRef, memorandum.getCmisVersionLabel(), startIndex, maxResults);
    }
    
    @Override
    public Integer findRecentMinorVersionsCount(String documentId, String documentRef) {
        final Memorandum memorandum = leosRepository.findLatestMajorVersionById(Memorandum.class, documentId);
        return leosRepository.findRecentMinorVersionsCount(Memorandum.class, documentRef, memorandum.getCmisVersionLabel());
    }
}
