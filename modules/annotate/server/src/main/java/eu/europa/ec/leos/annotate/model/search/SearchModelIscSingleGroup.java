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
package eu.europa.ec.leos.annotate.model.search;

import eu.europa.ec.leos.annotate.model.MetadataIdsAndStatuses;

import java.util.List;

/**
 * Search model class for search model ISC.1 and ISC.2: search for ISC system with single, specific group
 * 
 * note: meanwhile, this class is identical to its base class; could be combined once search model development has settled
 */
public class SearchModelIscSingleGroup extends SearchModel {

    // -------------------------------------
    // Constructors
    // -------------------------------------

    public SearchModelIscSingleGroup(final ResolvedSearchOptions rso, final List<MetadataIdsAndStatuses> metadataIds) {
        super(rso, metadataIds);
    }

}
