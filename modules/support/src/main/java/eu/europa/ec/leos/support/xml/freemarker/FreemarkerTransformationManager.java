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
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Stopwatch;

import eu.europa.ec.leos.support.xml.TransformationManager;
import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
import freemarker.template.Template;

@Component
public class FreemarkerTransformationManager implements TransformationManager {

    private static final Logger LOG = LoggerFactory.getLogger(FreemarkerTransformationManager.class);

    @Autowired
    private Configuration freemarkerConfiguration;

    @Value("${leos.freemarker.ftl.documentView}")
    private String editableXHtmlTemplate;

    @Value("${leos.freemarker.ftl.htmlPreview}")
    private String readOnlyHtmlTemplate;
    
    @Value("${leos.freemarker.ftl.feedbackView}")
    private String nonEditableXHtmlTemplate;

    @Value("${leos.freemarker.ftl.fragmentXmlWrapper}")
    private String nonEditableFragmentTemplate;
    
    /** This method will return XML along with wrappers to invoke CKEditor */
    @Override
    public String toEditableXml(final InputStream documentStream, String contextPath ){
        return transform(documentStream, editableXHtmlTemplate, contextPath);
    }

    /** This method will return XML along with CSS to display the content of the document on the browser */
    @Override
    public String toHtmlForPreview(final InputStream documentStream,String contextPath) {
        return transform(documentStream, readOnlyHtmlTemplate, contextPath);
    }

    @Override
    public String toNonEditableXml(final InputStream documentStream,String contextPath) {
        return transform(documentStream, nonEditableXHtmlTemplate, contextPath);
    }
    
    @Override
    public String toXmlFragmentWrapper(InputStream documentStream, String contextPath) {
        return transform(documentStream, nonEditableFragmentTemplate, contextPath);
    }
    
    private String transform(InputStream documentStream, String templateName ,String  contextPath) {
        LOG.trace("Transforming document using {} template...", templateName);
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            StringWriter outputWriter = new StringWriter();
            Template template = freemarkerConfiguration.getTemplate(templateName);
            
            NodeModel nodeModel = XmlNodeModelHandler.parseXmlStream(documentStream);

            Map headers =new HashMap<String, String>();
            headers.put("contextPath", contextPath);

            Map root = new HashMap<String, Object>();
            root.put("xml_data", nodeModel);
            root.put("headers", headers);
            
            template.process(root, outputWriter);
            return outputWriter.getBuffer().toString();
        } catch (Exception ex) {
            LOG.error("Transformation error!", ex);
            throw new RuntimeException(ex);
        } finally {
            stopwatch.stop();
            LOG.trace("Transformation finished! ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }
}
