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

import eu.europa.ec.leos.annotate.model.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache for temporarily storing user detail information to avoid unnecessarily repeating calls to
 *   the external UD-repo within a short time frame
 * The cache cleans itself during first access after a given time period  
 */
@Component
public class UserDetailsCache {

    private static final Logger LOG = LoggerFactory.getLogger(UserDetailsCache.class);

    private final Map<String, UserDetails> userDetailsCache;
    private LocalDateTime nextCacheCleanupTime = null; // time after which cache should be wiped

    // time in minutes after which cache should be wiped
    private final static int USER_DETAILS_CACHE_CLEANUP_TIME_MINUTES = 10;

    // -------------------------------------
    // Constructor
    // -------------------------------------
    public UserDetailsCache() {
        this.userDetailsCache = new HashMap<String, UserDetails>();
    }

    // -------------------------------------
    // Service functionality
    // -------------------------------------

    /**
     * retrieve a {@link UserDetails} object previously cached
     * note: might initiate a cache clean, if necessary
     * 
     * @param login the login serving as cache key
     * @return found UserDetails object, or null
     */
    public UserDetails getCachedUserDetails(final String login) {

        if (StringUtils.isEmpty(login)) {
            LOG.warn("Cannot search for cached user details based on empty login");
            return null;
        }

        if (nextCacheCleanupTime == null) {
            // initially schedule the first cleanup
            setNextCacheCleanupInterval();

        } else if (nextCacheCleanupTime.isBefore(LocalDateTime.now())) {

            // time for cleaning the cache (we do not want it to grow forever)
            LOG.debug("User details cache will be cleared now");
            clear();
            setNextCacheCleanupInterval();
        }

        return userDetailsCache.get(login);
    }

    /**
     * add a given {@link UserDetails} object to cache
     * 
     * @param login the user's login, serving as cache key
     * @param details the details of the user
     */
    public void cache(final String login, final UserDetails details) {

        if (StringUtils.isEmpty(login)) {
            LOG.warn("Cannot cache user details without key");
            return;
        }

        if (details == null) {
            LOG.warn("Cannot cache empty user details for key {}", login);
            return;
        }

        userDetailsCache.put(login, details);
    }

    /**
     * clean the cache
     */
    public void clear() {
        userDetailsCache.clear();
    }

    /**
     * report the number of cached items
     * 
     * @return number of cached items
     */
    public int size() {
        return userDetailsCache.size();
    }

    /**
     * set the next cleanup time to a certain timestamp
     * 
     * @param nextTime date/time of next scheduled cleanup
     */
    public void setNextCacheCleanupTime(final LocalDateTime nextTime) {
        
        if(nextTime != null) {
            nextCacheCleanupTime = nextTime;
        }
    }
    /**
     *  schedule next time after which user details cache should be cleaned
     */
    private void setNextCacheCleanupInterval() {

        nextCacheCleanupTime = LocalDateTime.now().plusMinutes(USER_DETAILS_CACHE_CLEANUP_TIME_MINUTES);
        LOG.debug("Next user cache cleanup time scheduled for {}", nextCacheCleanupTime);
    }
}
