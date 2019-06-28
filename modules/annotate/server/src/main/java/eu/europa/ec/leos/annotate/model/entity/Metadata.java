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
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import javax.persistence.*;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "METADATA", indexes = {
        @Index(columnList = "SYSTEM_ID", name = "METADATA_IX_SYSTEM_ID"),
        @Index(columnList = "RESPONSE_STATUS", name = "METADATA_IX_RESPONSE_STATUS")})
public class Metadata {

    /**
     * Class representing a set of metadata logically assigned to a group and a document 
     */

    private static final Logger LOG = LoggerFactory.getLogger(Metadata.class);

    // -----------------------------------------------------------
    // Constants for known metadata properties
    // -----------------------------------------------------------
    private static final String PROP_SYSTEM_ID = "systemId";
    private static final String PROP_RESPONSE_STATUS = "responseStatus";
    private static final String PROP_ISC_REF = "ISCReference";
    private static final List<String> PROPS_OWN_COLS = Arrays.asList(PROP_SYSTEM_ID, PROP_RESPONSE_STATUS);

    // -------------------------------------
    // column definitions
    // -------------------------------------

    @Id
    @Column(name = "ID", nullable = false)
    @GenericGenerator(name = "metadataSequenceGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
            @Parameter(name = "sequence_name", value = "METADATA_SEQ"),
            @Parameter(name = "increment_size", value = "1")
    })
    @GeneratedValue(generator = "metadataSequenceGenerator")
    @SuppressWarnings("PMD.ShortVariable")
    private long id;

    // document ID column, filled by hibernate
    @Column(name = "DOCUMENT_ID", insertable = false, updatable = false, nullable = false)
    private long documentId;

    // associate user, mapped by hibernate using DOCUMENTS.DOCUMENT_ID column
    @OneToOne
    @JoinColumn(name = "DOCUMENT_ID")
    private Document document;

    // group ID column, filled by hibernate
    @Column(name = "GROUP_ID", insertable = false, updatable = false, nullable = false)
    private long groupId;

    // associate user, mapped by hibernate using GROUPS.GROUP_ID column
    @OneToOne
    @JoinColumn(name = "GROUP_ID")
    private Group group;

    // systemId calling
    @Column(name = "SYSTEM_ID", nullable = false)
    private String systemId;

    // response status enum
    @Column(name = "RESPONSE_STATUS")
    @Enumerated(EnumType.ORDINAL)
    private ResponseStatus responseStatus;

    // line-separated list of key-value pairs (metadata name, metadata value)
    @Column(name = "KEYVALUES")
    private String keyValuePairs;

    // track the datetime of modification of the response status
    // note: NOT auto-filled by DB trigger as this would require different implementations for Oracle and H2
    @Column(name = "RESPONSE_STATUS_UPDATED", nullable = true)
    private LocalDateTime responseStatusUpdated;

    // track who modified the responsestatus
    // (user id, but we don't create a foreign key as it might slow down things unintentionally)
    @Column(name = "RESPONSE_STATUS_UPDATED_BY", nullable = true)
    private Long responseStatusUpdatedBy;

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------
    public Metadata() {
        // parameterless constructor required by JPA
    }

    public Metadata(final Document document, final Group group, final String systemId) {
        this.document = document;
        this.group = group;
        this.systemId = systemId;
    }

    public Metadata(final Document document, final Group group, final String systemId, final SimpleMetadata otherProps) {
        this.document = document;
        this.group = group;
        this.systemId = systemId;
        this.setKeyValuePropertyFromSimpleMetadata(otherProps);
    }

    public Metadata(final Metadata other) {
        
        // copy constructor
        this.document = other.document;
        this.group = other.group;
        this.systemId = other.systemId;
        this.keyValuePairs = other.keyValuePairs;
        this.responseStatus = other.responseStatus;
        this.responseStatusUpdated = other.responseStatusUpdated;
        this.responseStatusUpdatedBy = other.responseStatusUpdatedBy;
    }
    
    // -----------------------------------------------------------
    // Enums
    // -----------------------------------------------------------
    public enum ResponseStatus {
        UNKNOWN(0), IN_PREPARATION(1), SENT(2);

        private int enumValue;

        ResponseStatus(final int value) {
            this.enumValue = value;
        }

        public int getEnumValue() {
            return enumValue;
        }

        public void setEnumValue(final int enumValue) {
            if (enumValue >= 0 && enumValue <= 2) {
                this.enumValue = enumValue;
            }
        }
    }

    // -----------------------------------------------------------
    // Useful getters & setters (exceeding plain POJOs)
    // -----------------------------------------------------------
    public void setKeyValuePropertyFromSimpleMetadata(final SimpleMetadata hashMap) {

        if (hashMap == null) {
            return;
        }

        String foundValue = hashMap.getOrDefault(PROP_SYSTEM_ID, null);
        if (!StringUtils.isEmpty(foundValue)) {
            this.systemId = foundValue;
        }

        foundValue = hashMap.getOrDefault(PROP_RESPONSE_STATUS, null);
        if (!StringUtils.isEmpty(foundValue)) {
            try {
                this.responseStatus = ResponseStatus.valueOf(foundValue);
            } catch (IllegalArgumentException e) {
                LOG.error("Found invalid value for response status in hashMap", e);
            }
        }

        final StringBuilder dbKeyValueList = new StringBuilder();
        hashMap.forEach((key, value) -> {
            if (!PROPS_OWN_COLS.contains(key)) {
                dbKeyValueList.append(key).append(':').append(value).append('\n');
            }
        });
        this.keyValuePairs = dbKeyValueList.toString();
    }

    /**
     * returns a {@link SimpleMetadata} (={@link Map}) containing all items stored in the key-values column
     * note: does not return the system ID or response status! 
     */
    @Nonnull
    public SimpleMetadata getKeyValuePropertyAsSimpleMetadata() {

        final SimpleMetadata result = new SimpleMetadata();

        if (!StringUtils.isEmpty(this.keyValuePairs)) {
            final List<String> parts = Arrays.asList(this.keyValuePairs.split("\n"));
            for (final String part : parts) {
                // each part has the format: key:"value"
                if (part.contains(":")) {
                    final String key = part.substring(0, part.indexOf(':'));
                    final String value = part.substring(part.indexOf(':') + 1);
                    result.put(key, value.replace("\r", ""));
                }
            }
        }

        return result;
    }

    /**
     * returns a {@link SimpleMetadata} (={@link Map}) containing all metadata items, 
     * including the system ID and response status! 
     */
    public SimpleMetadata getAllMetadataAsSimpleMetadata() {

        // retrieve key-value pairs
        final SimpleMetadata result = getKeyValuePropertyAsSimpleMetadata();

        // add response status, if available
        if (this.responseStatus != null) {
            result.put(PROP_RESPONSE_STATUS, this.responseStatus.toString());
        }

        // add system id
        result.put(PROP_SYSTEM_ID, this.systemId);

        return result;
    }

    // extract the ISCReference
    public String getIscReference() {

        final SimpleMetadata props = getKeyValuePropertyAsSimpleMetadata();
        if (props.isEmpty()) {
            return "";
        }

        return props.get(PROP_ISC_REF);
    }

    // check if the metadata was "SENT" already
    public boolean isResponseStatusSent() {

        return this.responseStatus == ResponseStatus.SENT;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    public long getId() {
        return id;
    }

    public void setId(final long newId) {
        this.id = newId;
    }

    public long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(final long documentId) {
        this.documentId = documentId;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(final Document document) {
        this.document = document;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(final long groupId) {
        this.groupId = groupId;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(final Group group) {
        this.group = group;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(final String systemId) {
        this.systemId = systemId;
    }

    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(final ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public LocalDateTime getResponseStatusUpdated() {
        return responseStatusUpdated;
    }

    public void setResponseStatusUpdated(final LocalDateTime upd) {
        this.responseStatusUpdated = upd;
    }

    public Long getResponseStatusUpdatedBy() {
        return responseStatusUpdatedBy;
    }

    public void setResponseStatusUpdatedBy(final Long userId) {
        this.responseStatusUpdatedBy = userId;
    }

    public String getKeyValuePairs() {
        return keyValuePairs;
    }

    public void setKeyValuePairs(final String keyValuePairs) {
        this.keyValuePairs = keyValuePairs;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(id, documentId, document, groupId, group, systemId,
                responseStatus, responseStatusUpdated, responseStatusUpdatedBy, keyValuePairs);
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
        final Metadata other = (Metadata) obj;
        return Objects.equals(this.id, other.id) &&
                Objects.equals(this.documentId, other.documentId) &&
                Objects.equals(this.document, other.document) &&
                Objects.equals(this.groupId, other.groupId) &&
                Objects.equals(this.group, other.group) &&
                Objects.equals(this.systemId, other.systemId) &&
                Objects.equals(this.responseStatus, other.responseStatus) &&
                Objects.equals(this.responseStatusUpdated, other.responseStatusUpdated) &&
                Objects.equals(this.responseStatusUpdatedBy, other.responseStatusUpdatedBy) &&
                Objects.equals(this.keyValuePairs, other.keyValuePairs);
    }
}
