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

import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.ui.model.ActType;
import eu.europa.ec.leos.ui.model.ProcedureType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DocumentVO {

    private String id;
    private String title;
    private String createdBy;
    private Date createdOn;
    private String updatedBy;
    private Date updatedOn;
    private String language;
    private String template;
    private User author;
    private int docNumber;//optional
    private List<DocumentVO> childDocuments = new ArrayList<>();

    private LeosCategory documentType;
    private ProcedureType procedureType;
    private ActType actType;

    public DocumentVO(XmlDocument xmlDocument) {
        if (xmlDocument != null) {
            this.id = xmlDocument.getId();
            this.createdBy = xmlDocument.getCreatedBy();
            this.createdOn = Date.from(xmlDocument.getCreationInstant());
            this.updatedBy = xmlDocument.getLastModifiedBy();
            this.updatedOn = Date.from(xmlDocument.getLastModificationInstant());
            this.template = xmlDocument.getTemplate();
            this.language = xmlDocument.getLanguage();
            this.title = xmlDocument.getTitle();
            this.documentType = xmlDocument.getCategory();
            // FIXME set remaining properties
        }
    }

    public DocumentVO(String documentId, String language, LeosCategory docType, String updatedBy, Date updatedOn) {
        this.id = documentId;
        this.language = language;
        this.documentType = docType;
        this.updatedBy = updatedBy;
        this.updatedOn = updatedOn;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public LeosCategory getDocumentType() {
        return documentType;
    }

    public void setDocumentType(LeosCategory documentType) {
        this.documentType = documentType;
    }

    public ProcedureType getProcedureType() {
        return procedureType;
    }

    public void setProcedureType(ProcedureType procedureType) {
        this.procedureType = procedureType;
    }

    public ActType getActType() {
        return actType;
    }

    public void setActType(ActType actType) {
        this.actType = actType;
    }

    public int getDocNumber() {
        return docNumber;
    }

    public void setDocNumber(int docNumber) {
        this.docNumber = docNumber;
    }

    public List<DocumentVO> getChildDocuments() {
        return Collections.unmodifiableList(childDocuments);
    }

    public void addChildDocument(DocumentVO childDocument) {
        childDocuments.add(childDocument);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DocumentVO that = (DocumentVO) o;

        if (!getId().equals(that.getId())) return false;
        if (getTitle() != null ? !getTitle().equals(that.getTitle()) : that.getTitle() != null)
            return false;
        if (getCreatedBy() != null ? !getCreatedBy().equals(that.getCreatedBy()) : that.getCreatedBy() != null)
            return false;
        if (getCreatedOn() != null ? !getCreatedOn().equals(that.getCreatedOn()) : that.getCreatedOn() != null)
            return false;
        if (getUpdatedBy() != null ? !getUpdatedBy().equals(that.getUpdatedBy()) : that.getUpdatedBy() != null)
            return false;
        if (getUpdatedOn() != null ? !getUpdatedOn().equals(that.getUpdatedOn()) : that.getUpdatedOn() != null)
            return false;
        if (getLanguage() != null ? !getLanguage().equals(that.getLanguage()) : that.getLanguage() != null)
            return false;
        return getTemplate().equals(that.getTemplate());
    }

    @Override public int hashCode() {
        int result = getId().hashCode();
        result = 31 * result + (getTitle() != null ? getTitle() .hashCode() : 0);
        result = 31 * result + (getCreatedBy() != null ? getCreatedBy().hashCode() : 0);
        result = 31 * result + (getCreatedOn() != null ? getCreatedOn().hashCode() : 0);
        result = 31 * result + (getUpdatedBy() != null ? getUpdatedBy().hashCode() : 0);
        result = 31 * result + (getUpdatedOn() != null ? getUpdatedOn().hashCode() : 0);
        result = 31 * result + (getLanguage() != null ? getLanguage().hashCode() : 0);
        result = 31 * result + (getTemplate() != null ? getTemplate().hashCode() : 0);
        return result;
    }
}
