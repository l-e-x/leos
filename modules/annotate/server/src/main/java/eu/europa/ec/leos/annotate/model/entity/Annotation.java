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

import javax.persistence.*;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "ANNOTATIONS", indexes = {
        @Index(columnList = "USER_ID", name = "ANNOTATIONS_IX_USERS"),
        @Index(columnList = "GROUP_ID", name = "ANNOTATIONS_IX_GROUPS"),
        @Index(columnList = "METADATA_ID", name = "ANNOTATIONS_IX_METADATA"),
        @Index(columnList = "DOCUMENT_ID", name = "ANNOTATIONS_IX_DOCUMENTS")})
public class Annotation {

    /**
     *  class corresponding to the main annotation metadata
     */

    // -------------------------------------
    // column definitions
    // -------------------------------------

    // URL-safe UUID
    @Id
    @Column(name = "ANNOTATION_ID", unique = true, nullable = false)
    private String id;

    // the text of the annotation
    @Column(name = "TEXT")
    private String text;

    @Column(name = "CREATED", nullable = false)
    private LocalDateTime created;

    @Column(name = "UPDATED", nullable = false)
    private LocalDateTime updated;

    // flag indicating whether the annotation can be seen by the originating only (false)
    // or whether it is public in the user's group (true)
    @Column(name = "SHARED", nullable = false)
    private boolean shared;

    // selectors to the target location that is annotated
    // treated as a black box string
    @Column(name = "TARGET_SELECTORS", nullable = false)
    private String targetSelectors;

    // comma-separated list of strings (annotation IDs representing parent hierarchy in case of replies to annotations)
    @Column(name = "REFERENCES")
    private String references;

    // computed database column, contains the first annotation id stored in the 'references' list, if any
    @Column(name = "ROOT", insertable = false, updatable = false)
    private String rootAnnotationId;

    // user ID column, filled by hibernate
    @Column(name = "USER_ID", insertable = false, updatable = false, nullable = false)
    private long userId;

    // associate user, mapped by hibernate using USERS.USER_ID column
    @OneToOne
    @JoinColumn(name = "USER_ID")
    private User user;

    // group ID column, filled by hibernate
    @Column(name = "GROUP_ID", insertable = false, updatable = false, nullable = false)
    private long groupId;

    // associate group, mapped by hibernate using GROUPS.GROUP_ID column
    @OneToOne
    @JoinColumn(name = "GROUP_ID")
    private Group group;

    // document ID column, filled by hibernate
    @Column(name = "DOCUMENT_ID", insertable = false, updatable = false, nullable = false)
    private long documentId;

    // associate document, mapped by hibernate using DOCUMENTS.DOCUMENT_ID column
    @OneToOne
    @JoinColumn(name = "DOCUMENT_ID")
    private Document document;

    /** 
     * associate tags, mapped bidirectional by hibernate using Tag.annotation property
     * notes:
     * - without the "mappedBy", hibernate would require an intermediate mapping table 
     * - with lazy loading, there are LazyInitializationExceptions; but as we require the tags anyway, using eager loading is ok 
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "annotation", fetch = FetchType.EAGER)
    private List<Tag> tags = new ArrayList<Tag>();

    // metadata ID column, filled by hibernate
    @Column(name = "METADATA_ID", insertable = false, updatable = false, nullable = false)
    private long metadataId;
    
    // associate metadata, mapped by hibernate using METADATA.ID column
    @OneToOne
    @JoinColumn(name = "METADATA_ID")
    private Metadata metadata;
    
    // -------------------------------------
    // constructor
    // -------------------------------------

    public Annotation() {
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public String getTargetSelectors() {
        return targetSelectors;
    }

    public void setTargetSelectors(String targetSelectors) {
        this.targetSelectors = targetSelectors;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(long documentId) {
        this.documentId = documentId;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public void setMetadataId(long metadataId) {
        this.metadataId = metadataId;
    }
    
    public long getMetadataId() {
        return metadataId;
    }
    
    public void setMetadata(Metadata meta) {
        this.metadata = meta;
    }
    
    public Metadata getMetadata() {
        return this.metadata;
    }
    public String getRootAnnotationId() {
        return rootAnnotationId;
    }

    public void setRootAnnotationId(String rootAnnotationId) {
        this.rootAnnotationId = rootAnnotationId;
    }

    public String getReferences() {
        return references;
    }

    public void setReferences(String references) {
        this.references = references;
    }

    public List<String> getReferencesList() {
        if (this.references == null || this.references.isEmpty()) {
            return null;
        } else {
            // split by comma
            List<String> result = Arrays.asList(this.references.split(","));
            return result;
        }
    }

    public void setReferences(List<String> references) {

        if (references == null || references.size() == 0) {
            this.references = null;
        } else {
            this.references = String.join(",", references);
        }
    }

    @Transient
    // help function stating whether the object denotes a reply to an annotation
    public boolean isReply() {
        return this.getReferences() != null && !this.getReferences().isEmpty();
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Override
    public int hashCode() {
        return Objects.hash(created, updated, id, documentId, groupId, userId, rootAnnotationId, shared, text, targetSelectors, references,
                document, group, tags, user);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Annotation other = (Annotation) obj;
        return Objects.equals(this.id, other.id) &&
                Objects.equals(this.created, other.created) &&
                Objects.equals(this.updated, other.updated) &&
                Objects.equals(this.documentId, other.documentId) &&
                Objects.equals(this.groupId, other.groupId) &&
                Objects.equals(this.userId, other.userId) &&
                Objects.equals(this.rootAnnotationId, other.rootAnnotationId) &&
                Objects.equals(this.references, other.references) &&
                Objects.equals(this.shared, other.shared) &&
                Objects.equals(this.text, other.text) &&
                Objects.equals(this.targetSelectors, other.targetSelectors) &&
                Objects.equals(this.document, other.document) &&
                Objects.equals(this.group, other.group) &&
                Objects.equals(this.user, other.user) &&
                Objects.equals(this.tags, other.tags);
    }

}
