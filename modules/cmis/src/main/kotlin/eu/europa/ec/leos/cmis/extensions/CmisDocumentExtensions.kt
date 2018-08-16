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
package eu.europa.ec.leos.cmis.extensions

import eu.europa.ec.leos.cmis.domain.ContentImpl
import eu.europa.ec.leos.cmis.domain.SourceImpl
import eu.europa.ec.leos.cmis.mapping.CmisProperties
import eu.europa.ec.leos.cmis.mapping.CmisProperties.DOCUMENT_CATEGORY
import eu.europa.ec.leos.domain.document.Content
import eu.europa.ec.leos.domain.document.LeosCategory
import eu.europa.ec.leos.domain.document.LeosCategory.*
import eu.europa.ec.leos.domain.document.LeosDocument
import eu.europa.ec.leos.domain.document.LeosDocument.ConfigDocument
import eu.europa.ec.leos.domain.document.LeosDocument.MediaDocument
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.*
import eu.europa.ec.leos.domain.common.LeosAuthority
import io.atlassian.fugue.Option
import mu.KLogging
import org.apache.chemistry.opencmis.client.api.Document
import org.apache.chemistry.opencmis.client.api.Property
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

inline fun <reified T : LeosDocument> Document.toLeosDocument(): T {
    return this.toLeosDocument(T::class)
}

@Suppress("UNCHECKED_CAST")
fun <T : LeosDocument> Document.toLeosDocument(type: KClass<out T>, fetchContent: Boolean = true): T {
    return when (this.category) {
        PROPOSAL -> if (Proposal::class.isSubclassOf(type)) {
            this.toLeosProposal(fetchContent) as T
        } else {
            error("Incompatible types! [category=${this.category}, mappedType=${Proposal::class.simpleName}, wantedType=${type.simpleName}]")
        }
        MEMORANDUM -> if (Memorandum::class.isSubclassOf(type)) {
            this.toLeosMemorandum(fetchContent) as T
        } else {
            error("Incompatible types! [category=${this.category}, mappedType=${Memorandum::class.simpleName}, wantedType=${type.simpleName}]")
        }
        BILL -> if (Bill::class.isSubclassOf(type)) {
            this.toLeosBill(fetchContent) as T
        } else {
            error("Incompatible types! [category=${this.category}, mappedType=${Bill::class.simpleName}, wantedType=${type.simpleName}]")
        }
        ANNEX -> if (Annex::class.isSubclassOf(type)) {
            this.toLeosAnnex(fetchContent) as T
        } else {
            error("Incompatible types! [category=${this.category}, mappedType=${Annex::class.simpleName}, wantedType=${type.simpleName}]")
        }
        MEDIA -> if (MediaDocument::class.isSubclassOf(type)) {
            this.toLeosMediaDocument(fetchContent) as T
        } else {
            error("Incompatible types! [category=${this.category}, mappedType=${MediaDocument::class.simpleName}, wantedType=${type.simpleName}]")
        }
        CONFIG -> if (ConfigDocument::class.isSubclassOf(type)) {
            this.toLeosConfigDocument(fetchContent) as T
        } else {
            error("Incompatible types! [category=${this.category}, mappedType=${ConfigDocument::class.simpleName}, wantedType=${type.simpleName}]")
        }
        else -> error("Unknown LEOS category! [category=${this.category}]")
    }
}

private fun Document.toLeosProposal(fetchContent: Boolean): Proposal {
    return Proposal(
            this.id, this.name,
            this.createdBy, this.creationInstant, this.lastModifiedBy, this.lastModificationInstant,
            this.versionSeriesId, this.versionLabel, this.checkinComment, this.isMajorVersion, this.isLatestVersion,
            this.title,
            this.collaborators,
            this.contentOption(fetchContent),
            this.proposalMetadataOption)
}

private fun Document.toLeosMemorandum(fetchContent: Boolean): Memorandum {
    return Memorandum(
            this.id, this.name,
            this.createdBy, this.creationInstant, this.lastModifiedBy, this.lastModificationInstant,
            this.versionSeriesId, this.versionLabel, this.checkinComment, this.isMajorVersion, this.isLatestVersion,
            this.title,
            this.collaborators,
            this.contentOption(fetchContent),
            this.memorandumMetadataOption)
}

private fun Document.toLeosBill(fetchContent: Boolean): Bill {
    return Bill(
            this.id, this.name,
            this.createdBy, this.creationInstant, this.lastModifiedBy, this.lastModificationInstant,
            this.versionSeriesId, this.versionLabel, this.checkinComment, this.isMajorVersion, this.isLatestVersion,
            this.title,
            this.collaborators,
            this.contentOption(fetchContent),
            this.billMetadataOption)
}

private fun Document.toLeosAnnex(fetchContent: Boolean): Annex {
    return Annex(
            this.id, this.name,
            this.createdBy, this.creationInstant, this.lastModifiedBy, this.lastModificationInstant,
            this.versionSeriesId, this.versionLabel, this.checkinComment, this.isMajorVersion, this.isLatestVersion,
            this.title,
            this.collaborators,
            this.contentOption(fetchContent),
            this.annexMetadataOption)
}

private fun Document.toLeosMediaDocument(fetchContent: Boolean): MediaDocument {
    return MediaDocument(
            this.id, this.name,
            this.createdBy, this.creationInstant, this.lastModifiedBy, this.lastModificationInstant,
            this.versionSeriesId, this.versionLabel, this.checkinComment, this.isMajorVersion, this.isLatestVersion,
            this.contentOption(fetchContent))
}

private fun Document.toLeosConfigDocument(fetchContent: Boolean): ConfigDocument {
    return ConfigDocument(
            this.id, this.name,
            this.createdBy, this.creationInstant, this.lastModifiedBy, this.lastModificationInstant,
            this.versionSeriesId, this.versionLabel, this.checkinComment, this.isMajorVersion, this.isLatestVersion,
            this.contentOption(fetchContent))
}

private val Document.category: LeosCategory
    get() {                                                                 // FIXME add check for leos:document primary type???
        val cmisCategory = this.getPropertyValue<String>(DOCUMENT_CATEGORY.id)
        return LeosCategory.valueOf(cmisCategory)
    }

private val Document.creationInstant: Instant
    get() = this.creationDate?.toInstant() ?: Instant.MIN

private val Document.lastModificationInstant: Instant
    get() = this.lastModificationDate?.toInstant() ?: Instant.MIN

private fun Document.contentOption(fetchContent: Boolean): Option<Content> {
    var content: Content? = null
    if (fetchContent) {
        this.contentStream?.let {
            content = ContentImpl(
                    it.fileName,
                    it.mimeType,
                    it.length,
                    SourceImpl(it.stream)
            )
        }
    }
    return Option.option(content);
}

val Document.collaborators: Map<String, LeosAuthority>
    get() {
        val users: MutableMap<String, LeosAuthority> = HashMap()
        val propertyValues: List<String> = this.getProperty<Property<*>>(CmisProperties.COLLABORATORS.id).values as List<String>
        for (value in propertyValues) {
            try {
                val values = value.split("::")
                if (values.size != 2) throw UnknownFormatConversionException("User record is in incorrect format, required format[login::Authority ], present value=" + values)
                users.put(values[0], LeosAuthority.valueOf(values[1]))
            } catch(ex: Exception) {
                KLogging().logger.error("Failure in processing user record [value=$value], continuing...", ex)
            }
        }
        return users
    }

// FIXME maybe move title property to metadata or remove it entirely
private val Document.title: String                                      // FIXME add check for leos:xml primary type
    get() = this.getPropertyValue<String>("leos:title")                 // TODO replace hardcoded property id string
