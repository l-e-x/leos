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
package eu.europa.ec.leos.vo.coedition;

import java.util.Objects;

public final class CoEditionVO {

    private final String sessionId;
    private final String presenterId;
    private final String userLoginName;
    private final String userName;
    private final String entity;
    private final String userEmail;
    private final String documentId;
    private final String elementId;
    private final InfoType infoType;
    private final Long editionTime; // in milliseconds

    public CoEditionVO(String sessionId, String presenterId, String userLoginName, String userName, String entity, String userEmail, String documentId,
            String elementId, InfoType infoType, Long editionTime) {
        this.sessionId = sessionId;
        this.presenterId = presenterId;
        this.userLoginName = userLoginName;
        this.userName = userName;
        this.entity = entity;
        this.userEmail = userEmail;
        this.documentId = documentId;
        this.elementId = elementId;
        this.infoType = infoType;
        this.editionTime = editionTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getPresenterId() {
        return presenterId;
    }

    public String getUserLoginName() {
        return userLoginName;
    }

    public String getUserName() {
        return userName;
    }

    public String getEntity() {
        return entity;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getElementId() {
        return elementId;
    }

    public InfoType getInfoType() {
        return infoType;
    }

    public Long getEditionTime() {
        return editionTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final CoEditionVO other = (CoEditionVO) obj;
        return Objects.equals(this.documentId, other.documentId) && Objects.equals(this.presenterId, other.presenterId) &&
                Objects.equals(this.infoType, other.infoType) && (InfoType.DOCUMENT_INFO.equals(this.infoType) || InfoType.TOC_INFO.equals(this.infoType) ||
                        (InfoType.ELEMENT_INFO.equals(this.infoType) && Objects.equals(this.elementId, other.elementId)));
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId, presenterId, elementId);
    }
}
