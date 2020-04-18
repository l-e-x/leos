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
package eu.europa.ec.leos.services.export;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
class ExportHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ExportServiceImpl.class);

    private final Configuration freemarkerConfiguration;

    @Value("${leos.freemarker.ftl.export.legiswrite.pdf}")
    private String exportTemplateLW_pdf;

    @Value("${leos.freemarker.ftl.export.legiswrite.word}")
    private String exportTemplateLW_word;
    
    @Value("${leos.freemarker.ftl.export.docuwrite.pdf_legaltext}")
    private String exportTemplateDW_pdfLegalText;

    @Value("${leos.freemarker.ftl.export.docuwrite.word_legaltext}")
    private String exportTemplateDW_wordLegalText;

    @Value("${leos.freemarker.ftl.export.docuwrite.pdf}")
    private String exportTemplateDW_pdf;

    @Value("${leos.freemarker.ftl.export.docuwrite.word}")
    private String exportTemplateDW_word;

    @Value("${leos.freemarker.ftl.export.docuwrite.word_annex}")
    private String exportTemplateDW_wordAnnex;

    @Value("${leos.freemarker.ftl.export.docuwrite.pdf_annex}")
    private String exportTemplateDW_pdfAnnex;

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
                case TO_PDF_LW:
                    template = freemarkerConfiguration.getTemplate(exportTemplateLW_pdf);
                    break;
                case TO_WORD_LW:
                    template = freemarkerConfiguration.getTemplate(exportTemplateLW_word);
                    break;
                case TO_PDF_DW_LT:
                    template = freemarkerConfiguration.getTemplate(exportTemplateDW_pdfLegalText);
                    break;
                case TO_PDF_DW:
                    template = freemarkerConfiguration.getTemplate(exportTemplateDW_pdf);
                    break;
                case TO_WORD_DW_LT:
                    template = freemarkerConfiguration.getTemplate(exportTemplateDW_wordLegalText);
                    break;
                case TO_WORD_DW:
                    template = freemarkerConfiguration.getTemplate(exportTemplateDW_word);
                    break;
                case TO_PDF_DW_ANNEX:
                    template = freemarkerConfiguration.getTemplate(exportTemplateDW_pdfAnnex);
                    break;
                case TO_WORD_DW_ANNEX:
                    template = freemarkerConfiguration.getTemplate(exportTemplateDW_wordAnnex);
                    break;
                default:
                    throw new Exception("Export Options not valid");
            }

            Map<String, ExportResource> resources = new HashMap<String, ExportResource>();
            resources.put("resource_tree", exportRootNode);
            
            template.process(resources, outputWriter);
            String result = outputWriter.getBuffer().toString();
            byteOutputStream.write(result.getBytes(UTF_8));
        } catch (TemplateException| IOException| JAXBException ex) {
            LOG.error("Error while creating content xml file {}", ex.getMessage());
            throw ex;
        }
        return byteOutputStream;
    }
}
