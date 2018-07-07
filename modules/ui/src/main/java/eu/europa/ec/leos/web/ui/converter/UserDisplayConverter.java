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
import eu.europa.ec.leos.vo.UserVO;

import java.util.Locale;

public class UserDisplayConverter implements Converter<String, UserVO> {

    @Override
    public UserVO convertToModel(String value, Class<? extends UserVO> targetType, Locale locale) throws ConversionException {
        throw new ConversionException("Not Implemented Method");
    }

    @Override
    public String convertToPresentation(UserVO value, Class<? extends String> targetType, Locale locale) throws ConversionException {
        return value.getName();
    }

    @Override
    public Class<UserVO> getModelType() {
        return UserVO.class;
    }

    @Override
    public Class<String> getPresentationType() {
        return String.class;
    }
}
