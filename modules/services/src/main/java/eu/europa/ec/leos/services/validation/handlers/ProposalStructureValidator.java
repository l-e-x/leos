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
package eu.europa.ec.leos.services.validation.handlers;

import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.common.ErrorCode;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.ErrorVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ProposalStructureValidator implements Validator {

    private static final LeosCategory[] MANDATORY_DOCUMENTS = new LeosCategory[]{LeosCategory.PROPOSAL, LeosCategory.BILL};

    @Override
    public void validate(DocumentVO documentVO, final List<ErrorVO> result) throws Exception {
        validateProposalStructure(documentVO, result);
    }

    /**
     * A valid proposal should contain instances of this documentvo: memo, bill.
     *
     * @param documentVO
     */
    private void validateProposalStructure(DocumentVO documentVO, final List<ErrorVO> result) {
        String docId = documentVO.getId();
        List<LeosCategory> mandatoryCategories = new ArrayList<>(Arrays.asList(MANDATORY_DOCUMENTS));
        mandatoryCategories.remove(documentVO.getCategory());
        if (documentVO.getChildDocuments() != null) {
            for (DocumentVO childDoc : documentVO.getChildDocuments()) {
                mandatoryCategories.remove(childDoc.getCategory());
            }
        }
        for (LeosCategory category : mandatoryCategories) {
            result.add(new ErrorVO(ErrorCode.DOCUMENT_NOT_FOUND, docId, StringUtils.capitalize(category.name().toLowerCase())));
        }
    }
}
