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
package eu.europa.ec.leos.services.support.xml;

import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.instance.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.ARTICLE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.RECITAL;

@Component
@Instance(InstanceType.COUNCIL)
public class MandateNumberingProcessor implements NumberProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MandateNumberingProcessor.class);

    ElementNumberingHelper elementNumberingHelper;

    @Autowired
    public MandateNumberingProcessor(ElementNumberingHelper elementNumberingHelper) {
        this.elementNumberingHelper = elementNumberingHelper;
    }

    @Override
    public String renumberImportedArticle(String xmlContent, String language) {
        String updatedElements;
        elementNumberingHelper.setImportAticleDefaultProperties();
        try {
            updatedElements = new String(elementNumberingHelper.renumberElements(ARTICLE, xmlContent, false));
        } catch (Exception e) {
            LOG.error("Unable to perform the renumberArticles operation", e);
            throw new RuntimeException("Unable to perform the renumberArticles operation", e);
        } finally {
            elementNumberingHelper.resetImportAticleDefaultProperties();
        }
        return updatedElements; 
    }

    @Override
    public String renumberImportedRecital(String xmlContent) {
        //No need to do pre process as this is done later stages
        return xmlContent;
    }

    @Override
    public byte[] renumberArticles(byte[] xmlContent) {
        LOG.trace("Start renumbering Articles");
        try {
        	return elementNumberingHelper.renumberElements(ARTICLE, xmlContent, true);
        } catch (Exception e) {
            LOG.error("Unable to perform the renumber Articles operation", e);
            throw new RuntimeException("Unable to perform the renumber Articles operation", e);
        }
    }

    @Override
    public byte[] renumberRecitals(byte[] xmlContent) {
        LOG.trace("Start renumbering Recitals");
        try {
            return elementNumberingHelper.renumberElements(RECITAL, xmlContent, true);
        } catch (Exception e) {
            LOG.error("Unable to perform the renumber Recitals operation", e);
            throw new RuntimeException("Unable to perform the renumber Recitals operation", e);
        }
    }

    @Override
    public byte[] renumberLevel(byte[] xmlContent) {
        return xmlContent;
    }

}