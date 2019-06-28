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
package eu.europa.ec.leos.domain.cmis.document;

import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import io.atlassian.fugue.Option;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Bill extends XmlDocument {
    private final Option<BillMetadata> metadata;

    public Bill(String id, String name, String createdBy, Instant creationInstant, String lastModifiedBy, Instant lastModificationInstant,
                String versionSeriesId, String versionLabel, String versionComment, boolean isMajorVersion, boolean isLatestVersion,
                String title, Map<String, String> collaborators, List<String> milestoneComments, Option<Content> content, Option<BillMetadata> metadata) {

        super(LeosCategory.BILL, id, name, createdBy, creationInstant, lastModifiedBy, lastModificationInstant,
                versionSeriesId, versionLabel, versionComment, isMajorVersion, isLatestVersion, title, collaborators,
                milestoneComments, content);
        this.metadata = metadata;
    }

    public Option<BillMetadata> getMetadata() {
        return metadata;
    }
}
