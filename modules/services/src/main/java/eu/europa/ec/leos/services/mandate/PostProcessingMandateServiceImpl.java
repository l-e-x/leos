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
package eu.europa.ec.leos.services.mandate;

import eu.europa.ec.leos.domain.common.ErrorCode;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PostProcessingMandateServiceImpl implements PostProcessingMandateService {
    private final XmlContentProcessor xmlContentProcessor;

    private static final String BILL = "bill";
    private static final String DOC = "doc";

    @Autowired
    PostProcessingMandateServiceImpl(XmlContentProcessor xmlContentProcessor) {
        this.xmlContentProcessor = xmlContentProcessor;
    }

    public Result<String> processMandate(DocumentVO documentVO) {
        if (documentVO.getCategory().equals(LeosCategory.PROPOSAL)) {
            for(DocumentVO doc: documentVO.getChildDocuments()) {
                try {
                    if (doc.getCategory().equals(LeosCategory.BILL) || doc.getCategory().equals(LeosCategory.ANNEX) || doc.getCategory().equals(LeosCategory.MEMORANDUM)) {
                        byte[] docContent = doc.getSource();
                        byte[] updatedDocContent = xmlContentProcessor.setOriginAttribute(docContent, doc.getCategory().equals(LeosCategory.BILL) ? BILL : DOC);
                        doc.setSource(updatedDocContent);
                    }
                } catch (Exception e) {
                    return new Result<String>(e.getMessage(), ErrorCode.EXCEPTION);
                }
            }
        }
        return new Result<String>("OK", null);
    }
}