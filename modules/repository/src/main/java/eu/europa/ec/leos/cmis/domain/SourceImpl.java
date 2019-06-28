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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static java.util.Arrays.copyOf;
import static org.apache.commons.io.IOUtils.toByteArray;

public class SourceImpl implements Content.Source {

    private final byte[] content;

    public SourceImpl(InputStream inputStream) {
        try {
            this.content = toByteArray(inputStream);
            inputStream.close();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the content of the cmis", e);
        }
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public byte[] getBytes() {
        return copyOf(content, content.length);
    }

    @Override
    public String toString() {
        return new String(content, Charset.forName("UTF-8"));
    }
}
