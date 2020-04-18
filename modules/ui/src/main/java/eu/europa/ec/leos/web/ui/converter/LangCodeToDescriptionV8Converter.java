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
package eu.europa.ec.leos.web.ui.converter;

import com.vaadin.data.Converter;
import com.vaadin.data.Result;
import com.vaadin.data.ValueContext;
import eu.europa.ec.leos.i18n.LanguageHelper;

public class LangCodeToDescriptionV8Converter implements Converter<String, String> {

    private LanguageHelper langHelper;

    public LangCodeToDescriptionV8Converter(LanguageHelper langHelper) {
        super();
        this.langHelper = langHelper;
    }

    @Override
    public Result<String> convertToModel(String value, ValueContext context) {
        return Result.ok(langHelper.getLanguageCode(value));
    }

    @Override
    public String convertToPresentation(String value, ValueContext context) {
        return (value == null)? null: langHelper.getLanguageDescription(value);
    }
}
