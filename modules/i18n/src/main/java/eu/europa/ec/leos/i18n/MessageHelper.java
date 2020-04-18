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
package eu.europa.ec.leos.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;

public abstract class MessageHelper {

    private static final Logger LOG = LoggerFactory.getLogger(MessageHelper.class);

    @Autowired
    private LanguageHelper languageHelper;

    @Autowired
    @Qualifier("webMessageSource")
    protected MessageSource messageSource;

    protected abstract String getPrefix();
    
    /**
     * Try to get the value based on the "<key>". If not present, try to find the key with suffix "getPrefix().<key>".
     * If still not present, return the original "<key>"
     */
    public String getMessage(String key, Object... args) {
        String message = messageSource.getMessage(key, args, languageHelper.getCurrentLocale());
        
        // if not found, check if there is one with prefix
        if(message.equals(key)){
            final String newKey = getPrefix() + key;
            message = messageSource.getMessage(newKey, args, languageHelper.getCurrentLocale());
            
            // if still not found, return the original key
            if(message.equals(newKey)){
                message = key;
            }
        }
        return message;
    }

}
