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
package eu.europa.ec.leos.services.locking.handler;

import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockData;

/**
 * @author kaushvi
 *
 */

public interface LockHandler {
    /**
     * This method checks and allocates the lock on a document for the user,session as passed in input
     * @param lockData this object contains the information about the lock which is to be allocated 
     * @return information about if operation was successful(Lock allocated)+ current locks
     */
    LockActionInfo acquireLock(LockData lockData);
    /**
     * This method releases the lock for the user, session, lockId passed in input
     * @param lockData this object contains the information about the lock which is to be released 
     * @return information about if operation was successful(Lock deallocated)+ current locks after deallocation
     */
    LockActionInfo releaseLock(LockData lockData);
    /**
     * This method checks if the lock with same contents exists for combination of info user,  lockId,etc passed in input
     * if session id is not mandatory.if it is passed it will be used for comparision of locks else it would be excluded 
     * @param lockData this object contains the information about the lock which is to be allocated 
     * @return information about if operation was successful(Lock present)+ current locks
     */
    LockActionInfo checkIfLockPresent(LockData lockData);

}
