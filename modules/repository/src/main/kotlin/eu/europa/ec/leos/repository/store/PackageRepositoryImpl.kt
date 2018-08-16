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
package eu.europa.ec.leos.repository.store

import eu.europa.ec.leos.domain.document.LeosDocument
import eu.europa.ec.leos.domain.document.LeosPackage
import eu.europa.ec.leos.repository.LeosRepository
import mu.KLogging
import org.springframework.stereotype.Repository

/**
 * Package Repository implementation.
 *
 * @constructor Creates a specific Package Repository, injected with a generic LEOS Repository.
 */
@Repository
internal class PackageRepositoryImpl(
        private val leosRepository: LeosRepository
) : PackageRepository {

    companion object : KLogging()

    override fun createPackage(path: String, name: String): LeosPackage {
        logger.debug { "Creating Package... [path=$path, name=$name]" }
        return leosRepository.createPackage(path, name)
    }

    override fun deletePackage(path: String) {
        logger.debug { "Deleting Package... [path=$path]" }
        return leosRepository.deletePackage(path)
    }

    override fun findPackageByDocumentId(documentId: String): LeosPackage {
        logger.debug { "Finding Package by document ID... [documentId=$documentId]" }
        return leosRepository.findPackageByDocumentId(documentId)
    }

    override fun <D : LeosDocument> findDocumentsByPackagePath(path: String, type: Class<out D>, fetchContent: Boolean): List<D> {
        logger.debug{ "Finding document by package path... [path=$path, type=${type.simpleName}]" }
        return leosRepository.findDocumentsByParentPath(path, type.kotlin,false, fetchContent)
    }

    override fun <D : LeosDocument> findDocumentByPackagePathAndName(path: String, name: String, type: Class<out D>): D {
        logger.debug{ "Finding document by package path... [path=$path, name=$name, type=${type.simpleName}]" }
        return leosRepository.findDocumentByParentPath(path, name, type.kotlin)
    }
}
