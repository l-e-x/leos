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
package eu.europa.ec.leos.annotate.repository.impl;

import eu.europa.ec.leos.annotate.model.entity.Annotation;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;

/**
 * Search specification class for searching Annotations based on their ID;
 * this could of course be done simpler, but this is the easiest way to have
 * the DB do paging also 
 */
public class AnnotationByIdSearchSpec implements Specification<Annotation> {

    // -------------------------------------
    // Private variables
    // -------------------------------------

    private final List<String> annotationIds;

    // -------------------------------------
    // Constructor
    // -------------------------------------

    /**
     * receives the following parameter:
     * 
     * @param annonationIds
     *        the IDs of the annotations whose replies are wanted
     */
    public AnnotationByIdSearchSpec(final List<String> annotationIds) {

        this.annotationIds = annotationIds;
    }

    // -------------------------------------
    // Search predicate
    // -------------------------------------
    @Override
    @SuppressWarnings({"PMD.OptimizableToArrayCall"})
    public Predicate toPredicate(final Root<Annotation> root, final CriteriaQuery<?> query, final CriteriaBuilder critBuilder) {

        final List<Predicate> predicates = new ArrayList<>();

        // the id must be one of our given values
        predicates.add(root.get("id").in(this.annotationIds));

        // note: predicate stating whether the executing user has the permission to see an annotation is not evaluated here
        return critBuilder.and(predicates.toArray(new Predicate[0]));
    }
}
