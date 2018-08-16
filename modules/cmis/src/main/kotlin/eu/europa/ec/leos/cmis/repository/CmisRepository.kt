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
package eu.europa.ec.leos.cmis.repository

import eu.europa.ec.leos.cmis.search.SearchStrategy
import eu.europa.ec.leos.cmis.search.SearchStrategyDelegate
import eu.europa.ec.leos.domain.document.LeosCategory
import mu.KLogging
import org.apache.chemistry.opencmis.client.api.*
import org.apache.chemistry.opencmis.commons.PropertyIds
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId
import org.apache.chemistry.opencmis.commons.enums.UnfileObject
import org.apache.chemistry.opencmis.commons.enums.Updatability
import org.apache.chemistry.opencmis.commons.enums.VersioningState
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException
import org.springframework.stereotype.Repository
import java.io.ByteArrayInputStream
import java.lang.IllegalStateException


@Repository
internal class CmisRepository(
        private val cmisSession: Session
) {

    private companion object : KLogging()

    private val searchStrategy: SearchStrategy by SearchStrategyDelegate(cmisSession)

    fun createFolder(path: String, name: String): Folder {
        logger.trace { "Creating folder... [path=$path, name=$name]" }
        val context = cmisSession.defaultContext                        // FIXME use optimized context
        val parentFolder = findFolderByPath(path, context)
        val properties = mapOf(
                PropertyIds.OBJECT_TYPE_ID to BaseTypeId.CMIS_FOLDER.value(),
                PropertyIds.NAME to name)
        return parentFolder.createFolder(properties, null, null, null, context)
    }

    fun deleteFolder(path: String) {
        logger.trace { "Deleting folder... [path=$path]" }
        val context = cmisSession.defaultContext                        // FIXME use optimized context
        val folder = findFolderByPath(path, context)
        folder.deleteTree(true, UnfileObject.DELETE, true)
    }

    fun createDocumentFromSource(sourceId: String, path: String, properties: Map<String, Any>): Document {
        logger.trace { "Creating document from source... [sourceId=$sourceId]" }
        val context = cmisSession.defaultContext                        // FIXME use optimized context
        val targetFolder = findFolderByPath(path, context)
        val sourceDoc = findDocumentById(sourceId, false, context)
        return sourceDoc.copy(targetFolder, properties, VersioningState.MAJOR, null, null, null, context)
    }

    fun deleteDocumentById(id: String) {
        logger.trace { "Deleting document... [id=$id]" }
        val context = cmisSession.defaultContext                        // FIXME use optimized context
        val cmisObject = cmisSession.getObject(id, context)
        require(cmisObject is Document) { "CMIS object referenced by id [$id] is not a Document!" }
        cmisObject.delete(true)
    }

    fun updateDocument(id: String, properties: Map<String, Any>): Document {
        logger.trace { "Updating document properties... [id=$id]" }
        val context = cmisSession.defaultContext                        // FIXME use optimized context
        val document = findDocumentById(id, true, context)
        val cmisObject = document.updateProperties(properties)
        return cmisObject as Document
    }

    fun updateDocument(id: String, properties: Map<String, Any>, updatedDocumentBytes: ByteArray, major: Boolean, comment: String?): Document {
        logger.trace("Updating document properties and content... [id=$id]")
        var udpatedDocument: Document? = null

        /*Needs to rethink checkin strategy as it requires 4 CMIS operations
        1.get Doc, 2.get PWC, 3.Update PWC 4.Get Updated Docuemnt */
        val pwc = checkOutWorkingCopy(id)
        udpatedDocument = checkInWorkingCopy(pwc, properties, updatedDocumentBytes, major, comment)
        logger.trace("Updated document properties and content...")
        return udpatedDocument ?: throw IllegalStateException("Update not successful for document:$id");
    }

    private fun checkInWorkingCopy(pwc: Document, properties: Map<String, Any>, updatedDocumentBytes: ByteArray, major: Boolean, comment: String?): Document {

        val updatedProperties = mutableMapOf<String, Any?>()
        // KLUGE LEOS-2408 workaround for issue related to reset properties values with OpenCMIS In-Memory server
        logger.trace { "KLUGE LEOS-2408 workaround for reset properties values..." }
        pwc.properties.forEach {
            if (Updatability.READWRITE == it.definition.updatability) {
                updatedProperties[it.id] = it.getValue()
            }
        }

        // add input properties to existing properties map,
        // eventually overriding old properties values
        updatedProperties.putAll(properties)

        val byteStream: ByteArrayInputStream = ByteArrayInputStream(updatedDocumentBytes)
        val context = cmisSession.defaultContext
        var updatedDocId: ObjectId
        byteStream.use {
            try {
                val contentStream = cmisSession.getObjectFactory()
                        .createContentStream(pwc.contentStream.fileName, -1, pwc.contentStream.mimeType, byteStream)
                updatedDocId = pwc.checkIn(major, updatedProperties, contentStream, comment)
                logger.trace("Document checked-in successfully...[updated document id:${updatedDocId.id}]")
            } catch (e: CmisBaseException) {
                logger.error("Document update failed, trying to cancel the checkout", e)
                pwc.cancelCheckOut()
                throw e
            }
            // If major version, remove previous version
            // FIXME: Couldn't update major version flag and comment in CMIS without checking out and checking in 
            if (pwc.getAllVersions().size > 1) {
                val beforeLastVersion = pwc.getAllVersions().get(1)
                if (major && !beforeLastVersion.isMajorVersion &&  beforeLastVersion.getLastModifiedBy() == pwc.getLastModifiedBy()) {
                    logger.trace("Major version - Removing the before last version...")
                    beforeLastVersion.delete(false) //Remove the before last version
                }
            }
            return findDocumentById(updatedDocId.id, true, context)
        }
    }

    private fun checkOutWorkingCopy(id: String): Document {
        val context = cmisSession.defaultContext                        // FIXME use optimized context
        val document = findDocumentById(id, true, context)
        //Note: Some repositories do not allow PWC access if different user has created it.
        var pwcId: String
        if (document.isVersionSeriesCheckedOut) {
            pwcId = document.versionSeriesCheckedOutId
            logger.trace("Document already check out ... [id=$id, pwc id=${document.versionSeriesCheckedOutId}")
        } else {
            pwcId = document.checkOut().id
        }
        val pwc = findDocumentById(pwcId, false, context)
        logger.trace("Document checked-out... [id=$id, pwc id=${pwc.id}]")
        return pwc
    }

    // FIXME replace primaryType string with some enum value
    fun findDocumentsByParentPath(path: String, primaryType: String, categories: Set<LeosCategory>, descendants: Boolean): List<Document> {
        logger.trace { "Finding documents by parent path... [path=$path, primaryType=$primaryType, categories=$categories, descendants=$descendants]" }
        val context = cmisSession.defaultContext                        // FIXME use optimized context
        val folder = findFolderByPath(path, context)
        val documents = searchStrategy.findDocuments(folder, primaryType, categories, descendants)
        logger.trace { "Found ${documents.size} CMIS document(s)." }
        return documents
    }

    fun findDocumentByParentPath(path: String, name: String): Document {
        logger.trace { "Finding document by parent path... [path=$path, name=$name]" }
        val context = cmisSession.defaultContext                        // FIXME use optimized context
        val cmisObject = cmisSession.getObjectByPath(path, name, context)
        return cmisObject as Document
    }

    fun findDocumentByPath(path: String): Document {
        logger.trace { "Finding document by path... [path=$path]" }
        val context = cmisSession.defaultContext                        // FIXME use optimized context
        val cmisObject = cmisSession.getObjectByPath(path, context)
        require(cmisObject is Document) { "CMIS object referenced by path [$path] is not a Document!" }
        return cmisObject as Document
    }

    fun findDocumentById(id: String, latest: Boolean): Document {
        logger.trace { "Finding document by id... [id=$id, latest=$latest]" }
        val context = cmisSession.defaultContext                        // FIXME use optimized context
        return findDocumentById(id, latest, context)
    }

    fun findAllVersions(id: String): List<Document> {
        logger.trace { "Finding all document versions... [id=$id]" }
        val context = cmisSession.defaultContext                        // FIXME use optimized context
        val document = findDocumentById(id, false, context)
        val versions = document.allVersions
        logger.trace { "Found ${versions.size} CMIS version(s)." }
        return versions
    }

    private fun findDocumentById(id: String, latest: Boolean, context: OperationContext): Document {
        val cmisObject = when {
            latest -> cmisSession.getLatestDocumentVersion(id, context)
            else -> cmisSession.getObject(id, context)
        }
        require(cmisObject is Document) { "CMIS object referenced by id [$id] is not a Document!" }
        return cmisObject as Document
    }

    private fun findFolderByPath(path: String, context: OperationContext): Folder {
        val pathAvailable = cmisSession.existsPath(path)
        require(pathAvailable) { "Path [$path] is not available in CMIS repository!" }

        val cmisObject = cmisSession.getObjectByPath(path, context)
        require(cmisObject is Folder) { "CMIS object referenced by path [$path] is not a Folder!" }
        return cmisObject as Folder
    }
}
