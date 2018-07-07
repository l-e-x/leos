/*
 * Copyright 2017 European Commission
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
import eu.europa.ec.leos.domain.document.Content;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument;
import eu.europa.ec.leos.services.support.xml.freemarker.XmlNodeModelHandler;
import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class TransformationServiceImpl implements TransformationService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformationServiceImpl.class);
    
    @Value("${leos.freemarker.ftl.documentView}")
    private String editableBillXHtmlTemplate;

    @Value("${leos.freemarker.ftl.annexView}")
    private String editableAnnexXHtmlTemplate;

    @Value("${leos.freemarker.ftl.memorandumView}")
    private String editableMemorandumXHtmlTemplate;

    @Value("${leos.freemarker.ftl.readOnly}")
    private String readOnlyHtmlTemplate;

    @Value("${leos.freemarker.ftl.fragmentXmlWrapper}")
    private String nonEditableFragmentTemplate;
    
    @Value("${leos.freemarker.ftl.import}")
    private String importXHtmlTemplate;

    private Configuration freemarkerConfiguration;

    @Autowired
    public TransformationServiceImpl(Configuration freemarkerConfiguration) {
        this.freemarkerConfiguration = freemarkerConfiguration;
    }
    
    @Override
    public String toEditableXml(final InputStream documentStream, String contextPath, LeosCategory category){
        String template;
        switch (category){
            case ANNEX:
                template = editableAnnexXHtmlTemplate;
                break;
            case MEMORANDUM:
                template = editableMemorandumXHtmlTemplate;
                break;
            case BILL:
                template = editableBillXHtmlTemplate;
                break;
            default:
                throw new UnsupportedOperationException("No transformation supported for this category");
        }
        return transform(documentStream, template, contextPath);
    }

    @Override
    public String toXmlFragmentWrapper(InputStream documentStream, String contextPath) {
        return transform(documentStream, nonEditableFragmentTemplate, contextPath);
    }
    
    @Override
    public String toImportXml(InputStream documentStream, String contextPath) {
        return transform(documentStream, importXHtmlTemplate, contextPath);
    }
    
    @Override
    public String formatToHtml(XmlDocument versionDocument, String contextPath) {
        LOG.debug("formatToHtml service invoked for version id:{})", versionDocument.getId());
        
        try {
            // 1. get document content
            final Content content = versionDocument.getContent().getOrError(() -> "Version document content is required!");
            final byte[] contentBytes = content.getSource().getByteString().toByteArray();
            // 2. transform it
            return transform(new ByteArrayInputStream(contentBytes), readOnlyHtmlTemplate, contextPath);
        } catch (Exception e) {
            throw new RuntimeException("Unable to format to HTML");
        }
    }

    private String transform(InputStream documentStream, String templateName, String contextPath) {
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
