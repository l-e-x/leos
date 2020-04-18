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
package eu.europa.ec.leos.web.event.view.document;

import eu.europa.ec.leos.domain.cmis.common.VersionType;

public class SaveIntermediateVersionEvent {

    private String checkinComment;
    private VersionType versionType;
    
    public SaveIntermediateVersionEvent(String checkinComment, VersionType versionType) {
        super();
        this.checkinComment = checkinComment;
        this.versionType = versionType;
    }

    public String getCheckinComment() {
        return checkinComment;
    }

    public VersionType getVersionType() {
        return versionType;
    }    
    
}
