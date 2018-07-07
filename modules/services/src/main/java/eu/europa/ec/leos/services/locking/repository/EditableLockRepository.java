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
package eu.europa.ec.leos.services.locking.repository;

import eu.europa.ec.leos.vo.lock.LockData;

public interface EditableLockRepository  extends ReadableRepository {
    /**This method stores the locks in repository. It is a dumb store so upper layer needs to make functional Checks.
     * @param lock
     * @return lock if the lock is successfully added to repository else null
     */
    public LockData store(LockData lock );


    /**This method removes the lock from repository. It is a dumb remove so upper layer needs to make functional Checks.
     * @param lock
     * @return removed lockinfo if the lock is successfully removed from repository else null
     */
    public LockData remove(LockData lock);
}
