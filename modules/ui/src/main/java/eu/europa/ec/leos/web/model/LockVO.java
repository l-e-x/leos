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
package eu.europa.ec.leos.web.model;

import eu.europa.ec.leos.vo.lock.LockData;

import java.io.Serializable;
import java.util.Date;

public class LockVO implements Serializable {

    private static final long serialVersionUID = 2208198405431L;

    private String lockId;
    private String userLogin;
    private String lockLevel;
    private String userName;
    private String elementId;
    private Date lockAcquiredOn;

    public LockVO() {
        //mandatory constructor
    }

    public LockVO(LockData lockData) {
        lockId = lockData.getLockId();
        userLogin = lockData.getUserLoginName();
        lockLevel = lockData.getLockLevel().toString();
        userName = lockData.getUserName();
        elementId = lockData.getElementId();
        lockAcquiredOn = new Date(lockData.getLockingAcquiredOn());
    }

    public String getLockId() {
        return lockId;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public String getLockLevel() {
        return lockLevel;
    }

    public String getUserName() {
        return userName;
    }

    public String getElementId() {
        return elementId;
    }

    public Date getLockAcquiredOn() {
        return lockAcquiredOn;
    }

    public void setLockAcquiredOn(Date lockAcquiredOn) {
        this.lockAcquiredOn = lockAcquiredOn;
    }

    public void setLockId(String lockId) {
        this.lockId = lockId;
    }

    public void setLockLevel(String lockLevel) {
        this.lockLevel = lockLevel;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }
}