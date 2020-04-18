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
package eu.europa.ec.leos.annotate.model;

import eu.europa.ec.leos.annotate.model.entity.Group;

import java.io.Serializable;
import java.util.Comparator;

/**
 * comparator for groups that respects the ordering of groups to be applied for the groups API
 */
public class GroupComparator implements Comparator<Group>, Serializable {

    private static final long serialVersionUID = 454410401690111487L;

    private final String defaultGroupName;

    // -------------------------------------
    // Constructor
    // -------------------------------------
    public GroupComparator(final String defaultGroupName) {
        this.defaultGroupName = defaultGroupName;
    }

    /**
     * desired order of the groups:
     * - scoped open groups (not supported yet)
     * - world public group
     * - other public groups (not specified in https://github.com/hypothesis/product-backlog/issues/461, but supported for the moment)
     * - private groups
     * 
     * within each block, ordering by displayname is applied
     */
    @Override
    public int compare(final Group first, final Group second) {

        // general reminder:
        // negative return value: first appears before second
        // positive return value: second appears before first

        if (first.isPublicGroup() && second.isPublicGroup()) {

            // filter out the world public group
            if (first.getName().equals(this.defaultGroupName)) {
                return -1;
            } else if (second.getName().equals(this.defaultGroupName)) {
                return 1;
            } else {
                // neither is the world public group - order by displayname
                return first.getDisplayName().compareTo(second.getDisplayName());
            }
        } else if (first.isPublicGroup() && !second.isPublicGroup()) {
            // first is public -> first item first
            return -1;
        } else if (!first.isPublicGroup() && second.isPublicGroup()) {
            // second is public -> second item first
            return 1;
        } else {
            // both are private groups - order by displayname
            return first.getDisplayName().compareTo(second.getDisplayName());
        }
    }

}
