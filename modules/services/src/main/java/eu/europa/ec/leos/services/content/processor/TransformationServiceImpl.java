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
package eu.europa.ec.leos.services.content.processor;

import com.google.common.base.Stopwatch;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.services.support.xml.freemarker.XmlNodeModelHandler;
import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateHashModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class TransformationServiceImpl implements TransformationService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformationServiceImpl.class);
    
    @Value("${leos.freemarker.ftl.documentView}")
    private String editableXHtmlTemplate;

    @Value("${leos.freemarker.ftl.fragmentXmlWrapper}")
    private String nonEditableFragmentTemplate;
    
    @Value("${leos.freemarker.ftl.import}")
    private String importXHtmlTemplate;

    private Configuration freemarkerConfiguration;

    private TemplateHashModel enumModels;

    @Autowired
    public TransformationServiceImpl(Configuration freemarkerConfiguration, TemplateHashModel enumModels){
        this.freemarkerConfiguration = freemarkerConfiguration;
        this.enumModels = enumModels;
    }
    
    @Override
    public String toEditableXml(final InputStream documentStream, String contextPath, LeosCategory category, List<LeosPermission> permissions) {
        String template;
        switch (category){
            case ANNEX:
                template = editableXHtmlTemplate;
                break;
            case MEMORANDUM:
                template = editableXHtmlTemplate;
                break;
            case BILL:
                template = editableXHtmlTemplate;
                break;
            default:
                throw new UnsupportedOperationException("No transformation supported for this category");
        }
        return transform(documentStream, template, contextPath, permissions);
    }

    @Override
    public String toXmlFragmentWrapper(InputStream documentStream, String contextPath, List<LeosPermission> permissions) {
        return transform(documentStream, nonEditableFragmentTemplate, contextPath, permissions);
    }
    
    @Override
    public String toImportXml(InputStream documentStream, String contextPath, List<LeosPermission> permissions) {
        return transform(documentStream, importXHtmlTemplate, contextPath, permissions);
    }
    
    @Override
    public String formatToHtml(XmlDocument versionDocument, String contextPath, List<LeosPermission> permissions) {
        LOG.debug("formatToHtml service invoked for version id:{})", versionDocument.getId());
        try (final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream
                (versionDocument.getContent().getOrError(() -> "Version document content is required!")
                        .getSource().getBytes())) {
            return transform(byteArrayInputStream, editableXHtmlTemplate, contextPath, permissions);
        } catch (Exception e) {
            throw new RuntimeException("Unable to format to HTML");
        }
    }
    
    @Override
    public String formatToHtml(InputStream documentStream, String contextPath, List<LeosPermission> permissions) {
        try {
            return transform(documentStream, editableXHtmlTemplate, contextPath, permissions);
        } catch (Exception e) {
            throw new RuntimeException("Unable to format to HTML");
        }
    }
    
    /**
     *  Transforms a documentStream using a freemarker template 
     * @param documentStream
     * @param templateName
     * @param contextPath
     * @param permissions list of actions (permissions) that a user can perform on the given document.
     * @return
     */
    private String transform(InputStream documentStream, String templateName, String contextPath, List<LeosPermission> permissions) {
        LOG.trace("Transforming document using {} template...", templateName);
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            StringWriter outputWriter = new StringWriter();
            Template template = freemarkerConfiguration.getTemplate(templateName);

            NodeModel nodeModel = XmlNodeModelHandler.parseXmlStream(documentStream);

            Map headers = new HashMap<String, Object>();
            headers.put("contextPath", contextPath);
            headers.put("userPermissions", (permissions != null) ? permissions : Collections.emptyList());
            headers.put("LeosPermission", enumModels.get(LeosPermission.class.getName()));

            Map root = new HashMap<String, Object>();
            root.put("xml_data", nodeModel);
            root.put("headers", headers);

            template.process(root, outputWriter);
            return outputWriter.getBuffer().toString();
        } catch (Exception ex) {
            LOG.error("Transformation error!", ex);
            throw new RuntimeException(ex);
        } finally {
            try {
                documentStream.close();
            } catch (IOException ioe){
                 //omitted
            }
            stopwatch.stop();
            LOG.trace("Transformation finished! ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }
}
