/*
 * Copyright 2018 European Commission
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

import eu.europa.ec.leos.annotate.model.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResult;
import eu.europa.ec.leos.annotate.services.exceptions.*;

import java.util.List;

public interface AnnotationService {

    // creating an annotation
    JsonAnnotation createAnnotation(JsonAnnotation annot, String userlogin)
            throws CannotCreateAnnotationException;

    // updating an annotation
    JsonAnnotation updateAnnotation(String annotationId, JsonAnnotation jsonAnnotation, String userlogin)
            throws CannotUpdateAnnotationException, MissingPermissionException;

    // retrieval of a single annotation
    Annotation findAnnotationById(String id);

    Annotation findAnnotationById(String id, String userlogin)
            throws MissingPermissionException;

    // deleting an annotation
    void deleteAnnotationById(String annotationId, String userlogin)
            throws CannotDeleteAnnotationException;

    // checking annotation type
    boolean isSuggestion(Annotation sugg);
    
    // searching for annotations and their replies
    List<Annotation> searchAnnotations(AnnotationSearchOptions options, String userlogin);

    List<Annotation> searchRepliesForAnnotations(List<Annotation> annotations, AnnotationSearchOptions options, String userlogin);

    // conversion of annotations to model representation later for Json responses
    JsonAnnotation convertToJsonAnnotation(Annotation annot);

    JsonSearchResult convertToJsonSearchResult(List<Annotation> annotations, List<Annotation> replies, AnnotationSearchOptions options);

    // functionality related to suggestions (i.e. specific annotations)
    void acceptSuggestionById(String suggestionId, String userlogin)
            throws CannotAcceptSuggestionException, NoSuggestionException, MissingPermissionException, CannotDeleteAnnotationException;
    
    void rejectSuggestionById(String suggestionId, String userlogin)
            throws CannotRejectSuggestionException,  NoSuggestionException, MissingPermissionException, CannotDeleteAnnotationException;
}
