/*
 * Copyright 2019 European Commission
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
package eu.europa.ec.leos.repository.document;

import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.model.filter.QueryFilter;
import org.springframework.security.access.prepost.PostAuthorize;

import java.util.List;
import java.util.stream.Stream;

/**
 * Bill Repository interface.
 * <p>
 * Represents collections of *Bill* documents, with specific methods to persist and retrieve.
 * Allows CRUD operations based on strongly typed Business Entities: [Bill] and [BillMetadata].
 */
public interface BillRepository {

    /**
     * Creates a [Bill] document from a given template and with the specified characteristics.
     *
     * @param templateId the ID of the template for the bill.
     * @param path       the path where to create the bill.
     * @param name       the name of the bill.
     * @param metadata   the metadata of the bill.
     * @return the created bill document.
     */
    Bill createBill(String templateId, String path, String name, BillMetadata metadata);

    /**
     * Creates a [Bill] document from a given content and with the specified characteristics.
     *
     * @param path     the path where to create the bill.
     * @param name     the name of the bill.
     * @param metadata the metadata of the bill.
     * @param content  the content of the bill.
     * @return the created bill document.
     */
    Bill createBillFromContent(String path, String name, BillMetadata metadata, byte[] content);

    /**
     * Updates a [Bill] document with the given metadata and content.
     *
     * @param id       the ID of the bill document to update.
     * @param metadata the metadata of the bill.
     * @param content  the content of the bill.
     * @param versionType  the version type to be created
     * @param comment  the comment of the update, optional.
     * @return the updated bill document.
     */
    Bill updateBill(String id, BillMetadata metadata, byte[] content, VersionType versionType, String comment);

    /**
     * Updates a [Bill] document with the given metadata.
     *
     * @param id       the ID of the bill document to update.
     * @param metadata the metadata of the bill.
     * @return the updated bill document.
     */
    Bill updateBill(String id, BillMetadata metadata);

    /**
     * Updates a [Bill] document with the given metadata.
     *
     * @param id                the ID of the bill document to update.
     * @param milestoneComments the milestoneComments of the bill document to update.
     * @param versionType       the version type to be created
     * @param comment           checking comment.
     * @return the updated bill document.
     */
    Bill updateMilestoneComments(String id, List<String> milestoneComments, byte[] content, VersionType versionType, String comment);

    /**
     * Updates a [Bill] document with the given metadata.
     *
     * @param id                the ID of the bill document to update.
     * @param milestoneComments the milestoneComments of the bill document to update.
     * @return the updated bill document.
     */
    Bill updateMilestoneComments(String id, List<String> milestoneComments);

    /**
     * Finds a [Bill] document with the specified characteristics.
     *
     * @param id     the ID of the bill document to retrieve.
     * @param latest retrieves the latest version of the proposal document, when *true*.
     * @return the found bill document.
     */
    Bill findBillById(String id, boolean latest);

    /**
     * Finds all versions of a [Bill] document with the specified characteristics.
     *
     * @param id           the ID of the bill document to retrieve.
     * @param fetchContent streams the content
     * @return the list of found bill document versions or empty.
     */
    List<Bill> findBillVersions(String id, boolean fetchContent);
    
    /**
     * Finds a [Bill] document with the specified characteristics.
     *
     * @param ref the reference metadata of the bill document to retrieve.
     * @return the found bill document.
     */
    @PostAuthorize("hasPermission(returnObject, 'CAN_READ')")
    Bill findBillByRef(String ref);
    
    List<Bill> findAllMinorsForIntermediate(String docRef, String curr, String prev, int startIndex, int maxResults);

    int findAllMinorsCountForIntermediate(String docRef, String currIntVersion, String prevIntVersion);

    Integer findAllMajorsCount(String docRef);

    List<Bill> findAllMajors(String docRef, int startIndex, int maxResult);

    List<Bill> findRecentMinorVersions(String documentId, String documentRef, int startIndex, int maxResults);

    Integer findRecentMinorVersionsCount(String documentId, String documentRef);

}
