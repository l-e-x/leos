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

import eu.europa.ec.leos.annotate.model.PublishContributionsResult;
import eu.europa.ec.leos.annotate.model.ResponseStatusUpdateResult;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.web.PublishContributionsRequest;
import eu.europa.ec.leos.annotate.model.web.StatusUpdateRequest;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateMetadataException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotPublishContributionsException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotUpdateAnnotationStatusException;
import eu.europa.ec.leos.annotate.services.exceptions.MissingPermissionException;

import javax.annotation.Nonnull;

/**
 * service responsible for changing annotation (response) status
 * moved out of {@link AnnotationService}
 */
public interface StatusUpdateService {

    /**
     * update the response status of annotations matching certain criteria
     * 
     * @param updateRequest 
     *        defines the annotations whose response status is to be updated (identified via metadata)
     * @param userInfo 
     *        information about the user launching the update
     * 
     * @return {@link ResponseStatusUpdateResult} containing lists of annotation IDs affected by the update 
     *         (those whose response status has changed and those who were deleted)
     * 
     * @throws CannotUpdateAnnotationStatusException
     *         thrown when required information is missing or no metadata found as requested
     * @throws MissingPermissionException
     *         thrown when requesting user does not have appropriate permissions for changing the response status
     */
    @Nonnull
    ResponseStatusUpdateResult updateAnnotationResponseStatus(final StatusUpdateRequest updateRequest, final UserInformation userInfo)
            throws CannotUpdateAnnotationStatusException, MissingPermissionException;

    /**
     * publish private annotations of a contributor (ISC context) publicly into the group
     * 
     * @param publishRequest 
     *        a {@link PublishContributionsRequest} defining the parameters like group, user login, ISC reference, document URI
     * @param userInfo 
     *        information about the user launching the update
     * 
     * @return {@link PublishContributionsResult} containing lists of annotation IDs affected by the update 
     * 
     * @throws CannotPublishContributionsException
     *         thrown when required information is missing or no metadata or annotations found as requested
     * @throws MissingPermissionException
     *         thrown when requesting user does not have appropriate permissions for publishing the annotations
     * @throws CannotCreateMetadataException
     *         thrown in case of errors in metadata management
     */
    @Nonnull
    PublishContributionsResult publishContributions(PublishContributionsRequest publishRequest, UserInformation userInfo)
            throws CannotPublishContributionsException, MissingPermissionException, CannotCreateMetadataException;
}
