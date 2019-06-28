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
package eu.europa.ec.leos.cmis.authentication;

import org.apache.chemistry.opencmis.client.bindings.spi.StandardAuthenticationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import java.security.Principal;

public class LeosCmisAuthenticationProvider extends StandardAuthenticationProvider {

    private static final long serialVersionUID = 1L;
    private static final PropertiesHelper propertiesHelper = new PropertiesHelper();

    @Override
    protected String getUser() {
        // NOTE when no username is configured, link the CMIS session to the LEOS User, through the user's login
        String name = getTechnicalUserName();
        if (name == null || name.isEmpty()) {
            Principal principal = getPrincipal();
            if (principal != null) {
                name = principal.getName();
            }
        }
        return name;
    }

    @Override
    protected String getPassword() {
        return propertiesHelper.repositoryPassword;
    }

   protected String getTechnicalUserName(){
        return propertiesHelper.repositoryUsername;
   }

    protected Authentication getPrincipal() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static class PropertiesHelper extends SpringBeanAutowiringSupport {

        @Value("${leos.cmis.repository.username}")
        public String repositoryUsername;

        @Value("${leos.cmis.repository.password}")
        public String repositoryPassword;
    }
}
