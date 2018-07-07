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
package eu.europa.ec.leos.web.support;

import com.google.common.eventbus.EventBus;
import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.locking.LockingService;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.vo.lock.LockLevel;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.NotificationEvent.Type;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**This class is intended to help presenters with locks and encapsulate locking implementation
 * This class shall not make any changes to UI doirectly but request Presenter to make the change */
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

    @Autowired
    private MessageHelper messageHelper;

    private static final String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT);

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

    /** get doc id fvorm session*/
    public boolean lockDocument(){
        return lockDocument(getDocumentId());
    }

    public boolean lockDocument(String documentId){
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

    /** get doc id fvorm session*/
    public boolean unlockDocument() {
        return unlockDocument(getDocumentId());
    }

    public boolean unlockDocument(String documentId) {
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
            eventBus.post(new NotificationEvent(Type.ERROR, "document.locked", lockingInfo.getUserName(), lockingInfo.getUserLoginName(),
                    dateFormatter.format(new Date(lockingInfo.getLockingAcquiredOn()))));
        }
        else if(!elementLocks.isEmpty()){
            LockData lockingInfo = elementLocks.get(0);//TODO need to check the appropriate lock to send
            eventBus.post(new NotificationEvent(Type.ERROR, "document.locked.article", lockingInfo.getUserName(), lockingInfo.getUserLoginName(), lockingInfo.getElementId(),
                    dateFormatter.format(new Date(lockingInfo.getLockingAcquiredOn()))));
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

    public String constructUserNote(String leosId, List<LockData> arrLocks){
        StringBuilder sb= new StringBuilder();
        for(LockData lockData: arrLocks){
            if(messageHelper!=null){
                switch (lockData.getLockLevel()){
                    case READ_LOCK:
                        sb.append(messageHelper.getMessage("document.locked.read", lockData.getUserName(), lockData.getUserLoginName(),
                                dateFormatter.format(new Date(lockData.getLockingAcquiredOn()))));
                        break;
                    case ELEMENT_LOCK:
                        sb.append(messageHelper.getMessage("document.locked.article", lockData.getUserName(), lockData.getUserLoginName(),lockData.getElementId(),
                                dateFormatter.format(new Date(lockData.getLockingAcquiredOn()))));
                        break;
                    case DOCUMENT_LOCK:
                        sb.append(messageHelper.getMessage("document.locked", lockData.getUserName(), lockData.getUserLoginName(),
                                dateFormatter.format(new Date(lockData.getLockingAcquiredOn()))));
                        break;
                }//end switch
                sb.append("<br> ");
            }
        }
        return sb.toString();
    }
}
