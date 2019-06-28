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
package eu.europa.ec.leos.repository.store;

import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.document.LegDocument;
import eu.europa.ec.leos.domain.cmis.document.LeosDocument;
import eu.europa.ec.leos.repository.LeosRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Package Repository implementation.
 *
 * @constructor Creates a specific Package Repository, injected with a generic LEOS Repository.
 */
@Repository
public class PackageRepositoryImpl implements PackageRepository {

    private static final Logger logger = LoggerFactory.getLogger(PackageRepositoryImpl.class);

    private final LeosRepository leosRepository;

    @Autowired
    public PackageRepositoryImpl(LeosRepository leosRepository) {
        this.leosRepository = leosRepository;
    }

    @Override
    public LeosPackage createPackage(String path, String name) {
        logger.debug("Creating Package... [path=" + path + ", name=" + name + "]");
        return leosRepository.createPackage(path, name);
    }

    @Override
    public void deletePackage(String path) {
        logger.debug("Deleting Package... [path=" + path + "]");
        leosRepository.deletePackage(path);
    }

    @Override
    public LegDocument createLegDocumentFromContent(String path, String name, String jobId, List<String> milestoneComments, byte[] contentBytes, LeosLegStatus status) {
        logger.debug("Creating Leg document from content... [path=" + path + ", name=" + name + "]");
        return leosRepository.createLegDocumentFromContent(path, name, jobId, milestoneComments, contentBytes, status);
    }

    @Override
    public LegDocument findLegDocumentById(String id, boolean latest) {
        logger.debug("Finding  Leg document by ID... [id=" + id + ", latest=" + latest + "]");
        return leosRepository.findDocumentById(id, LegDocument.class, latest);
    }

    @Override
    public LegDocument updateLegDocument(String id, LeosLegStatus status) {
        logger.debug("Updating Leg document status... [id=" + id + ", status=" + status.name() + "]");
        return leosRepository.updateLegDocument(id, status);
    }

    @Override
    public LegDocument updateLegDocument(String id, LeosLegStatus status, byte[] content, boolean major, String comment) {
        logger.debug("Updating Leg document status and content... [id=" + id + ", status=" + status.name() + ", content size=" + content.length + ", major=" + major + ", comment=" + comment + "]");
        return leosRepository.updateLegDocument(id, status, content, major, comment);
    }

    @Override
    public LeosPackage findPackageByDocumentId(String documentId) {
        logger.debug("Finding Package by document ID... [documentId=" + documentId + "]");
        return leosRepository.findPackageByDocumentId(documentId);
    }

    @Override
    public <D extends LeosDocument> List<D> findDocumentsByPackagePath(String path, Class<? extends D> type, boolean fetchContent) {
        logger.debug("Finding document by package path... [path=" + path + ", type=" + type.getSimpleName() + "]");
        return leosRepository.findDocumentsByParentPath(path, type, false, fetchContent);
    }

    @Override
    public <D extends LeosDocument> D findDocumentByPackagePathAndName(String path, String name, Class<? extends D> type) {
        logger.debug("Finding document by package path... [path=" + path + ", name=" + name + ", type=" + type.getSimpleName() + "]");
        return leosRepository.findDocumentByParentPath(path, name, type);
    }

    @Override
    public <D extends LeosDocument> List<D> findDocumentsByPackageId(String id, Class<? extends D> type, boolean allVersion, boolean fetchContent) {
        logger.debug("Finding document by package id... [pkgId=" + id + ", type=" + type.getSimpleName() + "]");
        return leosRepository.findDocumentsByPackageId(id, type, allVersion, fetchContent);
    }

    @Override
    public <D extends LeosDocument> List<D> findDocumentsByUserId(String userId, Class<? extends D> type, String leosAuthority) {
        logger.debug("Finding document by user... userId=" + userId);
        return leosRepository.findDocumentsByUserId(userId, type, leosAuthority);
    }

    @Override
    public <D extends LeosDocument> List<D> findDocumentsByStatus(LeosLegStatus status, Class<? extends D> type) {
        logger.debug("Finding documents by status... status=" + status);
        return leosRepository.findDocumentsByStatus(status, type);
    }
}
