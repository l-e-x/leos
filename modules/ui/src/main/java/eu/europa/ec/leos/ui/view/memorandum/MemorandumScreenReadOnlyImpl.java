/*
 * Copyright 2019 European Commission
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
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.web.event.view.document.InstanceTypeResolver;
import eu.europa.ec.leos.web.event.view.document.FetchUserPermissionsResponse;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.component.actions.MemorandumActionsMenuBar;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@ViewScope
@SpringComponent
@Instance(InstanceType.COUNCIL)
class MemorandumScreenReadOnlyImpl extends MemorandumScreenImpl {

    private static final long serialVersionUID = 1L;

    @Autowired
    MemorandumScreenReadOnlyImpl(SecurityContext securityContext, EventBus eventBus, MessageHelper messageHelper, ConfigurationHelper cfgHelper,
            UserHelper userHelper, InstanceTypeResolver instanceTypeResolver, MemorandumActionsMenuBar actionsMenuBar) {
        super(securityContext, eventBus, messageHelper, cfgHelper, userHelper, instanceTypeResolver);
    }

    @Override
    public void setPermissions(DocumentVO memorandum){
        actionsMenuBar.setMajorVersionVisible(false);
        memorandumToc.setPermissions(false);
    }

    @Override
    public void sendUserPermissions(List<LeosPermission> userPermissions) {
        userPermissions = userPermissions.stream().filter(permission -> permission.compareTo(LeosPermission.CAN_SUGGEST) != 0).collect(Collectors.toList());
        eventBus.post(new FetchUserPermissionsResponse(userPermissions));
    }

    @Override
    public void populateMarkedContent(String markedContent) {
    }

    @Override
    public void scrollToMarkedChange(String elementId) {
    }
}
