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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import eu.europa.ec.leos.annotate.Generated;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.web.helper.LocalDateTimeDeserializer;
import eu.europa.ec.leos.annotate.model.web.helper.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.util.Objects;

public class JsonAnnotationStatus {

    /**
     * class providing information about the status of an annotation: state, time of change, ...
     */
    
    private AnnotationStatus status;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updated;

    private String updated_by;

    private String user_info;
    
    @JsonProperty(value = "sent_deleted")
    private boolean sentDeleted;

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    
    @Generated
    public AnnotationStatus getStatus() {
        return status;
    }

    @Generated
    public void setStatus(final AnnotationStatus status) {
        this.status = status;
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
    public String getUpdated_by() {
        return updated_by;
    }

    @Generated
    public void setUpdated_by(final String updatedBy) {
        this.updated_by = updatedBy;
    }

    @Generated
    public String getUser_info() {
        return user_info;
    }

    @Generated
    public void setUser_info(final String userInfo) {
        this.user_info = userInfo;
    }
    
    @Generated
    public boolean isSentDeleted() {
        return sentDeleted;
    }
    
    @Generated
    public void setSentDeleted(final boolean sentDeletedNow) {
        this.sentDeleted = sentDeletedNow;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(status, updated, updated_by, user_info, sentDeleted);
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
        final JsonAnnotationStatus other = (JsonAnnotationStatus) obj;
        return Objects.equals(this.status, other.status) &&
                Objects.equals(this.updated, other.updated) &&
                Objects.equals(this.updated_by, other.updated_by) &&
                Objects.equals(this.user_info,  other.user_info) &&
                Objects.equals(this.sentDeleted, other.sentDeleted);
    }
}
