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
package eu.europa.ec.leos.services.export;

import eu.europa.ec.leos.integration.toolbox.ExportResource;
import eu.europa.ec.leos.integration.toolbox.jaxb.beans.ImportOptions;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Component
class ExportHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ExportServiceImpl.class);

    private final Configuration freemarkerConfiguration;

    @Value("${leos.freemarker.ftl.export.pdf}")
    private String exportTemplatePdf;

    @Value("${leos.freemarker.ftl.export.legiswrite}")
    private String exportTemplateLegisWrite;

    @Autowired
    public ExportHelper (Configuration freemarkerConfiguration) {
        this.freemarkerConfiguration = freemarkerConfiguration;
    }

    public ByteArrayOutputStream createContentFile(ExportOptions exportOptions, ExportResource exportRootNode) throws Exception {
        Validate.notNull(exportOptions);
        Validate.notNull(exportRootNode);

        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

        LOG.trace("Creating content file document...");
        try {
            StringWriter outputWriter = new StringWriter();
            Template template;
            switch (exportOptions) {
                case TO_PDF:
                    template = freemarkerConfiguration.getTemplate(exportTemplatePdf);
                    break;
                case TO_LEGISWRITE:
                    template = freemarkerConfiguration.getTemplate(exportTemplateLegisWrite);
                    break;
                default:
                    throw new Exception("Export Options not valid");
            }
 
            Map root = new HashMap<String, ExportResource>();
            root.put("resource_tree", exportRootNode);

            template.process(root, outputWriter);
            String result = outputWriter.getBuffer().toString();

            if (validateXml(result)) {
                byteOutputStream.write(result.getBytes());
            }
            else {
                throw new RuntimeException("Content XML is not valid!");
            }
        } catch (TemplateException| IOException| JAXBException ex) {
            LOG.error("Error while creating content xml file {}", ex.getMessage());
            throw ex;
        }
        return byteOutputStream;
    }

    private boolean validateXml(String xmlContent) {
        //Checks that the transformation result is a valid content
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance( ImportOptions.class );
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    
            StringReader reader = new StringReader(xmlContent);
            ImportOptions importOptions = (ImportOptions) jaxbUnmarshaller.unmarshal(reader);
            return true;
        } catch (JAXBException ex) {
            return false;
        }
    }
}
