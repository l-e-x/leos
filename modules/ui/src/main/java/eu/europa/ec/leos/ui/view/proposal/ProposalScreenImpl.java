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
package eu.europa.ec.leos.ui.view.proposal;

import com.google.common.eventbus.EventBus;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.data.Binder;
import com.vaadin.data.ReadOnlyHasValue;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.server.*;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.*;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.declarative.Design;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.data.util.BeanItem;
import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.MetadataVO;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.export.ExportOptions;
import eu.europa.ec.leos.ui.event.CloseScreenRequestEvent;
import eu.europa.ec.leos.web.event.view.document.OpenLegalTextEvent;
import eu.europa.ec.leos.web.event.view.memorandum.OpenMemorandumEvent;
import eu.europa.ec.leos.web.event.view.proposal.*;
import eu.europa.ec.leos.web.event.window.SaveMetaDataRequestEvent;
import eu.europa.ec.leos.web.model.CollaboratorVO;
import eu.europa.ec.leos.web.model.UserVO;
import eu.europa.ec.leos.web.support.UrlBuilder;
import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.component.AnnexBlockComponent;
import eu.europa.ec.leos.web.ui.component.EditBoxComponent;
import eu.europa.ec.leos.web.ui.component.HeadingComponent;
import eu.europa.ec.leos.web.ui.component.collaborators.CollaboratorsComponent;
import eu.europa.ec.leos.web.ui.converter.LangCodeToDescriptionV8Converter;
import eu.europa.ec.leos.web.ui.converter.UserLoginDisplayConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.vaadin.dialogs.ConfirmDialog;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringComponent
@ViewScope
@DesignRoot("ProposalScreenDesign.html")
class ProposalScreenImpl extends VerticalLayout implements ProposalScreen {

	private static final Logger LOG = LoggerFactory.getLogger(ProposalScreenImpl.class);

    public static SimpleDateFormat dataFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    private static final long serialVersionUID = 1L;

    private FileDownloader fileDownloader;

    // Top block components
    protected Label docStage;
    protected Label docType;
    protected EditBoxComponent docPurpose;
    protected Button deleteProposal;
    protected Link shareProposalLink;

    protected MenuBar exportProposal;
    protected MenuItem exportProposalToPdf;
    protected MenuItem exportProposalToLegiswrite;
    protected Button downloadProposal;
    protected Button closeButton;

    // Details
    protected HeadingComponent detailsBlockHeading;
    protected NativeSelect<MetadataVO.SecurityLevel> securityLevel;
    protected EditBoxComponent packageTitle;
    protected EditBoxComponent template;
    protected EditBoxComponent internalRef;
    protected Label proposalLanguage;
    protected CheckBox eeaRelevance;
    private boolean enableSave;
    private Binder<DocumentVO> legalTextBinder;
    private Binder<DocumentVO> memorandumBinder;
    private Binder<MetadataVO> metadataBinder;
    private BeanItem<MetadataVO> beanItem = new BeanItem<>(new MetadataVO(), MetadataVO.class);//TODO remove

    // memorandum block components
    protected VerticalLayout memorandumBlock;
    protected HeadingComponent memorandumBlockHeading;
    protected Button memorandumOpenButton;
    protected Label memorandumLanguage;
    protected Label memorandumLastUpdated;

    // legal text block
    protected VerticalLayout legalTextBlock;
    protected HeadingComponent legalTextBlockHeading;
    protected Button legalTextOpenButton;
    protected Label legalTextLanguage;
    protected Label legalTextLastUpdated;

    // Annexes
    protected Button createAnnexButton = new Button(); // initialized to avoid unmapped field exception from design
    protected HeadingComponent annexesBlockHeading;
    protected VerticalLayout annexesLayout;

    //UserManagement
    protected HeadingComponent collaboratorsBlockHeading;
    protected CollaboratorsComponent collaboratorsComponent;

    //General
    private MessageHelper messageHelper;
    private EventBus eventBus;
    private LangCodeToDescriptionV8Converter langConverter;
    private WebApplicationContext webApplicationContext;
    private SecurityContext securityContext;
    private UserHelper userHelper;
    private UrlBuilder urlBuilder;

    @Autowired
    ProposalScreenImpl(UserHelper userHelper, MessageHelper messageHelper, EventBus eventBus, LanguageHelper langHelper
            , WebApplicationContext webApplicationContext, SecurityContext securityContext, UrlBuilder urlBuilder) {
        Design.read(this);
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.langConverter = new LangCodeToDescriptionV8Converter(langHelper);
        this.webApplicationContext = webApplicationContext;
        this.securityContext = securityContext;
        this.userHelper = userHelper;
        this.urlBuilder = urlBuilder;

        initStaticData();
        initListeners();
        bind(beanItem);
    }

    private class ExportToPdfCommand implements Command {

        private static final long serialVersionUID = -2486576157838091413L;

        @Override
        public void menuSelected(MenuItem selectedItem) {
            eventBus.post(new ExportProposalEvent(ExportOptions.TO_PDF));
        }
    }

    private class ExportToLegiswriteCommand implements Command {

        private static final long serialVersionUID = -2486576157838091413L;

        @Override
        public void menuSelected(MenuItem selectedItem) {
            eventBus.post(new ExportProposalEvent(ExportOptions.TO_LEGISWRITE));
        }
    }

    private void initStaticData() {
        detailsBlockHeading.setCaption(messageHelper.getMessage("proposal.block.caption.details"));
        closeButton.setDescription(messageHelper.getMessage("proposal.description.button.close"));
        closeButton.setCaption(messageHelper.getMessage("proposal.caption.button.close"));
        deleteProposal.setCaption(messageHelper.getMessage("proposal.caption.button.delete"));
        deleteProposal.setDescription(messageHelper.getMessage("proposal.description.button.delete"));
        exportProposal.setDescription(messageHelper.getMessage("proposal.description.menuitem.export"));
        shareProposalLink.setDescription(messageHelper.getMessage("proposal.description.button.share.page.url"));

        MenuItem exportMenu = exportProposal.addItem(messageHelper.getMessage("proposal.caption.menuitem.export"), null);
        exportProposalToPdf = exportMenu.addItem(messageHelper.getMessage("proposal.caption.menuitem.export.pdf"), new ExportToPdfCommand());
        exportProposalToPdf.setDescription(messageHelper.getMessage("proposal.description.menuitem.export.pdf"));
        exportProposalToLegiswrite = exportMenu.addItem(messageHelper.getMessage("proposal.caption.menuitem.export.legiswrite"), new ExportToLegiswriteCommand());
        exportProposalToLegiswrite.setDescription(messageHelper.getMessage("proposal.description.menuitem.export.legiswrite"));
        downloadProposal.setCaption(messageHelper.getMessage("proposal.caption.button.download"));
        downloadProposal.setDescription(messageHelper.getMessage("proposal.description.button.download"));
        packageTitle.setCaption(messageHelper.getMessage("proposal.caption.package.title"));
        template.setCaption(messageHelper.getMessage("proposal.caption.template"));
        proposalLanguage.setCaption(messageHelper.getMessage("proposal.caption.language"));
        internalRef.setCaption(messageHelper.getMessage("proposal.caption.internal.ref"));
        eeaRelevance.setCaption(messageHelper.getMessage("proposal.caption.eea.relevance"));
        securityLevel.setCaption(messageHelper.getMessage("proposal.caption.security.level"));
        // handle security levels..
        securityLevel.setEmptySelectionAllowed(false);
        securityLevel.setDataProvider(new ListDataProvider<>(Arrays.asList(MetadataVO.SecurityLevel.values())));
        securityLevel.setItemCaptionGenerator((ItemCaptionGenerator<MetadataVO.SecurityLevel>) item ->
                messageHelper.getMessage("proposal.caption.security." + item.toString().toLowerCase()));

        memorandumBlockHeading.setCaption(messageHelper.getMessage("proposal.block.caption.memorandum"));
        memorandumOpenButton.addClickListener(listener -> openMemorandum());
        memorandumOpenButton.setCaption(messageHelper.getMessage("leos.button.open"));
        memorandumLanguage.setCaption(messageHelper.getMessage("proposal.caption.language"));

        legalTextBlockHeading.setCaption(messageHelper.getMessage("proposal.block.caption.legal.text"));
        legalTextOpenButton.setCaption(messageHelper.getMessage("leos.button.open"));
        legalTextLanguage.setCaption(messageHelper.getMessage("proposal.caption.language"));

        annexesBlockHeading.addRightButton(addCreateAnnexButton());
        annexesBlockHeading.setCaption(messageHelper.getMessage("proposal.block.caption.annexes"));

        collaboratorsBlockHeading.addRightButton(addCollaboratorButton());
        collaboratorsBlockHeading.setCaption(messageHelper.getMessage("proposal.block.caption.collaborator"));
        docPurpose.setRequired(messageHelper.getMessage("proposal.editor.purpose.error.empty"));
        initDownloader();
    }

    private void initListeners() {
        closeButton.addClickListener(event -> eventBus.post(new CloseScreenRequestEvent()));
        deleteProposal.addClickListener(event -> eventBus.post(new DeleteProposalRequest()));

        docPurpose.addValueChangeListener(event -> saveData());
        packageTitle.addValueChangeListener(event -> saveData());
        internalRef.addValueChangeListener(event -> saveData());
        template.addValueChangeListener(event -> saveData());
        securityLevel.addValueChangeListener(event -> saveData());
        eeaRelevance.addValueChangeListener(event -> saveData());

        legalTextOpenButton.addClickListener(clickEvent -> openLegalText());
        createAnnexButton.addClickListener(clickEvent -> createAnnex());
    }

    private void setProposalURL() {
        String proposalLink = urlBuilder.getDocumentUrl(this.getUI().getPage());
        StringBuilder externalResource = new StringBuilder("mailto:?subject=Share Proposal&body=").append(proposalLink);
        shareProposalLink.setResource(new ExternalResource(externalResource.toString()));
    }

    @Override
    public void populateData(DocumentVO proposalVO) {
        resetBasedOnPermissions(proposalVO);

        populateDetailsData(proposalVO.getMetadata());
        populateMemorandumData(proposalVO.getChildDocument(LeosCategory.MEMORANDUM));
        populateLegalTextData(proposalVO.getChildDocument(LeosCategory.BILL));

        populateCollaborators(getCollaboratorVOs(userHelper::getUser, proposalVO.getCollaborators()));
        setProposalURL();
    }

    private void initDownloader() {
        //Resource cannot be null at instantiation time of the FileDownloader, creating a dummy one
        FileResource downloadStreamResource = new FileResource(new File(""));
        fileDownloader = new FileDownloader(downloadStreamResource) {
            private static final long serialVersionUID = -4584979099145066535L;
            @Override
            public boolean handleConnectorRequest(VaadinRequest request, VaadinResponse response, String path) throws IOException {
                boolean result =false;
				try {
					eventBus.post(new DownloadProposalEvent());
					result = super.handleConnectorRequest(request, response, path);
				} catch (Exception exception) {
					LOG.error("Error occured in download", exception.getMessage());
				}

                return result;
            }
        };
        fileDownloader.extend(downloadProposal);
    }

    private void resetBasedOnPermissions(DocumentVO proposalVO){
        boolean enableExportToLegiswrite = securityContext.hasPermission(proposalVO, LeosPermission.CAN_PRINT_LW);
        exportProposalToLegiswrite.setVisible(enableExportToLegiswrite);

        boolean enableExportToPdf = securityContext.hasPermission(proposalVO, LeosPermission.CAN_READ);
        exportProposalToPdf.setVisible(enableExportToPdf);

        boolean enableDelete = securityContext.hasPermission(proposalVO, LeosPermission.CAN_DELETE);
        deleteProposal.setVisible(enableDelete);

        boolean enableShare = securityContext.hasPermission(proposalVO, LeosPermission.CAN_SHARE);
        collaboratorsBlockHeading.getRightButton().setVisible(enableShare);
        collaboratorsComponent.setEnabled(enableShare);

        //Download button should only be visible to Support or higher role 
        boolean enableDownload = securityContext.hasRole(LeosAuthority.SUPPORT);
        downloadProposal.setVisible(enableDownload);

        boolean enableUpdate = securityContext.hasPermission(proposalVO, LeosPermission.CAN_UPDATE);
        docPurpose.setEnabled(enableUpdate);
        createAnnexButton.setVisible(enableUpdate);
        createAnnexButton.setEnabled(enableUpdate);
    }

    @Override
    public void confirmProposalDeletion() {
        // ask confirmation before delete
        ConfirmDialog confirmDialog = ConfirmDialog.getFactory().create(
                messageHelper.getMessage("proposal.delete.confirmation.title"),
                messageHelper.getMessage("proposal.delete.confirmation.message"),
                messageHelper.getMessage("proposal.delete.confirmation.confirm"),
                messageHelper.getMessage("proposal.delete.confirmation.cancel"), null);
        confirmDialog.setContentMode(ConfirmDialog.ContentMode.HTML);

        confirmDialog.show(getUI(),
                new ConfirmDialog.Listener() {
                    private static final long serialVersionUID = 144198814274639L;

                    public void onClose(ConfirmDialog dialog) {
                        if (dialog.isConfirmed()) {
                            eventBus.post(new DeleteProposalEvent());
                        }
                    }
                }, true);
    }

    @Override
    public void confirmAnnexDeletion(DocumentVO annex) {
        // ask confirmation before delete
        ConfirmDialog confirmDialog = ConfirmDialog.getFactory().create(
                messageHelper.getMessage("proposal.annex.delete.confirmation.title"),
                messageHelper.getMessage("proposal.annex.delete.confirmation.message"),
                messageHelper.getMessage("proposal.annex.delete.confirmation.confirm"),
                messageHelper.getMessage("proposal.annex.delete.confirmation.cancel"), null);
        confirmDialog.setContentMode(ConfirmDialog.ContentMode.HTML);

        confirmDialog.show(getUI(),
                new ConfirmDialog.Listener() {
                    private static final long serialVersionUID = 144198814274639L;

                    public void onClose(ConfirmDialog dialog) {
                        if (dialog.isConfirmed()) {
                            eventBus.post(new DeleteAnnexEvent(annex));
                        }
                    }
                }, true);
    }

    private void bind(BeanItem<MetadataVO> beanItem) {
        docPurpose.setPropertyDataSource(beanItem.getItemProperty("docPurpose"));
        packageTitle.setPropertyDataSource(beanItem.getItemProperty("packageTitle"));
        template.setPropertyDataSource(beanItem.getItemProperty("template"));
        internalRef.setPropertyDataSource(beanItem.getItemProperty("internalRef"));

        metadataBinder = new Binder<>();
        metadataBinder.forField(new ReadOnlyHasValue<>(docStage::setValue)).bind(MetadataVO::getDocStage, MetadataVO::setDocStage);
        metadataBinder.forField(new ReadOnlyHasValue<>(docType::setValue)).bind(MetadataVO::getDocType, MetadataVO::setDocType);
        metadataBinder.forField(eeaRelevance).bind(MetadataVO::isEeaRelevance, MetadataVO::setEeaRelevance);
        metadataBinder.forField(securityLevel).bind(MetadataVO::getSecurityLevel, MetadataVO::setSecurityLevel);
        metadataBinder.forField(new ReadOnlyHasValue<>(proposalLanguage::setValue))
                .withConverter(langConverter)
                .bind(MetadataVO::getLanguage, MetadataVO::setLanguage);

        //Bind legal texts
        legalTextBinder = new Binder<>();
        legalTextBinder.forField(new ReadOnlyHasValue<>(legalTextLanguage::setValue))
                .withConverter(langConverter)
                .bind(DocumentVO::getLanguage, DocumentVO::setLanguage);
        legalTextBinder.forField(new ReadOnlyHasValue<>(legalTextLastUpdated::setValue))
                .bind(this::setLastUpdated, null);

        //Bind memo
        memorandumBinder = new Binder<>();
        memorandumBinder.forField(new ReadOnlyHasValue<>(memorandumLanguage::setValue))
                .withConverter(langConverter)
                .bind(DocumentVO::getLanguage, DocumentVO::setLanguage);

        memorandumBinder.forField(new ReadOnlyHasValue<>(memorandumLastUpdated::setValue))
                .bind(this::setLastUpdated, null);
    }

    private void saveData() {
        if (enableSave) {
            MetadataVO metadataViewObj = beanItem.getBean();

            eventBus.post(new SaveMetaDataRequestEvent(metadataViewObj));
        }
    }

    private Button addCreateAnnexButton() {
        createAnnexButton.setIcon(FontAwesome.PLUS_CIRCLE);
        createAnnexButton.setDescription(messageHelper.getMessage("proposal.description.button.create.annex"));
        createAnnexButton.addStyleName("create-annex-button");
        createAnnexButton.setDisableOnClick(true);
        return createAnnexButton;
    }

    private Button addCollaboratorButton() {
        Button addCollaboratorButton = new Button();
        addCollaboratorButton.setIcon(FontAwesome.PLUS_CIRCLE);
        addCollaboratorButton.addStyleName("add-collaborator-button");
        addCollaboratorButton.addClickListener(event -> collaboratorsComponent.addCollaborator());
        return addCollaboratorButton;
    }

    private void populateDetailsData(MetadataVO metadataVO) {
        enableSave = false; //To avoid triggering save on load of data
        metadataBinder.setBean(metadataVO);

        BeanItem newItem = new BeanItem<>(metadataVO, MetadataVO.class);
        if (newItem != null) {
            beanItem.getItemPropertyIds().forEach(id -> {
                Property oldProperty = beanItem.getItemProperty(id);
                Object oldPropertyValue = beanItem.getItemProperty(id).getValue();
                Object newPropertyValue = newItem.getItemProperty(id).getValue();
                if (oldPropertyValue == null || !oldPropertyValue.equals(newPropertyValue)) {
                    oldProperty.setValue(newPropertyValue);
                }
            });
        }
        enableSave = true;
    }

    private void populateMemorandumData(DocumentVO explanatoryMemorandum) {
        if (explanatoryMemorandum == null) {
            memorandumBlock.setVisible(false);
        }
        else {
            memorandumBlock.setVisible(true);
            memorandumBlock.setData(explanatoryMemorandum);
            memorandumBinder.setBean(explanatoryMemorandum);
        }
    }

    private void populateLegalTextData(DocumentVO legalTextVo){
        if(legalTextVo == null ){
            legalTextBlock.setVisible(false);
        }
        else {
            legalTextBlock.setVisible(true);
            legalTextBlock.setData(legalTextVo);
            legalTextBinder.setBean(legalTextVo);

            annexesLayout.removeAllComponents();
            if (legalTextVo.getChildDocuments().size() > 0) {
                legalTextVo.getChildDocuments().forEach(annex -> addAnnex(annex));
            } else {
                Label noAnnexMessage = new Label(messageHelper.getMessage("proposal.message.no.annex"));
                noAnnexMessage.addStyleName("sub-block");
                annexesLayout.addComponent(noAnnexMessage);
            }
        }
    }

    private void populateCollaborators(List<CollaboratorVO> collaborators){
        collaboratorsComponent.populateData(collaborators);
    }

    private List<CollaboratorVO> getCollaboratorVOs(Function<String, User> userConverter, Map<String, LeosAuthority> collaborators) {
        return Collections.unmodifiableList(collaborators.entrySet().stream()
                .map(entry -> createCollaboratorVO(entry.getKey(), entry.getValue(), userConverter))
                .filter(option -> option.isPresent())
                .map(option -> option.get())
                .collect(Collectors.toList()));
    }

    private Optional<CollaboratorVO> createCollaboratorVO(String login, LeosAuthority authority, Function<String, User> converter) {
        try {
            return Optional.of(new CollaboratorVO(new UserVO(converter.apply(login)),authority));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void proposeUsers(List<UserVO> users) {
        eventBus.post(new SearchUserResponse(users));
    }

    private void addAnnex(DocumentVO annex) {
        AnnexBlockComponent annexComponent = webApplicationContext.getBean(AnnexBlockComponent.class);
        annexesLayout.addComponent(annexComponent);
        annexComponent.populateData(annex);
    }

    private void openMemorandum() {
        eventBus.post(new OpenMemorandumEvent((DocumentVO) memorandumBlock.getData()));
    }

    private void openLegalText() {
        eventBus.post(new OpenLegalTextEvent(((DocumentVO) legalTextBlock.getData()).getId()));
    }

    private void createAnnex() {
        eventBus.post(new CreateAnnexRequest());
    }

    private String setLastUpdated(DocumentVO vo) {
        return messageHelper.getMessage("proposal.caption.document.lastupdated",
                dataFormat.format(vo.getUpdatedOn()),
                new UserLoginDisplayConverter(userHelper).convertToPresentation(vo.getUpdatedBy(), null, null));
    }

    public void setDownloadStreamResource(Resource downloadResource) {
        fileDownloader.setFileDownloadResource(downloadResource);
    }
}