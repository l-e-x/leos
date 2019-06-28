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
package eu.europa.ec.leos.annotate.model.search;

import eu.europa.ec.leos.annotate.Generated;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class AnnotationSearchCountOptions {

    /**
     *  class representing the options available for searching for the number of annotations of a URI/group/metadata
     */

    // -------------------------------------
    // Available properties
    // -------------------------------------

    // URI of the document for which annotations are wanted
    private URI uri;

    // group name in which the annotations are published
    private String group;

    // list of metadata sets requested (JSON format)
    // can also contain the statuses that should be matched
    private String metadatasets;

    // -------------------------------------
    // Constructors
    // -------------------------------------

    public AnnotationSearchCountOptions() {
        // default constructor
    }

    // constructor with mandatory search parameters
    public AnnotationSearchCountOptions(final String uri, final String group, final String serializedMetadata) {

        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot search as given URI is invalid", e);
        }

        this.group = group;
        this.metadatasets = serializedMetadata;
    }

    // -------------------------------------
    // Useful functions
    // -------------------------------------
    public void decodeEscapedBrackets() {

        // at least during test scenarios, we experienced problems when sending JSON metadata
        // with only one entry - therefore, we had encoded the curly brackets URL-conform,
        // and have to decode this again here
        if (!StringUtils.isEmpty(this.metadatasets)) {
            this.metadatasets = this.metadatasets.replace("%7B", "{")
                    .replace("%7D", "}")
                    .replace("\\", "");
        }
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------
    public URI getUri() {
        return uri;
    }

    public void setUri(final URI uri) {
        this.uri = uri;
    }

    public void setUri(final String uri) {
        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Given search URI is invalid", e);
        }
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public String getMetadatasets() {
        return metadatasets;
    }

    public void setMetadatasets(final String metadataMap) {
        this.metadatasets = metadataMap;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(uri, group, metadatasets);
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

        final AnnotationSearchCountOptions other = (AnnotationSearchCountOptions) obj;
        return Objects.equals(this.uri, other.uri) &&
                Objects.equals(this.group, other.group) &&
                Objects.equals(this.metadatasets, other.metadatasets);
    }

}
