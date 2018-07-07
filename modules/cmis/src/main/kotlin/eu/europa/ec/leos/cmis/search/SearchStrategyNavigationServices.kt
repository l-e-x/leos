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
package eu.europa.ec.leos.cmis.search

import eu.europa.ec.leos.cmis.mapping.CmisProperties
import eu.europa.ec.leos.domain.document.LeosCategory
import mu.KLogging
import org.apache.chemistry.opencmis.client.api.*

internal class SearchStrategyNavigationServices(
        private val cmisSession: Session
) : SearchStrategy {

    private companion object : KLogging()

    override fun findDocuments(folder: Folder, primaryType: String, categories: Set<LeosCategory>, descendants: Boolean): List<Document> {
        logger.trace { "Finding documents..." }
        val categoryList = categories.map { it.name }
        val documents = when(descendants) {
            true -> findDescendants(folder, primaryType)
            else -> findChildren(folder, primaryType)
        }
        return documents.filter { categoryList.contains(it.getPropertyValue(CmisProperties.DOCUMENT_CATEGORY.id)) }
    }

    private fun findChildren(folder: Folder, primaryType: String): List<Document> {
        val context = cmisSession.defaultContext                        // FIXME use optimized context
        val children = folder.getChildren(context)
        return children.filter { it.type.id == primaryType }.map { it as Document }
    }

    private fun findDescendants(folder: Folder, primaryType: String): List<Document> {
        val context = cmisSession.defaultContext                        // FIXME use optimized context
        val documents = ArrayList<Document>()
        val descendants = folder.getDescendants(-1, context)
        flattenAndFilter(descendants, primaryType, documents)
        return documents
    }

    fun flattenAndFilter(nodes: List<Tree<FileableCmisObject>>, primaryType: String, documents: MutableList<Document>) {
        nodes.forEach {
            if (it.item.type.id == primaryType) {
                documents.add(it.item as Document)
            }
            flattenAndFilter(it.children, primaryType, documents)
        }
    }
}
