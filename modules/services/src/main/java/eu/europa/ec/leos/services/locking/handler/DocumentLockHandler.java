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

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.europa.ec.leos.services.locking.repository.EditableLockRepository;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockActionInfo.Operation;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.vo.lock.LockLevel;

@Component("DOCUMENT_LOCK")
public class DocumentLockHandler implements LockHandler {

    @Autowired
    private EditableLockRepository lockRepository; 

    @Autowired
    private LockHandlerFactory lockHandlerFactory;

    public LockActionInfo acquireLock(LockData lockData) {
        Validate.isTrue(LockLevel.DOCUMENT_LOCK.equals(lockData.getLockLevel()), "Invalid Lock Type");
        String lockId= lockData.getLockId();

        LockActionInfo lockActionInfo= canAquireLock(lockData);///check if can be allocated if yes then allocated

        if(lockActionInfo.sucesss()) {
            LockData addedLock = lockRepository.store(lockData);//dumb store
            lockActionInfo = new LockActionInfo(addedLock!=null, Operation.ACQUIRE, addedLock, lockRepository.getCurrentLocks(lockId));
        }
        return lockActionInfo;
    }

    @Override
    public LockActionInfo releaseLock(LockData lockData) {
        Validate.isTrue(LockLevel.DOCUMENT_LOCK.equals(lockData.getLockLevel()), "Invalid Lock Type");

        String lockId= lockData.getLockId();
        //no check required for deallocated as if lock is not present then false will be returned by underlaying layer
        LockData removedLock = lockRepository.remove(lockData);
        LockActionInfo lockActionInfo = new LockActionInfo(removedLock!=null,Operation.RELEASE, removedLock, lockRepository.getCurrentLocks(lockId));
        return lockActionInfo;
    }
    
  /**
  * This method checks if passed lock can be allocated on a document within application rules
  * @param lockData this object contains the information about the lock which is to be allocated 
  * @return information about if operation was successful(Lock can be allocated)+ already existing locks
  */
   private LockActionInfo canAquireLock(LockData lockData) {
        Validate.isTrue(LockLevel.DOCUMENT_LOCK.equals(lockData.getLockLevel()), "Invalid Lock Type");

        boolean isAvailable =true;

        String lockId= lockData.getLockId();
        List<LockData> documentLocks = lockRepository.getCurrentLocks(lockId);

        forloop: for(LockData exisingLock: documentLocks){

            switch(exisingLock.getLockLevel()){
                case READ_LOCK :
                    break;

                case DOCUMENT_LOCK :
                    isAvailable=false;
                    break forloop;

                case ELEMENT_LOCK : 
                    isAvailable=false;
                    break forloop;
                default:
                    break;
            }
        }
        return new LockActionInfo(isAvailable, Operation.ACQUIRE, lockData, lockRepository.getCurrentLocks(lockId));
    }

    @Override
    public LockActionInfo checkIfLockPresent(LockData lockData) {
        Validate.isTrue(LockLevel.DOCUMENT_LOCK.equals(lockData.getLockLevel()), "Invalid Lock Type");

        String lockId = lockData.getLockId();
        List<LockData> documentLocks = lockRepository.getCurrentLocks(lockId);

        for (LockData exisingLock : documentLocks) {
            if (exisingLock.getUserLoginName().equalsIgnoreCase(lockData.getUserLoginName())
                    && LockLevel.DOCUMENT_LOCK.equals(exisingLock.getLockLevel())
                    && (lockData.getSessionId()==null ||lockData.getSessionId().equals(exisingLock.getSessionId()))) {
                return new LockActionInfo(true,Operation.EXISTS, exisingLock,lockRepository.getCurrentLocks(lockId));
            }
        }
        return new LockActionInfo(false,Operation.EXISTS, lockData, lockRepository.getCurrentLocks(lockId));
    }

}
