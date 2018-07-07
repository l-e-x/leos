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
package eu.europa.ec.leos.web.event.view.document;

import eu.europa.ec.leos.model.user.User;

public class CreateSuggestionResponseEvent {

    private final String elementId;
    private final String elementFragment;
    private final User user;

    public CreateSuggestionResponseEvent(String elementId, String elementFragment, User user) {
        this.elementId = elementId;
        this.elementFragment = elementFragment;
        this.user = user;
    }

    public String getElementId() {
        return elementId;
    }

    public String getElementFragment() {
        return elementFragment;
    }

    public User getUser() {
        return user;
    }
}