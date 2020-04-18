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
package eu.europa.ec.leos.cmis.domain;

import eu.europa.ec.leos.domain.cmis.Content;

public class ContentImpl implements Content {

    private final String fileName;
    private final String mimeType;
    private final long length;
    private final Source source;

    public ContentImpl(String fileName, String mimeType, long length, Source source) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.length = length;
        this.source = source;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public Source getSource() {
        return source;
    }
}
