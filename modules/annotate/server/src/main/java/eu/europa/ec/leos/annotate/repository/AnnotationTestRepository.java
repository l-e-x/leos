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
package eu.europa.ec.leos.annotate.repository;

import eu.europa.ec.leos.annotate.model.entity.Annotation;
import org.springframework.stereotype.Repository;

/**
 * This is an extension of the {@link AnnotationRepository} with functions that should be called only from unit tests.
 * In order to allow Spring autowiring to properly inject the correct Repository, this class needs to be referred to as follows:
 * 
 * @Autowired
 * @Qualifier("annotationTestRepos")
 * private AnnotationTestRepository annotRepos;
 * 
 */
@Repository("annotationTestRepos")
public interface AnnotationTestRepository extends AnnotationRepository {

    /**
     * find an annotation based on its ID
     * note: returns the annotation, independent whether it was deleted already
     * 
     * @param annotId the annotation's ID
     * 
     * @return found Annotation, or {@literal null}
     */
    Annotation findById(String annotId);

    /**
     * count all items having a root annotation id set (i.e. replies)
     * 
     * @return number of found annotations
     */
    long countByRootAnnotationIdNotNull();
}
