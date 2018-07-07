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
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.vo.lock.LockLevel;
import eu.europa.ec.leos.vo.lock.LockActionInfo.Operation;

@Component("ELEMENT_LOCK")
public class ElementLockHandler implements LockHandler {

    @Autowired
    private EditableLockRepository lockRepository; 

    public LockActionInfo acquireLock(LockData lockData) {
        Validate.isTrue(LockLevel.ELEMENT_LOCK.equals(lockData.getLockLevel()), "Invalid Lock Type");

        LockActionInfo lockActionInfo= canAquireLock(lockData);

        if(lockActionInfo.sucesss()) {
            String lockId= lockData.getLockId();
            LockData addedLock = lockRepository.store(lockData);//dumb store
            lockActionInfo = new LockActionInfo(addedLock!=null, Operation.ACQUIRE, addedLock, lockRepository.getCurrentLocks(lockId));
        }
        return lockActionInfo;
    }

    @Override
    public LockActionInfo releaseLock(LockData lockData) {
        Validate.isTrue(LockLevel.ELEMENT_LOCK.equals(lockData.getLockLevel()), "Invalid Lock Type");

        String lockId= lockData.getLockId();
        LockData removedLock = lockRepository.remove(lockData);
        LockActionInfo lockActionInfo = new LockActionInfo(removedLock!=null,Operation.RELEASE, removedLock, lockRepository.getCurrentLocks(lockId));

        return lockActionInfo;
    }
  /**
  * This method checks if passed lock can be allocated on a Lockid (key) within application rules
  * @param lockData this object contains the information about the lock which is to be allocated 
  * @return information about if operation was successful(Lock can be allocated)+ already existing locks
  */
    private LockActionInfo canAquireLock(LockData lockData) {
        Validate.isTrue(LockLevel.ELEMENT_LOCK.equals(lockData.getLockLevel()), "Invalid Lock Type");

        String lockId= lockData.getLockId();
        List<LockData> documentLocks = lockRepository.getCurrentLocks(lockId);
        boolean isAvailable =true;

        for(LockData exisingLock: documentLocks){
            switch(exisingLock.getLockLevel()){
                case READ_LOCK :
                    continue;

                case DOCUMENT_LOCK :
                    isAvailable=false;
                    break;

                case ELEMENT_LOCK :
                    if(exisingLock.getElementId().equals(lockData.getElementId())){
                        isAvailable=false;
                    }
                    break;
                    
                default:
                    break;
            }//end switch
            
            if(!isAvailable){//break the loop
                break;
            }
        }//end for

        return new LockActionInfo(isAvailable,Operation.ACQUIRE, lockData, lockRepository.getCurrentLocks(lockId));
    }

    @Override
    public LockActionInfo checkIfLockPresent(LockData lockData) {
        Validate.isTrue(LockLevel.ELEMENT_LOCK.equals(lockData.getLockLevel()), "Invalid Lock Type");

        String lockId = lockData.getLockId();

        List<LockData> documentLocks = lockRepository.getCurrentLocks(lockId);
        for (LockData exisingLock : documentLocks) {
            if (exisingLock.getUserLoginName().equals(lockData.getUserLoginName())
                    && LockLevel.ELEMENT_LOCK.equals(exisingLock.getLockLevel())
                    && exisingLock.getElementId().equals(lockData.getElementId())
                    && (lockData.getSessionId()==null ||lockData.getSessionId().equals(exisingLock.getSessionId()))) {
                return new LockActionInfo(true,Operation.EXISTS, exisingLock,lockRepository.getCurrentLocks(lockId));
            }
        }
        return new LockActionInfo(false,Operation.EXISTS, lockData, lockRepository.getCurrentLocks(lockId));
    }

}
