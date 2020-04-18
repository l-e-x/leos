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
package eu.europa.ec.leos.ui.view.workspace;

import com.google.common.eventbus.EventBus;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.UI;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.i18n.LanguageHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.security.LeosPermissionAuthorityMapHelper;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.ui.wizard.document.CreateDocumentWizard;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.web.event.view.repository.DocumentCreateWizardRequestEvent;
import eu.europa.ec.leos.web.support.user.UserHelper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static eu.europa.ec.leos.security.LeosPermission.CAN_UPLOAD;

@ViewScope
@SpringComponent
@Instance(instances = {InstanceType.COMMISSION, InstanceType.OS})
public class WorkspaceScreenProposalImpl extends WorkspaceScreenImpl {

    private static final long serialVersionUID = 1L;

    @Autowired
    WorkspaceScreenProposalImpl(SecurityContext securityContext, EventBus eventBus, MessageHelper messageHelper, LanguageHelper langHelper, UserHelper userHelper, LeosPermissionAuthorityMapHelper authorityMapHelper) {
        super(securityContext, eventBus, messageHelper, langHelper, userHelper, authorityMapHelper);
        initSpecificStaticData();
        initSpecificListeners();
    }

    private void initSpecificStaticData() {
        createDocumentButton.setCaption(messageHelper.getMessage("repository.create.document"));
        createDocumentButton.setDescription(messageHelper.getMessage("repository.create.document.tooltip"));

        resetBasedOnPermissions();
    }

    private void resetBasedOnPermissions() {
        //Upload button should only be visible to Support or higher role 
        boolean enableUpload = securityContext.hasPermission(null, CAN_UPLOAD);
        uploadDocumentButton.setVisible(enableUpload);
    }

    private void initSpecificListeners() {
        createDocumentButton.addClickListener(clickEvent -> eventBus.post(new DocumentCreateWizardRequestEvent()));
    }

    @Override
    public void showCreateMandateWizard() {
    }

    @Override
    public void showCreateDocumentWizard(List<CatalogItem> templates) {
        CreateDocumentWizard createDocumentWizard = new CreateDocumentWizard(templates, messageHelper, langHelper, eventBus);
        UI.getCurrent().addWindow(createDocumentWizard);
        createDocumentWizard.focus();
    }
}