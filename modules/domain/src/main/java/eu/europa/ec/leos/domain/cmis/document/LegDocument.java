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
package eu.europa.ec.leos.domain.cmis.document;

import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import io.atlassian.fugue.Option;

import java.time.Instant;
import java.util.List;

public final class LegDocument extends LeosDocument {
    private final List<String> milestoneComments;
    private final String initialCreatedBy;
    private final Instant initialCreationInstant;
    private final String jobId;
    private final Instant jobDate;
    private final LeosLegStatus status;
    private final List<String> containedDocuments;

    public LegDocument(String id, String name, String createdBy, Instant creationInstant, String lastModifiedBy,
                       Instant lastModificationInstant, String versionSeriesId, String cmisVersionLabel, String versionLabel,
                       String versionComment, VersionType versionType, boolean isLatestVersion, List<String> milestoneComments,
                       Option<Content> content, String initialCreatedBy, Instant initialCreationInstant,
                       String jobId, Instant jobDate, LeosLegStatus status, List<String> containedDocuments) {

        super(LeosCategory.LEG, id, name, createdBy, creationInstant, lastModifiedBy, lastModificationInstant,
                versionSeriesId, cmisVersionLabel, versionLabel, versionComment, versionType, isLatestVersion, content);
        this.milestoneComments = milestoneComments;
        this.initialCreatedBy = initialCreatedBy;
        this.initialCreationInstant = initialCreationInstant;
        this.jobId = jobId;
        this.jobDate = jobDate;
        this.status = status;
        this.containedDocuments = containedDocuments;
    }

    public List<String> getMilestoneComments() {
        return milestoneComments;
    }

    public String getInitialCreatedBy() {
        return initialCreatedBy;
    }

    public Instant getInitialCreationInstant() {
        return initialCreationInstant;
    }

    public String getJobId() {
        return jobId;
    }

    public Instant getJobDate() {
        return jobDate;
    }

    public LeosLegStatus getStatus() {
        return status;
    }

    public List<String> getContainedDocuments() {
        return containedDocuments;
    }
}