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

import org.springframework.data.repository.CrudRepository;

/**
 * custom repository extension of the {@link AnnotationRepository}
 * needed for functionality bypassing the {@link CrudRepository} implementation, e.g. to circumvent alleged hibernate cleverness
 */
public interface AnnotationRepositoryCustom {

    /**
     * redefine the function from the CrudRepository interface
     * in order to assure that the custom implementation is called instead of the default implementation
     */
    void deleteAll();
}
