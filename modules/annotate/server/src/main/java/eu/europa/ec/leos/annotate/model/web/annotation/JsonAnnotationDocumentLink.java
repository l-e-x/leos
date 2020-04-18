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
package eu.europa.ec.leos.annotate.model.web.annotation;

import eu.europa.ec.leos.annotate.Generated;

import java.net.URI;
import java.util.Objects;

public class JsonAnnotationDocumentLink {

    /**
     * Class representing the structure received by the hypothesis client for a document link
     */

    private URI href;

    // -------------------------------------
    // Constructors
    // -------------------------------------
    public JsonAnnotationDocumentLink() {
        // default constructor
    }
    
    public JsonAnnotationDocumentLink(final JsonAnnotationDocumentLink orig) {
        // copy constructor
        if(orig.href != null) {
            this.href = URI.create(orig.href.toString());
        }
    }
    
    public JsonAnnotationDocumentLink(final URI uri) {
        this.href = uri;
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------

    @Generated
    public URI getHref() {
        return href;
    }

    @Generated
    public void setHref(final URI href) {
        this.href = href;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(href);
    }

    @Generated
    @Override
    public boolean equals(final Object obj) {
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
