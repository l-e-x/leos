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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

import eu.europa.ec.leos.vo.lock.LockActionInfo;

@Service
class LockUpdateBroadcaster implements Serializable {

    private static final long serialVersionUID = 5496281237174169812L;

    private static ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private LinkedList<LockUpdateBroadcastListener> listeners =
            new LinkedList<LockUpdateBroadcastListener>();

    public synchronized void register(
            LockUpdateBroadcastListener listener) {
        listeners.add(listener);
    }

    public synchronized void unregister(
            LockUpdateBroadcastListener listener) {
        listeners.remove(listener);
    }

    public synchronized void broadcastLockUpdate(final LockActionInfo lockActionInfo) {
        for (final LockUpdateBroadcastListener listener : listeners){
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    //call only for listeners on same lock or for repository listeners   
                    //we could do loop without this check and update all but that would be less efficient as listeners needs to check which events are relevent for them.
                    if(LockUpdateBroadcastListener.REPO_ID.equalsIgnoreCase(listener.getLockId()) 
                       || lockActionInfo.getLock().getLockId().equalsIgnoreCase(listener.getLockId())){
                            listener.onLockUpdate(lockActionInfo);
                    }
                }
            });
        }//end for
    }
}