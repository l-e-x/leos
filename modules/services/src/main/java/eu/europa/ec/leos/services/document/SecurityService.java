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
package eu.europa.ec.leos.services.document;

import java.util.Map;

import eu.europa.ec.leos.domain.document.LeosDocument;
import eu.europa.ec.leos.domain.common.LeosAuthority;

public interface SecurityService {

    <T extends LeosDocument.XmlDocument> T addOrUpdateCollaborators(String id, Map<String, LeosAuthority> collaborators, Class<T> type);

    <T extends LeosDocument.XmlDocument> T addOrUpdateCollaborator(String id, String userLogin, LeosAuthority authority, Class<T> type);

    <T extends LeosDocument.XmlDocument> T removeCollaborator(String id, String userLogin, Class<T> type);
}

