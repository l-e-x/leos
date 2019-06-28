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
package eu.europa.ec.leos.model.notification.collaborators;

import java.util.*;

import eu.europa.ec.leos.model.notification.EmailNotification;
import eu.europa.ec.leos.model.user.User;

abstract public class CollaboratorEmailNotification implements EmailNotification {
    private List<String> recipients = new ArrayList();
    private String leosAuthority;
    private String leosAuthorityName;
    private String link;
    private String documentId;
    private User recipient;
    private String title;
    private String emailBody;
    private String emailSubject;
    private String collaboratorPlural;
    private Map<String, String> collaboratorsMap = new LinkedHashMap<>();
    private Map<String, String> collaboratorNoteMap = new HashMap<>();
    
    public CollaboratorEmailNotification(User recipient, String leosAuthority, String documentId, String link) {
        this.documentId = documentId;
        this.link = link;
        this.leosAuthority = leosAuthority;
        this.recipient = recipient;
        recipients.add(recipient.getEmail());
    }
    
    public String getEmailBody() {
        return emailBody;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public String getNotificationName() {
        return this.getClass().getSimpleName();
    }


    public String getDocumentId() {
        return documentId;
    }

    public User getRecipient() {
        return recipient;
    }

    public String getLink() {
        return link;
    }

    public String getLeosAuthority() {
        return leosAuthority;
    }

    public String getLeosAuthorityName() {
        return leosAuthorityName;
    }

    public void setLeosAuthorityName(String leosAuthorityName) {
        this.leosAuthorityName = leosAuthorityName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }
    
    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public Map<String, String> getCollaboratorsMap() {
        return collaboratorsMap;
    }

    public Map<String, String> getCollaboratorNoteMap() {
        return collaboratorNoteMap;
    }

    public String getCollaboratorPlural() {
        return collaboratorPlural;
    }

    public void setCollaboratorPlural(String collaboratorPlural) {
        this.collaboratorPlural = collaboratorPlural;
    }

    abstract public String getEmailSubjectKey();

   
    
}
