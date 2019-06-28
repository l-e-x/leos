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
import eu.europa.ec.leos.annotate.model.SimpleMetadata;

import java.util.List;
import java.util.Objects;

public class JsonAnnotationDocument {

    /**
     * Class representing the structure received by the hypothesis client for a document 
     */

    private String title;
    private List<JsonAnnotationDocumentLink> link;
    private SimpleMetadata metadata;

    // -------------------------------------
    // Getters & setters
    // -------------------------------------

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public List<JsonAnnotationDocumentLink> getLink() {
        return link;
    }

    public void setLink(final List<JsonAnnotationDocumentLink> link) {
        this.link = link;
    }

    public SimpleMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(final SimpleMetadata metadata) {
        this.metadata = metadata;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(title, link);
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
        final JsonAnnotationDocument other = (JsonAnnotationDocument) obj;
        return Objects.equals(this.title, other.title) &&
                Objects.equals(this.link, other.link) &&
                Objects.equals(this.metadata, other.metadata);
    }
}