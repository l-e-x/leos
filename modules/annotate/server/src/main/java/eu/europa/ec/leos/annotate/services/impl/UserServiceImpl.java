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

import eu.europa.ec.leos.annotate.Authorities;
import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.web.user.JsonGroup;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserProfile;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserShowSideBarPreference;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.GroupService;
import eu.europa.ec.leos.annotate.services.UserService;
import eu.europa.ec.leos.annotate.services.UserServiceWithTestFunctions;
import eu.europa.ec.leos.annotate.services.exceptions.DefaultGroupNotFoundException;
import eu.europa.ec.leos.annotate.services.exceptions.UserAlreadyExistingException;
import eu.europa.ec.leos.annotate.services.exceptions.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Service responsible for managing annotating users Note: communicates with external UD-repo for retrieval of user
 * detail information
 */
@Service
public class UserServiceImpl implements UserService, UserServiceWithTestFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);

    private final static String HYPOTHESIS_ACCOUNT_PREFIX = "acct:";

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    // URL to the external user repository
    @Value("${user.repository.url}")
    private String repositoryUrl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupService groupService;

    // REST template used for calling UD-repo
    @Autowired
    private RestTemplate restOperations;

    // cache for the user details
    @Autowired
    private UserDetailsCache userDetailsCache;

    // -------------------------------------
    // Constructors and other functions used for testing; these test functions are not part of the UserService interface
    // -------------------------------------
    // note: use a custom constructor in order to ease testability by benefiting from dependency injection
    @Autowired
    public UserServiceImpl(final RestTemplate restOps) {
        if (this.restOperations == null) {
            this.restOperations = restOps;
        }
        if (this.userDetailsCache == null) {
            this.userDetailsCache = new UserDetailsCache();
        }
    }

    // custom constructor in order to ease testability by benefiting from mock object injection via Mockito
    public UserServiceImpl(final UserRepository userRepos, final GroupService groupService) {
        this.userRepository = userRepos;
        this.groupService = groupService;
    }

    // possibility to inject a custom RestTemplate - USED FOR TESTING, NOT CONTAINED IN PUBLIC SERVICE INTERFACE
    @Override
    public void setRestTemplate(final RestTemplate restOps) {
        this.restOperations = restOps;
    }

    // possibility to cache a custom user entry - USED FOR TESTING, NOT CONTAINED IN PUBLIC SERVICE INTERFACE
    @Override
    public void cacheUserDetails(final String key, final UserDetails details) {
        this.userDetailsCache.cache(key, details);
    }

    // possibility to inject a custom group service - USED FOR TESTING, NOT CONTAINED IN PUBLIC SERVICE INTERFACE
    @Override
    public void setGroupService(final GroupService grpServ) {
        this.groupService = grpServ;
    }

    // -------------------------------------
    // Service functionality
    // -------------------------------------
    /**
     * saving a given user note: ID of the user is set
     *
     * @param user the user to be saved
     * @throws UserAlreadyExistingException if the user is already registered with his login, this exception is thrown
     * @throws DefaultGroupNotFoundException if the name of the default user group is not found, this exception is thrown
     * 
     * @return saved {@link User} object, with properties like Id updated
     */
    @Override
    public User createUser(final User user) throws UserAlreadyExistingException, DefaultGroupNotFoundException {

        // if the default user group is not available, we stop here
        groupService.throwIfNotExistsDefaultGroup();

        LOG.info("Save user with login '{}' in the database", user.getLogin());

        User modifiedUser = null;
        try {
            modifiedUser = userRepository.save(user); // updates the ID
            LOG.debug("User '{}' created with id {}", modifiedUser.getLogin(), modifiedUser.getId());
        } catch (DataIntegrityViolationException dive) {
            LOG.error("The user '{}' already exists", user.getLogin());
            throw new UserAlreadyExistingException(dive);
        } catch (Exception ex) {
            LOG.error("Exception while creating user", ex);
            throw new UserAlreadyExistingException(ex);
        }

        // assign to default group
        groupService.assignUserToDefaultGroup(modifiedUser);

        return modifiedUser;
    }

    /**
     * saving a given user
     *
     * @param login the login of the user to be saved
     * @throws UserAlreadyExistingException if the user is already registered with his login, this exception is thrown
     * @throws DefaultGroupNotFoundException if the name of the default user group is not found, this exception is thrown
     * 
     * @return saved {@link User} object, with properties like Id updated
     */
    @Override
    public User createUser(final String login) throws UserAlreadyExistingException, DefaultGroupNotFoundException {

        final User user = new User(login);
        return createUser(user);
    }

    /**
     * check if a user is already registered - register him if not
     *
     * @param login the login of the user to be saved
     * @throws UserAlreadyExistingException if the user is already registered with his login, this exception is thrown
     * @throws DefaultGroupNotFoundException if the name of the default user group is not found, this exception is thrown
     */
    @Override
    public User createUserIfNotExists(final String login) throws UserAlreadyExistingException, DefaultGroupNotFoundException {

        Assert.isTrue(!StringUtils.isEmpty(login), "Cannot register user without valid login");

        final User registeredUser = findByLogin(login);
        if (registeredUser == null) {
            return createUser(login);
        } else {
            LOG.debug("User '{}' already exists, no need for registering.", login);
            return registeredUser;
        }
    }

    /**
     * add a user to a group if it has its associate entity property set
     * 
     *  @param userinfo the {@link UserInformation} collected about the user, contains info from our database and from UD repo
     *  
     *  @return flag indicating success or nothing to do; FALSE indicates a problem
     */
    @Override
    public boolean addUserToEntityGroup(final UserInformation userInfo) {

        Assert.notNull(userInfo, "Received invalid user object when trying to assign user to entity-based group");

        final User user = userInfo.getUser();
        Assert.notNull(user, "Received invalid user object when trying to assign user to entity-based group");

        final UserDetails userDetails = userInfo.getUserDetails();
        Assert.notNull(userDetails, "Received invalid user details object when trying to assign user to entity-based group");

        if (StringUtils.isEmpty(userDetails.getEntity())) {
            LOG.debug("User '{}' does not have an entity assigned; won't assign to any further group though", userDetails.getLogin());
            return false;
        }

        // if we reach this point, the user is associated to an entity
        // -> retrieve the corresponding group
        Group entityGroup = groupService.findGroupByName(userDetails.getEntity());
        if (entityGroup == null) {

            // group not yet defined, so this must be the first user associated to the entity logging in -> create non-public group
            try {
                entityGroup = groupService.createGroup(userDetails.getEntity(), false);
            } catch (Exception e) {
                LOG.error("Received error creating group:", e);
            }
            if (entityGroup == null) {
                LOG.warn("It seems the new group with name '{}' could not be created! Cannot assign user '{}' to it.", userDetails.getEntity(),
                        userDetails.getLogin());
                return false;
            }
        }

        // now the group is available - either was already, or has newly been created -> assign user
        return groupService.assignUserToGroup(user, entityGroup);
    }

    /**
     * extract the user id from the content received in JSON (e.g.: acct:xy@domain.eu)
     *
     * @param hypoClientUser the account data as received by hypothesis client
     * @return extracted user id
     */
    @Override
    public String getUserIdFromHypothesisUserAccount(final String hypoClientUser) {

        if (StringUtils.isEmpty(hypoClientUser)) {
            return null;
        }

        // the hypothesis client provide the user name as follows:
        // "user": "acct:sela83@hypothes.is"
        // i.e. "acct:" + <user id> + "@" + <authority>
        if (!hypoClientUser.toLowerCase(Locale.ENGLISH).startsWith(HYPOTHESIS_ACCOUNT_PREFIX) || hypoClientUser.length() <= 5) {

            LOG.error("Given user name (" + hypoClientUser + ") is not in expected format");
            // expected format not present
            return null;
        }

        return hypoClientUser.substring(5);
    }

    /**
     * wrap a user name in the format expected by hypothesis client
     *
     * @param username the user name to be converted
     * @return converted user name
     */
    @Override
    public String getHypothesisUserAccountFromUserName(final String username) {

        if (StringUtils.isEmpty(username)) {
            return "";
        }

        return HYPOTHESIS_ACCOUNT_PREFIX + username;
    }

    /**
     * retrieve the user login of a user and convert it for hypothes.is format
     *
     * @param user the user object for which to retrieve hypothes.is user id format
     * @param authority the authority of the user
     * @return converted user login
     */
    @Override
    public String getHypothesisUserAccountFromUser(final User user, final String authority) {

        if (user == null) {
            LOG.error("Cannot determine annotate/hypothesis user account: no user given!");
            return "";
        }
        if (StringUtils.isEmpty(authority)) {
            LOG.error("Cannot determine annotate/hypothesis user account: no authority given!");
            return "";
        }

        final String userId = user.getLogin() + "@" + authority;
        return getHypothesisUserAccountFromUserName(userId);
    }

    /**
     * retrieve the user login of a user and convert it for hypothes.is format
     *
     * @param userId the internal database ID of the user for which to retrieve hypothes.is user id format
     * @param authority the authority of the user
     * @return converted user login
     */
    @Override
    public String getHypothesisUserAccountFromUserId(final long userId, final String authority) {
        
        final User user = userRepository.findById(userId);
        if(user == null) {
            LOG.error("Cannot determine annotate/hypothesis user account: user not found in database!");
            return "";
        }
        
        return getHypothesisUserAccountFromUser(user, authority);
    }
    
    /**
     * search for a user given its login name
     *
     * @param login the user's login name
     * @return found user, or {@literal null}
     */
    @Override
    public User findByLogin(final String login) {

        final User foundUser = userRepository.findByLogin(login);
        LOG.debug("Found user based on login: {}", foundUser != null);
        return foundUser;
    }

    /**
     * retrieve the full user profile for the hypothes.is client
     *
     * @param userInfo the user for which the profile is to be retrieved
     *
     * @return {@link JsonUserProfile} containing all relevant information (preferences, groups, ...)
     */
    @Override
    public JsonUserProfile getUserProfile(final UserInformation userInfo) throws UserNotFoundException {
        
        Assert.notNull(userInfo, "userInfo undefined");

        final User user = userInfo.getUser();
        if (user == null) {
            LOG.error("User '" + userInfo.getLogin() + "' is unknown; cannot return its profile.");
            throw new UserNotFoundException(userInfo.getLogin());
        }

        final JsonUserProfile profile = new JsonUserProfile();
        profile.setAuthority(userInfo.getAuthority());

        profile.setUserid(userInfo.getAsHypothesisAccount());

        final UserDetails userInfoFromRepo = getUserDetailsFromUserRepo(user.getLogin());
        if (userInfoFromRepo != null) {
            profile.setDisplayName(userInfoFromRepo.getDisplayName());
            profile.setEntityName(userInfoFromRepo.getEntity());
        }

        final List<Group> groups = groupService.getGroupsOfUser(user);
        if (groups != null) {
            
            if(Authorities.isIsc(userInfo.getAuthority())) {
                // for ISC, we should not return the default group -> filter out
                LOG.debug("Remove default group for ISC user {}", user.getLogin());
                groups.remove(groupService.findDefaultGroup());
            }
            
            for (final Group g : groups) {
                profile.addGroup(new JsonGroup(g.getDisplayName(), g.getName(), g.isPublicGroup()));
            }
        }

        // preferences
        final JsonUserShowSideBarPreference showSidebar = new JsonUserShowSideBarPreference();
        showSidebar.setShow_sidebar_tutorial(!user.isSidebarTutorialDismissed());
        profile.setPreferences(showSidebar);

        return profile;
    }

    /**
     * update the user's setting whether the sidebar tutorial is to be shown
     *
     * @param userLogin the user's login name
     * @param visible the new value
     *
     * @throws UserNotFoundException throws this exception when the given user is unknown
     */
    @Override
    public User updateSidebarTutorialVisible(final String userLogin, final boolean visible) throws UserNotFoundException {

        Assert.isTrue(!StringUtils.isEmpty(userLogin), "Required user login missing");

        final User foundUser = userRepository.findByLogin(userLogin);
        if (foundUser == null) {
            LOG.error("User '" + userLogin + "' is unknown; cannot update its preferences.");
            throw new UserNotFoundException(userLogin);
        }

        foundUser.setSidebarTutorialDismissed(!visible);
        userRepository.save(foundUser);

        return foundUser;
    }

    /**
     * retrieve user details (email address, name, DG, display name, ...) from the external user repository (via REST
     * interface) note: information is cached in order to avoid too many unnecessary calls; cache will be wiped after a
     * few minutes
     *
     * @param login the login of the user for which details are required
     * @return {@link UserDetails} object containing all user properties
     */
    @Override
    public UserDetails getUserDetailsFromUserRepo(final String login) {

        // check cache first - especially useful when returning search result with multiple annotations from save user!
        final UserDetails cachedDetails = userDetailsCache.getCachedUserDetails(login);
        if (cachedDetails != null) {
            LOG.debug("User details for user '{}' still cached, use cached info", login);
            return cachedDetails;
        }

        final Map<String, String> params = new HashMap<String, String>();
        params.put("userId", login);

        // contact the ud-repo and cache the result
        try {
            LOG.debug("Searching for user '{}' in user repository", login);
            final UserDetails foundUser = restOperations.getForObject(repositoryUrl, UserDetails.class, params);
            userDetailsCache.cache(login, foundUser);
            return foundUser;
        } catch (RestClientException e) {
            LOG.warn("Exception while getting user by login: {}", e.getMessage());
            return null;
        }
    }

}
