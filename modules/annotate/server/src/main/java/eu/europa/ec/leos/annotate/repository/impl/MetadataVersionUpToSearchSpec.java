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

import eu.europa.ec.leos.annotate.model.entity.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;

public class MetadataVersionUpToSearchSpec implements Specification<Metadata> {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataVersionUpToSearchSpec.class);

    // -------------------------------------
    // Private variables
    // -------------------------------------

    private final String version;
    private final List<Long> metadataIds;

    // -------------------------------------
    // Constructor
    // -------------------------------------

    /**
     * receives the following parameters:
     * 
     * @param version
     *        maximum version to be included into the result
     * @param metadataIds
     *        list of IDs of associate metadata sets of the annotations
     */
    public MetadataVersionUpToSearchSpec(final String version,
            final List<Long> metadataIds) {

        this.version = version;
        this.metadataIds = metadataIds;
    }

    // -------------------------------------
    // Search predicate
    // -------------------------------------
    @Override
    @SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.OptimizableToArrayCall", "PMD.EmptyIfStmt"})
    public Predicate toPredicate(final Root<Metadata> root, final CriteriaQuery<?> query, final CriteriaBuilder critBuilder) {

        final List<Predicate> predicates = new ArrayList<>();

        // filter for a version using "<=" operator
        if (!StringUtils.isEmpty(this.version)) {
            LOG.trace("filter version<={}", this.version);
            predicates.add(critBuilder.lessThanOrEqualTo(root.get("version"), this.version));
        }

        // filter for metadata IDs
        Predicate metaPred = null;
        LOG.trace("filter metadataIds={}", this.metadataIds.toString());
        
        if (this.metadataIds.size() == 0) {
            // don't include the predicate
        } else if (this.metadataIds.size() > 1) {
            metaPred = root.get("id").in(this.metadataIds);
        } else {
            // small fine-tuning of the internal generated query if there is only one element; becomes slightly more efficient this way
            metaPred = critBuilder.equal(root.get("id"), this.metadataIds.get(0));
        }
        
        if(metaPred != null) {
            predicates.add(metaPred);
        }

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
