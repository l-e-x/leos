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
package eu.europa.ec.leos.domain.document

import eu.europa.ec.leos.domain.document.LeosCategory.*

sealed class LeosMetadata(
        val category: LeosCategory
) {

    data class ProposalMetadata(
            val stage: String,
            val type: String,
            val purpose: String,
            val docTemplate:String
    ) : LeosMetadata(PROPOSAL) {

        fun withPurpose(p: String): ProposalMetadata = copy(purpose = p)
    }

    data class MemorandumMetadata(
            val stage: String,
            val type: String,
            val purpose: String,
            val docTemplate:String
    ) : LeosMetadata(MEMORANDUM) {

        fun withPurpose(p: String): MemorandumMetadata = copy(purpose = p)
    }

    data class BillMetadata(
            val stage: String,
            val type: String,
            val purpose: String,
            val docTemplate:String
    ) : LeosMetadata(BILL) {

        fun withPurpose(p: String): BillMetadata = copy(purpose = p)
    }

    data class AnnexMetadata(
            val stage: String,
            val type: String,
            val purpose: String,
            val docTemplate:String,
            val index: Int,
            val number: String,
            val title: String
    ) : LeosMetadata(ANNEX) {

        fun withPurpose(p: String): AnnexMetadata = copy(purpose = p)

        fun withIndex(i: Int): AnnexMetadata = copy(index = i)

        fun withNumber(n: String): AnnexMetadata = copy(number = n)

        fun withTitle(t: String): AnnexMetadata = copy(title = t)
    }
}
