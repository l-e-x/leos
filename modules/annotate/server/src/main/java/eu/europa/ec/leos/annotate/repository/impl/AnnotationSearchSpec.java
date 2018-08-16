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
 * Search specification class for searching for Annotation objects (those not being replies!)
 */
public class AnnotationSearchSpec implements Specification<Annotation> {

    // -------------------------------------
    // Private variables
    // -------------------------------------

    private long executingUserId;
    private Long optionalUserIdToFilter;
    private long documentId;
    private long groupId;
    private List<Long> userIdsOfThisGroup;

    // -------------------------------------
    // Constructor
    // -------------------------------------
    
    /**
     * receives the following parameters:
     * 
     * @param executingUserId
     *        the ID of the user running the search - influences what is visible to him
     * @param documentId
     *        the ID of the document to which annotations belong to
     * @param groupId
     *        the ID group in which the user posted his annotations 
     * @param optionalUserIdToFilter
     *        optional user ID if only annotations of a specific user are wanted
     * @param userIdsOfThisGroup
     *        the IDs of other users being member of the desired group
     */
    public AnnotationSearchSpec(long executingUserId, long documentId, long groupId, Long optionalUserIdToFilter, List<Long> userIdsOfThisGroup) {

        this.executingUserId = executingUserId;
        this.documentId = documentId;
        this.groupId = groupId;
        this.optionalUserIdToFilter = optionalUserIdToFilter;
        this.userIdsOfThisGroup = userIdsOfThisGroup;
    }

    // -------------------------------------
    // Search predicate
    // -------------------------------------
    @Override
    public Predicate toPredicate(Root<Annotation> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

        List<Predicate> predicates = new ArrayList<>();

        // filter for mandatory document and group IDs
        predicates.add(cb.equal(root.get("documentId"), this.documentId));
        predicates.add(cb.equal(root.get("groupId"), this.groupId));

        // filter for a user ID, if requested
        if (this.optionalUserIdToFilter != null) {
            predicates.add(cb.equal(root.get("userId"), this.optionalUserIdToFilter));
        }

        // the rootAnnotationId must be empty - this denotes that annotations are wanted only (i.e. no replies)
        predicates.add(cb.isNull(root.get("rootAnnotationId")));

        // predicate stating whether the executing user has the permission to see an annotation
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

        return andTogether(predicates, cb);
    }

    private Predicate andTogether(List<Predicate> predicates, CriteriaBuilder cb) {
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
