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

import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.locking.handler.LockHandler;
import eu.europa.ec.leos.services.locking.handler.LockHandlerFactory;
import eu.europa.ec.leos.services.locking.repository.ReadableRepository;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockActionInfo.Operation;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.vo.lock.LockLevel;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


@Service
public class LockingServiceImpl implements LockingService {

    @Autowired
    private LockUpdateBroadcaster lockUpdateBroadcaster;

    @Autowired
    private LockHandlerFactory lockHandlerFactory;

    @Autowired
    private ReadableRepository lockRepository; 

    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private static final Logger LOG = LoggerFactory.getLogger(LockingServiceImpl.class);

    @Override
    public synchronized LockActionInfo lockDocument(String lockId, User user, String sessionId, LockLevel lockLevel, String elementId) {

        Validate.notNull(lockId, "The lockId must not be null!");
        Validate.notNull(user, "User must not be null!");
        Validate.notNull(lockLevel, "Lock Level must be specified");
        Validate.isTrue( (LockLevel.ELEMENT_LOCK.equals(lockLevel) && elementId !=null ) ||
                (!LockLevel.ELEMENT_LOCK.equals(lockLevel)&& elementId ==null)  ,"Element Id is mandatory for Element Level Lock"); 

        LockData lockData =new LockData(lockId, System.currentTimeMillis(), user.getLogin(), user.getName(), sessionId, lockLevel, elementId);
        LockHandler lHandler=lockHandlerFactory.getHandler(lockLevel.toString());

        LockActionInfo lockActionInfo =lHandler.checkIfLockPresent(lockData);//check and allocate
        if(!lockActionInfo.sucesss()){// if same lock existing for same user and same session, return true
            lockActionInfo = lHandler.acquireLock(lockData);
            if(lockActionInfo.sucesss()){
                lockUpdateBroadcaster.broadcastLockUpdate(lockActionInfo);
            }
        }
        return lockActionInfo;
    }

    @Override
    public synchronized LockActionInfo lockDocument(String lockId, User user, String sessionId, LockLevel lockLevel) {
        Validate.isTrue( LockLevel.READ_LOCK.equals(lockLevel) 
                || LockLevel.DOCUMENT_LOCK.equals(lockLevel),
                "This method can be called with READ_LOCK or DOCUEMNT_LOCK");
        return lockDocument( lockId,  user,  sessionId,  lockLevel,  null);
    }

    @Override
    public synchronized LockActionInfo unlockDocument(String lockId, String userLogin,String sessionId, LockLevel lockLevel) {
        Validate.notNull(lockId, "The lockId must not be null!");
        Validate.notNull(userLogin, "The user login must not be null!");

        Validate.isTrue( LockLevel.READ_LOCK.equals(lockLevel) || LockLevel.DOCUMENT_LOCK.equals(lockLevel), 
                "This method can be called with READ_LOCK or DOCUEMNT_LOCK");

        return unlockDocument( lockId,  userLogin,  sessionId, lockLevel, null);
    }

    @Override
    public synchronized LockActionInfo unlockDocument(String lockId, String userLogin, String sessionId, LockLevel lockLevel, String elementId) {
        Validate.notNull(lockId, "The lockId must not be null!");
        Validate.notNull(userLogin, "userLogin must not be null!");
        Validate.notNull(lockLevel, "Lock Level must be specified");
        Validate.isTrue( (LockLevel.ELEMENT_LOCK.equals(lockLevel) && elementId !=null ) ||
                (!LockLevel.ELEMENT_LOCK.equals(lockLevel)&& elementId ==null)  ,"Element Id is mandatory for Element Level Lock"); 

        LockData lockData =new LockData(lockId, System.currentTimeMillis(), userLogin, null, sessionId, lockLevel, elementId);
        LockHandler lHandler=lockHandlerFactory.getHandler(lockLevel.toString());

        LockActionInfo lockActionInfo =lHandler.checkIfLockPresent(lockData);//check and deallocate
        if(lockActionInfo.sucesss()){
            lockActionInfo= lHandler.releaseLock(lockData);
            if(lockActionInfo.sucesss()){
                lockUpdateBroadcaster.broadcastLockUpdate(lockActionInfo);
            }
        }
        return lockActionInfo;
    }

    @Override
    public synchronized LockActionInfo releaseLocksForSession(String lockId, String sessionId){
        boolean operationStatus=true;
        LockActionInfo lockActionInfo = null;
        ArrayList<LockData> lstLockInfo=new  ArrayList<LockData>(); 

        List<LockData> docLocks = getLockingInfo(lockId);
        lstLockInfo.addAll(docLocks);

        for(LockData lockData:lstLockInfo){
            if(lockData.getSessionId().equals(sessionId)){//release all locks for session
                LockHandler lHandler=lockHandlerFactory.getHandler(lockData.getLockLevel().toString());
                lockActionInfo = lHandler.releaseLock(lockData);
                LOG.trace("Lock id {},Operation{}, status:{} ,", lockId, lockActionInfo.getOperation(), lockActionInfo.sucesss());
                operationStatus&=lockActionInfo.sucesss();
                if(lockActionInfo.sucesss()){
                    lockUpdateBroadcaster.broadcastLockUpdate(lockActionInfo);
                }
            }
        }

        return new LockActionInfo(operationStatus,Operation.RELEASE, null, getLockingInfo(lockId));
    }


    @Override
    public  List<LockData> getLockingInfo(String lockId) {
        Validate.notNull(lockId, "The lockId must not be null!");

        return  lockRepository.getCurrentLocks(lockId);
    }

    @Scheduled(fixedDelayString = "${maintenance.log.lock.millisec}")
    public void logAllLocks() {
        List<LockData> lockInfoList = lockRepository.getAllLocks();
        LOG.info("Logging all locks :");
        for(LockData lockData:lockInfoList){
            LOG.info("Lock with Id: " + lockData.getLockId() + " was acquired by " + lockData.getUserLoginName() + " on " +
                    dateFormatter.format(lockData.getLockingAcquiredOn()));
        }
    }

    @Scheduled(cron = "${maintenance.session.invalid.cron}")
    public void releaseAllLocksAtMidnight() {
        List<LockData> lockInfoList = lockRepository.getAllLocks();

        LOG.warn("Releasing all locks (via batch)");
        for(LockData lockData:lockInfoList){
            unlockDocument(lockData.getLockId(), lockData.getUserLoginName(), null, lockData.getLockLevel(), lockData.getElementId());
        }
    }

    @Override
    public void registerLockInfoBroadcastListener(LockUpdateBroadcastListener lockUpdateBroadcastListener) {
        lockUpdateBroadcaster.register(lockUpdateBroadcastListener);
    }

    @Override
    public void unregisterLockInfoBroadcastListener(LockUpdateBroadcastListener lockUpdateBroadcastListener) {
        lockUpdateBroadcaster.unregister(lockUpdateBroadcastListener);
    }

    @Override
    public boolean isDocumentLockedFor(String lockId, String userLogin, String sessionId) {
        Validate.notNull(lockId, "The lockId must not be null!");
        Validate.notNull(userLogin, "The userLogin must not be null!");

        LockData lockData = new LockData(lockId,  null, userLogin, null, sessionId, LockLevel.DOCUMENT_LOCK);

        LockHandler lHandler=lockHandlerFactory.getHandler(LockLevel.DOCUMENT_LOCK.toString());
        LockActionInfo lockActionInfo =lHandler.checkIfLockPresent(lockData);
        return lockActionInfo.sucesss();
    }

    @Override
    public boolean isElementLockedFor(String lockId, String userLogin, String sessionId, String elementId) {
        Validate.notNull(lockId, "The lockId must not be null!");
        Validate.notNull(elementId, "The element id must not be null!");
        Validate.notNull(userLogin, "The userLogin must not be null!");
        LockData lockData = new LockData(lockId, null, userLogin, null, sessionId, LockLevel.ELEMENT_LOCK, elementId);

        LockHandler lHandler=lockHandlerFactory.getHandler(LockLevel.ELEMENT_LOCK.toString());
        LockActionInfo lockActionInfo =lHandler.checkIfLockPresent(lockData);
        return lockActionInfo.sucesss();
    }
}
