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
package eu.europa.ec.leos.services.mandate;

import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.common.ErrorCode;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;

@Service
public class PostProcessingMandateServiceImpl implements PostProcessingMandateService {

    private final XmlContentProcessor xmlContentProcessor;

    @Autowired
    PostProcessingMandateServiceImpl(XmlContentProcessor xmlContentProcessor) {
        this.xmlContentProcessor = xmlContentProcessor;
    }

    public Result<String> processMandate(DocumentVO documentVO) {
        if (documentVO.getCategory().equals(LeosCategory.PROPOSAL)) {
            for (DocumentVO doc : documentVO.getChildDocuments()) {
                try {
                    if (!doc.getCategory().equals(LeosCategory.PROPOSAL)) {
                        byte[] docContent = doc.getSource();
                        if (doc.getCategory().equals(LeosCategory.BILL)) {
                            
                            byte[] updatedDocContent = xmlContentProcessor.setAttributeForAllChildren(docContent, BILL, Collections.emptyList(), LEOS_ORIGIN_ATTR, EC);
                            updatedDocContent = xmlContentProcessor.setAttributeForAllChildren(updatedDocContent, BODY, Arrays.asList(ARTICLE),
                                    LEOS_DELETABLE_ATTR, "false");
                            updatedDocContent = xmlContentProcessor.setAttributeForAllChildren(updatedDocContent, BILL,
                                    Arrays.asList(CITATIONS, RECITALS, ARTICLE), LEOS_EDITABLE_ATTR, "false");
                            
                            doc.setSource(updatedDocContent);
                            
                            for (DocumentVO annex : doc.getChildDocuments()) {
                                byte[] annexContent = annex.getSource();
                                byte[] updatedDocContentAnnex = xmlContentProcessor.setAttributeForAllChildren(annexContent, DOC, Collections.emptyList(), LEOS_ORIGIN_ATTR, EC);
                                annex.setSource(updatedDocContentAnnex);
                            }
                        } else {
                            byte[] updatedDocContent = xmlContentProcessor.setAttributeForAllChildren(docContent, DOC, Collections.emptyList(), LEOS_ORIGIN_ATTR, EC);
                            doc.setSource(updatedDocContent);
                        }
                    }
                } catch (Exception e) {
                    return new Result<String>(e.getMessage(), ErrorCode.EXCEPTION);
                }
            }
        }
        return new Result<String>("OK", null);
    }
}