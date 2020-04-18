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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import eu.europa.ec.leos.annotate.Generated;
import eu.europa.ec.leos.annotate.model.web.helper.LocalDateTimeDeserializer;
import eu.europa.ec.leos.annotate.model.web.helper.LocalDateTimeSerializer;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("PMD.GodClass")
public class JsonAnnotation {

    private static final Logger LOG = LoggerFactory.getLogger(JsonAnnotation.class);

    /**
     * Class representing the top-level structure received by the hypothesis client for an annotation 
     */

    // Metadata received from the hypothesis client when adding an annotation
    // complex objects are defined in neighbouring classes
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime created;
    private JsonAnnotationDocument document;
    private String text, group;
    private List<String> tags;
    private URI uri;
    private List<JsonAnnotationTargets> target;
    private List<String> references; // list of parent items
    private String user;
    private JsonAnnotationPermissions permissions;

    // Metadata filled in by the service after the annotation was persisted
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updated;
    @SuppressWarnings("PMD.ShortVariable")
    private String id;

    @JsonProperty(value = "linkedAnnot")
    private String linkedAnnotationId;

    // User meta data
    private JsonUserInfo user_info;

    // annotation status
    private JsonAnnotationStatus status;

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------
    public JsonAnnotation() {
        // default constructor
    }
    
    public JsonAnnotation(final JsonAnnotation orig) {
        
        // copy constructor
        this.created = orig.created;
        if(orig.document != null) {
            this.document = new JsonAnnotationDocument(orig.document);
        }
        this.group = orig.group;
        this.id = orig.id;
        this.linkedAnnotationId = orig.linkedAnnotationId;
        if(orig.permissions != null) {
            this.permissions = new JsonAnnotationPermissions(orig.permissions);
        }
        if(orig.references != null) {
            this.references = new ArrayList<String>();
            orig.references.forEach(refstring -> this.references.add(refstring));
        }
        this.status = orig.status;
        if(orig.tags != null) {
            this.tags = new ArrayList<String>();
            orig.tags.forEach(tag -> this.tags.add(tag));
        }
        if(orig.target != null) {
            this.target = new ArrayList<JsonAnnotationTargets>();
            orig.target.forEach(annotTarget -> this.target.add(annotTarget.copy()));
        }
        this.text = orig.text;
        this.updated = orig.updated;
        this.uri = orig.uri;
        this.user = orig.user;
        if(orig.user_info != null) {
            this.user_info = new JsonUserInfo(orig.user_info);
        }
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------

    @Generated
    public LocalDateTime getCreated() {
        return created;
    }

    @Generated
    public void setCreated(final LocalDateTime created) {
        this.created = created;
    }

    @Generated
    public LocalDateTime getUpdated() {
        return updated;
    }

    @Generated
    public void setUpdated(final LocalDateTime updated) {
        this.updated = updated;
    }

    @Generated
    public String getText() {
        return text;
    }

    @Generated
    public void setText(final String text) {
        this.text = text;
    }

    @Generated
    public String getGroup() {
        return group;
    }

    @Generated
    public void setGroup(final String group) {
        this.group = group;
    }

    @Generated
    public String getId() {
        return id;
    }

    @Generated
    public void setId(final String annotId) {
        this.id = annotId;
    }

    @Generated
    public String getLinkedAnnotationId() {
        return linkedAnnotationId;
    }

    @Generated
    public void setLinkedAnnotationId(final String linkedId) {
        this.linkedAnnotationId = linkedId;
    }

    @Generated
    public List<String> getTags() {
        return tags;
    }

    @Generated
    public void setTags(final List<String> tags) {
        this.tags = tags;
    }

    @Generated
    public JsonAnnotationDocument getDocument() {
        return document;
    }

    @Generated
    public void setDocument(final JsonAnnotationDocument document) {
        this.document = document;
    }

    @Generated
    public URI getUri() {
        return uri;
    }

    @Generated
    public void setUri(final URI uri) {
        this.uri = uri;
    }

    @Generated
    public String getUser() {
        return user;
    }

    @Generated
    public void setUser(final String user) {
        this.user = user;
    }

    @Generated
    public JsonAnnotationPermissions getPermissions() {
        return permissions;
    }

    @Generated
    public void setPermissions(final JsonAnnotationPermissions permissions) {
        this.permissions = permissions;
    }

    @Generated
    public List<String> getReferences() {
        return references;
    }

    @Generated
    public void setReferences(final List<String> references) {
        this.references = references;
    }

    @Generated
    public List<JsonAnnotationTargets> getTarget() {
        return target;
    }

    @Generated
    public void setTarget(final List<JsonAnnotationTargets> target) {
        this.target = target;
    }

    @Generated
    public JsonUserInfo getUser_info() {
        return user_info;
    }

    @Generated
    public void setUser_info(final JsonUserInfo user_info) {
        this.user_info = user_info;
    }

    @Generated
    public JsonAnnotationStatus getStatus() {
        return status;
    }

    @Generated
    public void setStatus(final JsonAnnotationStatus newStatus) {
        this.status = newStatus;
    }

    // -------------------------------------
    // Helper functions
    // -------------------------------------

    @JsonIgnore
    public String getSerializedTargets() {

        try {
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(target);
        } catch (JsonProcessingException e) {
            LOG.error("Annotation target selectors could not be serialized to JSON", e);
        }
        return "";
    }

    @JsonIgnore
    // help function stating whether the object denotes a reply to an annotation
    public boolean isReply() {
        return this.getReferences() != null && !this.getReferences().isEmpty();
    }

    @JsonIgnore
    // help function that retrieves the root of a communication thread (for replies)
    public String getRootAnnotationId() {

        if (!isReply()) {
            return null;
        }

        return this.getReferences().get(0);
    }

    @JsonIgnore
    // return whether the object features metadata
    public boolean hasMetadata() {
        return getDocument() != null && getDocument().getMetadata() != null;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(id, linkedAnnotationId, created, document, text, group, tags,
                uri, target, references, user, permissions, updated, user_info,
                status);
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
        final JsonAnnotation other = (JsonAnnotation) obj;
        return Objects.equals(this.id, other.id) &&
                Objects.equals(this.linkedAnnotationId, other.linkedAnnotationId) &&
                Objects.equals(this.created, other.created) &&
                Objects.equals(this.updated, other.updated) &&
                Objects.equals(this.text, other.text) &&
                Objects.equals(this.group, other.group) &&
                Objects.equals(this.user, other.user) &&
                Objects.equals(this.references, other.references) &&
                Objects.equals(this.uri, other.uri) &&
                Objects.equals(this.document, other.document) &&
                Objects.equals(this.tags, other.tags) &&
                Objects.equals(this.target, other.target) &&
                Objects.equals(this.permissions, other.permissions) &&
                Objects.equals(this.user_info, other.user_info) &&
                Objects.equals(this.status, other.status);
    }
}
