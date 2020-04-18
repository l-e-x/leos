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

public final class AnnexMetadata extends LeosMetadata {
    private final int index;
    private final String number;
    private final String title;

    public AnnexMetadata(String stage, String type, String purpose, String template, String language, String docTemplate,
                         String ref, int index, String number, String title, String objectId, String docVersion) {
        super(LeosCategory.ANNEX, stage, type, purpose, template, language, docTemplate, ref, objectId, docVersion);
        this.index = index;
        this.number = number;
        this.title = title;
    }

    public int getIndex() {
        return index;
    }

    public String getNumber() {
        return number;
    }

    public String getTitle() {
        return title;
    }

    public final AnnexMetadata withPurpose(String purpose) {
        return new AnnexMetadata(stage, type, purpose, template, language, docTemplate, ref, index, number, title, objectId, docVersion);
    }

    public final AnnexMetadata withIndex(int index) {
        return new AnnexMetadata(stage, type, purpose, template, language, docTemplate, ref, index, number, title, objectId, docVersion);
    }

    public final AnnexMetadata withNumber(String number) {
        return new AnnexMetadata(stage, type, purpose, template, language, docTemplate, ref, index, number, title, objectId, docVersion);
    }

    public final AnnexMetadata withTitle(String title) {
        return new AnnexMetadata(stage, type, purpose, template, language, docTemplate, ref, index, number, title, objectId, docVersion);
    }

    public final AnnexMetadata withRef(String ref) {
        return new AnnexMetadata(stage, type, purpose, template, language, docTemplate, ref, index, number, title, objectId, docVersion);
    }

    public final AnnexMetadata withObjectId(String objectId) {
        return new AnnexMetadata(stage, type, purpose, template, language, docTemplate, ref, index, number, title, objectId, docVersion);
    }

    public final AnnexMetadata withType(String type) {
        return new AnnexMetadata(stage, type, purpose, template, language, docTemplate, ref, index, number, title, objectId, docVersion);
    }

    public final AnnexMetadata withTemplate(String template) {
        return new AnnexMetadata(stage, type, purpose, template, language, docTemplate, ref, index, number, title, objectId, docVersion);
    }
    
    public final AnnexMetadata withDocVersion(String docVersion) {
        return new AnnexMetadata(stage, type, purpose, template, language, docTemplate, ref, index, number, title, objectId, docVersion);
    }
    
    public final AnnexMetadata withDocTemplate(String docTemplate) {
        return new AnnexMetadata(stage, type, purpose, template, language, docTemplate, ref, index, number, title, objectId, docVersion);
    }

    @Override
    public String toString() {
        return "AnnexMetadata{" +
                "index=" + index +
                ", number='" + number + '\'' +
                ", title='" + title + '\'' +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnexMetadata that = (AnnexMetadata) o;
        return index == that.index &&
                Objects.equals(stage, that.stage) &&
                Objects.equals(type, that.type) &&
                Objects.equals(purpose, that.purpose) &&
                Objects.equals(template, that.template) &&
                Objects.equals(language, that.language) &&
                Objects.equals(docTemplate, that.docTemplate) &&
                Objects.equals(ref, that.ref) &&
                Objects.equals(number, that.number) &&
                Objects.equals(title, that.title) &&
                Objects.equals(objectId, that.objectId) &&
                Objects.equals(docVersion, that.docVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stage, type, purpose, template, language, docTemplate, ref, index, number, title, objectId, docVersion);
    }
}
