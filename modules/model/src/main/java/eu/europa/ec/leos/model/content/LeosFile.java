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

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;


public class LeosFile extends LeosObject implements LeosFileProperties {

    protected final Document cmisDocument;

    public LeosFile(@Nonnull final Document leosFile) {
        super(leosFile);
        cmisDocument = leosFile;

        Validate.isTrue(
                LeosTypeId.LEOS_FILE.checkIfType(cmisDocument.getType()),
                "CMIS document is not a LEOS File! [objectId=%s, typeId=%s , leosId= %s]",
                cmisDocument.getId(),
                cmisDocument.getType().getId(),
                cmisDocument.getVersionSeriesId());
    }
    
    /** this method overrides the getLeosId as for versioned files. Object id changes for the CMIS implementation and 
     * LEOS application needs a invariable id. 
     */
    @Override
    public String getLeosId() {
        return cmisDocument.getVersionSeriesId();
    }

    public @Nullable
    String getContentString() {
        try (final InputStream inputStream = getContentStream()) {
            return (inputStream != null) ?
                    IOUtils.toString(inputStream, "UTF-8") :
                        null;
        } catch (Exception e) {
            throw new RuntimeException("Unable to read file content!", e);
        }
    }

    public @Nullable
    InputStream getContentStream() {
        final ContentStream contentStream = cmisDocument.getContentStream();
        return (contentStream != null) ?
                new AutoCloseInputStream(contentStream.getStream()) :
                    null;
    }

    @Override
    public String getVersionId() {
        return cmisDocument.getId();
    }

    @Override
    public String getVersionLabel() {
        return  cmisDocument.getVersionLabel();
    }

    @Override
    public String getVersionComment() {
        return cmisDocument.getCheckinComment();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (!(o instanceof LeosFile))
            return false;

        LeosFile leosFile = (LeosFile) o;
        if (!getLeosId().equals(leosFile.getLeosId()))
            return false;
        if (getVersionLabel() != null ? !getVersionLabel().equals(leosFile.getVersionLabel()) : leosFile.getVersionLabel() != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return getLeosId().hashCode()
                + (getVersionLabel() == null ? getVersionLabel().hashCode() : 0);
    }
}
