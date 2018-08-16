/*
 * Copyright 2018 European Commission
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
package eu.europa.ec.leos.domain.common;

public class InstanceContext {
    public enum Type {
        COUNCIL("COUNCIL"),
        COMMISSION("COMMISSION");

        private String value;

        Type(String val) {
            this.value = val;
        }

        public String getValue() {
            return value;
        }
    }

    final private Type instance;

    public InstanceContext(String instance) {
        this.instance = Type.valueOf(instance);
    }

    public boolean isCouncil() {
        return Type.COUNCIL.equals(instance);
    }

    public boolean isCommission() {
        return Type.COMMISSION.equals(instance);
    }

    public Type getType() {
        return instance;
    }
}
