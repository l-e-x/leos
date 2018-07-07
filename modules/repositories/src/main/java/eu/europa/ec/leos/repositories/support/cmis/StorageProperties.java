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

import java.util.HashMap;
import java.util.Map;

import eu.europa.ec.leos.model.content.LeosObject;
import org.apache.chemistry.opencmis.commons.PropertyIds;

import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.model.content.LeosTypeId;

public class StorageProperties {

    private Map<String, Object> properties = new HashMap<String, Object>();

    public StorageProperties(LeosTypeId typeId){
        this.set(EditableProperty.TYPE, typeId.value());
    }
    
    public Map<String, Object> getAllProperties() {
        return properties;
    }
    
    public Object getValue(Property key) {
        return properties.get(key.getCMISKey());
    }
    
    public void set(EditableProperty key, Object value) {
        properties.put(key.getCMISKey(), value);
    }

    public interface Property  {
        public String getCMISKey();
    }

    /** Enums to encapsulate the CMIS properties*/
    public enum EditableProperty implements Property {
        NAME(PropertyIds.NAME),
        TYPE(PropertyIds.OBJECT_TYPE_ID),          //valid values : LeosTypeId.LEOS_DOCUMENT.value() or LeosTypeId.LEOS_FILE.value()
        TITLE(LeosDocumentProperties.TITLE),
        LANGUAGE(LeosDocumentProperties.LANGUAGE),
        TEMPLATE(LeosDocumentProperties.TEMPLATE),
        SYSTEM(LeosDocumentProperties.SYSTEM),
        STAGE(LeosDocumentProperties.STAGE),
        CHECKIN_COMMENT(PropertyIds.CHECKIN_COMMENT),
        AUTHOR_ID(LeosObject.AUTHOR_ID),
        AUTHOR_NAME(LeosObject.AUTHOR_NAME),
        CONTRIBUTOR_IDS(LeosObject.CONTRIBUTOR_IDS),
        CONTRIBUTOR_NAMES(LeosObject.CONTRIBUTOR_NAMES);
        
        private final String cmisKey;

        private EditableProperty(String cmisKey) {
            this.cmisKey = cmisKey;
        }

        public String getCMISKey() {
            return cmisKey;
        }  
    }// end EditableProperty

    public enum NonEditableProperty implements Property{

        DESCRIPTION(PropertyIds.DESCRIPTION),
        CREATED_BY(PropertyIds.CREATED_BY),
        CREATION_DATE(PropertyIds.CREATION_DATE),
        LAST_MODIFIED_BY(PropertyIds.LAST_MODIFIED_BY),
        LAST_MODIFICATION_DATE(PropertyIds.LAST_MODIFICATION_DATE),
        IS_LATEST_VERSION(PropertyIds.IS_LATEST_VERSION),
        VERSION_SERIES_ID(PropertyIds.VERSION_SERIES_ID),
        VERSION_LABEL(PropertyIds.VERSION_LABEL),
        CHECKIN_COMMENT(PropertyIds.CHECKIN_COMMENT),
        CHANGE_TOKEN(PropertyIds.CHANGE_TOKEN);

        private final String cmisKey;

        private NonEditableProperty(String cmisKey) {
            this.cmisKey = cmisKey;
        }

        public String getCMISKey() {
            return cmisKey;
        }
    }// end NonEditableProperty

}
