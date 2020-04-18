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
package eu.europa.ec.leos.annotate.services;

import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserProfile;
import eu.europa.ec.leos.annotate.services.exceptions.DefaultGroupNotFoundException;
import eu.europa.ec.leos.annotate.services.exceptions.UserAlreadyExistingException;
import eu.europa.ec.leos.annotate.services.exceptions.UserNotFoundException;

public interface UserService {

    // creation of new users
    User createUser(User user) throws UserAlreadyExistingException, DefaultGroupNotFoundException;

    User createUser(String userLogin) throws UserAlreadyExistingException, DefaultGroupNotFoundException;

    User createUserIfNotExists(String userLogin) throws UserAlreadyExistingException, DefaultGroupNotFoundException;

    // add a user to a group based on his associated entity
    boolean addUserToEntityGroup(UserInformation userInfo);

    // conversions
    String getUserIdFromHypothesisUserAccount(String jsonUser);

    String getHypothesisUserAccountFromUserName(String userId);

    String getHypothesisUserAccountFromUser(User user, String authority);
    String getHypothesisUserAccountFromUserId(long userId, String authority);

    // searching for user profile based on login
    User findByLogin(String login);
    User getUserById(Long userId);

    // retrieval of user profile and update of user preferences
    JsonUserProfile getUserProfile(UserInformation userInfo) throws UserNotFoundException;

    User updateSidebarTutorialVisible(String userLogin, boolean visible) throws UserNotFoundException;

    // connect to external user repository to retrieve all user-related data
    UserDetails getUserDetailsFromUserRepo(String login);

}
