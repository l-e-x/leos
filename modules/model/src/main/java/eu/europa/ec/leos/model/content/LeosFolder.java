/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.model.content;

import javax.annotation.Nonnull;

import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.commons.lang3.Validate;

public class LeosFolder extends LeosObject implements LeosFolderProperties {

    private Folder cmisFolder;

    public LeosFolder(@Nonnull final Folder folder) {
        super(folder);
        cmisFolder = folder;

        Validate.isTrue(
                LeosTypeId.LEOS_FOLDER.valueEquals(cmisFolder.getType().getId()),
                "CMIS folder is not a LEOS folder! [id=%s, typeId=%s]",
                cmisFolder.getId(),
                cmisFolder.getType().getId());
    }

    @Override
    public String getPath() {
        return cmisFolder.getPath();
    }
}
