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
package eu.europa.ec.leos.services.support.xml;

import com.ximpleware.AutoPilot;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XMLModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import eu.europa.ec.leos.domain.common.InstanceContext;
import eu.europa.ec.leos.services.support.flow.Workflow;

import java.util.Locale;

import static eu.europa.ec.leos.services.support.xml.VTDUtils.getStartTag;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.setupVTDNav;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.toByteArray;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@Workflow(InstanceContext.Type.COMMISSION)
class AutoNumberingProcessor implements NumberProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AutoNumberingProcessor.class);

    private static final String NUM = "num";
    private static final byte[] NUM_BYTES = "num".getBytes();
    private static final byte[] NUM_START_TAG = "<num>".getBytes();
    private static final String ARTICLE = "article";
    private static final String RECITAL = "recital";

    @Autowired
    private VtdXmlContentProcessor vtdXmlContentProcessor;
    
    @Autowired
    @Qualifier("servicesMessageSource")
    private MessageSource servicesMessageSource;

    @Override
    public byte[] renumberArticles(byte[] xmlContent, String language) {
        LOG.trace("Start renumberArticles ");
        byte[] element;
        try {
            Locale languageLocale = new Locale(language);
            VTDGen vtdGen = new VTDGen();
            vtdGen.setDoc(xmlContent);
            vtdGen.parse(false);
            VTDNav vtdNav = vtdGen.getNav();
            XMLModifier xmlModifier = new XMLModifier(vtdNav);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.selectElement(ARTICLE);
            long number = 1L;
            while (autoPilot.iterate()) {
                byte[] articleNum = servicesMessageSource.getMessage("legaltext.article.num", new Object[]{(number++)}, languageLocale).getBytes(UTF_8);

                int currentIndex = vtdNav.getCurrentIndex();
                if (vtdNav.toElement(VTDNav.FIRST_CHILD, NUM)) {
                    byte[] numTag = getStartTag(vtdNav);
                    element = XmlHelper.buildTag(numTag, NUM_BYTES, articleNum);
                    xmlModifier.remove();
                } else {
                    // build num if not exists
                    element = XmlHelper.buildTag(NUM_START_TAG, NUM_BYTES, articleNum);
                }
                vtdNav.recoverNode(currentIndex);
                xmlModifier.insertAfterHead(element);
            }

            return toByteArray(xmlModifier);
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the renumberArticles operation", e);
        }
    }

    @Override
    public String renumberImportedArticle(String xmlContent) {
        //No need to do pre process as this is done later stages 
        return xmlContent;
    }
    
    @Override
    public byte[] renumberRecitals(byte[] xmlContent) {
        LOG.trace("Start renumberRecitals");
        byte[] element;
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            XMLModifier xmlModifier = new XMLModifier(vtdNav);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.selectElement(RECITAL);
            long number = 1L;
            while (autoPilot.iterate()) {
                // get num + update
                String recitalNum = "(" + number++ + ")";
                byte[] recitalNumBytes = recitalNum.getBytes(UTF_8);

                int currentIndex = vtdNav.getCurrentIndex();
                if (vtdNav.toElement(VTDNav.FIRST_CHILD, NUM)) {
                    byte[] numTag = getStartTag(vtdNav);
                    element = XmlHelper.buildTag(numTag, NUM_BYTES, recitalNumBytes);
                    xmlModifier.remove();
                } else {
                    // build num if not exists
                    element = XmlHelper.buildTag(NUM_START_TAG, NUM_BYTES, recitalNumBytes);
                }
                vtdNav.recoverNode(currentIndex);
                xmlModifier.insertAfterHead(element);
            }

            byte[] updatedContent = toByteArray(xmlModifier);
            updatedContent = vtdXmlContentProcessor.doXMLPostProcessing(updatedContent);

            return updatedContent;
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the renumberRecitals operation", e);
        }
    }

    @Override
    public String renumberImportedRecital(String xmlContent) {
        //No need to do pre process as this is done later stages
        return xmlContent;
    }

}
