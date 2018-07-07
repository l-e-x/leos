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
package eu.europa.ec.leos.repository.store

import eu.europa.ec.leos.domain.document.LeosDocument.ConfigDocument
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument
import eu.europa.ec.leos.repository.LeosRepository
import mu.KLogging
import org.springframework.stereotype.Repository

/**
 * Configuration Repository implementation.
 *
 * @constructor Creates a specific Configuration Repository, injected with a generic LEOS Repository.
 */
@Repository
internal class ConfigurationRepositoryImpl(
        private val leosRepository: LeosRepository
) : ConfigurationRepository {

    companion object : KLogging()

    override fun findConfiguration(path: String, name: String): ConfigDocument {
        logger.debug { "Finding configuration... [path=$path, name=$name]" }
        return leosRepository.findDocumentByParentPath(path, name, ConfigDocument::class)
    }

    override fun findTemplate(path: String, name: String): XmlDocument {
        logger.debug { "Finding template... [path=$path, name=$name]" }
        return leosRepository.findDocumentByParentPath(path, name, XmlDocument::class)
    }

}
