/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
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

import com.vaadin.data.util.converter.Converter;
import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

import java.util.Locale;

public class StageValueConverter implements Converter<String, LeosDocumentProperties.Stage> {

    private MessageHelper messageHelper;
    public StageValueConverter(MessageHelper messageHelper) {
        super();
        this.messageHelper=messageHelper;
    }

    @Override
    public LeosDocumentProperties.Stage convertToModel(String value, Class<? extends LeosDocumentProperties.Stage> targetType, Locale locale) throws ConversionException {
        throw new ConversionException("Not Implemented converter");
    }

    @Override
    public String convertToPresentation(LeosDocumentProperties.Stage value, Class<? extends String> targetType, Locale locale) throws ConversionException {
        return (value==null)
                ?null
                : messageHelper.getMessage("stage.caption."+ value.toString().toLowerCase());
    }

    @Override
    public Class<LeosDocumentProperties.Stage> getModelType() {
        return LeosDocumentProperties.Stage.class;
    }

    @Override
    public Class<String> getPresentationType() {
        return String.class;
    }
}
