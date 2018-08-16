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

import java.util.Date;
import java.util.List;

public class DocumentValidationNotification extends ValidationEmailNotification {

    public DocumentValidationNotification(String recipient, List<String> errors, String updatedBy, Date updatedOn, String title) {
        super(recipient, errors, updatedBy, updatedOn, title);
    }

    @Override
    public String getEmailSubjectKey() {
        return "notification.validation.document.subject";
    }

}
