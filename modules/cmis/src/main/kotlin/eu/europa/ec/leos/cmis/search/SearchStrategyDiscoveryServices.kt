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

import eu.europa.ec.leos.domain.document.LeosCategory
import mu.KLogging
import org.apache.chemistry.opencmis.client.api.Document
import org.apache.chemistry.opencmis.client.api.Folder
import org.apache.chemistry.opencmis.client.api.Session

internal class SearchStrategyDiscoveryServices(
        private val cmisSession: Session
) : SearchStrategy {

    private companion object : KLogging()

    override fun findDocuments(folder: Folder, primaryType: String, categories: Set<LeosCategory>, descendants: Boolean): List<Document> {
        logger.trace { "Finding documents..." }
        val context = cmisSession.defaultContext                        // FIXME use optimized context
        val categoryStr = categories.joinToString { "'${it.name}'" }

        val whereClause = if (descendants) {
            "IN_TREE('${folder.id}') AND leos:category IN ($categoryStr)"
        } else {
            "IN_FOLDER('${folder.id}') AND leos:category IN ($categoryStr)"
        }
        logger.trace { "Querying CMIS objects... [primaryType=$primaryType, where=$whereClause]" }

        // NOTE only the latest version (major or minor) of each document should be returned (searchAllVersions = false)
        val cmisObjects = cmisSession.queryObjects(primaryType, whereClause, false, context)
        return cmisObjects.map { it as Document }
    }
}
