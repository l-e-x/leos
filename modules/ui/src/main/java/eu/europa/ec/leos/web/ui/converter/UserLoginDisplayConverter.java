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
package eu.europa.ec.leos.web.ui.converter;

import com.vaadin.v7.data.util.converter.Converter;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.web.support.user.UserHelper;

import java.util.Locale;

public class UserLoginDisplayConverter implements Converter<String, String> {
    private UserHelper userHelper;

    public UserLoginDisplayConverter(UserHelper userHelper) {
        super();
        this.userHelper = userHelper;
    }

    @Override
    public String convertToModel(String value, Class<? extends String> targetType, Locale locale) throws ConversionException {
        throw new ConversionException("Not Implemented Method");
    }

    @Override
    // From user's login, get the name of the user, if user is not found it returns the login. 
    public String convertToPresentation(String value, Class<? extends String> targetType, Locale locale) throws ConversionException {
        try {
            User user = userHelper.getUser(value);
            value = ((user.getName() == null) || (user.getName().isEmpty())) ? user.getLogin() : user.getName();
        }
        finally {
            return value;
        }
    }

    @Override
    public Class<String> getModelType() {
        return String.class;
    }

    @Override
    public Class<String> getPresentationType() {
        return String.class;
    }
}
