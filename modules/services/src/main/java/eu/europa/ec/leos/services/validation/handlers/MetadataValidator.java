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

import eu.europa.ec.leos.domain.common.ErrorCode;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.ErrorVO;
import eu.europa.ec.leos.domain.vo.MetadataVO;
import eu.europa.ec.leos.services.store.TemplateService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MetadataValidator implements Validator {

    private final TemplateService templateService;

    @Autowired
    MetadataValidator(TemplateService templateService) {
        this.templateService = templateService;
    }

    @Override
    public void validate(DocumentVO documentVO, final List<ErrorVO> result) throws Exception {
        validateMetadata(documentVO, result);
    }

    /**
     * Method to check the metadata of the document.
     * @param documentVO
     */
    private void validateMetadata(DocumentVO documentVO, final List<ErrorVO> errors) {
        String docId = documentVO.getId();
        MetadataVO metadata = documentVO.getMetadata();
        // check valid docpurpose
        if (StringUtils.isEmpty(metadata.getDocPurpose())) {
            errors.add(new ErrorVO(ErrorCode.DOCUMENT_PURPOSE_NOT_FOUND, docId));
        }
        // check is valid template
        try {
            if (templateService.getTemplate(metadata.getDocTemplate()) == null) {
                errors.add(new ErrorVO(ErrorCode.DOCUMENT_TEMPLATE_NOT_FOUND, docId));
            }
        } catch (Exception e) {
            errors.add(new ErrorVO(ErrorCode.DOCUMENT_TEMPLATE_NOT_FOUND, docId));
        }
        // check valid templateName
        if (StringUtils.isEmpty(metadata.getTemplateName())) {
            errors.add(new ErrorVO(ErrorCode.DOCUMENT_PROPOSAL_TEMPLATE_NOT_FOUND, docId));
        }
        if (StringUtils.isEmpty(metadata.getTemplate())) {
            errors.add(new ErrorVO(ErrorCode.DOCUMENT_PROPOSAL_TEMPLATE_NOT_FOUND, docId));
        }

        if (documentVO.getCategory() == LeosCategory.ANNEX) {
            // checks for type annex
            if (StringUtils.isEmpty(metadata.getIndex())) {
                errors.add(new ErrorVO(ErrorCode.DOCUMENT_ANNEX_INDEX_NOT_FOUND, docId));
            }
            //LEOS-2840 can be empty but not null. The tag leos:annexTitle must be in the xml.
            if (metadata.getTitle() == null) {
                errors.add(new ErrorVO(ErrorCode.DOCUMENT_ANNEX_TITLE_NOT_FOUND, docId));
            }
            if (StringUtils.isEmpty(metadata.getNumber())) {
                errors.add(new ErrorVO(ErrorCode.DOCUMENT_ANNEX_NUMBER_NOT_FOUND, docId));
            }
        }
    }
}
