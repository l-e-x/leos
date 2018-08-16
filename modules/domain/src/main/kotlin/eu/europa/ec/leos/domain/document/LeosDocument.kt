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
package eu.europa.ec.leos.domain.document

import eu.europa.ec.leos.domain.common.LeosAuthority
import eu.europa.ec.leos.domain.document.LeosCategory.*
import eu.europa.ec.leos.domain.document.LeosMetadata.*
import eu.europa.ec.leos.domain.document.common.*
import io.atlassian.fugue.Option
import java.time.Instant

sealed class LeosDocument(
        val category: LeosCategory, val id: String, val name: String,
        createdBy: String, creationInstant: Instant, lastModifiedBy: String, lastModificationInstant: Instant,
        versionSeriesId: String, versionLabel: String, versionComment: String?, isMajorVersion: Boolean, isLatestVersion: Boolean,
        val content: Option<Content>
) : Auditable by AuditData(createdBy, creationInstant, lastModifiedBy, lastModificationInstant),
        Versionable by VersionData(versionSeriesId, versionLabel, versionComment, isMajorVersion, isLatestVersion) {

    sealed class XmlDocument(
            category: LeosCategory, id: String, name: String,
            createdBy: String, creationInstant: Instant, lastModifiedBy: String, lastModificationInstant: Instant,
            versionSeriesId: String, versionLabel: String, versionComment: String?, isMajorVersion: Boolean, isLatestVersion: Boolean,
            val title: String, collaborators: Map<String, LeosAuthority>,
            content: Option<Content>
    ) : LeosDocument(
            category, id, name,
            createdBy, creationInstant, lastModifiedBy, lastModificationInstant,
            versionSeriesId, versionLabel, versionComment, isMajorVersion, isLatestVersion,
            content),
            Securable by SecurityData(collaborators) {

        class Proposal(
                id: String, name: String,
                createdBy: String, creationInstant: Instant, lastModifiedBy: String, lastModificationInstant: Instant,
                versionSeriesId: String, versionLabel: String, versionComment: String?, isMajorVersion: Boolean, isLatestVersion: Boolean,
                title: String, collaborators: Map<String, LeosAuthority>,
                content: Option<Content> = Option.none(),
                val metadata: Option<ProposalMetadata> = Option.none()
        ) : XmlDocument(
                PROPOSAL, id, name,
                createdBy, creationInstant, lastModifiedBy, lastModificationInstant,
                versionSeriesId, versionLabel, versionComment, isMajorVersion, isLatestVersion,
                title, collaborators,
                content)

        class Memorandum(
                id: String, name: String,
                createdBy: String, creationInstant: Instant, lastModifiedBy: String, lastModificationInstant: Instant,
                versionSeriesId: String, versionLabel: String, versionComment: String?, isMajorVersion: Boolean, isLatestVersion: Boolean,
                title: String, collaborators: Map<String, LeosAuthority>,
                content: Option<Content> = Option.none(),
                val metadata: Option<MemorandumMetadata> = Option.none()
        ) : XmlDocument(
                MEMORANDUM, id, name,
                createdBy, creationInstant, lastModifiedBy, lastModificationInstant,
                versionSeriesId, versionLabel, versionComment, isMajorVersion, isLatestVersion,
                title, collaborators,
                content)

        class Bill(
                id: String, name: String,
                createdBy: String, creationInstant: Instant, lastModifiedBy: String, lastModificationInstant: Instant,
                versionSeriesId: String, versionLabel: String, versionComment: String?, isMajorVersion: Boolean, isLatestVersion: Boolean,
                title: String, collaborators: Map<String, LeosAuthority>,
                content: Option<Content> = Option.none(),
                val metadata: Option<BillMetadata> = Option.none()
        ) : XmlDocument(
                BILL, id, name,
                createdBy, creationInstant, lastModifiedBy, lastModificationInstant,
                versionSeriesId, versionLabel, versionComment, isMajorVersion, isLatestVersion,
                title, collaborators,
                content)

        class Annex(
                id: String, name: String,
                createdBy: String, creationInstant: Instant, lastModifiedBy: String, lastModificationInstant: Instant,
                versionSeriesId: String, versionLabel: String, versionComment: String?, isMajorVersion: Boolean, isLatestVersion: Boolean,
                title: String, collaborators: Map<String, LeosAuthority>,
                content: Option<Content> = Option.none(),
                val metadata: Option<AnnexMetadata> = Option.none()
        ) : XmlDocument(
                ANNEX, id, name,
                createdBy, creationInstant, lastModifiedBy, lastModificationInstant,
                versionSeriesId, versionLabel, versionComment, isMajorVersion, isLatestVersion,
                title, collaborators,
                content)
    }

    class MediaDocument(
            id: String, name: String,
            createdBy: String, creationInstant: Instant, lastModifiedBy: String, lastModificationInstant: Instant,
            versionSeriesId: String, versionLabel: String, versionComment: String?, isMajorVersion: Boolean, isLatestVersion: Boolean,
            content: Option<Content> = Option.none()
    ) : LeosDocument(
            MEDIA, id, name,
            createdBy, creationInstant, lastModifiedBy, lastModificationInstant,
            versionSeriesId, versionLabel, versionComment, isMajorVersion, isLatestVersion,
            content)

    class ConfigDocument(
            id: String, name: String,
            createdBy: String, creationInstant: Instant, lastModifiedBy: String, lastModificationInstant: Instant,
            versionSeriesId: String, versionLabel: String, versionComment: String?, isMajorVersion: Boolean, isLatestVersion: Boolean,
            content: Option<Content> = Option.none()
    ) : LeosDocument(
            CONFIG, id, name,
            createdBy, creationInstant, lastModifiedBy, lastModificationInstant,
            versionSeriesId, versionLabel, versionComment, isMajorVersion, isLatestVersion,
            content)
}
