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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.persistence.*;

import java.util.*;

@Entity
@Table(name = "METADATA", indexes = {
        @Index(columnList = "SYSTEM_ID", name = "METADATA_IX_SYSTEM_ID"),
        @Index(columnList = "RESPONSE_STATUS", name = "METADATA_IX_RESPONSE_STATUS")}, uniqueConstraints = {
                @UniqueConstraint(columnNames = {"DOCUMENT_ID", "GROUP_ID", "SYSTEM_ID"})
        })
public class Metadata {

    private static final Logger LOG = LoggerFactory.getLogger(Metadata.class);

    /**
     * Class representing a set of metadata logically assigned to a group and a document 
     */
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

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------
    public Metadata() {
        // parameterless constructor required by JPA
    }

    public Metadata(Document document, Group group, String systemId) {
        this.document = document;
        this.group = group;
        this.systemId = systemId;
    }

    // -----------------------------------------------------------
    // Enums
    // -----------------------------------------------------------
    public enum ResponseStatus {
        UNKNOWN(0), IN_PREPARATION(1), SENT(2);

        private int enumValue;

        ResponseStatus(int value) {
            this.enumValue = value;
        }

        public int getEnumValue() {
            return enumValue;
        }

        public void setEnumValue(int enumValue) {
            this.enumValue = enumValue;
        }
    }

    // -----------------------------------------------------------
    // Constants for known metadata properties
    // -----------------------------------------------------------
    private static final String PROP_SYSTEM_ID = "systemId";
    private static final String PROP_RESPONSE_STATUS = "responseStatus";
    private static List<String> KNOWN_PROPERTIES = Arrays.asList(PROP_SYSTEM_ID, PROP_RESPONSE_STATUS);

    // -----------------------------------------------------------
    // Useful getters & setters (exceeding plain POJOs)
    // -----------------------------------------------------------
    public void setKeyValuePropertyFromHashMap(HashMap<String, String> hashMap) {

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

        StringBuilder dbKeyValueList = new StringBuilder();
        hashMap.forEach((k, v) -> {
            if (!KNOWN_PROPERTIES.contains(k)) {
                dbKeyValueList.append(k).append(":").append(v).append("\n");
            }
        });
        this.keyValuePairs = dbKeyValueList.toString();
    }

    public HashMap<String, String> getKeyValuePropertyAsHashMap() {

        HashMap<String, String> result = new LinkedHashMap<String, String>();

        if (this.responseStatus != null) {
            result.put(PROP_RESPONSE_STATUS, this.responseStatus.toString());
        }

        result.put(PROP_SYSTEM_ID, this.systemId);

        if (!StringUtils.isEmpty(this.keyValuePairs)) {
            List<String> parts = Arrays.asList(this.keyValuePairs.split("\n"));
            for (String part : parts) {
                // each part has the format: key:"value"
                if (part.contains(":")) {
                    String key = part.substring(0, part.indexOf(":"));
                    String value = part.substring(part.indexOf(":") + 1);
                    result.put(key, value);
                }
            }
        }

        return result;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getKeyValuePairs() {
        return keyValuePairs;
    }

    public void setKeyValuePairs(String keyValuePairs) {
        this.keyValuePairs = keyValuePairs;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Override
    public int hashCode() {
        return Objects.hash(id, documentId, document, groupId, group, systemId, responseStatus, keyValuePairs);
    }

    @Override
    public boolean equals(Object obj) {

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
                Objects.equals(this.keyValuePairs, other.keyValuePairs);
    }
}
