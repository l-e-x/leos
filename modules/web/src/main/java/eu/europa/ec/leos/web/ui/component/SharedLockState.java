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

import java.util.List;

import com.vaadin.shared.ui.JavaScriptComponentState;

import eu.europa.ec.leos.web.model.LockVO;
/**
 * This class contains the lock state which is shared with client javascript side to enable or disable article editing.
 */
public class SharedLockState extends JavaScriptComponentState  {
    
    private static final long serialVersionUID = 10001L;
    
    private List<LockVO> lstLockVO;

    public List<LockVO> getLocks() {
        return lstLockVO;
    }

    public void setLocks(List<LockVO> lstLockVO) {
        this.lstLockVO = lstLockVO;
    }
}
