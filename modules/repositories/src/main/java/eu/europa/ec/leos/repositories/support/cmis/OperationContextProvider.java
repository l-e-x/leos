/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.repositories.support.cmis;

import java.util.HashSet;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OperationContextProvider {

    @Autowired
    private Session session;

    public OperationContext getMinimalContext() {
        // create the context
        OperationContext context = session.createOperationContext();
        Validate.notNull(context, "The operation context must not be null!");

        // select only the wanted properties, for better performance
        // the following properties are always retrieved, So not adding those to the list
        // - object id
        // - base type id
        // - object type id
        Set<String> properties = new HashSet<>();
        for(StorageProperties.Property property :StorageProperties.NonEditableProperty.values()){
            properties.add(property.getCMISKey());    
        }
        for(StorageProperties.Property property :StorageProperties.EditableProperty.values()){
            properties.add(property.getCMISKey());    
        }
        
        // configure the context
        context.setFilter(properties);
        context.setOrderBy(null);
        context.setIncludeAcls(false);
        context.setIncludeAllowableActions(false);
        context.setIncludePathSegments(false);
        context.setIncludePolicies(false);
        context.setIncludeRelationships(IncludeRelationships.NONE);
        context.setRenditionFilterString(CmisConstants.RENDITION_NONE);

        // enabling cache must be analysed on a case-by-case basis,
        // side effects will depend on the specific usage scenarios
        context.setCacheEnabled(false);

        return context;
    }
}
