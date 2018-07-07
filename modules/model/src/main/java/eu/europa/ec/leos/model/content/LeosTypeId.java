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
package eu.europa.ec.leos.model.content;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.commons.lang3.Validate;

public enum LeosTypeId {

    LEOS_FOLDER("leos:folder"),
    LEOS_FILE("leos:file"),
    LEOS_DOCUMENT("leos:document");

    private final String value;

    LeosTypeId(@Nonnull final String v) {
        Validate.notNull(v, "The type id value must not be null!");
        value = v;
    }

    public @Nonnull String value() {
        return value;
    }

    public boolean valueEquals(@Nullable final String v) {
        return value.equals(v);
    }

    public static @Nullable LeosTypeId fromValue(@Nullable final String v) {
        LeosTypeId leosTypeId = null;

        for (LeosTypeId t : LeosTypeId.values()) {
            if (t.value.equals(v)) {
                leosTypeId = t;
                break;
            }
        }

        return leosTypeId;
    }

    //check if the cmisType is equal to type of any of the super type
    //this method does not check for base types but only Leos Types
    public boolean checkIfType(ObjectType cmisType){
        if(cmisType.getId().equals(value)){
            return true;
        }
        else if(cmisType.getParentType()!=null){
            return checkIfType(cmisType.getParentType());
        }
        else {
            return false;//for case (cmisType.getParentType()==null){// it means we reached base type
        }
    }
}
