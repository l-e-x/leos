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
package eu.europa.ec.leos.web.support.user;

import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.user.UserService;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Scope("singleton")
@Component("userHelper")
public class UserHelper {
    private static final Logger LOG = LoggerFactory.getLogger(UserHelper.class);

    @Autowired
    private UserService userService;
    @Autowired
    private MessageHelper messageHelper;

    private static final String DOMAIN_SEPARATOR="/";
    
    public User getUser(String login) {
        //Remove auth domain from CMIS. admin/XXX ecas/YYYY
        if(login.contains(DOMAIN_SEPARATOR))
        {
            login= login.substring(login.lastIndexOf(DOMAIN_SEPARATOR)+1);
        }
        User user = userService.getUser(login);
        if(user == null)
        {
            //If we don't find the user we return an instance of the user tagged as unavailable.
            user = new User(RandomUtils.nextLong(), login, messageHelper.getMessage("proposal.caption.unavailable.user",login),"","");
        }
        return user;
    }

    public List<User> searchUsersByKey(String key){
        return userService.searchUsersByKey(key);
    }
}
