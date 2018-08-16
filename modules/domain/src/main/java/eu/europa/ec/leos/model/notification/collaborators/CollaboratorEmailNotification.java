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
package eu.europa.ec.leos.model.notification.collaborators;

import java.util.ArrayList;
import java.util.List;

import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.model.notification.EmailNotification;
import eu.europa.ec.leos.model.user.User;

abstract public class CollaboratorEmailNotification implements EmailNotification {
    private List<String> recipients = new ArrayList();
    private LeosAuthority leosAuthority;
    private String leosAuthorityName;
    private String link;
    private String documentId;
    private User recipient;
    private String title;
    private String owners;
    private String reviewers;
    private String contributors;
    private String emailBody;
    private String emailSubject;
    
    public CollaboratorEmailNotification(User recipient, LeosAuthority leosAuthority, String documentId, String link) {
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

    public LeosAuthority getLeosAuthority() {
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

    public String getOwners() {
        return owners;
    }

    public void setOwners(String owners) {
        this.owners = owners;
    }

    public String getReviewers() {
        return reviewers;
    }

    public void setReviewers(String reviewers) {
        this.reviewers = reviewers;
    }

    public String getContributors() {
        return contributors;
    }

    public void setContributors(String contributors) {
        this.contributors = contributors;
    }
    
    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }
    
    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    abstract public String getEmailSubjectKey();

   
    
}
