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
package eu.europa.ec.leos.annotate.model.web.annotation;

import java.net.URI;
import java.util.Objects;

public class JsonAnnotationDocumentLink {

    /**
     * Class representing the structure received by the hypothesis client for a document link
     */

    private URI href;

    // -------------------------------------
    // Getters & setters
    // -------------------------------------
    
    public URI getHref() {
        return href;
    }

    public void setHref(URI href) {
        this.href = href;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------
    
    @Override
    public int hashCode() {
        return Objects.hash(href);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JsonAnnotationDocumentLink other = (JsonAnnotationDocumentLink) obj;
        return Objects.equals(this.href, other.href);
    }
}
