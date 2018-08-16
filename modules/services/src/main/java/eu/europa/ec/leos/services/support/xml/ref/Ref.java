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
package eu.europa.ec.leos.services.support.xml.ref;

import java.util.Objects;

public class Ref {
    private final String id;
    private final String href;

    public Ref(String id, String href) {
        this.id = id;
        this.href = href;
    }

    public String getId() {
        return id;
    }

    public String getHref() {
        return href;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ref)) return false;
        Ref ref = (Ref) o;
        return Objects.equals(id, ref.id) &&
                Objects.equals(href, ref.href);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, href);
    }
}
