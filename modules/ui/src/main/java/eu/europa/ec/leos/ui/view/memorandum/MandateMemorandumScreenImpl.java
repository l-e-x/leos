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
import com.google.common.eventbus.Subscribe;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.services.toc.StructureContext;
import eu.europa.ec.leos.ui.component.versions.VersionComparator;
import eu.europa.ec.leos.ui.component.versions.VersionsTab;
import eu.europa.ec.leos.ui.window.toc.TocEditor;
import eu.europa.ec.leos.web.event.component.LayoutChangeRequestEvent;
import eu.europa.ec.leos.web.event.view.document.FetchUserPermissionsResponse;
import eu.europa.ec.leos.web.event.view.document.InstanceTypeResolver;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Provider;
import java.util.List;
import java.util.stream.Collectors;

@ViewScope
@SpringComponent
@Instance(InstanceType.COUNCIL)
class MandateMemorandumScreenImpl extends MemorandumScreenImpl {

    private static final long serialVersionUID = 1L;

    @Autowired
    MandateMemorandumScreenImpl(SecurityContext securityContext, EventBus eventBus, MessageHelper messageHelper, ConfigurationHelper cfgHelper,
            UserHelper userHelper, TocEditor tocEditor, InstanceTypeResolver instanceTypeResolver, VersionsTab versionsTab,
            Provider<StructureContext> structureContext, PackageService packageService, VersionComparator versionComparator) {
        super(securityContext, eventBus, messageHelper, cfgHelper, userHelper, tocEditor, instanceTypeResolver,
                versionsTab, structureContext, packageService, versionComparator);
    }
    
    @Override
    public void showVersion(String content, String versionInfo) {
        throw new IllegalArgumentException("Operation not valid");
    }
    
    @Override
    public void cleanComparedContent() {
        throw new IllegalArgumentException("Operation not valid");
    }
    
    @Override
    public void populateComparisonContent(String comparedContent, String versionInfo) {
        throw new IllegalArgumentException("Operation not valid");
    }
    
    @Override
    public void populateDoubleComparisonContent(String comparedContent, String versionInfo) {
        throw new IllegalArgumentException("Operation not valid");
    }
    
    @Override
    public void setPermissions(DocumentVO memorandum){
        actionsMenuBar.setIntermediateVersionVisible(false);
        tableOfContentComponent.setPermissions(false);
    }

    @Override
    public void sendUserPermissions(List<LeosPermission> userPermissions) {
        userPermissions = userPermissions.stream().filter(permission -> permission.compareTo(LeosPermission.CAN_SUGGEST) != 0).collect(Collectors.toList());
        eventBus.post(new FetchUserPermissionsResponse(userPermissions));
    }
    
    @Subscribe
    void changePosition(LayoutChangeRequestEvent event) {
        screenLayoutHelper.changePosition(event.getPosition(), event.getOriginatingComponent());
    }
}
