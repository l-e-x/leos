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
package eu.europa.ec.leos.domain.vo;

import java.util.List;
import java.util.Objects;

public class LegDocumentVO {


    private String proposalId;
    private String documentTitle;
    private String milestoneComments;
    private String legFileId;
    private String legFileName;
    private String legFileStatus;

    public String getProposalId() { return proposalId; }

    public void setProposalId(String proposalId) { this.proposalId = proposalId; }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public String getMilestoneComments() {
        return milestoneComments;
    }

    public void setMilestoneComments(List<String> milestoneComments) {
        this.milestoneComments = String.join(":", milestoneComments);
    }

    public String getLegFileId() {
        return legFileId;
    }

    public void setLegFileId(String legFileId) {
        this.legFileId = legFileId;
    }

    public String getLegFileStatus() {
        return legFileStatus;
    }

    public void setLegFileStatus(String legFileStatus) {
        this.legFileStatus = legFileStatus;
    }

    public String getLegFileName() {
        return legFileName;
    }

    public void setLegFileName(String legFileName) {
        this.legFileName = legFileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LegDocumentVO that = (LegDocumentVO) o;
        return  Objects.equals(getProposalId(), that.getProposalId()) &&
                Objects.equals(getDocumentTitle(), that.getDocumentTitle()) &&
                Objects.equals(getMilestoneComments(), that.getMilestoneComments()) &&
                Objects.equals(getLegFileId(), that.getLegFileId()) &&
                Objects.equals(getLegFileName(), that.getLegFileName()) &&
                Objects.equals(getLegFileStatus(), that.getLegFileStatus());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProposalId(), getDocumentTitle(), getMilestoneComments(), getLegFileId(), getLegFileName(), getLegFileStatus());
    }
}
