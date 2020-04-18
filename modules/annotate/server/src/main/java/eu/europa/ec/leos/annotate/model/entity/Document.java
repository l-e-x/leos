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
package eu.europa.ec.leos.annotate.model.entity;

import eu.europa.ec.leos.annotate.Generated;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;

import java.net.URI;
import java.util.Objects;

@Entity
@Table(name = "DOCUMENTS", uniqueConstraints = @UniqueConstraint(columnNames = {
        "URI"}))
public class Document {

    /**
     * Class representing a document to which an annotation is associated 
     */

    // -------------------------------------
    // column definitions
    // -------------------------------------

    @Id
    @Column(name = "DOCUMENT_ID", nullable = false)
    @GenericGenerator(name = "documentsSequenceGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
            @Parameter(name = "sequence_name", value = "DOCUMENTS_SEQ"),
            // @Parameter(name = "initial_value", value = "1000"),
            @Parameter(name = "increment_size", value = "1")
    })
    @GeneratedValue(generator = "documentsSequenceGenerator")
    @SuppressWarnings("PMD.ShortVariable")
    private long id;

    @Column(name = "TITLE")
    private String title;

    @Column(name = "URI", nullable = false, unique = true)
    private String uri;

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------

    public Document() {
        // default constructor required by JPA
    }

    public Document(final URI uri, final String title) {
        this.uri = uri.toASCIIString();
        this.title = title;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------

    @Generated
    public Long getId() {
        return id;
    }

    @Generated
    public void setId(final Long newId) {
        this.id = newId;
    }

    @Generated
    public String getTitle() {
        return title;
    }

    @Generated
    public void setTitle(final String title) {
        this.title = title;
    }

    @Generated
    public String getUri() {
        return uri;
    }

    @Generated
    public void setUri(final String uri) {
        this.uri = uri;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(id, title, uri);
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
        final Document other = (Document) obj;
        return Objects.equals(this.id, other.id) &&
                Objects.equals(this.title, other.title) &&
                Objects.equals(this.uri, other.uri);
    }
}
