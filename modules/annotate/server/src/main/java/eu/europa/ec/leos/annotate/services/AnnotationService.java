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
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchCountOptions;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchResult;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.services.exceptions.*;

import javax.annotation.Nonnull;

import java.util.List;

public interface AnnotationService {

    // creating an annotation
    JsonAnnotation createAnnotation(JsonAnnotation annot, UserInformation userInfo)
            throws CannotCreateAnnotationException;

    // updating an annotation
    JsonAnnotation updateAnnotation(String annotationId, JsonAnnotation jsonAnnotation, UserInformation userInfo)
            throws CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException;

    // retrieval of a single annotation
    Annotation findAnnotationById(String id);

    Annotation findAnnotationById(String id, String userlogin)
            throws MissingPermissionException;

    // deleting an annotation
    void deleteAnnotationById(String annotationId, UserInformation userInfo)
            throws CannotDeleteAnnotationException, CannotDeleteSentAnnotationException;

    // deleting a set of annotations
    List<String> deleteAnnotationsById(List<String> annotationIds, UserInformation userInfo);
    
    // checking annotation type
    boolean isSuggestion(Annotation sugg);

    // searching for annotations and their replies
    AnnotationSearchResult searchAnnotations(AnnotationSearchOptions options, UserInformation userInfo);
    List<Annotation> searchRepliesForAnnotations(AnnotationSearchResult searchRes, AnnotationSearchOptions options, UserInformation userInfo);

    // search for specific annotations only (and exlude highlights)
    int getAnnotationsCount(AnnotationSearchCountOptions options, UserInformation userInfo) throws MissingPermissionException;

    // functionality related to suggestions (i.e. specific annotations)
    void acceptSuggestionById(String suggestionId, UserInformation userInfo)
            throws CannotAcceptSuggestionException, NoSuggestionException, 
            MissingPermissionException, CannotDeleteAnnotationException, 
            CannotAcceptSentSuggestionException;

    void rejectSuggestionById(String suggestionId, UserInformation userInfo)
            throws CannotRejectSuggestionException, NoSuggestionException, 
            MissingPermissionException, CannotDeleteAnnotationException,
            CannotRejectSentSuggestionException;
    
    // retrieve all annotation Ids of annotations assigned to a given metadata set
    @Nonnull
    List<String> getAnnotationIdsOfMetadata(List<Long> metadataId);

}
