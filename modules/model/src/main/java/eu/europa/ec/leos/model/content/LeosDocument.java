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

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.commons.lang3.Validate;

public class LeosDocument extends LeosFile implements LeosDocumentProperties {

    public LeosDocument(@Nonnull final Document document) {
        super(document);
        Validate.isTrue(
                LeosTypeId.LEOS_DOCUMENT.valueEquals(cmisDocument.getType().getId()),
                "CMIS document is not a LEOS document! [objectId=%s, typeId=%s , leosId= %s]",
                cmisDocument.getId(),
                cmisDocument.getType().getId(),
                cmisDocument.getVersionSeriesId());
    }


    @Override
    public String getTitle() {
        return cmisDocument.getProperty(TITLE).getValueAsString();
    }

    /** for LeosDocuments Title property is to be used as Name*/
    @Override
    public String getName() {
        return getTitle();
    }

    @Override
    public String getTemplate() {
        return cmisDocument.getProperty(TEMPLATE).getValueAsString();
    }

    @Override
    public String getLanguage() {
        return cmisDocument.getProperty(LANGUAGE).getValueAsString();
    }
    
    @Override
    public Stage getStage() {
        return Stage.getStage(cmisDocument.getProperty(STAGE).getValueAsString());
    }

    @Override
    public OwnerSystem getOwnerSystem() {
        return OwnerSystem.getOwnerSystem(cmisDocument.getProperty(SYSTEM).getValueAsString());
    }
}
