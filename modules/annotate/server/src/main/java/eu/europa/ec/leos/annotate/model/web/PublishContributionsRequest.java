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
package eu.europa.ec.leos.annotate.model.web;

import eu.europa.ec.leos.annotate.Generated;

import java.util.Objects;

/**
 * Class hosting the parameters given for a request to publish a contributor's annotations
 * within a group
 */
public class PublishContributionsRequest {

    // -----------------------------------------------------------
    // Fields
    // -----------------------------------------------------------
    private String docUri;
    private String group;
    private String userId;
    private String iscReference;

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------

    public PublishContributionsRequest() {
        // default constructor required for Mock MVC tests
    }
    
    public PublishContributionsRequest(final String docUri, final String group,
            final String userId, final String iscRef) {

        this.docUri = docUri;
        this.group = group;
        this.userId = userId;
        this.iscReference = iscRef;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------

    @Generated
    public String getDocUri() {
        return docUri;
    }

    @Generated
    public void setDocUri(final String uri) {
        this.docUri = uri;
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
    public String getUserid() {
        return userId;
    }

    @Generated
    public void setUserid(final String userId) {
        this.userId = userId;
    }

    @Generated
    public String getIscReference() {
        return iscReference;
    }

    @Generated
    public void setIscReference(final String iscReference) {
        this.iscReference = iscReference;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(docUri, group, userId, iscReference);
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
        final PublishContributionsRequest other = (PublishContributionsRequest) obj;
        return Objects.equals(this.docUri, other.docUri) &&
                Objects.equals(this.group, other.group) &&
                Objects.equals(this.userId, other.userId) &&
                Objects.equals(this.iscReference, other.iscReference);
    }
}
