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
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.entity.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;

import java.util.ArrayList;
import java.util.List;

public class AnnotationCountSearchSpec implements Specification<Annotation> {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationCountSearchSpec.class);

    // -------------------------------------
    // Private variables
    // -------------------------------------

    private final List<MetadataIdsAndStatuses> metadataIdsAndStatuses;

    // -------------------------------------
    // Constructor
    // -------------------------------------

    /**
     * receives the following parameters:
     * 
     * @param metadataId
     *        ID of associate metadata set (implicitly defines groupId and documentId)
     * @param statuses
     *        list of {@link AnnotationStatus} objects that annotations might have
     */
    public AnnotationCountSearchSpec(final List<MetadataIdsAndStatuses> metaStatus) {

        this.metadataIdsAndStatuses = metaStatus;
    }

    @SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.OptimizableToArrayCall"})
    @Override
    public Predicate toPredicate(final Root<Annotation> root, final CriteriaQuery<?> query, final CriteriaBuilder critBuilder) {

        final List<Predicate> predicates = new ArrayList<Predicate>();

        // note: we do not filter for documentId and groupId any more since Metadata = Document x Group
        // i.e. by filtering on metadata below, we implicitly filter by Document and Group

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
        predicates.add(critBuilder.or(metadataStatusPreds.toArray(new Predicate[0])));

        // the rootAnnotationId must be empty - this denotes that annotations are wanted only (i.e. no replies)
        LOG.trace("filter rootAnnotationId is null");
        predicates.add(critBuilder.isNull(root.get("rootAnnotationId")));

        // annotation must be published publicly
        LOG.trace("filter for shared (publicly visible) annotations");
        predicates.add(critBuilder.isTrue(root.get("shared")));

        // filter out highlights - therefore we need to join with the tags
        LOG.trace("filter for all annotation types but highlights");
        final Join<Annotation, Tag> tagJoin = root.join("tags", JoinType.LEFT);
        // predicates.add(critBuilder.notEqual(tagJoin.get("name"), Annotation.ANNOTATION_HIGHLIGHT));
        predicates.add(critBuilder.or(
                critBuilder.equal(tagJoin.get("name"), Annotation.ANNOTATION_COMMENT),
                critBuilder.isNull(tagJoin), // page note: has no tags written
                critBuilder.equal(tagJoin.get("name"), Annotation.ANNOTATION_SUGGESTION)));
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
