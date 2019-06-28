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


public class MetadataVO {

    private String docStage;
    private String docType;
    private String docPurpose;
    private String packageTitle;
    private String internalRef;
    private SecurityLevel securityLevel = SecurityLevel.STANDARD;
    private String language; // always should be set to language Code
    private boolean eeaRelevance;
    private String templateName;
    private String template;
    private String docTemplate;
    private String title;
    private String index;
    private String number;

    public MetadataVO() {
    }// added for early binding

    public MetadataVO(String docStage, String docType, String docPurpose, String template, String language) {
        this.docStage = docStage;
        this.docType = docType;
        this.docPurpose = docPurpose;
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String languageCode) {
        this.language = languageCode;
    }

    public enum SecurityLevel {
        STANDARD, SENSITIVE;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }
    
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getDocTemplate() {
        return docTemplate;
    }

    public void setDocTemplate(String docTemplate) {
        this.docTemplate = docTemplate;
    }

    public void clean() {
        this.setDocStage(null);
        this.setDocType(null);
        this.setDocPurpose(null);
        this.setPackageTitle(null);
        this.setInternalRef(null);
        this.setSecurityLevel(null);
        this.setLanguage(null); // always should be set to language Code
        this.setTemplateName(null);
        this.setTemplate(null);
        this.setDocTemplate(null);
        this.setTitle(null);
        this.setIndex(null);
        this.setNumber(null);
        this.setEeaRelevance(false);
    }
}
