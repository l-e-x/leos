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

import java.util.List;

public final class LockActionInfo {
    
    public static enum Operation {
        ACQUIRE("acquire"),
        RELEASE("release"),
        EXISTS("exists");
        
        private final String value;
        Operation(String value) {
            this.value=value;
        }
        public String getValue(){
            return value;
        }
    }
    
    private final boolean isSucessful;
    private final Operation operation;

    private final LockData lock;//lock on which action was performed
    private final List<LockData> currentLocks;//current/remaining list of locks after operation.
    
    public LockActionInfo(boolean isSucessful,Operation operation, LockData lock, List<LockData> currentLocks) {
        this.isSucessful = isSucessful;
        this.operation=operation;
        this.lock = lock;
        this.currentLocks = currentLocks;
    }

    public boolean sucesss() {
        return isSucessful;
    }

    public Operation getOperation() {
        return operation;
    }
    
    //lock on which operation was present. contains info about removed lock or added locks or for which operation was performed 
    public LockData getLock() {
        return lock;
    }
    
    // List of locks after operation
    public List<LockData> getCurrentLocks() {
        return currentLocks;
    }
}
