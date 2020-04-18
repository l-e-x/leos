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
package eu.europa.ec.leos.services.export;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LegPackage {
    private File file;
    private ExportResource exportResource;
    private List<String> milestoneComments;
    private List<String> containedFiles;

    public LegPackage() {
        milestoneComments = new ArrayList<>();
        containedFiles = new ArrayList<>();
    }

    public void addMilestoneComment (String milestoneComment){
        milestoneComments.add(milestoneComment);
    }

    public void addContainedFile (String containedFile) {
        containedFiles.add(containedFile);
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public ExportResource getExportResource() {
        return exportResource;
    }

    public void setExportResource(ExportResource exportResource) {
        this.exportResource = exportResource;
    }

    public List<String> getMilestoneComments() {
        return milestoneComments;
    }

    public List<String> getContainedFiles() {
        return containedFiles;
    }
}
