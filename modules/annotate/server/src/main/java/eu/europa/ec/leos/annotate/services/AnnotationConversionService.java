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
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchResult;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotationStatus;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResult;

import java.util.List;

public interface AnnotationConversionService {

    /**
     * convert a given {@link Annotation} object into {@link JsonAnnotation} format
     * 
     * @param annot
     *        the Annotation object to be converted
     * @param userInfo
     *        user information about the user requesting the action
     * @return the wrapped JsonAnnotation object
     */
    JsonAnnotation convertToJsonAnnotation(Annotation annot, UserInformation userInfo);
    
    /**
     * convert a given list of Annotation objects into JsonSearchResult format, taking search options into account
     * (e.g. whether replies should be listed separately)
     * 
     * @param annotationResult
     *        the wrapper object containing the list of Annotations objects to be converted
     * @param replies 
     *        the replies belonging to the found annotations
     * @param options 
     *        search options that might influence the result, e.g. whether replies should be listed separately
     * @return the wrapped JsonSearchResult object
     */
    JsonSearchResult convertToJsonSearchResult(AnnotationSearchResult annotations, List<Annotation> replies, 
            AnnotationSearchOptions options, UserInformation userInfo);

    /**
     * assemble the status information of an annotation in JSON format
     * 
     * @param annot 
     *        Annotation for which to assemble the information
     * @return filled-in {@link JsonAnnotationStatus} object
     */
    JsonAnnotationStatus getJsonAnnotationStatus(Annotation annot);
}
