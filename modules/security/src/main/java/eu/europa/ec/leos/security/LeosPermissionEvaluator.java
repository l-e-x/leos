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
package eu.europa.ec.leos.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/* This class is loosely based on custom behaviour of AbstractAccessDecisionManager*/
@Component
public class LeosPermissionEvaluator implements PermissionEvaluator {
    private static final Logger LOG = LoggerFactory.getLogger(LeosPermissionEvaluator.class);

    @Autowired
    private List<AccessDecisionVoter<? extends Object>> decisionVoters;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (permission == null || targetDomainObject == null) {
            return false; // if not permission/object is supplied, it is denied by default
        }

        List<ConfigAttribute> configAttributes = SecurityConfig.createListFromCommaDelimitedString((String) permission);
        Class domainClass = targetDomainObject.getClass();
        
        for (AccessDecisionVoter voter : decisionVoters) {
            if (voter.supports(domainClass) && voter.supports(configAttributes.get(0))) {
                // As of now, only voters with single ConfigAttributes are supported.

                int result = voter.vote(authentication, targetDomainObject, configAttributes);
                LOG.debug("Voter: " + voter + ", returned: " + result);

                // can abstain, grant or deny
                // if deterministic answer is given by any voter, use it and return
                switch (result) {
                    case AccessDecisionVoter.ACCESS_GRANTED:
                        return true;
                    case AccessDecisionVoter.ACCESS_DENIED:
                        break; //if access denied, keep looking for other voters
                    default:
                        // if voter abstain, keep looking for other voters who support the object
                        break;
                }// end switch
            }
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        throw new UnsupportedOperationException("Permission checking with ID are not supported :" + this.getClass().toString());
    }
}
