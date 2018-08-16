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
package eu.europa.ec.leos.services.notification;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import eu.europa.ec.leos.model.notification.EmailNotification;
import eu.europa.ec.leos.model.notification.validation.ValidationEmailNotification;

@Component
public class ValidationNotificationProcessor implements EmailNotificationProcessor<ValidationEmailNotification> {

    @Autowired
    @Qualifier("emailsMessageSource")
    private MessageSource emailsMessageSource;

    private final FreemarkerNotificationProcessor processor;

    @Autowired
    public ValidationNotificationProcessor(FreemarkerNotificationProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void process(Locale language, ValidationEmailNotification emailNotification) {
        buildEmailBody(language, emailNotification);
        buildEmailSubject(language, emailNotification);

    }

    @Override
    public boolean canProcess(EmailNotification emailNotification) {
        if (ValidationEmailNotification.class.isAssignableFrom(emailNotification.getClass())) {
            return true;
        } else {
            return false;
        }
    }

    private void buildEmailBody(Locale language, ValidationEmailNotification validationEmailNotification) {
        validationEmailNotification.setEmailBody(processor.processTemplate(validationEmailNotification));
    }

    private void buildEmailSubject(Locale language, ValidationEmailNotification validationEmailNotification) {
        String title = validationEmailNotification.getTitle();
        validationEmailNotification
                .setEmailSubject(emailsMessageSource.getMessage(validationEmailNotification.getEmailSubjectKey(), new Object[]{title}, language));
    }
}
