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
package eu.europa.ec.leos.model.notification.validation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.europa.ec.leos.domain.vo.ErrorVO;
import eu.europa.ec.leos.model.notification.EmailNotification;

abstract public class ValidationEmailNotification implements EmailNotification {

    private List<String> recipients = new ArrayList<String>();
    private List<String> errors;
    private String title;
    private String updatedBy;
    private Date updatedOn;
    private String recipient;
    private String emailBody;
    private String emailSubject;

    public ValidationEmailNotification(String recipient, List<String> errors, String updatedBy, Date updatedOn, String title) {
        this.errors = errors;
        this.updatedBy = updatedBy;
        this.updatedOn = updatedOn;
        this.recipient = recipient;
        this.title = title;
        recipients.add(recipient);

    }

    @Override
    public List<String> getRecipients() {
        return recipients;
    }

    @Override
    public String getNotificationName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getEmailSubject() {
        return emailSubject;
    }

    @Override
    public String getEmailBody() {
        return emailBody;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public String getTitle() {
        return title;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    abstract public String getEmailSubjectKey();

}
