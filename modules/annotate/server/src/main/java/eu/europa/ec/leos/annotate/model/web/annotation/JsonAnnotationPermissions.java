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

import eu.europa.ec.leos.annotate.Generated;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JsonAnnotationPermissions {

    /**
     * Class representing the structure received by the hypothesis client for permissions on an annotation 
     */

    private List<String> read, admin, update, delete;

    // -------------------------------------
    // Constructors
    // -------------------------------------
    public JsonAnnotationPermissions() {
        // default constructor
    }
    
    public JsonAnnotationPermissions(final JsonAnnotationPermissions orig) {
        if(orig.read != null) {
            this.read = new ArrayList<String>();
            orig.read.forEach(entry -> this.read.add(entry));
        }
        if(orig.admin != null) {
            this.admin = new ArrayList<String>();
            orig.admin.forEach(entry -> this.admin.add(entry));
        }
        if(orig.update != null) {
            this.update = new ArrayList<String>();
            orig.update.forEach(entry -> this.update.add(entry));
        }
        if(orig.delete != null) {
            this.delete = new ArrayList<String>();
            orig.delete.forEach(entry -> this.delete.add(entry));
        }
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------

    @Generated
    public List<String> getAdmin() {
        return admin;
    }

    @Generated
    public void setAdmin(final List<String> admin) {
        this.admin = admin;
    }

    @Generated
    public List<String> getRead() {
        return read;
    }

    @Generated
    public void setRead(final List<String> read) {
        this.read = read;
    }

    @Generated
    public List<String> getDelete() {
        return delete;
    }

    @Generated
    public void setDelete(final List<String> delete) {
        this.delete = delete;
    }

    @Generated
    public List<String> getUpdate() {
        return update;
    }

    @Generated
    public void setUpdate(final List<String> update) {
        this.update = update;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(read, admin, update, delete);
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
        final JsonAnnotationPermissions other = (JsonAnnotationPermissions) obj;
        return Objects.equals(this.read, other.read) &&
                Objects.equals(this.admin, other.admin) &&
                Objects.equals(this.update, other.update) &&
                Objects.equals(this.delete, other.delete);
    }
}
