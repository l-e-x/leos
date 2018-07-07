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
package eu.europa.ec.leos.support.xml.freemarker;

import java.io.InputStream;
import java.io.StringReader;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import freemarker.ext.dom.NodeModel;

public class XmlNodeModelHandler {

    private static final Logger LOG = LoggerFactory.getLogger(XmlNodeModelHandler.class);

    public static @Nonnull NodeModel parseXmlStream(@Nonnull final InputStream inStream) throws Exception {
        LOG.trace("Parsing XML stream into FreeMarker node model...");
        return parseInputSource(new InputSource(inStream));
    }

    public static @Nonnull NodeModel parseXmlString(@Nonnull final String xml) throws Exception {
        LOG.trace("Parsing XML string into FreeMarker node model...");
        return parseInputSource(new InputSource(new StringReader(xml)));
    }

    private static @Nonnull NodeModel parseInputSource(@Nonnull final InputSource inputSource) throws Exception {
        try {
            return NodeModel.parse(inputSource);
        } catch (Exception ex) {
            LOG.error("Exception when parsing XML into FreeMarker node model!", ex);
            throw ex;
        }
    }
}
