/*
 * Copyright 2018 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.ui.view.memorandum;

import com.google.common.eventbus.EventBus;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import eu.europa.ec.leos.domain.common.InstanceContext;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.support.flow.Workflow;
import eu.europa.ec.leos.web.event.view.document.FetchUserPermissionsResponse;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@ViewScope
@SpringComponent
@Workflow(InstanceContext.Type.COUNCIL)
class MemorandumScreenReadOnlyImpl extends MemorandumScreenImpl {

    private static final long serialVersionUID = 1L;

    @Autowired
    MemorandumScreenReadOnlyImpl(SecurityContext securityContext, EventBus eventBus, MessageHelper messageHelper, ConfigurationHelper cfgHelper,
            UserHelper userHelper, InstanceContext instanceContext) {
        super(securityContext, eventBus, messageHelper, cfgHelper, userHelper, instanceContext);
    }

    @Override
    public void setPermissions(DocumentVO memorandum){ 
        majorVersionButton.setVisible(false);
        memorandumToc.setPermissions(false);
    }

    @Override
    public void sendUserPermissions(List<LeosPermission> userPermissions) {
        userPermissions = userPermissions.stream().filter(permission -> permission.compareTo(LeosPermission.CAN_SUGGEST) != 0).collect(Collectors.toList());
        eventBus.post(new FetchUserPermissionsResponse(userPermissions));
    }
}
