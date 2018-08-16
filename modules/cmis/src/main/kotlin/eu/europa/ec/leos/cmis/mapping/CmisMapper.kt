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
package eu.europa.ec.leos.cmis.mapping

import eu.europa.ec.leos.domain.document.LeosCategory
import eu.europa.ec.leos.domain.document.LeosCategory.*
import eu.europa.ec.leos.domain.document.LeosDocument
import eu.europa.ec.leos.domain.document.LeosDocument.*
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.*
import kotlin.reflect.KClass

internal object CmisMapper {

    // FIXME move this mapping somewhere else or implement in better way?!!!
    private val documentCategoryMap = mapOf(
            LeosDocument::class to setOf(PROPOSAL, MEMORANDUM, BILL, ANNEX, MEDIA, CONFIG),
            XmlDocument::class to setOf(PROPOSAL, MEMORANDUM, BILL, ANNEX),
            Proposal::class to setOf(PROPOSAL),
            Memorandum::class to setOf(MEMORANDUM),
            Bill::class to setOf(BILL),
            Annex::class to setOf(ANNEX),
            MediaDocument::class to setOf(MEDIA),
            ConfigDocument::class to setOf(CONFIG)
    )

    // FIXME move this mapping somewhere else or implement in better way?!!!
    private val documentPrimaryTypeMap = mapOf(
            LeosDocument::class to "leos:document",
            XmlDocument::class to "leos:xml",
            Proposal::class to "leos:xml",
            Memorandum::class to "leos:xml",
            Bill::class to "leos:xml",
            Annex::class to "leos:xml",
            MediaDocument::class to "leos:media",
            ConfigDocument::class to "leos:config"
    )

    // FIXME move this mapping somewhere else or implement in better way?!!!
    fun <T : LeosDocument> cmisPrimaryType(type: KClass<out T>) : String {
        return documentPrimaryTypeMap[type] ?: throw IllegalArgumentException("Unknown CMIS primary type!")
    }

    // FIXME move this mapping somewhere else or implement in better way?!!!
    fun <T : LeosDocument> cmisCategories(type: KClass<out T>) : Set<LeosCategory> {
        return documentCategoryMap[type].orEmpty()
    }

//    inline fun <reified T : LeosDocument> typeFrom(): String = when(T::class) {
//        is LeosDocument -> "leos:document"
//        is LeosDocument.XmlDocument -> "leos:akomaNtoso"
//        is LeosDocument.MediaDocument -> "leos:media"
//        else -> "cmis:document"
//    }
}
