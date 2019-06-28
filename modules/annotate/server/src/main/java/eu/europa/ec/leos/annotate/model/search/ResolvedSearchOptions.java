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
package eu.europa.ec.leos.annotate.model.search;

import eu.europa.ec.leos.annotate.Generated;
import eu.europa.ec.leos.annotate.model.SimpleMetadataWithStatuses;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.web.helper.JsonConverter;
import eu.europa.ec.leos.annotate.model.entity.Document;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Token;
import eu.europa.ec.leos.annotate.model.entity.User;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ResolvedSearchOptions {

    // -------------------------------------
    // private properties
    // -------------------------------------

    private Group group;
    private User executingUser, filterUser;
    private Document document;
    private Token executingUserToken;
    private List<SimpleMetadataWithStatuses> metadataWithStatusesList;

    // -------------------------------------
    // Constructor
    // -------------------------------------
    public ResolvedSearchOptions() {
        // default constructor
        initAndCheckMetadataWithStatusesList();
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------
    public Group getGroup() {
        return group;
    }

    public void setGroup(final Group group) {
        this.group = group;
    }

    public User getExecutingUser() {
        return executingUser;
    }

    public void setExecutingUser(final User executingUser) {
        this.executingUser = executingUser;
    }

    public User getFilterUser() {
        return filterUser;
    }

    public void setFilterUser(final User filterUser) {
        this.filterUser = filterUser;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(final Document document) {
        this.document = document;
    }

    public Token getExecutingUserToken() {
        return executingUserToken;
    }

    public void setExecutingUserToken(final Token token) {
        this.executingUserToken = token;
    }

    public List<SimpleMetadataWithStatuses> getMetadataWithStatusesList() {
        return metadataWithStatusesList;
    }

    public void setMetadataWithStatusesList(final String newMetaAsJson) {

        this.metadataWithStatusesList = JsonConverter.convertJsonToSimpleMetadataWithStatusesList(newMetaAsJson);
        initAndCheckMetadataWithStatusesList();
    }

    public void setMetadataWithStatusesList(final List<SimpleMetadataWithStatuses> meta) {
        this.metadataWithStatusesList = meta;
        initAndCheckMetadataWithStatusesList();
    }

    private void initAndCheckMetadataWithStatusesList() {
        if (CollectionUtils.isEmpty(this.metadataWithStatusesList)) {
            this.metadataWithStatusesList = new ArrayList<SimpleMetadataWithStatuses>();
            this.metadataWithStatusesList.add(new SimpleMetadataWithStatuses(null, null));
        }

        // handle special status cases
        for (final SimpleMetadataWithStatuses smws : this.metadataWithStatusesList) {
            if (CollectionUtils.isEmpty(smws.getStatuses())) {
                smws.setStatuses(AnnotationStatus.getDefaultStatus());
            } else if (smws.getStatuses().contains(AnnotationStatus.ALL)) {
                smws.setStatuses(AnnotationStatus.getAllValues());
            }
        }
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(group, executingUser, filterUser, document,
                executingUserToken, metadataWithStatusesList);
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
        final ResolvedSearchOptions other = (ResolvedSearchOptions) obj;
        return Objects.equals(this.group, other.group) &&
                Objects.equals(this.executingUser, other.executingUser) &&
                Objects.equals(this.filterUser, other.filterUser) &&
                Objects.equals(this.document, other.document) &&
                Objects.equals(this.executingUserToken, other.executingUserToken) &&
                Objects.equals(this.metadataWithStatusesList, other.metadataWithStatusesList);
    }
}
