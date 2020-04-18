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
package eu.europa.ec.leos.ui.view.collection;

import com.google.common.eventbus.EventBus;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FileResource;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.MenuBar;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.i18n.LanguageHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.export.ExportOptions;
import eu.europa.ec.leos.ui.event.view.collection.DeleteCollectionEvent;
import eu.europa.ec.leos.ui.event.view.collection.DownloadProposalEvent;
import eu.europa.ec.leos.ui.event.view.collection.ExportProposalEvent;
import eu.europa.ec.leos.web.support.UrlBuilder;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.vaadin.dialogs.ConfirmDialog;

import java.io.File;
import java.io.IOException;

@ViewScope
@SpringComponent
@Instance(instances = {InstanceType.COMMISSION})
public class ProposalCollectionScreenImpl extends CollectionScreenImpl {
    private static final long serialVersionUID = 3475674834606896478L;

    private static final Logger LOG = LoggerFactory.getLogger(ProposalCollectionScreenImpl.class);

    protected MenuBar.MenuItem exportCollectionToPdf;
    protected MenuBar.MenuItem exportCollectionToLegiswrite;

    ProposalCollectionScreenImpl(UserHelper userHelper, MessageHelper messageHelper, EventBus eventBus, LanguageHelper langHelper,
            ConfigurationHelper cfgHelper,  WebApplicationContext webApplicationContext, SecurityContext securityContext,
            UrlBuilder urlBuilder) {
        super(userHelper, messageHelper, eventBus, langHelper, cfgHelper, webApplicationContext, securityContext, urlBuilder);

        initProposalStaticData();
    }

    @Override
    public void confirmCollectionDeletion() {
        // ask confirmation before delete
        ConfirmDialog confirmDialog = ConfirmDialog.getFactory().create(
                messageHelper.getMessage("collection.proposal.delete.confirmation.title"),
                messageHelper.getMessage("collection.proposal.delete.confirmation.message"),
                messageHelper.getMessage("collection.delete.confirmation.confirm"),
                messageHelper.getMessage("collection.delete.confirmation.cancel"), null);
        confirmDialog.setContentMode(ConfirmDialog.ContentMode.HTML);

        confirmDialog.show(getUI(),
                new ConfirmDialog.Listener() {
                    private static final long serialVersionUID = 144198814274639L;

                    public void onClose(ConfirmDialog dialog) {
                        if (dialog.isConfirmed()) {
                            eventBus.post(new DeleteCollectionEvent());
                        }
                    }
                }, true);
    }

    private class ExportToPdfCommand implements MenuBar.Command {

        private static final long serialVersionUID = 4268175635432779914L;

        @Override
        public void menuSelected(MenuBar.MenuItem selectedItem) {
            eventBus.post(new ExportProposalEvent(ExportOptions.TO_PDF_LW));
        }
    }

    private class ExportToLegiswriteCommand implements MenuBar.Command {

        private static final long serialVersionUID = -2338442355638473891L;

        @Override
        public void menuSelected(MenuBar.MenuItem selectedItem) {
            eventBus.post(new ExportProposalEvent(ExportOptions.TO_WORD_LW));
        }
    }

    private void initProposalStaticData() {
        exportCollection.setDescription(messageHelper.getMessage("collection.description.menuitem.export.legiswrite"));
        MenuBar.MenuItem exportMenu = exportCollection.addItem(messageHelper.getMessage("collection.caption.menuitem.export"), null);
        exportCollectionToPdf = exportMenu.addItem(messageHelper.getMessage("collection.caption.menuitem.export.pdf"), new ExportToPdfCommand());
        exportCollectionToPdf.setDescription(messageHelper.getMessage("collection.description.menuitem.export.pdf"));
        exportCollectionToLegiswrite = exportMenu.addItem(messageHelper.getMessage("collection.caption.menuitem.export.legiswrite"),
                new ExportToLegiswriteCommand());
        exportCollectionToLegiswrite.setDescription(messageHelper.getMessage("collection.description.menuitem.export.document.legiswrite"));

        downloadCollection.setDescription(messageHelper.getMessage("collection.description.button.download.legiswrite"));
    }

    @Override
    protected void resetBasedOnPermissions(DocumentVO proposalVO) {
        super.resetBasedOnPermissions(proposalVO);

        boolean enableExportToLegiswrite = securityContext.hasPermission(proposalVO, LeosPermission.CAN_EXPORT_LW);
        exportCollectionToLegiswrite.setVisible(enableExportToLegiswrite);

        boolean enableExportToPdf = securityContext.hasPermission(proposalVO, LeosPermission.CAN_READ);
        exportCollectionToPdf.setVisible(enableExportToPdf);
    }

    @Override
    protected void initDownloader() {
        // Resource cannot be null at instantiation time of the FileDownloader, creating a dummy one
        FileResource downloadStreamResource = new FileResource(new File(""));
        fileDownloader = new FileDownloader(downloadStreamResource) {
            private static final long serialVersionUID = -4584979099145066535L;

            @Override
            public boolean handleConnectorRequest(VaadinRequest request, VaadinResponse response, String path) throws IOException {
                boolean result = false;
                try {
                    eventBus.post(new DownloadProposalEvent());
                    result = super.handleConnectorRequest(request, response, path);
                } catch (Exception exception) {
                    LOG.error("Error occured in download", exception.getMessage());
                }

                return result;
            }
        };
        fileDownloader.extend(downloadCollection);
    }

    @Override
    public void setExportPdfStreamResource(Resource exportPdfStreamResource) {
    }
}