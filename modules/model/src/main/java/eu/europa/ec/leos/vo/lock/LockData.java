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
package eu.europa.ec.leos.vo.lock;

import java.util.Objects;

public final class LockData  {

    private final String lockId;//mandatory
    private final String userLoginName;//mandatory
    private final LockLevel lockLevel;//mandatory

    private final String userName;//optional
    private final String sessionId;//optional
    private final String elementId;//optional but mandatory for element type lock
    private final Long lockingAcquiredOn; // in milliseconds



    public LockData(String lockId, Long lockingAcquiredOn, String userLoginName, String userName, String sessionId, LockLevel lockLevel, String elementId) {
        this.lockId = lockId;
        this.userLoginName = userLoginName;
        this.lockLevel = lockLevel;
        this.elementId =elementId;
        this.lockingAcquiredOn = lockingAcquiredOn;
        this.sessionId = sessionId;
        this.userName = userName;
    }

    public LockData(String lockId, Long lockingAcquiredOn, String userLoginName, String userName, String sessionId, LockLevel lockLevel) {
    	this(lockId,  lockingAcquiredOn,  userLoginName,  userName,  sessionId, lockLevel, null);
    }
    
	public String getLockId() {
        return lockId;
    }

    public Long getLockingAcquiredOn() {
        return lockingAcquiredOn;
    }

    public String getUserLoginName() {
        return userLoginName;
    }

    public String getUserName() {
        return userName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getElementId() {
        return elementId;
    }

    public LockLevel getLockLevel() {
        return lockLevel;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final LockData other = (LockData) obj;
        return Objects.equals(this.lockId, other.lockId)
                && Objects.equals(this.userLoginName, other.userLoginName)
                && Objects.equals(this.lockLevel, other.lockLevel)
                && ( 	//we need to check article equality only when lock is Element level
                        ( LockLevel.DOCUMENT_LOCK.equals(this.lockLevel) ||LockLevel.READ_LOCK.equals(this.lockLevel))
                        ||(LockLevel.ELEMENT_LOCK.equals(this.lockLevel) &&  Objects.equals(this.elementId, other.elementId)) 
                        )
                        && (other.sessionId ==null || other.sessionId.equals(this.sessionId)) ;//added as unlock on read was removing all the read locks by same user

    }

    @Override
    public int hashCode() {
        return Objects.hash(lockId, userLoginName, lockLevel, elementId);
    }
}
