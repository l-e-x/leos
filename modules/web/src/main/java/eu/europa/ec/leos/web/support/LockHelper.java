/**
 * Copyright 2015 European Commission
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
package eu.europa.ec.leos.web.support;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.locking.LockingService;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.vo.lock.LockLevel;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.NotificationEvent.Type;

@Component
public class LockHelper {

    @Autowired
    private LockingService lockingService;

    @Autowired
    private SecurityContext leosSecurityContext;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private HttpSession session;

    private static final  String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";

    public boolean lockElement(String elementId){
        String documentId = getDocumentId();
        if (documentId == null) {
            return false;
        }
        User user = leosSecurityContext.getUser();
        LockActionInfo lockActionInfo = lockingService.lockDocument(documentId, user, session.getId(), LockLevel.ELEMENT_LOCK, elementId);

        if (!lockActionInfo.sucesss()) {
            handleLockFailure(lockActionInfo);
        }
        return lockActionInfo.sucesss(); 
    }

    public boolean lockDocument(){
        String documentId = getDocumentId();
        if (documentId == null) {
            return false;
        }
        
        User user = leosSecurityContext.getUser();
        LockActionInfo lockActionInfo = lockingService.lockDocument(documentId, user, session.getId(), LockLevel.DOCUMENT_LOCK);
        if (!lockActionInfo.sucesss()) {
            handleLockFailure(lockActionInfo);
        }
        return lockActionInfo.sucesss();
    }

    public boolean unlockDocument() {
        String documentId = getDocumentId();
        if (documentId == null) {
            return false;
        }
        User user = leosSecurityContext.getUser();
        LockActionInfo lockActionInfo = lockingService.unlockDocument(documentId, user.getLogin(), session.getId(), LockLevel.DOCUMENT_LOCK);
        if (!lockActionInfo.sucesss()) {
            handleLockFailure(lockActionInfo);
        }
        return lockActionInfo.sucesss();
    }

    public boolean unlockElement( String elementId){
        String documentId = getDocumentId();
        if (documentId == null) {
            return false;
        }
        User user = leosSecurityContext.getUser();
        LockActionInfo lockActionInfo = lockingService.unlockDocument(documentId, user.getLogin(), session.getId(), LockLevel.ELEMENT_LOCK, elementId);

        if (!lockActionInfo.sucesss()) {
            handleLockFailure(lockActionInfo);
        }
        return lockActionInfo.sucesss();
    }


    public void handleLockFailure(LockActionInfo lockActionInfo){

        //process failure
        List<LockData> elementLocks = new ArrayList<LockData>();
        List<LockData> documentLocks = new ArrayList<LockData>();

        for(LockData lockData:lockActionInfo.getCurrentLocks()){
            switch (lockData.getLockLevel()) {
                case READ_LOCK:
                    break;
                case ELEMENT_LOCK:
                    elementLocks.add(lockData);
                    break;
                case DOCUMENT_LOCK:
                    documentLocks.add(lockData);
                    break;
                default:
                    break;
            }
        }

        if(!documentLocks.isEmpty()){
            LockData lockingInfo = documentLocks.get(0);//Max one docuemnt level lock is expected
            eventBus.post(new NotificationEvent(Type.WARNING, "document.locked", lockingInfo.getUserName(), lockingInfo.getUserLoginName(),
                    (new SimpleDateFormat(DATE_FORMAT)).format(new Date(lockingInfo.getLockingAcquiredOn()))));
        }
        else if(!elementLocks.isEmpty()){
            LockData lockingInfo = elementLocks.get(0);//TODO need to check the appropriate lock to send
            eventBus.post(new NotificationEvent(Type.WARNING, "document.locked.article", lockingInfo.getUserName(), lockingInfo.getUserLoginName(), lockingInfo.getElementId(),
                    (new SimpleDateFormat(DATE_FORMAT)).format(new Date(lockingInfo.getLockingAcquiredOn()))));
        }//end else
    }//end method 

    public  boolean isDocumentLockedFor() {
        String documentId = getDocumentId();
        boolean isDocumentLockedFor =  documentId != null 
                                         && lockingService.isDocumentLockedFor(documentId, leosSecurityContext.getUser().getLogin(), session.getId());
        return isDocumentLockedFor;
    }

    public boolean isElementLockedFor(String elementId) {
        String documentId = getDocumentId();
        boolean isElementLockedFor = documentId != null 
                                        && lockingService.isElementLockedFor(documentId, leosSecurityContext.getUser().getLogin(), session.getId(), elementId);
        return isElementLockedFor;
    }

    private String getDocumentId() {
        return (String) session.getAttribute(SessionAttribute.DOCUMENT_ID.name());
    }
}
