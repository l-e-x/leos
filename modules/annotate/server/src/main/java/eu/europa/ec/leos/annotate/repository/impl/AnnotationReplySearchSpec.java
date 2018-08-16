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
 * Search specification class for searching for replies to Annotations
 */
public class AnnotationReplySearchSpec implements Specification<Annotation> {

    // -------------------------------------
    // Private variables
    // -------------------------------------

    private List<String> annotationIds;
    private long executingUserId;
    private List<Long> userIdsOfThisGroup;

    // -------------------------------------
    // Constructor
    // -------------------------------------

    /**
     * receives the following parameters:
     * 
     * @param annonationIds
     *        the IDs of the annotations whose replies are wanted
     * @param executingUserId
     *        the ID of the user running the search - influences what is visible to him
     * @param userIdsOfThisGroup
     *        the IDs of other users being member of the desired group
     *        
     */
    public AnnotationReplySearchSpec(List<String> annotationIds, long executingUserId, List<Long> userIdsOfThisGroup) {

        this.annotationIds = annotationIds;
        this.executingUserId = executingUserId;
        this.userIdsOfThisGroup = userIdsOfThisGroup;
    }

    // -------------------------------------
    // Search predicate
    // -------------------------------------
    @Override
    public Predicate toPredicate(Root<Annotation> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

        List<Predicate> predicates = new ArrayList<>();

        // the rootAnnotationId must be one of our given values - this denotes that an annotation is a reply to any of our given annotations
        predicates.add(root.get("rootAnnotationId").in(this.annotationIds));

        // predicate stating whether the executing user has the permission to see an annotation reply
        Predicate hasPermissionToSee = cb.or(

                // first possibility: if the requesting user created the annotation, then he may see it without further restrictions
                // aka: annot.getUser().getId().equals(executingUserId)
                cb.equal(root.get("userId"), this.executingUserId),

                // second possibility: annotation must be
                // a) shared (aka: annot.isShared())
                // and
                // b) user must be member of the group in which the annotation was published
                // aka: groupService.isUserMemberOfGroup(user, annot.getGroup())
                cb.and(cb.isTrue(root.get("shared")),
                        root.get("userId").in(this.userIdsOfThisGroup)));
        predicates.add(hasPermissionToSee);

        return cb.and(predicates.toArray(new Predicate[0]));
    }

}
