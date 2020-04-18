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
package eu.europa.ec.leos.model.action;

import eu.europa.ec.leos.domain.cmis.common.VersionType;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class VersionVO {
    
    private VersionType versionType;
    private String documentId;
    private String cmisVersionNumber;
    private String versionNumber;
    private String updatedDate;
    private String username;
    private String versionedReference;
    private List<VersionVO> subVersions = new ArrayList<>();
    private CheckinCommentVO checkinCommentVO;
    private boolean mostRecentVersion;
    
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public VersionType getVersionType() {
        return versionType;
    }
    
    public void setVersionType(VersionType versionType) {
        this.versionType = versionType;
    }
    
    public void setCmisVersionNumber(String cmisVersionNumber) {
        this.cmisVersionNumber = cmisVersionNumber;
    }
    
    public String getCmisVersionNumber() {
        return cmisVersionNumber;
    }
    
    public String getVersionNumber() {
        return versionNumber;
    }
    
    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }
    
    public String getUpdatedDate() {
        return updatedDate;
    }
    
    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }
    
    public void setUpdatedDate(Instant updatedDate) {
        this.updatedDate = dateFormatter.format(Date.from(updatedDate));
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }

    public String getVersionedReference() {
        return versionedReference;
    }

    public void setVersionedReference(String versionedReference) {
        this.versionedReference = versionedReference;
    }

    public List<VersionVO> getSubVersions() {
        return subVersions;
    }
    
    public void setSubVersions(List<VersionVO> subVersions) {
        this.subVersions = subVersions;
    }
    
    public CheckinCommentVO getCheckinCommentVO() {
        return checkinCommentVO;
    }
    
    public void setCheckinCommentVO(CheckinCommentVO checkinCommentVO) {
        this.checkinCommentVO = checkinCommentVO;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public boolean isMostRecentVersion() {
        return mostRecentVersion;
    }

    public void setMostRecentVersion(boolean mostRecentVersion) {
        this.mostRecentVersion = mostRecentVersion;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(versionType, documentId, cmisVersionNumber, versionNumber, updatedDate, username);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        final VersionVO other = (VersionVO) obj;
        return Objects.equals(this.versionType, other.versionType) &&
                Objects.equals(this.documentId, other.documentId) &&
                Objects.equals(this.cmisVersionNumber, other.cmisVersionNumber) &&
                Objects.equals(this.versionNumber, other.versionNumber) &&
                Objects.equals(this.updatedDate, other.updatedDate) &&
                Objects.equals(this.username, other.username);
    }
    
    @Override
    public String toString() {
        return "[versionType: " + versionType
                + ", documentId: " + documentId
                + ", cmisVersionNumber: " + cmisVersionNumber
                + ", versionNumber: " + versionNumber
                + ", updatedDate: " + updatedDate
                + ", username: " + username
                + ", subVersions: " + subVersions
                + ", checkinCommentVO: " + checkinCommentVO
                + "]" + "\n";
    }
}
