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
package eu.europa.ec.leos.annotate.model.entity;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "TAGS", uniqueConstraints = @UniqueConstraint(columnNames = {"NAME", "ANNOTATION_ID"}))
public class Tag {

    /**
     * Class representing a tag given to an annotation 
     */

    // -------------------------------------
    // column definitions
    // -------------------------------------

    @Id
    @Column(name = "TAG_ID", nullable = false)
    @GenericGenerator(name = "tagsSequenceGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
            @Parameter(name = "sequence_name", value = "TAGS_SEQ"),
            // @Parameter(name = "initial_value", value = "1000"),
            @Parameter(name = "increment_size", value = "1")
    })
    @GeneratedValue(generator = "tagsSequenceGenerator")
    private long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    // ID of the associate annotation; mapped by hibernate using ANNOTATION.ANNOTATION_ID column
    @ManyToOne
    @JoinColumn(name = "ANNOTATION_ID", nullable = false)
    private Annotation annotation;

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------

    public Tag() {
    }

    public Tag(String name, Annotation annotation) {
        this.annotation = annotation;
        this.name = name;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public void setAnnotation(Annotation annot) {
        this.annotation = annot;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Override
    public int hashCode() {
        return Objects.hash(id, name, annotation);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Tag other = (Tag) obj;
        return Objects.equals(this.id, other.id) &&
                Objects.equals(this.name, other.name) &&
                Objects.equals(this.annotation, other.annotation);
    }
}
