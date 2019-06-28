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

import eu.europa.ec.leos.annotate.model.MetadataIdsAndStatuses;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;

/**
 * Search specification class for all search models: search depending on user ID(s), metadata IDs, and annotation statuses
 */
public class AnnotationSearchSpec implements Specification<Annotation> {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationSearchSpec.class);

    // -------------------------------------
    // Private variables
    // -------------------------------------

    private final long executingUserId;
    private final Long optionalUserIdToFilter;
    private final List<MetadataIdsAndStatuses> metadataIdsAndStatuses;

    // -------------------------------------
    // Constructor
    // -------------------------------------

    /**
     * receives the following parameters:
     * 
     * @param executingUserId
     *        the ID of the user running the search - influences what is visible to him
     * @param optionalUserIdToFilter
     *        optional user ID if only annotations of a specific user are wanted
     * @param metadataAndStatus
     *        list of IDs of associate metadata sets of the annotations (implicitly replaces groupId and documentId), combined with statuses wanted
     */
    public AnnotationSearchSpec(final long executingUserId, final Long optionalUserIdToFilter,
            final List<MetadataIdsAndStatuses> metadataAndStatus) {

        this.executingUserId = executingUserId;
        this.optionalUserIdToFilter = optionalUserIdToFilter;
        this.metadataIdsAndStatuses = metadataAndStatus;
    }

    // -------------------------------------
    // Search predicate
    // -------------------------------------
    @Override
    @SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.OptimizableToArrayCall"})
    public Predicate toPredicate(final Root<Annotation> root, final CriteriaQuery<?> query, final CriteriaBuilder critBuilder) {

        final List<Predicate> predicates = new ArrayList<>();

        // note: we do not filter for documentId and groupId any more since Metadata = Document x Group
        // i.e. by filtering on metadata below, we implicitly filter by Document and Group

        // filter for a user ID, if requested
        if (this.optionalUserIdToFilter != null) {
            LOG.trace("filter userId={}", this.optionalUserIdToFilter);
            predicates.add(critBuilder.equal(root.get("userId"), this.optionalUserIdToFilter));
        }

        // filter for metadata and status
        // we have a list of metadata x status pairs; they should all be OR-combined,
        // i.e. (meta1 AND status1) OR (meta2 AND status2) OR ... (metaN AND statusN)
        final List<Predicate> metadataStatusPreds = new ArrayList<Predicate>();
        for (final MetadataIdsAndStatuses mias : this.metadataIdsAndStatuses) {

            Predicate metaPred;
            LOG.trace("filter metadataIds={}", mias.getMetadataIds().toString());
            if (mias.getMetadataIds().size() > 1) {
                metaPred = root.get("metadataId").in(mias.getMetadataIds());
            } else {
                // small fine-tuning of the internal generated query if there is only one element; becomes slightly more efficient this way
                metaPred = critBuilder.equal(root.get("metadataId"), mias.getMetadataIds().get(0));
            }

            // the annotations must have a particular status (or one of a given list of possible statuses)
            LOG.trace("filter status={}", mias.getStatuses().toString());
            Predicate statusPred;
            if (mias.getStatuses().size() > 1) {
                statusPred = root.get("status").in(mias.getStatuses());
            } else {
                statusPred = critBuilder.equal(root.get("status"), mias.getStatuses().get(0));
            }

            metadataStatusPreds.add(critBuilder.and(metaPred, statusPred));
        }
        // combine all metadata/status items with OR, and add it to the global AND criteria list
        predicates.add(critBuilder.or(metadataStatusPreds.toArray(new Predicate[0])));

        // the rootAnnotationId must be empty - this denotes that annotations are wanted only (i.e. no replies)
        LOG.trace("filter rootAnnotationId is null");
        predicates.add(critBuilder.isNull(root.get("rootAnnotationId")));

        // predicate stating whether the executing user has the permission to see an annotation
        LOG.trace("filter userId={} OR shared", this.executingUserId);
        final Predicate hasPermissionToSee = critBuilder.or(

                // first possibility: if the requesting user created the annotation, then he may see it without further restrictions
                // aka: annot.getUser().getId().equals(executingUserId)
                critBuilder.equal(root.get("userId"), this.executingUserId),

                // second possibility: annotation must be shared (aka: annot.isShared())
                // note: previously, we also checked if the user was member of the group in which the annotation was published
                // (aka: groupService.isUserMemberOfGroup(user, annot.getGroup()))
                // this check has been externalised, search is skipped if user is not member
                critBuilder.isTrue(root.get("shared")));
        predicates.add(hasPermissionToSee);

        return andTogether(predicates, critBuilder);
    }

    /**
     * combines given individual predicates as AND predicates
     * 
     * @param predicates list of predicates to be combined
     * @param critBuilder {@link CriteriaBuilder} doing the operation
     * 
     * @return combined {@link Predicate}
     */
    @SuppressWarnings("PMD.OptimizableToArrayCall")
    private Predicate andTogether(final List<Predicate> predicates, final CriteriaBuilder critBuilder) {
        return critBuilder.and(predicates.toArray(new Predicate[0]));
    }

}
