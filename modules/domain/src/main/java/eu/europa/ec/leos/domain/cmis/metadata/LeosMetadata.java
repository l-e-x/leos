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
package eu.europa.ec.leos.domain.cmis.metadata;

import eu.europa.ec.leos.domain.cmis.LeosCategory;

import java.util.Objects;

public abstract class LeosMetadata {

    private final LeosCategory category;
    protected final String stage;
    protected final String type;
    protected final String purpose;
    protected final String template;
    protected final String language;
    protected final String docTemplate;
    protected final String ref;
    protected final String objectId;

    protected LeosMetadata(LeosCategory category, String stage, String type, String purpose, String template,
                           String language, String docTemplate, String ref, String objectId) {
        this.category = category;
        this.stage = stage;
        this.type = type;
        this.purpose = purpose;
        this.template = template;
        this.language = language;
        this.docTemplate = docTemplate;
        this.ref = ref;
        this.objectId = objectId;
    }

    public String getStage() {
        return stage;
    }

    public String getType() {
        return type;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getTemplate() {
        return template;
    }

    public String getLanguage() {
        return language;
    }

    public String getDocTemplate() {
        return docTemplate;
    }

    public String getRef() {
        return ref;
    }

    public String getObjectId() {
        return objectId;
    }

    public final LeosCategory getCategory() {
        return this.category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeosMetadata that = (LeosMetadata) o;
        return category == that.category &&
                Objects.equals(stage, that.stage) &&
                Objects.equals(type, that.type) &&
                Objects.equals(purpose, that.purpose) &&
                Objects.equals(template, that.template) &&
                Objects.equals(language, that.language) &&
                Objects.equals(docTemplate, that.docTemplate) &&
                Objects.equals(ref, that.ref) &&
                Objects.equals(objectId, that.objectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, stage, type, purpose, template, language, docTemplate, ref, objectId);
    }

    @Override
    public String toString() {
        return "LeosMetadata{" +
                "category=" + category +
                ", stage='" + stage + '\'' +
                ", type='" + type + '\'' +
                ", purpose='" + purpose + '\'' +
                ", template='" + template + '\'' +
                ", language='" + language + '\'' +
                ", docTemplate='" + docTemplate + '\'' +
                ", ref='" + ref + '\'' +
                ", objectId='" + objectId + '\'' +
                '}';
    }

}
