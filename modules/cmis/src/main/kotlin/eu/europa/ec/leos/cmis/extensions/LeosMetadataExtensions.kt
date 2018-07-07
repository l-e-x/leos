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
package eu.europa.ec.leos.cmis.extensions

import eu.europa.ec.leos.cmis.mapping.CmisProperties.*
import eu.europa.ec.leos.domain.document.LeosMetadata
import eu.europa.ec.leos.domain.document.LeosMetadata.*

fun <M : LeosMetadata> M.toCmisProperties(): Map<String, Any> {
    return when(this) {
        is ProposalMetadata -> this.toCmisProperties()
        is MemorandumMetadata -> this.toCmisProperties()
        is BillMetadata -> this.toCmisProperties()
        is AnnexMetadata -> this.toCmisProperties()
        else -> error("Unknown LEOS Metadata! [type=${this::class.simpleName}]")
    }
}

fun ProposalMetadata.toCmisProperties(): Map<String, Any> {
    val title = arrayOf(this.stage, this.type, this.purpose).joinToString(" ")
    return mapOf(
            METADATA_STAGE.id to this.stage,
            METADATA_TYPE.id to this.type,
            METADATA_PURPOSE.id to this.purpose,
            METADATA_DOCTEMPLATE.id to this.docTemplate,
            DOCUMENT_TITLE.id to title
    )
}

fun MemorandumMetadata.toCmisProperties(): Map<String, Any> {
    return mapOf(
            METADATA_STAGE.id to this.stage,
            METADATA_TYPE.id to this.type,
            METADATA_PURPOSE.id to this.purpose,
            METADATA_DOCTEMPLATE.id to this.docTemplate
    )
}

fun BillMetadata.toCmisProperties(): Map<String, Any> {
    val title = arrayOf(this.stage, this.type, this.purpose).joinToString(" ")
    return mapOf(
            METADATA_STAGE.id to this.stage,
            METADATA_TYPE.id to this.type,
            METADATA_PURPOSE.id to this.purpose,
            METADATA_DOCTEMPLATE.id to this.docTemplate,
            DOCUMENT_TITLE.id to title
    )
}

fun AnnexMetadata.toCmisProperties(): Map<String, Any> {
    return mapOf(
            METADATA_STAGE.id to this.stage,
            METADATA_TYPE.id to this.type,
            METADATA_PURPOSE.id to this.purpose,
            METADATA_DOCTEMPLATE.id to this.docTemplate,
            ANNEX_INDEX.id to this.index,
            ANNEX_NUMBER.id to this.number,
            ANNEX_TITLE.id to this.title
    )
}
