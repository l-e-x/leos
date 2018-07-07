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
package eu.europa.ec.leos.services.locking.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import eu.europa.ec.leos.vo.lock.LockData;

@Component
@Scope("singleton")
public class LockRepository implements ReadableRepository, EditableLockRepository{

    //Data Structure to store the locks
    private ConcurrentHashMap<String, ArrayList<LockData>> lockingMap = new ConcurrentHashMap<String, ArrayList<LockData>>();

    @Override
    public LockData store(LockData lock ){
        String lockId= lock.getLockId();
        ArrayList<LockData> documentLocks =lockingMap.get(lockId)==null? new ArrayList<LockData>():lockingMap.get(lockId);
        lockingMap.put(lockId, documentLocks);
        return documentLocks.add(lock)?lock:null;
    }

    @Override
    public LockData remove(LockData lock ){
        String lockId= lock.getLockId();
        boolean removed=false;
        LockData removedLock=null;
        ArrayList<LockData> documentLocks =lockingMap.get(lockId);

        if (documentLocks==null)
            return null;

        Iterator<LockData> iterator = documentLocks.iterator();
        while (iterator.hasNext()){
            LockData exisingLock = iterator.next();
            if(exisingLock.equals(lock)){
                removedLock=exisingLock;
                iterator.remove();
                removed=true;
                //No Break.remove all the locks with same info. ideally there shouldn't be more than one.  
            }
        }

        if(documentLocks.isEmpty()){//clean up of map 
            lockingMap.remove(lockId);
        }
        return removed?removedLock:null;
    }

    @Override
    public  List<LockData> getCurrentLocks(String lockId) {
        Validate.notNull(lockId, "The document id must not be null!");
        return  Collections.unmodifiableList(lockingMap.get(lockId)==null
                ?new ArrayList<LockData>()
                        :(List<LockData>)lockingMap.get(lockId).clone());
    }

    @Override
    public List<LockData> getAllLocks() {
        Collection<ArrayList<LockData>> lockInfoList = lockingMap.values();
        ArrayList<LockData> allLocks = new ArrayList<LockData>();
        for (ArrayList<LockData> arrlockInfo : lockInfoList) {
            allLocks.addAll(arrlockInfo);
        }
        return Collections.unmodifiableList(allLocks);
    }
    
}
