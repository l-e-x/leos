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
package eu.europa.ec.leos.services.validation.handlers;

import com.ximpleware.VTDGen;
import eu.europa.ec.leos.domain.common.ErrorCode;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.ErrorVO;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeneralDocumentValidator implements Validator {

    @Override
    public void validate(DocumentVO documentVO, final List<ErrorVO> result) throws Exception {
        validateDocument(documentVO, result);
    }

    private void validateDocument(DocumentVO documentVO, final List<ErrorVO> result) {
        String docId = documentVO.getId();
        // check is valid category
        if (documentVO.getCategory() == null || !EnumUtils.isValidEnum(LeosCategory.class, documentVO.getCategory().name())) {
            result.add(new ErrorVO(ErrorCode.DOCUMENT_CATEGORY_NOT_FOUND, docId));
        }
        // check we have the source
        if (documentVO.getSource() == null) {
            result.add(new ErrorVO(ErrorCode.DOCUMENT_SOURCE_NOT_FOUND, docId));
        }
    }
}
