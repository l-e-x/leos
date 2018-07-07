/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.services.locking;

import java.util.List;

import javax.annotation.Nullable;

import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.vo.lock.*;

public interface LockingService {

    /**
     * Try to lock the document by the current user and returns the locking information of the actual lock. 
     * @param lockId
     * @param user User to lock the document for
     * @param sessionId of the session for which lock to release
     * @param lock level -READ_LOCK or DOCUMENT_LOCK
     * @return LockActionInfo containing the current locks, + the information if it was newly acquired
     */
    LockActionInfo lockDocument(String lockId, User user, String sessionId, LockLevel lockLevel);

    /**
     * Try to lock the element by the current user and returns the locking information of the actual lock if any 
     * other lock on the element or document level is not existing. 
     * @param lockId
     * @param user User to lock the document for
     * @param sessionId of the session for which lock to release
     * @param lock level possible value ELEMENT_LOCK
     * @param elementId To be locked
     * @return LockActionInfo containing the current lock, + the information if it was newly acquired
     */
    LockActionInfo lockDocument(String lockId, User user, String sessionId, LockLevel lockLevel, String elementId);

    /**
     * Try to unlock the document for the current user, session and level 
     * @param lockId
     * @param userLogin User to lock the document for
     * @param sessionId of the session for which lock to release
     * @param lock level READ_LOCK or DOCUMENT_LOCK
     * @return LockActionInfo containing the current locks after release+ the information if it was released
     */
    @Nullable
    LockActionInfo unlockDocument(String lockId, String userLogin, String sessionId, LockLevel lockLevel);

    /**
     * Try to unlock the document for the current user
     * @param lockId
     * @param userLogin User to lock the document for
     * @param sessionId of the session for which lock to release
     * @param lock level. possible value-ELEMENT_LOCK
     * @param elementId To be unlocked
     * @return LockActionInfo containing the current locks after release+ the information if it was released
     */
    @Nullable
    LockActionInfo unlockDocument(String lockId, String userLogin,String sessionId, LockLevel lockLevel, String elementId);

    /**
     * Try to unlock all the locks existing for that session for the document. 
     * (Assumption is that in single session user can work on single document).
     * @param lockId
     * @param session session id
     * @return LockActionInfo containing the current locks after release+ the information if it was released
     */
    @Nullable
    LockActionInfo releaseLocksForSession(String lockId, String sessionId);

    /**
     * Retrieve the unmodifiable lockInfo for the the given lockId
     * @param lockId
     * @return list of lockInfo 
     */
    @Nullable
    List<LockData> getLockingInfo(String lockId);

    /**
     * Register a listener to be informed about updated on the document locks
     * @param lockUpdateBroadcastListener
     */
    void registerLockInfoBroadcastListener(LockUpdateBroadcastListener lockUpdateBroadcastListener);

    /**
     * UnRegister a listener 
     * @param lockUpdateBroadcastListener
     */
    void unregisterLockInfoBroadcastListener(LockUpdateBroadcastListener lockUpdateBroadcastListener);

    /**
     * Check if a Document is locked by a given user in same Session/ if session id is passed then it is used for comparison else it is not used to identify the lock
     * 
     * @param lockId 
     * @param userLogin id
     * @param session session id
     * @return true if there is a lock for the given document by userLogin
     */
    boolean isDocumentLockedFor(String lockId, String userLogin, String sessionId );
    /**
     * Check if a element is locked by a given user in same Session/ if session id is passed then it is used for comparison else it is not used to identify the lock
     * 
     * @param lockId 
     * @param userLogin id
     * @param session session id
     * @param element id
     * @return true if there is a lock for element by userLogin
     */
    boolean isElementLockedFor(String lockId, String userLogin, String sessionId, String elementId);

}
