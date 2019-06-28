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
package eu.europa.ec.leos.annotate.services.impl;

import eu.europa.ec.leos.annotate.model.UserInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class for storing the currently authenticated user's information - used in a thread-local bean, see configuration class
 * (in so doing, the class can be integrated with Autowired annotation, no need of also requiring the AuthenticationService, 
 *  in which it was contained before)  
 */
public class AuthenticatedUserStore {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticatedUserStore.class);
    
    private UserInformation userInfo;

    // -----------------------------------------------------------
    // Public functionality
    // -----------------------------------------------------------

    // remove the reference to the user
    public void clear() {
        this.userInfo = null;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    public UserInformation getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(final UserInformation userInfo) {
        if(userInfo != null) {
            LOG.debug("Setting user info; user login='{}'", userInfo.getLogin());
        }
        this.userInfo = userInfo;
    }
}
