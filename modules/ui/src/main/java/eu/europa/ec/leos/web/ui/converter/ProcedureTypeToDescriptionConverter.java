/*
 * Copyright 2017 European Commission
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
package eu.europa.ec.leos.web.ui.converter;

import com.vaadin.v7.data.util.converter.Converter;
import eu.europa.ec.leos.ui.model.ProcedureType;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

import java.util.Locale;

public class ProcedureTypeToDescriptionConverter implements Converter<String, ProcedureType> {

    private MessageHelper messageHelper;

    public ProcedureTypeToDescriptionConverter(MessageHelper messageHelper) {
        super();
        this.messageHelper = messageHelper;
    }

    @Override
    public ProcedureType convertToModel(String value, Class<? extends ProcedureType> targetType, Locale locale) throws ConversionException {
        throw new ConversionException("Conversion not supported ");
    }

    @Override
    public String convertToPresentation(ProcedureType value, Class<? extends String> targetType, Locale locale) throws ConversionException {
        return (value == null)? null: messageHelper.getMessage("repository.caption.filters.procedure." + value.toString().toLowerCase());
    }

    @Override
    public Class<ProcedureType> getModelType() {
        return ProcedureType.class;
    }

    @Override
    public Class<String> getPresentationType() {
        return String.class;
    }
}
