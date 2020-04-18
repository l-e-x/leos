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
package eu.europa.ec.leos.cmis.support;

import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.commons.lang3.Validate;

public class OperationContextProvider {

    public static final int MAX_ITEMS_PER_PAGE = 40;

    public static OperationContext getMinimalContext(Session session) {
        return getOperationContext(session, null, MAX_ITEMS_PER_PAGE);
    }

    public static OperationContext getOperationContext(Session session, String orderBy) {
        return getOperationContext(session, orderBy, MAX_ITEMS_PER_PAGE);
    }

    public static OperationContext getOperationContext(Session session, int maxItemPerPage) {
        return getOperationContext(session, null, maxItemPerPage);
    }

    public static OperationContext getOperationContext(Session session, String orderBy, int maxItemPerPage) {
        // create the context
        OperationContext context = session.createOperationContext();
        Validate.notNull(context, "The operation context must not be null!");

        // configure the context
        context.setFilter(null);
        context.setOrderBy(orderBy);
        context.setIncludeAcls(false);
        context.setIncludeAllowableActions(false);
        context.setIncludePathSegments(false);
        context.setIncludePolicies(false);
        context.setIncludeRelationships(IncludeRelationships.NONE);
        context.setRenditionFilterString(null);
        // will get the first MAX_ITEMS_PER_PAGE if there are more docs it will continue getting pages until getting all
        context.setMaxItemsPerPage(maxItemPerPage <= 0 ? MAX_ITEMS_PER_PAGE : maxItemPerPage);
        // enabling cache must be analysed on a case-by-case basis,
        // side effects will depend on the specific usage scenarios
        context.setCacheEnabled(false);

        return context;
    }
}
