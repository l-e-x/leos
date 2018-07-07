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
package eu.europa.ec.leos.web.ui.component;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.annotations.JavaScript;
import com.vaadin.ui.AbstractJavaScriptComponent;

import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.web.model.LockVO;
import eu.europa.ec.leos.web.support.LeosCacheToken;

@JavaScript({"vaadin://js/web/connector/sharedLockConnector.js" + LeosCacheToken.TOKEN })
public class SharedLockComponent extends AbstractJavaScriptComponent {

    private static final long serialVersionUID = 22081984L;

    public void setLocks(List<LockData> lstLockInfo) {
        ArrayList<LockVO> arrayList=new ArrayList<LockVO>();
        for (LockData lockData : lstLockInfo) {
            arrayList.add(new LockVO(lockData));    
        } 
        getState().setLocks(arrayList); 
    }
    
    public List<LockVO> getLocks() {
        return getState().getLocks();
    }
    
    @Override
    protected SharedLockState getState() {
        return (SharedLockState) super.getState();
    }

}
