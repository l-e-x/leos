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
package eu.europa.ec.leos.web.support.xml;

import java.io.InputStream;

import com.vaadin.server.StreamResource;

public class DownloadStreamResource extends StreamResource {

    private static final long serialVersionUID = 1778173662096645775L;

    public DownloadStreamResource(final String fileName, final InputStream xmlContent) {
        super(null, fileName);

        setFilename(fileName);

        setStreamSource(new StreamSource() {
            private static final long serialVersionUID = -1688164788711218131L;

            @Override
            public InputStream getStream() {
                return xmlContent;
            }
        });
    }
}
