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
package eu.europa.ec.leos.web.model;

import java.util.List;

import eu.europa.ec.leos.model.content.LeosObjectProperties;
import eu.europa.ec.leos.vo.lock.LockData;

public class DocumentVO {

    public static enum LockState {UNLOCKED, LOCKED};
    private LeosObjectProperties leosObjectProperties;
    private List<LockData> arrLockInfo;
    private String msgForUser;
    private String documentId;
    private LockState lockState;

    public LockState getLockState() {
        return lockState;
    }

    public void setLockState(LockState lockState) {
        this.lockState = lockState;
    }

    public LeosObjectProperties getLeosObjectProperties() {
        return leosObjectProperties;
    }

    public void setLeosObjectProperties(LeosObjectProperties leosObjectProperties) {
        this.leosObjectProperties = leosObjectProperties;
    }

    public List<LockData> getLockInfo() {
        return arrLockInfo;
    }

    public void setLockInfo(List<LockData> arrLockInfo) {
        this.arrLockInfo = arrLockInfo;
    }

    public String getMsgForUser() {
        return msgForUser;
    }

    public void setMsgForUser(String msgForUser) {
        this.msgForUser = msgForUser;
    }
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

}
