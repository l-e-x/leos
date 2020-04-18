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
package eu.europa.ec.leos.integration.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.europa.ec.leos.integration.UsersProvider;
import eu.europa.ec.leos.model.user.User;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
class UsersClientImpl implements UsersProvider {
    private static Logger LOG = LoggerFactory.getLogger(UsersClientImpl.class);

    @Value("#{integrationProperties['leos.user.repository.url']}")
    private String repositoryUrl;

    @Value("#{integrationProperties['leos.user.repository.findbylogin.uri']}")
    private String findByLoginUri;

    @Value("#{integrationProperties['leos.user.repository.searchbykey.uri']}")
    private String searchByKeyUri;

    @Value("#{integrationProperties['leos.user.repository.searchbyentitykey.uri']}")
    private String findByEntityKeyUri;

    @Autowired
    private RestOperations restTemplate;
    
    @Override
    public List<User> searchUsers(String searchKey)
    {
        Validate.notNull(searchKey, "Search Key must not be null!");

        final String uri = repositoryUrl +  searchByKeyUri;
        Map<String, String> params = new HashMap<String, String>();
        params.put("searchKey", searchKey);

        List<UserJSON> results = null;
        try {
            ResponseEntity<List<UserJSON>> entity = restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<List<UserJSON>>() {}, params);
            results = entity.getBody();
        } catch (RestClientException e) {
            LOG.warn("Exception while getting searching user. Failed calling: {}, Exception: () ",  uri, e.getMessage());
            throw new RuntimeException("Unable to search for user", e);
        }
        List<User> users = (List<User>) (List<? extends User>) results;
        return users;
    }
    
    @Override
    public User getUserByLogin(String userId)
    {
        final String uri = repositoryUrl + findByLoginUri;
        Validate.notNull(userId, "User ID must not be null!");
        Map<String, String> params = new HashMap<String, String>();
        params.put("userId", userId);
        UserJSON result = null;
        try {
            LOG.debug("Searching for user: {}", userId);
            result = restTemplate.getForObject(uri, UserJSON.class, params);
        } catch (RestClientException e) {
            LOG.warn("Exception while getting user by login. Failed calling: {}, Exception: () ",  uri, e.getMessage());
            throw new RuntimeException("Unable to look at user with login ", e);
        }

        return result;
    }

    @Override
    public List<String> searchUsersByEntityIdAndKey(String entity, String searchKey)
    {
        final String uri = repositoryUrl + findByEntityKeyUri;

        Validate.notNull(entity, "Entity must not be null!");
        Validate.notNull(searchKey, "Search Key must not be null!");

        Map<String, String> params = new HashMap<String, String>();
        params.put("entity", entity);
        params.put("searchKey", searchKey);

        List<String> results = null;
        try {
            results = restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>() {}).getBody();
        } catch (RestClientException e) {
            LOG.warn("Exception while searching for users in an entity. Failed calling: {}, Exception: () ",  uri, e.getMessage());
            throw new RuntimeException("Unable to search for users in an entity ", e);
        }

        return results;
    }
}
