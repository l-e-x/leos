/*
 * Copyright 2017 European Commission
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

import eu.europa.ec.leos.domain.document.LeosMetadata.ProposalMetadata;

/* View Object*/
public class MetadataVO {

    private String docStage;// title is composition of ( docStage, docType, docPurpose)
    private String docType;
    private String docPurpose;

    private String packageTitle;   
    private String internalRef;
    private SecurityLevel securityLevel;
    private String template;
    private String language; // always should be set to language Code
    private boolean eeaRelevance;

    public MetadataVO(){}//added for early binding

    public MetadataVO(String docStage, String docType, String docPurpose, String packageTitle,  String internalRef,
            SecurityLevel securityLevel, String template, String language, boolean eeaRelevance ) {
        this.docStage = docStage;
        this.docType = docType;
        this.docPurpose = docPurpose;
        this.packageTitle = packageTitle;      
        this.internalRef = internalRef;
        this.securityLevel = securityLevel;
        this.eeaRelevance = eeaRelevance;
        this.template = template;
        this.language = language;
    }

    public MetadataVO(ProposalMetadata metadata, String template, String language) {
        this.docStage = metadata.getStage();
        this.docType = metadata.getType();
        this.docPurpose = metadata.getPurpose();
        this.template = template;
        this.language = language;
    }

    public String getDocStage() {
        return docStage;
    }

    public void setDocStage(String docStage) {
        this.docStage = docStage;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getDocPurpose() {
        return docPurpose;
    }

    public void setDocPurpose(String docPurpose) {
        this.docPurpose = docPurpose;
    }

    public String getPackageTitle() {
        return packageTitle;
    }

    public void setPackageTitle(String packageTitle) {
        this.packageTitle = packageTitle;
    }
 

    public String getInternalRef() {
        return internalRef;
    }

    public void setInternalRef(String internalRef) {
        this.internalRef = internalRef;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(SecurityLevel securityLevel) {
        this.securityLevel = securityLevel;
    }

    public boolean isEeaRelevance() {
        return eeaRelevance;
    }

    public void setEeaRelevance(boolean eeaRelevance) {
        this.eeaRelevance = eeaRelevance;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String languageCode) {
        this.language = languageCode;
    }

    public enum SecurityLevel{
        STANDARD,
        SENSITIVE;
    }
}
