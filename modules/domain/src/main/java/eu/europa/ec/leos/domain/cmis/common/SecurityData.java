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
package eu.europa.ec.leos.domain.cmis.common;

import java.util.Map;
import java.util.Objects;

public class SecurityData implements Securable {

    private final Map<String, String> collaborators;

    public SecurityData(Map<String, String> collaborators) {
        this.collaborators = collaborators;
    }

    @Override
    public Map<String, String> getCollaborators() {
        return collaborators;
    }

    @Override
    public String toString() {
        return "SecurityData{" +
                "collaborators=" + collaborators +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityData that = (SecurityData) o;
        return Objects.equals(collaborators, that.collaborators);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collaborators);
    }
}
