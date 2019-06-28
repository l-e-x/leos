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
package eu.europa.ec.leos.domain.cmis.common;

import java.util.Objects;

public class VersionData implements Versionable {

    private final String versionSeriesId;
    private final String versionLabel;
    private final String versionComment;
    private final boolean isMajorVersion;
    private final boolean isLatestVersion;

    public VersionData(String versionSeriesId, String versionLabel, String versionComment, boolean isMajorVersion, boolean isLatestVersion) {
        this.versionSeriesId = versionSeriesId;
        this.versionLabel = versionLabel;
        this.versionComment = versionComment;
        this.isMajorVersion = isMajorVersion;
        this.isLatestVersion = isLatestVersion;
    }

    @Override
    public String getVersionSeriesId() {
        return versionSeriesId;
    }

    @Override
    public String getVersionLabel() {
        return versionLabel;
    }

    @Override
    public String getVersionComment() {
        return versionComment;
    }

    @Override
    public boolean isMajorVersion() {
        return isMajorVersion;
    }

    @Override
    public boolean isLatestVersion() {
        return isLatestVersion;
    }

    @Override
    public String toString() {
        return "VersionData{" +
                "versionSeriesId='" + versionSeriesId + '\'' +
                ", versionLabel='" + versionLabel + '\'' +
                ", versionComment='" + versionComment + '\'' +
                ", isMajorVersion=" + isMajorVersion +
                ", isLatestVersion=" + isLatestVersion +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionData that = (VersionData) o;
        return isMajorVersion == that.isMajorVersion &&
                isLatestVersion == that.isLatestVersion &&
                Objects.equals(versionSeriesId, that.versionSeriesId) &&
                Objects.equals(versionLabel, that.versionLabel) &&
                Objects.equals(versionComment, that.versionComment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionSeriesId, versionLabel, versionComment, isMajorVersion, isLatestVersion);
    }
}
