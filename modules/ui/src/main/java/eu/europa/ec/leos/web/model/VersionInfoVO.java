/*
 * Copyright 2018 European Commission
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

public class VersionInfoVO {

    String documentVersion;
    String lastModifiedBy;
    String entity;
    String lastModificationInstant;
    boolean isMajor;

    public VersionInfoVO(String documentVersion, String lastModifiedBy, String entity, String lastModificationInstant, boolean isMajor) {
        super();
        this.documentVersion = documentVersion;
        this.lastModifiedBy = lastModifiedBy;
        this.entity = entity;
        this.lastModificationInstant = lastModificationInstant;
        this.isMajor = isMajor;
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
    
    public boolean isMajor() {
        return isMajor;
    }
    
    public void setMajor(boolean isMajor) {
        this.isMajor = isMajor;
    }
}
