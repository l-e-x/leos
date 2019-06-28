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

import eu.europa.ec.leos.annotate.model.SimpleMetadata;
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

    // find a set of metadata matching the given document, group, system ID and other metadata
    Metadata findExactMetadata(Document document, Group group, String systemId, Metadata otherMetadata);

    Metadata findExactMetadata(Document document, Group group, String systemId, SimpleMetadata otherMetadata);

    // find a set of metadata using the associated document, group and system id
    @Nonnull
    List<Metadata> findMetadataOfDocumentGroupSystemid(Document document, Group group, String systemId);

    // find a set of metadata using the associated document, group and system id, having a given response status set
    @Nonnull
    List<Metadata> findMetadataOfDocumentGroupSystemidSent(Document document, Group group, String systemId);

    // saving a given metadata set
    Metadata saveMetadata(Metadata metadata) throws CannotCreateMetadataException;

    // find a set of metadata associated to a document and system Id, having one of the given group IDs
    @Nonnull
    List<Metadata> findMetadataOfDocumentSystemidGroupIds(Document document, String systemId, List<Long> groupIds);

    // find a set of metadata associated to a document and system Id, having a given response status
    @Nonnull
    List<Metadata> findMetadataOfDocumentSystemidSent(Document document, String authorityIsc);

    // helper functions
    boolean areAllMetadataContainedInDbMetadata(SimpleMetadata metadataRequired, Metadata candidateMeta);

    List<Long> getIdsOfMatchingMetadatas(List<Metadata> candidates, List<SimpleMetadata> requested);
    List<Long> getIdsOfMatchingMetadatas(List<Metadata> candidates, SimpleMetadata requested);

    List<Long> getMetadataSetIds(List<Metadata> metaList);

    @Nonnull
    List<Long> getNonNullMetadataSetIds(List<Metadata> metaList);

    List<Long> getMetadataSetIds(List<Metadata> metaList1, List<Metadata> metaList2);

    // removing an entry
    void deleteMetadataById(long metadataId);

    // status update of annotation's (via associate metadata)
    @Nonnull
    List<Long> updateMetadata(StatusUpdateRequest updateRequest, UserInformation userInfo)
            throws CannotUpdateAnnotationStatusException, MissingPermissionException;
}
