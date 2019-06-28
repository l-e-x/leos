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
package eu.europa.ec.leos.model.event;

import eu.europa.ec.leos.model.user.User;

public class DocumentUpdatedByCoEditorEvent {

    private final User user;
    private final String documentId;
    private final String presenterId;

    public DocumentUpdatedByCoEditorEvent(User user, String documentId, String presenterId) {
        this.user = user;
        this.documentId = documentId;
        this.presenterId = presenterId;
    }

    public User getUser() {
        return user;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getPresenterId() {
        return presenterId;
    }
}
