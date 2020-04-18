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
package eu.europa.ec.leos.services.milestone;


import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import eu.europa.ec.leos.domain.cmis.document.LegDocument;
import eu.europa.ec.leos.services.export.LegPackage;
import eu.europa.ec.leos.services.store.LegService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.StampedLock;

public abstract class AbstractMilestoneService implements MilestoneService{

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMilestoneService.class);

    protected final LegService legService;

    private final StampedLock updateMilestoneLock = new StampedLock();

    public AbstractMilestoneService(LegService legService) {
        this.legService = legService;
    }

    @Nonnull
    protected abstract LegDocument createLegDocument(String proposalId, LegPackage legPackage) throws Exception;

    protected abstract LegPackage createLegPackage(String proposalId) throws IOException;

    @Override
    public LegDocument createMilestone(String proposalId, String milestoneComment) throws Exception {
        LOG.trace("Creating Milestone for Proposal... [proposalId={}]", proposalId);
        File legFileAsZip = null;
        try{
            LegPackage legPackage = createLegPackage(proposalId);
            legPackage.addMilestoneComment(milestoneComment);
            legFileAsZip = legPackage.getFile();
            LegDocument legDocument = createLegDocument(proposalId, legPackage);
            LOG.trace("Created LegDocument... [legDocumentId={}]", legDocument.getId());
            return legDocument;
        } finally {
            if (legFileAsZip != null && legFileAsZip.exists()) {
                if (!legFileAsZip.delete()) {
                    LOG.warn("Couldn't delete the leg file {} for proposal {}", legFileAsZip.getName(), proposalId);
                }
            }
        }
    }

    @Override
    public LegDocument updateMilestone(String legId, LeosLegStatus status) {
        LOG.trace("Updating Leg document status... [legId={}, status={}]", legId, status.name());
        if(legId != null && !legId.isEmpty()){
            long stamp = updateMilestoneLock.writeLock();
            try{
                return legService.updateLegDocument(legId, status);
            } finally {
                updateMilestoneLock.unlockWrite(stamp);
            }
        } else {
            return null;
        }
    }

    /**
     * Same logic used by Callback and Scheduler.
     * In case a scheduler runs before the callback,
     * or the findLegDocumentByStatus(IN_PREPARATION) inside the scheduler is executed before the callback changes the status,
     * synchronized on the method makes sure that each thread completes its job in atomicity.
     * ATTENTION: If this case happened (concurrency scenario) leg file will be increased with two major versions (1 from update of callback, 1 from the scheduler)
     * No mechanism to skip the update if the file is already in FILE_READY status
     */
    @Override
    public LegDocument updateMilestoneRendition(String documentId, String jobId, byte[] pdfJobZip, byte[] wordJobZip) {
        if (jobId != null && !jobId.isEmpty()
            && documentId != null && !documentId.isEmpty()
            && pdfJobZip != null && wordJobZip != null) {
            long stamp = updateMilestoneLock.writeLock();
            try {
                if (pdfJobZip.length > 0 && wordJobZip.length > 0) {
                    LOG.trace("Call to updateMilestoneRendition for Leg document  with jobId={} in the same package with any document that has documentId={} may be SUCCESSFUL.", jobId, documentId);
                    return updateMilestone(documentId, jobId, pdfJobZip, wordJobZip);
                } else {
                    LOG.warn("The files are empty: documentId={}, jobId={}, pdfJobZip={} and wordJobZip={}. Changing leg file status to {}.",
                            documentId, jobId, pdfJobZip, wordJobZip, LeosLegStatus.FILE_ERROR.name());
                    return updateMilestone(documentId, jobId);
                }
            } catch (Exception e) {
                LOG.error("coDeCallback for documentId={} with jobId={} FAILED. Changing leg file status to {}.",
                        documentId, jobId, LeosLegStatus.FILE_ERROR.name());
                return updateMilestone(documentId, jobId);
            } finally {
                updateMilestoneLock.unlockWrite(stamp);
            }
        } else {
            LOG.warn("documentId, jobId either null or empty: documentId={} with jobId={}", documentId, jobId);
            return null;
        }
    }

    private LegDocument updateMilestone(String documentId, String jobId) {
        LOG.trace("Updating the status to {} of the Leg document that has jobId={} and is in the same package with any document that has documentId={}.",LeosLegStatus.FILE_ERROR.name(), jobId, documentId);
        LegDocument legDocument = legService.findLegDocumentByAnyDocumentIdAndJobId(documentId, jobId);
        if(legDocument != null ){
            if(!LeosLegStatus.FILE_ERROR.equals(legDocument.getStatus())){
                return legService.updateLegDocument(legDocument.getId(), LeosLegStatus.FILE_ERROR);
            } else {
                return legDocument;
            }
        } else {
            return null;
        }
    }

    private LegDocument updateMilestone(String documentId, String jobId, byte[] pdfJobZip, byte[] wordJobZip) {
        LOG.trace("Updating status to {} and content with pdf and word renditions of the Leg document that has jobId={} and is in the same package with any document that has documentId={}.", LeosLegStatus.FILE_READY.name(), jobId, documentId);
        LegDocument legDocument = legService.findLegDocumentByAnyDocumentIdAndJobId(documentId, jobId);
        if(legDocument != null){
            if(LeosLegStatus.IN_PREPARATION.equals(legDocument.getStatus())){
                return legService.updateLegDocument(legDocument.getId(), pdfJobZip, wordJobZip);
            } else {
                return legDocument;
            }
        } else {
            return null;
        }
    }
}
