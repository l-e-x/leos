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
package eu.europa.ec.leos.ui.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MilestonesVO {
    private final String title;
    private final String legDocumentName;
    private final String createdDate;
    private Date updatedDate;
    private String status;

    private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public MilestonesVO(List<String> titles, Date createdDate, Date updatedDate, String status, String legDocumentName) {
        this.title = String.join(",", titles);
        this.updatedDate = updatedDate;
        this.createdDate = dateFormat.format(createdDate);
        this.status = status;
        this.legDocumentName = legDocumentName;
    }

    public String getTitle() {
        return title;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getLegDocumentName() {
        return legDocumentName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MilestonesVO that = (MilestonesVO) o;
        return Objects.equals(title, that.title) &&
                Objects.equals(status, that.status) &&
                Objects.equals(createdDate, that.createdDate) &&
                Objects.equals(updatedDate, that.updatedDate) &&
                Objects.equals(legDocumentName, that.legDocumentName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, status, createdDate, updatedDate, legDocumentName);
    }

    @Override public String toString() {
        return "MilestonesVO{" +
                "title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", createdDate='" + createdDate + '\'' +
                ", updatedDate=" + updatedDate +
                ", legDocumentName='" + legDocumentName + '\'' +
                '}';
    }
}
