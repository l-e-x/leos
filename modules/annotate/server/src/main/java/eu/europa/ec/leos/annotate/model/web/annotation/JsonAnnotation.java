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
import java.util.List;
import java.util.Objects;

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
    private List<String> references;
    private String user;
    private JsonAnnotationPermissions permissions;

    // Metadata filled in by the service after the annotation was persisted
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updated;
    @SuppressWarnings("PMD.ShortVariable")
    private String id;

    // User meta data
    private JsonUserInfo user_info;
    
    // annotation status
    private JsonAnnotationStatus status;

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(final LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(final LocalDateTime updated) {
        this.updated = updated;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public String getId() {
        return id;
    }

    public void setId(final String annotId) {
        this.id = annotId;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(final List<String> tags) {
        this.tags = tags;
    }

    public JsonAnnotationDocument getDocument() {
        return document;
    }

    public void setDocument(final JsonAnnotationDocument document) {
        this.document = document;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(final URI uri) {
        this.uri = uri;
    }

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public JsonAnnotationPermissions getPermissions() {
        return permissions;
    }

    public void setPermissions(final JsonAnnotationPermissions permissions) {
        this.permissions = permissions;
    }

    public List<String> getReferences() {
        return references;
    }

    public void setReferences(final List<String> references) {
        this.references = references;
    }

    public List<JsonAnnotationTargets> getTarget() {
        return target;
    }

    public void setTarget(final List<JsonAnnotationTargets> target) {
        this.target = target;
    }

    public JsonUserInfo getUser_info() {
        return user_info;
    }

    public void setUser_info(final JsonUserInfo user_info) {
        this.user_info = user_info;
    }

    public JsonAnnotationStatus getStatus() {
        return status;
    }
    
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
        return Objects.hash(id, created, document, text, group, tags, uri, target, references, user, permissions, updated, user_info, status);
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
