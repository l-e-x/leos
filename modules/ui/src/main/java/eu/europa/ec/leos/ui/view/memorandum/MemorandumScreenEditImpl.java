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
import eu.europa.ec.leos.ui.extension.ActionManagerExtension;
import eu.europa.ec.leos.ui.extension.LeosEditorExtension;
import eu.europa.ec.leos.web.event.view.document.FetchUserPermissionsResponse;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@ViewScope
@SpringComponent
@Workflow(InstanceContext.Type.COMMISSION)
class MemorandumScreenEditImpl extends MemorandumScreenImpl {

    private static final long serialVersionUID = 1L;

    protected LeosEditorExtension leosEditorExtension;
    protected ActionManagerExtension actionManagerExtension;

    @Autowired
    MemorandumScreenEditImpl(SecurityContext securityContext, EventBus eventBus, MessageHelper messageHelper, ConfigurationHelper cfgHelper,
            UserHelper userHelper, InstanceContext instanceContext) {
        super(securityContext, eventBus, messageHelper, cfgHelper, userHelper, instanceContext);
    }

    @Override
    public void setPermissions(DocumentVO memorandum){ 
        boolean enableUpdate = this.securityContext.hasPermission(memorandum, LeosPermission.CAN_UPDATE);
        majorVersionButton.setVisible(enableUpdate);
        memorandumToc.setPermissions(false);
        // add extensions only if the user has the permission.
        if(enableUpdate) {
            if(leosEditorExtension == null) {
                leosEditorExtension = new LeosEditorExtension<>(memorandumContent, eventBus);
            }
            if(actionManagerExtension == null) {
                actionManagerExtension = new ActionManagerExtension<>(memorandumContent);
            }
        }
    }

    @Override
    public void sendUserPermissions(List<LeosPermission> userPermissions) {
        eventBus.post(new FetchUserPermissionsResponse(userPermissions));
    }
}
