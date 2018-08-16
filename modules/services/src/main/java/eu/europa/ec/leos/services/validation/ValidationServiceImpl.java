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
package eu.europa.ec.leos.services.validation;

import eu.europa.ec.leos.domain.common.ErrorCode;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.ErrorVO;
import eu.europa.ec.leos.model.notification.validation.DocumentValidationNotification;
import eu.europa.ec.leos.services.notification.NotificationService;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
class ValidationServiceImpl implements ValidationService {
    private static final Logger LOG = LoggerFactory.getLogger(ValidationServiceImpl.class);
    private ValidatorFactory validatorFactory;
    private NotificationService notificationService;

    @Value("${validation.notification.functional.mailbox}")
    private String notificationRecepient;

    @Autowired
    public ValidationServiceImpl(ValidatorFactory validatorFactory, NotificationService notificationService) {
        this.validatorFactory = validatorFactory;
        this.notificationService = notificationService;
    }

    @Override
    public List<ErrorVO> validateDocument(DocumentVO documentVO) {
        Validate.notNull(documentVO.getId(), "Document id is required!");
        Validate.notNull(documentVO.getDocumentType(), "Document type is required!");
        final List<ErrorVO> result = new ArrayList<>();
        try {
            validatorFactory
                    .getValidationChain(documentVO)
                    .validate(documentVO, result);
        } catch (Exception ex) {
            LOG.error("Validation chain ended with error", ex);
            result.add(new ErrorVO(ErrorCode.EXCEPTION, documentVO.getId(), ex.getMessage()));
        }
        LOG.debug("Validation found {} issues with document", result.size());
        return result;
    }

    @Override
    @Async("simpleAsyncNonReusableThreadPool")
    public void validateDocumentAsync(DocumentVO documentVO) {
        List<ErrorVO> result = validateDocument(documentVO);
        if (result != null && !result.isEmpty()) {
            List<String> errors = new ArrayList<>();
            result.forEach(errorVo -> {
                errors.add(errorVo.toString());
                LOG.debug("Validation issue found with document - {}", errorVo.toString());
            });
            // FIXME LEOS-2894 Annex title will be empty if Annex title is left blank, titles for bill, memorandum and Annex are created differently, refer JIRA for details
            notificationService
                    .sendNotification(new DocumentValidationNotification(notificationRecepient, errors, documentVO.getUpdatedBy(), documentVO.getUpdatedOn(),
                            documentVO.getTitle()));
        }
    }
}
