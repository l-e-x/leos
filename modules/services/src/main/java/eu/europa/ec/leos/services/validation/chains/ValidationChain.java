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
package eu.europa.ec.leos.services.validation.chains;

import eu.europa.ec.leos.domain.common.ErrorCode;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.ErrorVO;
import eu.europa.ec.leos.services.validation.handlers.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public abstract class ValidationChain implements Validator {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationChain.class);
    protected final List<Validator> chain = new ArrayList<>();

    @Override
    public void validate(DocumentVO documentVO, List<ErrorVO> result) {
        final ListIterator<Validator> chainIterator = chain.listIterator();
        if (!chainIterator.hasNext()) {
            return;
        }

        while (chainIterator.hasNext()) {
            Validator handler = chainIterator.next();
            try {
                handler.validate(documentVO, result);
                LOG.trace("Handler:{} finished for {} ", handler.getClass().getSimpleName(), documentVO.getDocumentType());
            } catch (Exception ex) {
                LOG.error("Handler:{} error for {}", handler.getClass(), documentVO.getDocumentType(), ex);
                result.add(new ErrorVO(ErrorCode.EXCEPTION, documentVO.getId(), ex.getMessage()));
            }
        }
    }

    abstract public boolean supports(DocumentVO documentVO);

}
