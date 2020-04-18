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
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.services.toc.StructureContext;
import eu.europa.ec.leos.ui.component.ComparisonComponent;
import eu.europa.ec.leos.ui.component.markedText.MarkedTextComponent;
import eu.europa.ec.leos.ui.component.versions.VersionComparator;
import eu.europa.ec.leos.ui.component.versions.VersionsTab;
import eu.europa.ec.leos.ui.extension.ActionManagerExtension;
import eu.europa.ec.leos.ui.extension.LeosEditorExtension;
import eu.europa.ec.leos.ui.window.toc.TocEditor;
import eu.europa.ec.leos.web.event.component.LayoutChangeRequestEvent;
import eu.europa.ec.leos.web.event.view.document.FetchUserPermissionsResponse;
import eu.europa.ec.leos.web.event.view.document.InstanceTypeResolver;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.screen.document.ColumnPosition;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Provider;
import java.util.List;

@ViewScope
@SpringComponent
@Instance(instances = {InstanceType.COMMISSION, InstanceType.OS})
class ProposalMemorandumScreenImpl extends MemorandumScreenImpl {

    private static final long serialVersionUID = 1L;

    protected LeosEditorExtension leosEditorExtension;
    protected ActionManagerExtension actionManagerExtension;

    private MarkedTextComponent markedTextComponent;

    @Autowired
    ProposalMemorandumScreenImpl(SecurityContext securityContext, EventBus eventBus, MessageHelper messageHelper, ConfigurationHelper cfgHelper,
                                 UserHelper userHelper, TocEditor tocEditor, InstanceTypeResolver editElementResponseEventCreator, VersionsTab versionsTab,
                                 Provider<StructureContext> structureContextProvider, PackageService packageService,
                                 MarkedTextComponent markedTextComponent, VersionComparator versionComparator) {
        super(securityContext, eventBus, messageHelper, cfgHelper, userHelper, tocEditor, editElementResponseEventCreator, versionsTab,
                structureContextProvider, packageService, versionComparator);
        this.markedTextComponent = markedTextComponent;
    }
    
    @Override
    public void init() {
        super.init();
        actionsMenuBar.setChildComponentClass(MarkedTextComponent.class);
        screenLayoutHelper.addPane(comparisonComponent, 2, false);
        screenLayoutHelper.layoutComponents();
    }

    @Subscribe
    void changePosition(LayoutChangeRequestEvent event) {
        changeLayout(event, markedTextComponent);
    }

    @Override
    public void cleanComparedContent() {
        final String versionInfo = messageHelper.getMessage("document.compare.version.caption.simple");
        markedTextComponent.populateMarkedContent("", LeosCategory.MEMORANDUM, versionInfo);
    }

    @Override
    public void showVersion(String content, String versionInfo) {
        changePosition(new LayoutChangeRequestEvent(ColumnPosition.DEFAULT, ComparisonComponent.class));

        markedTextComponent.populateMarkedContent(content.replaceAll("(?i) id=\"", " id=\"marked-"), LeosCategory.MEMORANDUM, versionInfo);
        markedTextComponent.hideCompareButtons();
    }

    @Override
    public void populateComparisonContent(String comparedContent, String comparedInfo) {
        markedTextComponent.populateMarkedContent(comparedContent, LeosCategory.MEMORANDUM, comparedInfo);
        markedTextComponent.showCompareButtons();
    }

    @Override
    public void populateDoubleComparisonContent(String comparedContent, String versionInfo) {
        throw new IllegalArgumentException("Operation not valid");
    }

    @Override
    public void scrollToMarkedChange(String elementId) {
        markedTextComponent.scrollToMarkedChange(elementId);
    }

    @Override
    public void setPermissions(DocumentVO memorandum){ 
        boolean enableUpdate = this.securityContext.hasPermission(memorandum, LeosPermission.CAN_UPDATE);
        actionsMenuBar.setIntermediateVersionVisible(enableUpdate);
        tableOfContentComponent.setPermissions(false);
        // add extensions only if the user has the permission.
        if(enableUpdate) {
            if(leosEditorExtension == null) {
                leosEditorExtension = new LeosEditorExtension<>(memorandumContent, eventBus, cfgHelper, structureContextProvider.get().getTocItems(), null, getDocuments(memorandum), memorandum.getMetadata().getInternalRef());
            }
            if(actionManagerExtension == null) {
                actionManagerExtension = new ActionManagerExtension<>(memorandumContent, instanceTypeResolver.getInstanceType(), eventBus, structureContextProvider.get().getTocItems());
            }
        }
    }

    @Override
    public void sendUserPermissions(List<LeosPermission> userPermissions) {
        eventBus.post(new FetchUserPermissionsResponse(userPermissions));
    }

    private List<XmlDocument> getDocuments(DocumentVO document) {
        LeosPackage leosPackage = packageService.findPackageByDocumentId(document.getId());
        return packageService.findDocumentsByPackagePath(leosPackage.getPath(), XmlDocument.class, false);
    }

}
