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
import io.atlassian.fugue.Option;

import java.time.Instant;
import java.util.List;

public final class LegDocument extends LeosDocument {
    private final List<String> milestoneComments;
    private final String jobId;
    private final Instant jobDate;
    private final LeosLegStatus status;

    public LegDocument(String id, String name, String createdBy, Instant creationInstant, String lastModifiedBy,
                       Instant lastModificationInstant, String versionSeriesId, String versionLabel, String versionComment,
                       boolean isMajorVersion, boolean isLatestVersion, List<String> milestoneComments, Option<Content> content,
                       String jobId, Instant jobDate, LeosLegStatus status) {

        super(LeosCategory.LEG, id, name, createdBy, creationInstant, lastModifiedBy, lastModificationInstant,
                versionSeriesId, versionLabel, versionComment, isMajorVersion, isLatestVersion, content);
        this.milestoneComments = milestoneComments;
        this.jobId = jobId;
        this.jobDate = jobDate;
        this.status = status;
    }

    public List<String> getMilestoneComments() {
        return milestoneComments;
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
}
