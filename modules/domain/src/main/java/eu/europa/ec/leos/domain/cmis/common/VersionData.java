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
    private final VersionType versionType;
    private final boolean isLatestVersion;
    private final String cmisVersionLabel;

    public VersionData(String versionSeriesId, String cmisVersionLabel, String versionLabel, String versionComment, VersionType versionType, boolean isLatestVersion) {
        this.versionSeriesId = versionSeriesId;
        this.cmisVersionLabel = cmisVersionLabel;
        this.versionLabel = versionLabel;
        this.versionComment = versionComment;
        this.versionType = versionType;
        this.isLatestVersion = isLatestVersion;
    }

    @Override
    public String getVersionSeriesId() {
        return versionSeriesId;
    }
    
    @Override
    public String getCmisVersionLabel() {
        return cmisVersionLabel;
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
    public VersionType getVersionType() {
        return versionType;
    }

    @Override
    public boolean isLatestVersion() {
        return isLatestVersion;
    }

    @Override
    public String toString() {
        return "VersionData{" +
                "versionSeriesId='" + versionSeriesId + '\'' +
                ", cmisVersionLabel='" + cmisVersionLabel + '\'' +
                ", versionLabel='" + versionLabel + '\'' +
                ", versionComment='" + versionComment + '\'' +
                ", versionType=" + versionType +
                ", isLatestVersion=" + isLatestVersion +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionData that = (VersionData) o;
        return Objects.equals(versionType, that.versionType) &&
                isLatestVersion == that.isLatestVersion &&
                Objects.equals(versionSeriesId, that.versionSeriesId) &&
                Objects.equals(cmisVersionLabel, that.cmisVersionLabel) &&
                Objects.equals(versionLabel, that.versionLabel) &&
                Objects.equals(versionComment, that.versionComment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionSeriesId, cmisVersionLabel, versionLabel, versionComment, versionType, isLatestVersion);
    }
}
