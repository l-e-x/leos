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

import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.document.*;
import eu.europa.ec.leos.domain.cmis.metadata.AnnexMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.MemorandumMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.ProposalMetadata;
import eu.europa.ec.leos.domain.common.ActType;
import eu.europa.ec.leos.domain.common.ProcedureType;

import java.util.*;

public class DocumentVO {
    private String id;
    private String title;
    private String createdBy;
    private Date createdOn;
    private String updatedBy;
    private Date updatedOn;
    private String language;
    private String template;
    private int docNumber;// optional
    private byte[] source;
    private boolean uploaded;
    private String versionSeriesId;

    private LeosCategory documentType;
    private ProcedureType procedureType;
    private ActType actType;
    private List<DocumentVO> childDocuments = new ArrayList<>();
    private Map<String, String> collaborators = new HashMap<>();
    private MetadataVO metadata = new MetadataVO();


    public DocumentVO(XmlDocument xmlDocument) {
        if (xmlDocument != null) {
            this.id = xmlDocument.getId();
            this.createdBy = xmlDocument.getCreatedBy();
            this.createdOn = Date.from(xmlDocument.getCreationInstant());
            this.updatedBy = xmlDocument.getLastModifiedBy();
            this.updatedOn = Date.from(xmlDocument.getLastModificationInstant());
            this.title = xmlDocument.getTitle();
            this.documentType = xmlDocument.getCategory();

            populateMetadataValues(xmlDocument);
            // FIXME set remaining properties
        }
    }

    public DocumentVO(LeosCategory documentType) {
        this.documentType = documentType;
    }

    public DocumentVO(String documentId, String language, LeosCategory docType, String updatedBy, Date updatedOn) {
        this.id = documentId;
        this.language = language;
        this.documentType = docType;
        this.updatedBy = updatedBy;
        this.updatedOn = updatedOn;
    }

    private void populateMetadataValues(XmlDocument xmlDocument) {
        switch (xmlDocument.getCategory()) {
            case BILL:
                BillMetadata metadataB = ((Bill) xmlDocument).getMetadata().getOrError(() -> "Bill metadata is required");
                this.template = metadataB.getTemplate();
                this.language = metadataB.getLanguage();
                this.getMetadata().setDocPurpose(metadataB.getPurpose());
                this.getMetadata().setTemplate(metadataB.getTemplate());
                this.getMetadata().setDocTemplate(metadataB.getDocTemplate());
                this.getMetadata().setTemplateName(metadataB.getTemplate());
                
                break;
            case ANNEX:
                AnnexMetadata metadataA = ((Annex) xmlDocument).getMetadata()
                        .getOrError(() -> "Annex metadata is required");
                this.template = metadataA.getTemplate();
                this.language = metadataA.getLanguage();
                this.setDocNumber(metadataA.getIndex());
                this.setTitle(metadataA.getTitle());
                
                this.getMetadata().setIndex(String.valueOf(metadataA.getIndex()));
                this.getMetadata().setTitle(metadataA.getTitle());
                this.getMetadata().setNumber(metadataA.getNumber());
                
                this.setTemplate(metadataA.getTemplate());
                this.getMetadata().setDocPurpose(metadataA.getPurpose());
                this.getMetadata().setTemplate(metadataA.getTemplate());
                this.getMetadata().setDocTemplate(metadataA.getDocTemplate());
                this.getMetadata().setTemplateName(metadataA.getTemplate());
                
                break;
            case PROPOSAL:
                ProposalMetadata metadataP = ((Proposal) xmlDocument).getMetadata()
                        .getOrError(() -> "Proposal metadata is required");
                this.template = metadataP.getTemplate();
                this.language = metadataP.getLanguage();
                break;
            case MEMORANDUM:
                MemorandumMetadata metadataM = ((Memorandum) xmlDocument).getMetadata()
                        .getOrError(() -> "Memorandum metadata is required");
                this.template = metadataM.getTemplate();
                this.language = metadataM.getLanguage();
                this.getMetadata().setDocPurpose(metadataM.getPurpose());
                this.getMetadata().setTemplate(metadataM.getTemplate());
                this.getMetadata().setDocTemplate(metadataM.getDocTemplate());
                this.getMetadata().setTemplateName(metadataM.getTemplate());
                
                break;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public LeosCategory getDocumentType() {
        return documentType;
    }

    public void setDocumentType(LeosCategory documentType) {
        this.documentType = documentType;
    }

    public LeosCategory getCategory() {
        return documentType;
    }

    public void setCategory(LeosCategory documentType) {
        this.documentType = documentType;
    }

    public int getDocNumber() {
        return docNumber;
    }

    public void setDocNumber(int docNumber) {
        this.docNumber = docNumber;
    }

    public List<DocumentVO> getChildDocuments() {
        return childDocuments;
    }

    public void addChildDocument(DocumentVO childDocument) {
        childDocuments.add(childDocument);
    }

    public void setChildDocuments(List<DocumentVO> childDocuments) {
        this.childDocuments = childDocuments;
    }

    public byte[] getSource() {
        return source;
    }

    public void setSource(byte[] source) {
        this.source = source;
    }

    public boolean isUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    public MetadataVO getMetadata() {
        return metadata;
    }

    public void setMetaData(MetadataVO metadataVO) {
        this.metadata = metadataVO;
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

    public void addCollaborators(Map<String, String> collaborators) {
        this.collaborators.putAll(collaborators);
    }

    public void addCollaborator(String userLogin, String authority) {
        this.collaborators.put(userLogin, authority);
    }

    public Map<String, String> getCollaborators() {
        return collaborators;
    }

    public void setCollaborators(Map<String, String> collaborators) {
        this.collaborators = collaborators;
    }

    public DocumentVO getChildDocument(LeosCategory documentType) {
        DocumentVO document = null;
        for (DocumentVO childDocument : childDocuments) {
            if (childDocument.getCategory().equals(documentType)) {
                document = childDocument;
                break;
            }
        }
        return document;
    }

    public List<DocumentVO> getChildDocuments(LeosCategory documentType) {
        List<DocumentVO> documents = new ArrayList<>();
        for (DocumentVO childDoc : childDocuments) {
            if (childDoc.getCategory().equals(documentType)) {
                documents.add(childDoc);
            }
        }
        return documents;
    }

    public String getVersionSeriesId() {
        return versionSeriesId;
    }

    public void setVersionSeriesId(String versionSeriesId) {
        this.versionSeriesId = versionSeriesId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DocumentVO that = (DocumentVO) o;

        if (!getId().equals(that.getId())) return false;
        if (getTitle() != null ? !getTitle().equals(that.getTitle()) : that.getTitle() != null) return false;
        if (getCreatedBy() != null ? !getCreatedBy().equals(that.getCreatedBy()) : that.getCreatedBy() != null) return false;
        if (getCreatedOn() != null ? !getCreatedOn().equals(that.getCreatedOn()) : that.getCreatedOn() != null) return false;
        if (getUpdatedBy() != null ? !getUpdatedBy().equals(that.getUpdatedBy()) : that.getUpdatedBy() != null) return false;
        if (getUpdatedOn() != null ? !getUpdatedOn().equals(that.getUpdatedOn()) : that.getUpdatedOn() != null) return false;
        if (getLanguage() != null ? !getLanguage().equals(that.getLanguage()) : that.getLanguage() != null) return false;
        return getTemplate().equals(that.getTemplate());
    }

    @Override
    public int hashCode() {
        int result = getId().hashCode();
        result = 31 * result + (getTitle() != null ? getTitle().hashCode() : 0);
        result = 31 * result + (getCreatedBy() != null ? getCreatedBy().hashCode() : 0);
        result = 31 * result + (getCreatedOn() != null ? getCreatedOn().hashCode() : 0);
        result = 31 * result + (getUpdatedBy() != null ? getUpdatedBy().hashCode() : 0);
        result = 31 * result + (getUpdatedOn() != null ? getUpdatedOn().hashCode() : 0);
        result = 31 * result + (getLanguage() != null ? getLanguage().hashCode() : 0);
        result = 31 * result + (getTemplate() != null ? getTemplate().hashCode() : 0);
        return result;
    }

    public void clean() {
        this.setId(null);
        this.setDocumentType(null);
        this.getMetadata().clean();
        this.setSource(null);
        this.setTitle(null);
        this.setCreatedBy(null);
        this.setCreatedOn(null);
        this.setUpdatedBy(null);
        this.setUpdatedOn(null);
        this.setLanguage(null);
        this.setTemplate(null);
        this.setDocNumber(0);
        this.setDocumentType(null);
        this.setActType(null);
        this.setProcedureType(null);
        this.getChildDocuments().clear();
        this.getCollaborators().clear();
    }
}
