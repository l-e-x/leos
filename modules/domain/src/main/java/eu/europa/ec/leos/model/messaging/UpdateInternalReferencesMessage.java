package eu.europa.ec.leos.model.messaging;

public class UpdateInternalReferencesMessage {

    private String documentId;
    private String documentRef;
    private String presenterId;

    public UpdateInternalReferencesMessage() {//json deserialized need the empty constructor
    }

    public UpdateInternalReferencesMessage(String documentId, String documentRef, String presenterId) {
        this.documentId = documentId;
        this.documentRef = documentRef;
        this.presenterId = presenterId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getDocumentRef() {
        return documentRef;
    }

    public void setDocumentRef(String documentRef) {
        this.documentRef = documentRef;
    }

    public String getPresenterId() {
        return presenterId;
    }

    public void setPresenterId(String presenterId) {
        this.presenterId = presenterId;
    }

    @Override
    public String toString() {
        return "UpdateInternalReferencesMessage{" +
                "documentId='" + documentId + '\'' +
                ", documentRef='" + documentRef + '\'' +
                ", presenterId='" + presenterId + '\'' +
                '}';
    }

}

