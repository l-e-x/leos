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

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.europa.ec.leos.services.support.xml.freemarker.XmlNodeModelHandler;
import eu.europa.ec.leos.vo.toc.TableOfContentItemHtmlVO;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import freemarker.ext.dom.NodeModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.europa.ec.leos.model.rendition.RenderedDocument;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;


@Component
public class FreemarkerRenditionProcessor {

    @Value("${leos.freemarker.ftl.rendition}")
    private String renditionTemplate;
    
    @Value("${leos.freemarker.ftl.rendition_toc}")
    private String renditionTocTemplate;

    private final Configuration freemarkerConfiguration;

    public FreemarkerRenditionProcessor(Configuration freemarkerConfiguration) {
        this.freemarkerConfiguration = freemarkerConfiguration;
    }

    public String processTemplate(RenderedDocument document) {
        StringWriter outputWriter = new StringWriter();
        String result;
        try {
            Template template = freemarkerConfiguration.getTemplate(renditionTemplate);
            NodeModel nodeModel = XmlNodeModelHandler.parseXmlStream(document.getContent());
            Map root = new HashMap<String, Object>();
            root.put("xml_data", nodeModel);
            root.put("styleSheetName", document.getStyleSheetName());
            template.process(root, outputWriter);
            result = outputWriter.getBuffer().toString();
        } catch (MalformedTemplateNameException malformedTemplateNameException) {
            throw new RuntimeException("Invalid Template", malformedTemplateNameException);
        } catch (TemplateNotFoundException templateNotFoundException) {
            throw new RuntimeException("Tempalte not found", templateNotFoundException);
        } catch (TemplateException templateException) {
            throw new RuntimeException("Error occured while Template processing", templateException);
        } catch (Exception exception) {
            throw new RuntimeException(exception.getMessage());
        }
        return result;
    }
    
    public String processTocTemplate(RenderedDocument document, String tocList) {
        StringWriter outputWriter = new StringWriter();
        String result;
        try {
            Template template = freemarkerConfiguration.getTemplate(renditionTocTemplate);
            NodeModel nodeModel = XmlNodeModelHandler.parseXmlStream(document.getContent());
            
            Map root = new HashMap<String, Object>();
            root.put("xml_data", nodeModel);
            root.put("toc_data", tocList);
            root.put("styleSheetName", document.getStyleSheetName());
            template.process(root, outputWriter);
            result = outputWriter.getBuffer().toString();
        } catch (MalformedTemplateNameException malformedTemplateNameException) {
            throw new RuntimeException("Invalid Template", malformedTemplateNameException);
        } catch (TemplateNotFoundException templateNotFoundException) {
            throw new RuntimeException("Tempalte not found", templateNotFoundException);
        } catch (TemplateException templateException) {
            throw new RuntimeException("Error occured while Template processing", templateException);
        } catch (Exception exception) {
            throw new RuntimeException(exception.getMessage());
        }
        return result;
    }
}