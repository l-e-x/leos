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
package eu.europa.ec.leos.annotate.services;

import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Document;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Metadata;
import eu.europa.ec.leos.annotate.model.web.StatusUpdateRequest;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateMetadataException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotUpdateAnnotationStatusException;
import eu.europa.ec.leos.annotate.services.exceptions.MissingPermissionException;

import javax.annotation.Nonnull;

import java.util.List;

public interface MetadataService {

    /**
     * find {@link Metadata} objects given their linked {@link Document}, {@link Group} and system ID
     * 
     * @param document 
     *        the linked document
     * @param group
     *        the associate group
     * @param systemId 
     *        the system ID via which the metadata was given
     * 
     * @return found {@link Metadata} objects, or empty list
     */
    @Nonnull
    List<Metadata> findMetadataOfDocumentGroupSystemid(Document document, Group group, String systemId);

    /**
     * find {@link Metadata} objects given their linked document URI, group name and system ID
     * 
     * @param document 
     *        the linked document's URI
     * @param group
     *        the associate group name
     * @param systemId 
     *        the system ID via which the metadata was given
     * 
     * @return found {@link Metadata} objects, or empty list
     */
    @Nonnull
    List<Metadata> findMetadataOfDocumentGroupSystemid(String docUri, String group, String isc);
    
    /**
     * find {@link Metadata} objects given their linked {@link Document}, {@link Group}, system ID, and having response status SENT
     * 
     * @param document 
     *        the linked document
     * @param group
     *        the associate group
     * @param systemId
     *        the system ID via which the metadata was given
     * 
     * @return found Metadata objects, or empty list
     */
    @Nonnull
    List<Metadata> findMetadataOfDocumentGroupSystemidSent(Document document, Group group, String systemId);

    /**
     * save a given {@link Metadata} set
     * 
     * @param metadata 
     *        the metadata set to be saved
     * 
     * @return saved metadata, with IDs filled in
     * @throws CannotCreateMetadataException 
     */
    Metadata saveMetadata(Metadata metadata) throws CannotCreateMetadataException;

    /**
     * find all {@link Metadata} objects given linked {@link Document}, system ID and a list of allowed linked {@link Group}s
     * 
     * @param document 
     *        the linked document
     * @param systemId 
     *        the system ID via which the metadata was given
     * @param groupIds 
     *        the list of associate groups' IDs
     * 
     * @return found Metadata objects, or empty list
     * @throws IllegalArgumentException if groupIds are {@literal null}
     */
    @Nonnull
    List<Metadata> findMetadataOfDocumentSystemidGroupIds(Document document, String systemId, List<Long> groupIds);

    /**
     * find all {@link Metadata} objects given linked {@link Document}, system ID, and having response status SENT
     * 
     * @param document 
     *        the linked document
     * @param systemId 
     *        the system ID via which the metadata was given
     * 
     * @return found {@link Metadata} objects, or empty list
     */
    @Nonnull
    List<Metadata> findMetadataOfDocumentSystemidSent(Document document, String authorityIsc);

    /**
     * find all {@link Metadata} objects given linked {@link Document}, {@link Group}, system ID, and having response status IN_PREPARATION
     * 
     * @param document 
     *        the linked document
     * @param group 
     *        the linked group
     * @param systemId 
     *        the system ID via which the metadata was given
     * 
     * @return found {@link Metadata} objects, or empty list
     */
    @Nonnull
    List<Metadata> findMetadataOfDocumentGroupSystemidInPreparation(final Document document,
            final Group group, final String systemId);

    /**
     * removal of given {@link Metadata} set identified by its ID
     * 
     * @param metadataId 
     *        ID of the {@link Metadata} set to be removed
     */
    void deleteMetadataById(long metadataId);

    /**
     * update the response status of metadata sets
     * note: "at least" matching is applied, i.e. all metadata having *at least* the requested ones are updated
     * 
     * @param updateRequest 
     *        {@link StatusUpdateRequest} object containing the group and document URI whose metadata is to be updated;
     *        in addition, it contains the new responseStatus and further metadata properties for identifying target metadata
     * @param userInfo 
     *        information about the user requesting metadata update
     * 
     * @return returns the list {@link Metadata} sets that were updated; at least an empty list is returned, but not {@literal null}
     * 
     * @throws CannotUpdateAnnotationStatusException 
     *         whenever required information cannot be identified (e.g. related group/document)
     * @throws MissingPermissionException 
     *         thrown if user does not have required permissions
     */
    @Nonnull
    List<Metadata> updateMetadata(StatusUpdateRequest updateRequest, UserInformation userInfo)
            throws CannotUpdateAnnotationStatusException, MissingPermissionException;

    /**
     * create a {@link Metadata} instance based on the information contained in a {@link StatusUpdateRequest}
     * 
     * @param updateRequest
     *        incoming {@link StatusUpdateRequest}
     * @return assembled {@link Metadata}
     * @throws CannotUpdateAnnotationStatusException
     *         exception thrown when mandatory Metadata information is missing (e.g. document, group)
     */
    @Nonnull
    Metadata createMetadataFromStatusUpdateRequest(final StatusUpdateRequest updateRequest) throws CannotUpdateAnnotationStatusException;

}
