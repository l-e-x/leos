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

import eu.europa.ec.leos.annotate.model.entity.Annotation;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * custom Comparator for {@link Annotation} objects
 * used for sorting them, e.g. when merging a list of replies into a list of annotations
 */
public class AnnotationComparator implements Comparator<Annotation>, Serializable {

    private static final long serialVersionUID = -1674841791452196929L;

    private static final String CREATED_COLUMN = "created";
    private static final String UPDATED_COLUMN = "updated";
    private static final String SHARED_COLUMN = "shared";

    // list of column names that can be used for sorting
    // note: columns "text", "references" and the target selectors cannot be used for sorting since they are of CLOB data type
    public static final List<String> SORTABLE_COLUMN_NAMES = Collections.unmodifiableList(Arrays.asList(CREATED_COLUMN, UPDATED_COLUMN, SHARED_COLUMN));

    private String sortColumn;
    private int directionFlag = 1;

    // -------------------------------------
    // Constructor
    // -------------------------------------
    @SuppressWarnings("PMD.ConfusingTernary")
    public AnnotationComparator(final Sort sort) {

        if (sort.getOrderFor(CREATED_COLUMN) != null) {
            this.sortColumn = CREATED_COLUMN;
        } else if (sort.getOrderFor(UPDATED_COLUMN) != null) {
            this.sortColumn = UPDATED_COLUMN;
        } else if (sort.getOrderFor(SHARED_COLUMN) != null) {
            this.sortColumn = SHARED_COLUMN;
        }
        if (!StringUtils.isEmpty(this.sortColumn)) {
            this.directionFlag = sort.getOrderFor(sortColumn).getDirection().isAscending() ? 1 : -1;
        }
    }

    // -------------------------------------
    // Comparator implementation
    // -------------------------------------
    @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault")
    @Override
    public int compare(final Annotation arg0, final Annotation arg1) {

        if (StringUtils.isEmpty(this.sortColumn)) {
            return 0;
        }

        switch (this.sortColumn) {
            case CREATED_COLUMN:
                return this.directionFlag * arg0.getCreated().compareTo(arg1.getCreated());
            case UPDATED_COLUMN:
                return this.directionFlag * arg0.getUpdated().compareTo(arg1.getUpdated());
            case SHARED_COLUMN:
                if (arg0.isShared() == arg1.isShared()) {
                    return 0;
                } else if (arg0.isShared() && !arg1.isShared()) {
                    // we say "shared is 'larger'" than "not shared" -> return 1
                    return this.directionFlag;
                } else { // = !arg0.isShared() && arg1.isShared()
                    return -1 * this.directionFlag;
                }
        }
        
        return 0;
    }

}