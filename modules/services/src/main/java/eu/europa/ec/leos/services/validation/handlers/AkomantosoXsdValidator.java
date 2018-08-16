/*
 * Copyright 2018 European Commission
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
package eu.europa.ec.leos.services.validation.handlers;

import com.google.common.base.Stopwatch;
import eu.europa.ec.leos.domain.common.ErrorCode;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.ErrorVO;
import eu.europa.ec.leos.services.validation.handlers.util.LSInputImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.annotation.PostConstruct;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class AkomantosoXsdValidator implements Validator {
    private static final Logger LOG = LoggerFactory.getLogger(AkomantosoXsdValidator.class);

    @Value("${leos.schema.location:eu/europa/ec/leos/xsd}")
    private String SCHEMA_PATH;

    @Value("${leos.schema.akomantoso.name:akomantoso30.xsd}")
    private String SCHEMA_NAME;

    private Schema schema;

    @PostConstruct
    public void initXSD() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Resource resource = new ClassPathResource(SCHEMA_PATH + "/" + SCHEMA_NAME);
            factory.setResourceResolver(new LSResourceResolver() {
                public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
                    InputStream resourceAsStream = null;
                    try {
                        resourceAsStream = new ClassPathResource(SCHEMA_PATH + "/" + systemId).getInputStream();
                    } catch (IOException e) {
                        LOG.error("Failed while loading referenced resource {}", systemId, e);
                    }
                    LOG.trace("Loaded referenced resource {}", systemId);
                    return new LSInputImpl(publicId, systemId, resourceAsStream);
                }
            });

            schema = factory.newSchema(new StreamSource(resource.getInputStream()));
        } finally {
            LOG.trace("XSD loaded in {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public void validate(DocumentVO documentVO, final List<ErrorVO> result) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            String key = documentVO.getId() != null ? documentVO.getId() : documentVO.getDocumentType().toString();
            javax.xml.validation.Validator validator = schema.newValidator();
            validator.setErrorHandler(new XsdErrorHandler(result, key));
            StreamSource source = new StreamSource(new ByteArrayInputStream(documentVO.getSource()));
            validator.validate(source);
        } catch (SAXException e) {
            //SAX fatal exceptions are already added to result. 
        } catch (Exception e) {
            LOG.error("Exception occurred", e);
            result.add(new ErrorVO(ErrorCode.EXCEPTION, documentVO.getId(), e.getMessage()));
        } finally {
            LOG.debug("xml validated with xsd in {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    public class XsdErrorHandler implements ErrorHandler {
        private List<ErrorVO> result;
        private String key;

        XsdErrorHandler(List<ErrorVO> result, String key) {
            this.result = result;
            this.key = key;
        }

        @Override
        public void warning(SAXParseException exception) {
            handleMessage("Warning", exception);
        }

        @Override
        public void error(SAXParseException exception) {
            handleMessage("Error", exception);
        }

        @Override
        public void fatalError(SAXParseException exception) {
            handleMessage("Fatal", exception);
            //Validation will terminate with first Fatal error..for example if syntax is wrong 
        }

        private void handleMessage(String level, SAXParseException exception) {
            int lineNumber = exception.getLineNumber();
            int columnNumber = exception.getColumnNumber();
            String message = exception.getMessage();
            result.add(new ErrorVO(ErrorCode.DOCUMENT_XSD_VALIDATION_FAILED, key, String.format("[%s] line nr: %s column nr: %s message: %s", level, lineNumber, columnNumber, message)));
        }
    }
}
