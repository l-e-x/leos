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
package eu.europa.ec.leos.cmis.repository

import eu.europa.ec.leos.cmis.extensions.toCmisProperties
import eu.europa.ec.leos.cmis.extensions.toLeosDocument
import eu.europa.ec.leos.cmis.extensions.toLeosPackage
import eu.europa.ec.leos.cmis.mapping.CmisMapper
import eu.europa.ec.leos.cmis.mapping.CmisProperties
import eu.europa.ec.leos.domain.common.LeosAuthority
import eu.europa.ec.leos.domain.document.LeosDocument
import eu.europa.ec.leos.domain.document.LeosMetadata
import eu.europa.ec.leos.domain.document.LeosPackage
import eu.europa.ec.leos.repository.LeosRepository
import eu.europa.ec.leos.security.SecurityContext
import mu.KLogging
import org.apache.chemistry.opencmis.client.api.Document
import org.apache.chemistry.opencmis.client.api.Folder
import org.apache.chemistry.opencmis.commons.PropertyIds
import org.springframework.stereotype.Repository
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

/**
 * LEOS Repository implementation.
 *
 * @constructor Creates a generic LEOS Repository, injected with a CMIS Repository and a Security Context.
 */
@Repository
internal class LeosRepositoryImpl(
        private val cmisRepository: CmisRepository,
        private val securityContext: SecurityContext
) : LeosRepository {

    companion object : KLogging()

    override fun <D : LeosDocument, M : LeosMetadata> createDocument(templateId: String, path: String, name: String, metadata: M, type: KClass<out D>): D {
        logger.trace { "Creating document... [template=$templateId, path=$path, name=$name]" }
        var doc: Document? = null
        val time = measureTimeMillis {
            val properties = mutableMapOf<String, Any>(PropertyIds.NAME to name)
            properties.putAll(metadata.toCmisProperties())
            properties.put(CmisProperties.COLLABORATORS.id, listOf(getAccessRecord(securityContext.user.login, LeosAuthority.OWNER)))
            doc = cmisRepository.createDocumentFromSource(templateId, path, properties)
        }
        logger.trace { "CMIS Repository document creation took $time milliseconds." }
        return doc?.toLeosDocument(type) ?: throw IllegalStateException("Unable to create document! [template=$templateId, path=$path, name=$name]")
    }

    override fun <D : LeosDocument, M : LeosMetadata> updateDocument(id: String, metadata: M, type: KClass<out D>): D {
        logger.trace { "Updating document metadata... [id=$id]" }
        var doc: Document? = null
        val time = measureTimeMillis {
            val properties = metadata.toCmisProperties()
            doc = cmisRepository.updateDocument(id, properties)
        }
        logger.trace { "CMIS Repository document update took $time milliseconds." }
        return doc?.toLeosDocument(type) ?: throw IllegalStateException("Unable to update document! [id=$id]")
    }

    override fun <D : LeosDocument> updateDocument(id: String, content: ByteArray, major: Boolean, comment: String?, type: KClass<out D>): D {
        logger.trace { "Updating document content... [id=$id, comment=$comment]" }
        var doc: Document? = null
        val time = measureTimeMillis {
            doc = cmisRepository.updateDocument(id, emptyMap(), content, major, comment)
        }
        logger.trace { "CMIS Repository document update took $time milliseconds." }
        return doc?.toLeosDocument(type) ?: throw IllegalStateException("Unable to update document! [id=$id, comment=$comment]")
    }

    override fun <D : LeosDocument, M : LeosMetadata> updateDocument(id: String, metadata: M, content: ByteArray, major: Boolean, comment: String?, type: KClass<out D>): D {
        logger.trace { "Updating document metadata and content... [id=$id, comment=$comment]" }
        var doc: Document? = null
        val time = measureTimeMillis {
            val properties = metadata.toCmisProperties()
            doc = cmisRepository.updateDocument(id, properties, content, major, comment)
        }
        logger.trace { "CMIS Repository document update took $time milliseconds." }
        return doc?.toLeosDocument(type) ?: throw IllegalStateException("Unable to update document! [id=$id, comment=$comment]")
    }

    override fun <D : LeosDocument> updateDocument(id: String, collaborators: Map<String, LeosAuthority>, type: KClass<out D>): D {
        logger.trace { "Updating document collaborators... [id=$id]" }
        var doc: Document? = null
        val time = measureTimeMillis {
            val properties = mapOf<String, Any>(
                    CmisProperties.COLLABORATORS.id to collaborators.map { getAccessRecord(it.key, it.value) })
            doc = cmisRepository.updateDocument(id, properties)
        }
        logger.trace { "CMIS Repository document update took $time milliseconds." }
        return doc?.toLeosDocument(type) ?: throw IllegalStateException("Unable to update document! [id=$id]")
    }

    override fun <D : LeosDocument> findDocumentById(id: String, type: KClass<out D>, latest: Boolean): D {
        logger.trace { "Finding document by ID... [id=$id, latest=$latest]" }
        var doc: Document? = null
        val time = measureTimeMillis {
            doc = cmisRepository.findDocumentById(id, latest)
        }
        logger.trace { "CMIS Repository document search took $time milliseconds." }
        return doc?.toLeosDocument(type) ?: throw IllegalArgumentException("Document not found! [id=$id, latest=$latest]")
    }

    override fun <D : LeosDocument> findDocumentByParentPath(path: String, name: String, type: KClass<out D>): D {
        logger.trace { "Finding document by parent path... [path=$path, name=$name]" }
        var doc: Document? = null
        val time = measureTimeMillis {
            doc = cmisRepository.findDocumentByParentPath(path, name)
        }
        logger.trace { "CMIS Repository document search took $time milliseconds." }
        return doc?.toLeosDocument(type) ?: throw IllegalArgumentException("Document not found! [path=$path, name=$name]")
    }

    override fun <D : LeosDocument> findDocumentsByParentPath(path: String, type: KClass<out D>): List<D> {
        logger.trace { "Finding documents by parent path... [path=$path, type=${type.simpleName}]" }
        var docs: List<Document>? = null
        val time = measureTimeMillis {
            val primaryType = CmisMapper.cmisPrimaryType(type)
            val categories = CmisMapper.cmisCategories(type)
            docs = cmisRepository.findDocumentsByParentPath(path, primaryType, categories)
        }
        logger.trace { "CMIS Repository document search took $time milliseconds." }
        return docs?.map { it.toLeosDocument(type) } ?: emptyList()
    }

    override fun <D : LeosDocument> findDocumentVersionsById(id: String, type: KClass<out D>): List<D> {
        logger.trace { "Finding document versions by ID... [id=$id]" }
        var docs: List<Document>? = null
        val time = measureTimeMillis {
            docs = cmisRepository.findAllVersions(id)
        }
        logger.trace { "CMIS Repository versions search took $time milliseconds." }
        return docs?.map { it.toLeosDocument(type) } ?: emptyList()
    }

    override fun deleteDocumentById(id: String) {
        logger.trace { "Deleting Document... [id=$id]" }
        val time = measureTimeMillis {
            cmisRepository.deleteDocumentById(id)
        }
        logger.trace { "CMIS Repository document deletion took $time milliseconds." }
    }

    private fun getAccessRecord(userLogin: String, authority: LeosAuthority): String {
        return "$userLogin::${authority.name}"
    }

    override fun createPackage(path: String, name: String): LeosPackage {
        logger.trace { "Creating package... [path=$path, name=$name]" }
        var folder: Folder? = null
        val time = measureTimeMillis {
            folder = cmisRepository.createFolder(path, name)
        }
        logger.trace { "CMIS Repository package creation took $time milliseconds." }
        return folder?.toLeosPackage() ?: throw IllegalStateException("Unable to create Package! [path=$path, name=$name]")
    }

    override fun deletePackage(path: String): Unit {
        logger.trace { "Deleting package... [path=$path]" }
        val time = measureTimeMillis {
            cmisRepository.deleteFolder(path)
        }
        logger.trace { "CMIS Repository package deletion took $time milliseconds." }
    }

    override fun findPackageByDocumentId(documentId: String): LeosPackage {
        logger.trace { "Finding package by document ID... [documentId=$documentId]" }
        var folder: Folder? = null
        val time = measureTimeMillis {
            val doc = cmisRepository.findDocumentById(documentId, false)
            folder = doc.parents.firstOrNull()
        }
        logger.trace { "CMIS Repository package search took $time milliseconds." }
        return folder?.toLeosPackage() ?: throw IllegalStateException("Package not found! [documentId=$documentId]")
    }
}
