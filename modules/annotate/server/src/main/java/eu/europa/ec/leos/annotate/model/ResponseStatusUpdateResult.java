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
package eu.europa.ec.leos.annotate.model;

import eu.europa.ec.leos.annotate.Generated;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * POJO representing the result of updating the response status of annotations
 */
public class ResponseStatusUpdateResult {

    // -------------------------------------
    // private properties
    // -------------------------------------

    private final List<String> updatedAnnotIds;
    private final List<String> deletedAnnotIds;

    // -------------------------------------
    // Constructors
    // -------------------------------------

    public ResponseStatusUpdateResult() {

        // default constructor

        this.updatedAnnotIds = new ArrayList<String>();
        this.deletedAnnotIds = new ArrayList<String>();
    }

    public ResponseStatusUpdateResult(final List<String> updIds, final List<String> delIds) {
        this.updatedAnnotIds = (updIds == null ? new ArrayList<String>() : updIds);
        this.deletedAnnotIds = (delIds == null ? new ArrayList<String>() : delIds);
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------

    @Generated
    public List<String> getUpdatedAnnotIds() {
        return updatedAnnotIds;
    }

    @Generated
    public List<String> getDeletedAnnotIds() {
        return deletedAnnotIds;
    }
    
 // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(updatedAnnotIds);
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
        final ResponseStatusUpdateResult other = (ResponseStatusUpdateResult) obj;
        return Objects.equals(this.updatedAnnotIds, other.updatedAnnotIds) &&
                Objects.equals(this.deletedAnnotIds, other.deletedAnnotIds);
    }
}
