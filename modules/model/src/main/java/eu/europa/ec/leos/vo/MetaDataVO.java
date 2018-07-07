/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.vo;

public class MetaDataVO {

    //default values
    private String template;
    private String language; // always should be set to language Code
    private String docStage;
    private String docType;
    private String docPurpose;
    private String internalRef;
    
    public MetaDataVO(String template, String language, String docStage, String docType, String docPurpose, String internalRef) {
        super();
        this.template=template;
        this.language = language;
        this.docStage = docStage;
        this.docType = docType;
        this.docPurpose = docPurpose;
        this.internalRef = internalRef;
    }

    public MetaDataVO() {
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

    public void setLanguage(String language) {
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

    public String getInternalRef() {
        return internalRef;
    }

    public void setInternalRef(String internalRef) {
        this.internalRef = internalRef;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((docPurpose == null) ? 0 : docPurpose.hashCode());
        result = prime * result + ((docStage == null) ? 0 : docStage.hashCode());
        result = prime * result + ((docType == null) ? 0 : docType.hashCode());
        result = prime * result + ((template == null) ? 0 : template.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MetaDataVO other = (MetaDataVO) obj;
        if (docPurpose == null) {
            if (other.docPurpose != null) return false;
        } else if (!docPurpose.equals(other.docPurpose)) return false;
        if (docStage == null) {
            if (other.docStage != null) return false;
        } else if (!docStage.equals(other.docStage)) return false;
        if (docType == null) {
            if (other.docType != null) return false;
        } else if (!docType.equals(other.docType)) return false;
        if (template == null) {
            if (other.template != null) return false;
        } else if (!template.equals(other.template)) return false;
        return true;
    }
}
