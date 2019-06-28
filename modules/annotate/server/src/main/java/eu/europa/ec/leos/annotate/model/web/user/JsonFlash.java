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
package eu.europa.ec.leos.annotate.model.web.user;

import eu.europa.ec.leos.annotate.Generated;

import java.util.List;
import java.util.Objects;

/**
 * Class representing hypothesis client features for the user - for message popups?; used during user profile retrieval
 * note: currently more or less a black box for us
 */
public class JsonFlash {

    private List<String> info;
    private List<String> warning;
    private List<String> success;
    private List<String> error;

    // -----------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------
    public JsonFlash() {
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    public List<String> getInfo() {
        return info;
    }

    public void setInfo(final List<String> info) {
        this.info = info;
    }

    public List<String> getWarning() {
        return warning;
    }

    public void setWarning(final List<String> warning) {
        this.warning = warning;
    }

    public List<String> getSuccess() {
        return success;
    }

    public void setSuccess(final List<String> success) {
        this.success = success;
    }

    public List<String> getError() {
        return error;
    }

    public void setError(final List<String> error) {
        this.error = error;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(info, warning, success, error);
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
        final JsonFlash other = (JsonFlash) obj;
        return Objects.equals(this.info, other.info) &&
                Objects.equals(this.warning, other.warning) &&
                Objects.equals(this.success, other.success) &&
                Objects.equals(this.error, other.error);
    }
}
