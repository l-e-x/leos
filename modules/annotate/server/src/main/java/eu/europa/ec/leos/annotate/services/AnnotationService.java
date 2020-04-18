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
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Metadata;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchCountOptions;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchResult;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.services.exceptions.*;

import javax.annotation.Nonnull;

import java.util.List;

public interface AnnotationService {

    /**
     * create an annotation in the database based on an incoming, JSON-deserialized annotation
     * 
     * @param webAnnot
     *        the incoming annotation
     * @param userInfo
     *        information about the user wanting to create an annotation
     * @throws CannotCreateAnnotationException 
     *         the exception is thrown when the annotation cannot be created due to unfulfilled constraints
     *         (e.g. missing document or user information)
     */
    JsonAnnotation createAnnotation(JsonAnnotation annot, UserInformation userInfo)
            throws CannotCreateAnnotationException;

    /**
     * update an existing annotation in the database based on an incoming, JSON-deserialized annotation
     * note: if a SENT annotation is updated in ISC (if permitted), then a new annotation is created 
     *       and returned!
     * 
     * @param annotationId 
     *        id of the annotation to be updated
     * @param webAnnot 
     *        the incoming annotation
     * @param userInfo
     *        information about the user wanting to update an annotation
     * @return updated annotation (which is a new one for SENT ISC annotations)
     *        
     * @throws CannotUpdateAnnotationException 
     *         the exception is thrown when the annotation cannot be updated (e.g. when it is not existing)
     * @throws CannotUpdateSentAnnotationException
     *         exception thrown when an annotation with response status SENT is tried to be updated
     * @throws MissingPermissionException 
     *         the exception is thrown when the user lacks permissions for updating the annotation
     */
    JsonAnnotation updateAnnotation(String annotationId, JsonAnnotation jsonAnnotation, UserInformation userInfo)
            throws CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException;

    /**
     * simple function that just looks up an annotation based on its ID WITHOUT permission checks
     * to be used only from tests or from services layer
     *  
     * @param annotId
     *        the ID of the wanted annotation
     *        
     * @return returns the found annotation object, or {@literal null}; note: only non-deleted annotations are returned
     */
    Annotation findAnnotationById(String annotId);

    /**
     * look up an annotation based on its ID, taking permissions into account
     * 
     * @param annotId
     *        the ID of the wanted annotation
     * @param userlogin
     *        the login of the user requesting to get the annotation
     *        
     * @return returns the found annotation object, or null
     * @throws MissingPermissionException 
     *         this exception is thrown when the user does not have the permission to view this annotation
     */
    Annotation findAnnotationById(String annotId, String userlogin)
            throws MissingPermissionException;

    /**
     * find sentDeleted annotations having certain statuses and metadata IDs
     * 
     * @param metadataIds
     *        list of {@link Metadata} IDs to match
     * @param statuses
     *        list of {@link AnnotationStatus} to match
     * @return found list of {@Annotation}s, or empty list
     */
    @Nonnull
    List<Annotation> findSentDeletedByMetadataIdAndStatus(
            final List<Long> metadataIds, final List<AnnotationStatus> statuses);

    /**
     * find annotations having certain status and metadata IDs
     * 
     * @param metadataIds
     *        list of {@link Metadata} IDs to match
     * @param status
     *        the {@link AnnotationStatus} to match
     * @return found list of {@Annotation}s, or empty list
     */
    @Nonnull
    List<Annotation> findByMetadataAndStatus(final List<Long> metadataIds, 
            final AnnotationStatus status);

    /**
     * find annotations having certain metadata IDs
     * 
     * @param metadataIds
     *        list of {@link Metadata} IDs to match
     * @return found list of {@Annotation}s, or empty list
     */
    @Nonnull
    List<Annotation> findByMetadata(final List<Long> metadataIds);
    
    /**
     * save a given list of {@link Annotation}s
     * this method just encapsules the {@link AnnotationRepository}
     * 
     * @param annot
     *        annotation to be saved
     * @return saved annotation
     */
    List<Annotation> saveAll(final List<Annotation> annot);
    
    /**
     * delete an annotation in the database based on an annotation ID
     * 
     * @param annotationId
     *        the ID of the annotation to be deleted
     * @param userInfo
     *        information about the user requesting to delete an annotation
     * @throws CannotDeleteAnnotationException 
     *         the exception is thrown when the annotation cannot be deleted, e.g. when it is not existing or due to unexpected database error)
     * @throws CannotDeleteSentAnnotationException
     *         the exception is thrown when trying to delete a SENT annotation
     */
    void deleteAnnotationById(String annotationId, UserInformation userInfo)
            throws CannotDeleteAnnotationException, CannotDeleteSentAnnotationException;

    /**
     * delete a set of annotations in the database based on their IDs
     * 
     * @param annotationIds
     *        the list of IDs of the annotation to be deleted
     * @param userInfo
     *        information about the user requesting to delete annotations
     * @return returns a list of annotations that were successfully deleted
     */
    List<String> deleteAnnotationsById(List<String> annotationIds, UserInformation userInfo);

    /**
     * check whether a given annotation represents a suggestion
     * 
     * @param sugg 
     *        the annotation to be checked
     * 
     * @return true if the annotation was identified as a suggestion
     */
    boolean isSuggestion(Annotation sugg);

    /**
     * check whether a given annotation represents a highlight
     * 
     * @param ann 
     *        the annotation to be checked
     * 
     * @return true if the annotation was identified as a highlight
     */
    boolean isHighlight(final Annotation ann);

    /** 
     * search for annotations meeting certain criteria and for a given user
     * (user influences which other users' annotations are included)
     * 
     * NOTE: search for tags is currently not supported - seems a special functionality from hypothesis client, but does not respect documented API
     * 
     * @param options
     *        the {@link AnnotationSearchOptions} detailing search criteria like group, number of results, ...
     * @param userInfo
     *        information about the user for which the search is being executed
     *        
     * @return returns a list of {@link Annotation} objects meeting the search criteria
     *         returns an empty list in case search could not be run due to unfulfilled requirements  
     */
    AnnotationSearchResult searchAnnotations(AnnotationSearchOptions options, UserInformation userInfo);

    /**
     * search for the replies belonging to a given set of annotations
     * (given user influences which other users' replies are included)
     * 
     * NOTE: search for tags is currently not supported - seems a special functionality from hypothesis client, but does not respect documented API
     * 
     * @param annotSearchRes
     *        the {@link AnnotationSearchResult} resulting from a previously executed search for (root) annotations
     * @param options
     *        the search options - required for sorting and ordering
     * @param userInfo
     *        information about the user requesting the search ({@link UserInformation})
     *        
     * @return returns a list of Annotation objects meeting belonging to the annotations and visible for the user
     */
    List<Annotation> searchRepliesForAnnotations(AnnotationSearchResult searchRes, AnnotationSearchOptions options, UserInformation userInfo);

    /**
     * retrieve the number of annotations for a given document/group/metadata
     * hereby, only public annotations are counted, and highlights are ignored
     * 
     * @param options 
     *        the options for the retrieval of annotations
     * @param userInfo 
     *        information about the user requesting the number of annotations
     * 
     * @throws MissingPermissionException
     *         this exception is thrown when requesting user is no ISC user, 
     *         or when other than ISC annotations are wanted
     * @return number of annotations, or -1 when retrieval could not even be launched
     *         due to failing precondition checks
     */
    int getAnnotationsCount(AnnotationSearchCountOptions options, UserInformation userInfo) throws MissingPermissionException;

    /**
     * accept a suggestion, taking permissions and authority into account
     * 
     * @param suggestionId
     *        the ID of the suggestion (annotation) to be accepted
     * @param userInfo
     *        information about the user requesting to accept the suggestion
     *        
     * @return returns the found annotation object, or {@literal null}
     * 
     * @throws CannotAcceptSuggestionException
     *         this exception is thrown when the referenced suggestion does not exist
     * @throws NoSuggestionException
     *         this exception is thrown when the referenced annotation is not a suggestion, but a different kind of annotation
     * @throws CannotAcceptSentSuggestionException
     *         this exception is thrown when the annotation has response status SENT and cannot be accepted
     * @throws MissingPermissionException 
     *         this exception is thrown when the user does not have the permission to reject the suggestion
     */
    void acceptSuggestionById(String suggestionId, UserInformation userInfo)
            throws CannotAcceptSuggestionException, NoSuggestionException,
            MissingPermissionException, CannotDeleteAnnotationException,
            CannotAcceptSentSuggestionException;

    /**
     * reject a suggestion, taking permissions and authority into account
     * 
     * @param suggestionId
     *        the ID of the suggestion (annotation) to be rejected
     * @param userInfo
     *        information about the user requesting to reject the suggestion
     *        
     * @return returns the found annotation object, or {@literal null}
     * 
     * @throws CannotRejectSuggestionException
     *         this exception is thrown when the referenced suggestion does not exist
     * @throws NoSuggestionException
     *         this exception is thrown when the referenced annotation is not a suggestion, but a different kind of annotation
     * @throws CannotRejectSentSuggestionException
     *         this exception is thrown when the annotation has response status SENT and cannot be accepted
     * @throws MissingPermissionException 
     *         this exception is thrown when the user does not have the permission to reject the suggestion
     */
    void rejectSuggestionById(String suggestionId, UserInformation userInfo)
            throws CannotRejectSuggestionException, NoSuggestionException,
            MissingPermissionException, CannotDeleteAnnotationException,
            CannotRejectSentSuggestionException;

    /**
     * soft deletion of an annotation
     * (recursive if annotation is a root annotation)
     *  
     * @param annot 
     *        the annotation to be deleted
     * @param userId 
     *        ID of the user requesting rejection
     * 
     * @throws CannotDeleteAnnotationException
     *         the exception is thrown when the annotation cannot be deleted, e.g. when it is not existing or due to unexpected database error)
     */
    void softDeleteAnnotation(final Annotation annot, final long userId) throws CannotDeleteAnnotationException;

    /**
     * change a given set of annotations to become public and save them
     * 
     * @param annots
     *        list of {@link Annotation}s to be made public
     */
    void makeShared(final List<Annotation> annots);

}
