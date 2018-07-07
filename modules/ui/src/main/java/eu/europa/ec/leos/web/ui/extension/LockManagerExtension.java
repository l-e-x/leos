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
package eu.europa.ec.leos.web.ui.extension;

import com.vaadin.annotations.JavaScript;
import com.vaadin.server.AbstractClientConnector;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.vo.UserVO;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.web.model.LockVO;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@JavaScript({"vaadin://../js/web/lockManagerConnector.js" + LeosCacheToken.TOKEN })
public class LockManagerExtension extends LeosJavaScriptExtension {

    private static final Logger LOG = LoggerFactory.getLogger(LockManagerExtension.class);

    public LockManagerExtension(User user) {
        getState().user = createUserVO(user);
    }

    public void extend(AbstractClientConnector target) {
        super.extend(target);
    }

    @Override
    protected LockManagerState getState() {
        return (LockManagerState) super.getState();
    }

    @Override
    protected LockManagerState getState(boolean markAsDirty) {
        return (LockManagerState) super.getState(markAsDirty);
    }

    public void updateLocks(List<LockData> locks) {
        LOG.trace("Updating locks...");
        getState().locks = createLockVOs(locks);
    }

    private List<LockVO> createLockVOs(List<LockData> locks) {
        List<LockVO> lockVOs = new ArrayList<>();
        if (locks != null) {
            for (LockData lock : locks) {
                lockVOs.add(new LockVO(lock));
            }
        }
        return lockVOs;
    }

    private UserVO createUserVO(User user){
        return new UserVO(user);
    }
}