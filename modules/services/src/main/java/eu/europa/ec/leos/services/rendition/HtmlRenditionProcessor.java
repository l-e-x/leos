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
package eu.europa.ec.leos.services.rendition;

import eu.europa.ec.leos.model.rendition.RenderedDocument;
import eu.europa.ec.leos.services.support.xml.freemarker.XmlNodeModelHandler;
import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Component
public class HtmlRenditionProcessor {

    @Value("${leos.freemarker.ftl.rendition}")
    private String renditionTemplate;
    
    @Value("${leos.freemarker.ftl.rendition_toc_file}")
    private String renditionJsTocTemplate;

    private final Configuration freemarkerConfiguration;

    public HtmlRenditionProcessor(Configuration freemarkerConfiguration) {
        this.freemarkerConfiguration = freemarkerConfiguration;
    }

    public String processTemplate(RenderedDocument document) {
        return processTocTemplate(document, null);
    }
    
    public String processTocTemplate(RenderedDocument document, String tocFile) {
        try{
            final Template template = getTemplate(renditionTemplate);
            final NodeModel nodeModel = XmlNodeModelHandler.parseXmlStream(document.getContent());
            final Map root = new HashMap<String, Object>();
            root.put("xml_data", nodeModel);
            root.put("toc_file", tocFile);
            root.put("styleSheetName", document.getStyleSheetName());
            
            return process(template, root);
        } catch (Exception exception) {
            throw new RuntimeException(exception.getMessage());
        }
    }
    
    public String processJsTemplate(String tocJson) {
        final Template template = getTemplate(renditionJsTocTemplate);
        final Map root = new HashMap<String, Object>();
        root.put("tocJson", tocJson);
    
        return process(template, root);
    }
    
    private String process(Template template, Map root) {
        try {
            StringWriter outputWriter = new StringWriter();
            template.process(root, outputWriter);
            return outputWriter.getBuffer().toString();
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while Template processing" + e);
        }
    }
    
    private Template getTemplate(String templateName) {
        try {
            return freemarkerConfiguration.getTemplate(templateName);
        } catch (MalformedTemplateNameException malformedTemplateNameException) {
            throw new RuntimeException("Invalid Template", malformedTemplateNameException);
        } catch (TemplateNotFoundException templateNotFoundException) {
            throw new RuntimeException("Template not found", templateNotFoundException);
        } catch (Exception exception) {
            throw new RuntimeException(exception.getMessage());
        }
    }
}