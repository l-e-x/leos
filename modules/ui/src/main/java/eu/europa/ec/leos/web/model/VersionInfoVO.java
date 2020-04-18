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
package eu.europa.ec.leos.web.model;

import eu.europa.ec.leos.domain.cmis.common.VersionType;

public class VersionInfoVO {

    String documentVersion;
    String lastModifiedBy;
    String entity;
    String lastModificationInstant;
    VersionType versionType;

    public VersionInfoVO(String documentVersion, String lastModifiedBy, String entity, String lastModificationInstant, VersionType versionType) {
        super();
        this.documentVersion = documentVersion;
        this.lastModifiedBy = lastModifiedBy;
        this.entity = entity;
        this.lastModificationInstant = lastModificationInstant;
        this.versionType = versionType;
    }
    
    public String getDocumentVersion() {
        return documentVersion;
    }
    
    public void setDocumentVersion(String documentVersion) {
        this.documentVersion = documentVersion;
    }
    
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }
    
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
    
    public String getEntity() {
        return entity;
    }
    
    public void setEntity(String entity) {
        this.entity = entity;
    }
    
    public String getLastModificationInstant() {
        return lastModificationInstant;
    }
    
    public void setLastModificationInstant(String lastModificationInstant) {
        this.lastModificationInstant = lastModificationInstant;
    }
    
    public VersionType getVersionType() {
        return versionType;
    }
    
    public void setVersionType(VersionType versionType) {
        this.versionType = versionType;
    }
}
