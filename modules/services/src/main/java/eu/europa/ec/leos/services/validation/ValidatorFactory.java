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

import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.services.validation.chains.ValidationChain;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ValidatorFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ValidatorFactory.class);

    @Autowired
    private List<ValidationChain> allChains;

    public ValidationChain getValidationChain(DocumentVO documentVO) {
        Validate.notNull(documentVO.getDocumentType(), "Document must have a valid LeosCategory");

        for (ValidationChain chain : allChains) {
            if (chain.supports(documentVO)) {
                LOG.trace("Returning: {} chain for :{}", chain.getClass(), documentVO.getDocumentType());
                return chain;
            }
        }
        throw new IllegalStateException("Invalid document category" + documentVO.getDocumentType());
    }
}
