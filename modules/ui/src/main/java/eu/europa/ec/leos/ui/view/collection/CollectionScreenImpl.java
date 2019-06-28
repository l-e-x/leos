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
import com.vaadin.annotations.DesignRoot;
import com.vaadin.data.Binder;
import com.vaadin.data.ReadOnlyHasValue;
import com.vaadin.data.ValidationException;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.*;
import com.vaadin.ui.declarative.Design;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.MetadataVO;
import eu.europa.ec.leos.i18n.LanguageHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.LeosPermissionAuthorityMapHelper;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.ui.component.milestones.MilestonesComponent;
import eu.europa.ec.leos.ui.event.CloseScreenRequestEvent;
import eu.europa.ec.leos.ui.event.CreateMilestoneRequestEvent;
import eu.europa.ec.leos.ui.event.view.collection.CreateAnnexRequest;
import eu.europa.ec.leos.ui.event.view.collection.DeleteAnnexEvent;
import eu.europa.ec.leos.ui.event.view.collection.DeleteCollectionRequest;
import eu.europa.ec.leos.ui.event.view.collection.SearchUserResponse;
import eu.europa.ec.leos.ui.model.MilestonesVO;
import eu.europa.ec.leos.ui.window.MilestoneWindow;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.coedition.InfoType;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.view.document.OpenLegalTextEvent;
import eu.europa.ec.leos.web.event.view.memorandum.OpenMemorandumEvent;
import eu.europa.ec.leos.web.event.window.SaveMetaDataRequestEvent;
import eu.europa.ec.leos.web.model.CollaboratorVO;
import eu.europa.ec.leos.web.model.UserVO;
import eu.europa.ec.leos.web.support.UrlBuilder;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.component.AnnexBlockComponent;
import eu.europa.ec.leos.web.ui.component.EditBoxComponent;
import eu.europa.ec.leos.web.ui.component.HeadingComponent;
import eu.europa.ec.leos.web.ui.component.collaborators.CollaboratorsComponent;
import eu.europa.ec.leos.web.ui.converter.LangCodeToDescriptionV8Converter;
import eu.europa.ec.leos.web.ui.converter.UserLoginDisplayConverter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.WebApplicationContext;
import org.vaadin.dialogs.ConfirmDialog;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringComponent
@ViewScope
@DesignRoot("CollectionScreenDesign.html")
abstract class CollectionScreenImpl extends VerticalLayout implements CollectionScreen {

    private static final Logger LOG = LoggerFactory.getLogger(CollectionScreenImpl.class);

    public static SimpleDateFormat dataFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    private static final long serialVersionUID = 1L;
    private static final int TITLE_TEXT_MAX_LEGTH  = 2000;

    protected FileDownloader fileDownloader;

    // Top block components
    protected Label docStage;
    protected Label docType;
    protected EditBoxComponent docPurpose;
    protected Button deleteCollection;
    protected Link shareCollectionLink;

    protected MenuBar exportCollection;
    protected Button downloadCollection;
    protected Button closeButton;

    // Details
    protected HeadingComponent detailsBlockHeading;
    protected NativeSelect<MetadataVO.SecurityLevel> securityLevel;
    protected EditBoxComponent packageTitle;
    protected EditBoxComponent template;
    protected EditBoxComponent internalRef;
    protected Label collectionLanguage;
    protected CheckBox eeaRelevance;
    private boolean enableSave;
    private Binder<DocumentVO> legalTextBinder;
    private Binder<DocumentVO> memorandumBinder;
    private Binder<MetadataVO> metadataBinder;

    protected VerticalLayout secondColumn;
    // memorandum block components
    protected VerticalLayout memorandumBlock;
    protected HeadingComponent memorandumBlockHeading;
    protected Label memorandumUserCoEdition;
    protected Button memorandumOpenButton;
    protected Label memorandumLanguage;
    protected Label memorandumLastUpdated;

    // legal text block
    protected VerticalLayout legalTextBlock;
    protected HeadingComponent legalTextBlockHeading;
    protected Label legalTextUserCoEdition;
    protected Button legalTextOpenButton;
    protected Label legalTextLanguage;
    protected Label legalTextLastUpdated;

    // Annexes
    protected Button createAnnexButton = new Button(); // initialized to avoid unmapped field exception from design
    protected HeadingComponent annexesBlockHeading;
    protected VerticalLayout annexesLayout;

    // UserManagement
    protected HeadingComponent collaboratorsBlockHeading;
    protected CollaboratorsComponent collaboratorsComponent;

    // Milestone
    protected HeadingComponent milestonesBlockHeading;
    protected Button createMilestoneButton = new Button(); // initialized to avoid unmapped field exception from design
    protected MilestonesComponent milestonesComponent;

    // General
    protected MessageHelper messageHelper;
    protected EventBus eventBus;
    private LangCodeToDescriptionV8Converter langConverter;
    private WebApplicationContext webApplicationContext;
    protected SecurityContext securityContext;
    private UserHelper userHelper;
    private UrlBuilder urlBuilder;

    @Value("${leos.coedition.sip.enabled}")
    private boolean coEditionSipEnabled;

    @Value("${leos.coedition.sip.domain}")
    private String coEditionSipDomain;

    @Autowired
    LeosPermissionAuthorityMapHelper authorityMapHelper;

    @Autowired CollectionScreenImpl(UserHelper userHelper, MessageHelper messageHelper, EventBus eventBus, LanguageHelper langHelper,
                      WebApplicationContext webApplicationContext, SecurityContext securityContext, UrlBuilder urlBuilder) {
        Design.read(this);
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.langConverter = new LangCodeToDescriptionV8Converter(langHelper);
        this.webApplicationContext = webApplicationContext;
        this.securityContext = securityContext;
        this.userHelper = userHelper;
        this.urlBuilder = urlBuilder;

        initLayout();
        initStaticData();
        initListeners();
        bind();
    }

    private void initLayout() {
        secondColumn.setMargin(new MarginInfo(false, false, false, true));
    }

    private void initStaticData() {
        detailsBlockHeading.setCaption(messageHelper.getMessage("collection.block.caption.details"));
        closeButton.setDescription(messageHelper.getMessage("collection.description.button.close"));
        closeButton.setCaption(messageHelper.getMessage("collection.caption.button.close"));
        deleteCollection.setCaption(messageHelper.getMessage("collection.caption.button.delete"));
        deleteCollection.setDescription(messageHelper.getMessage("collection.description.button.delete"));
        shareCollectionLink.setDescription(messageHelper.getMessage("collection.description.button.share.page.url"));

        downloadCollection.setCaption(messageHelper.getMessage("collection.caption.button.download"));
        packageTitle.setCaption(messageHelper.getMessage("collection.caption.package.title"));
        template.setCaption(messageHelper.getMessage("collection.caption.template"));
        collectionLanguage.setCaption(messageHelper.getMessage("collection.caption.language"));
        internalRef.setCaption(messageHelper.getMessage("collection.caption.internal.ref"));
        eeaRelevance.setCaption(messageHelper.getMessage("collection.caption.eea.relevance"));
        securityLevel.setCaption(messageHelper.getMessage("collection.caption.security.level"));
        // handle security levels..
        securityLevel.setEmptySelectionAllowed(false);
        securityLevel.setDataProvider(new ListDataProvider<>(Arrays.asList(MetadataVO.SecurityLevel.values())));
        securityLevel.setItemCaptionGenerator((ItemCaptionGenerator<MetadataVO.SecurityLevel>) item -> messageHelper
                .getMessage("collection.caption.security." + item.toString().toLowerCase()));

        memorandumBlockHeading.setCaption(messageHelper.getMessage("collection.block.caption.memorandum"));
        memorandumOpenButton.addClickListener(listener -> openMemorandum());
        memorandumOpenButton.setCaption(messageHelper.getMessage("leos.button.open"));
        memorandumLanguage.setCaption(messageHelper.getMessage("collection.caption.language"));

        legalTextBlockHeading.setCaption(messageHelper.getMessage("collection.block.caption.legal.text"));
        legalTextOpenButton.setCaption(messageHelper.getMessage("leos.button.open"));
        legalTextLanguage.setCaption(messageHelper.getMessage("collection.caption.language"));

        annexesBlockHeading.addRightButton(addCreateAnnexButton());
        annexesBlockHeading.setCaption(messageHelper.getMessage("collection.block.caption.annexes"));

        collaboratorsBlockHeading.addRightButton(addCollaboratorButton());
        collaboratorsBlockHeading.setCaption(messageHelper.getMessage("collection.block.caption.collaborator"));

        milestonesBlockHeading.addRightButton(addCreateMilestoneButton());
        milestonesBlockHeading.setCaption(messageHelper.getMessage("collection.block.caption.milestones"));

        docPurpose.setRequired(messageHelper.getMessage("collection.editor.purpose.error.empty"));
        initDownloader();
    }

    private void initListeners() {
        closeButton.addClickListener(event -> eventBus.post(new CloseScreenRequestEvent()));
        deleteCollection.addClickListener(event -> eventBus.post(new DeleteCollectionRequest()));
        createMilestoneButton.addClickListener(event -> eventBus.post(new CreateMilestoneRequestEvent()));

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
        shareCollectionLink.setResource(new ExternalResource(externalResource.toString()));
    }

    @Override
    public void populateData(DocumentVO proposalVO) {
        resetBasedOnPermissions(proposalVO);

        populateDetailsData(proposalVO.getMetadata());
        populateMemorandumData(proposalVO.getChildDocument(LeosCategory.MEMORANDUM));
        populateLegalTextData(proposalVO.getChildDocument(LeosCategory.BILL));

        populateCollaborators(getCollaboratorVOs(userHelper::getUser, proposalVO.getCollaborators()));
        setProposalURL();
        docPurpose.setTitleMaxSize(TITLE_TEXT_MAX_LEGTH);
    }

    @Override
    public void populateMilestones(Set<MilestonesVO> milestonesVO) {
        milestonesComponent.populateData(milestonesVO);
    }

    abstract protected void initDownloader();

    protected void resetBasedOnPermissions(DocumentVO proposalVO) {
        boolean enableDelete = securityContext.hasPermission(proposalVO, LeosPermission.CAN_DELETE);
        deleteCollection.setVisible(enableDelete);

        boolean enableShare = securityContext.hasPermission(proposalVO, LeosPermission.CAN_ADD_REMOVE_COLLABORATOR);
        collaboratorsBlockHeading.getRightButton().setVisible(enableShare);
        collaboratorsComponent.setEnabled(enableShare);

        // Download button should only be visible to Support or higher role
        boolean enableDownload = securityContext.hasPermission(proposalVO, LeosPermission.CAN_DOWNLOAD_PROPOSAL);
        downloadCollection.setVisible(enableDownload);

        boolean enableUpdate = securityContext.hasPermission(proposalVO, LeosPermission.CAN_UPDATE);
        docPurpose.setEnabled(enableUpdate);
        createAnnexButton.setVisible(enableUpdate);
        createAnnexButton.setEnabled(enableUpdate);

        boolean enableMilestoneCreation = securityContext.hasPermission(proposalVO, LeosPermission.CAN_CREATE_MILESTONE);
        createMilestoneButton.setVisible(enableMilestoneCreation);
        createMilestoneButton.setEnabled(enableMilestoneCreation);
    }

    @Override
    public void confirmAnnexDeletion(DocumentVO annex) {
        // ask confirmation before delete
        ConfirmDialog confirmDialog = ConfirmDialog.getFactory().create(
                messageHelper.getMessage("collection.annex.delete.confirmation.title"),
                messageHelper.getMessage("collection.annex.delete.confirmation.message"),
                messageHelper.getMessage("collection.annex.delete.confirmation.confirm"),
                messageHelper.getMessage("collection.annex.delete.confirmation.cancel"), null);
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

    private void bind() {
        metadataBinder = new Binder<>();
        metadataBinder.forField(docPurpose).bind(MetadataVO::getDocPurpose, MetadataVO::setDocPurpose);
        metadataBinder.forField(packageTitle).bind(MetadataVO::getPackageTitle, MetadataVO::setPackageTitle);
        metadataBinder.forField(template).bind(MetadataVO::getTemplate, MetadataVO::setTemplate);
        metadataBinder.forField(internalRef).bind(MetadataVO::getInternalRef, MetadataVO::setInternalRef);
        metadataBinder.forField(new ReadOnlyHasValue<>(docStage::setValue)).bind(MetadataVO::getDocStage, MetadataVO::setDocStage);
        metadataBinder.forField(new ReadOnlyHasValue<>(docType::setValue)).bind(MetadataVO::getDocType, MetadataVO::setDocType);
        metadataBinder.forField(eeaRelevance).bind(MetadataVO::isEeaRelevance, MetadataVO::setEeaRelevance);
        metadataBinder.forField(securityLevel).bind(MetadataVO::getSecurityLevel, MetadataVO::setSecurityLevel);
        metadataBinder.forField(new ReadOnlyHasValue<>(collectionLanguage::setValue))
                .withConverter(langConverter)
                .bind(MetadataVO::getLanguage, MetadataVO::setLanguage);

        // Bind legal texts
        legalTextBinder = new Binder<>();
        legalTextBinder.forField(new ReadOnlyHasValue<>(legalTextLanguage::setValue))
                .withConverter(langConverter)
                .bind(DocumentVO::getLanguage, DocumentVO::setLanguage);
        legalTextBinder.forField(new ReadOnlyHasValue<>(legalTextLastUpdated::setValue))
                .bind(this::setLastUpdated, null);

        // Bind memo
        memorandumBinder = new Binder<>();
        memorandumBinder.forField(new ReadOnlyHasValue<>(memorandumLanguage::setValue))
                .withConverter(langConverter)
                .bind(DocumentVO::getLanguage, DocumentVO::setLanguage);

        memorandumBinder.forField(new ReadOnlyHasValue<>(memorandumLastUpdated::setValue))
                .bind(this::setLastUpdated, null);
    }

    private void saveData() {
        if (enableSave) {
        	try {
        		metadataBinder.writeBean(metadataBinder.getBean()); // Updates the metadataVO bean
                eventBus.post(new SaveMetaDataRequestEvent(metadataBinder.getBean()));
        	} catch (ValidationException exception) {
        		LOG.error("Error occured in saveData", exception.getMessage());
        		eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "metadata.edit.saved.error"));
        	}
        }
    }

    private Button addCreateAnnexButton() {
        createAnnexButton.setIcon(VaadinIcons.PLUS_CIRCLE);
        createAnnexButton.setDescription(messageHelper.getMessage("collection.description.button.create.annex"));
        createAnnexButton.addStyleName("create-annex-button");
        createAnnexButton.setDisableOnClick(true);
        return createAnnexButton;
    }

    private Button addCollaboratorButton() {
        Button addCollaboratorButton = new Button();
        addCollaboratorButton.setIcon(VaadinIcons.PLUS_CIRCLE);
        addCollaboratorButton.addStyleName("add-button");
        addCollaboratorButton.addClickListener(event -> collaboratorsComponent.addCollaborator());
        return addCollaboratorButton;
    }

    private Button addCreateMilestoneButton() {
        createMilestoneButton.setIcon(VaadinIcons.PLUS_CIRCLE);
        createMilestoneButton.setDescription(messageHelper.getMessage("collection.block.description.button.create.milestone"));
        createMilestoneButton.addStyleName("add-button");
        return createMilestoneButton;
    }

    private void populateDetailsData(MetadataVO metadataVO) {
        enableSave = false; // To avoid triggering save on load of data
        metadataBinder.setBean(metadataVO);
        enableSave = true;
    }

    private void populateMemorandumData(DocumentVO explanatoryMemorandum) {
        if (explanatoryMemorandum == null) {
            memorandumBlock.setVisible(false);
        } else {
            memorandumBlock.setVisible(true);
            memorandumBlock.setData(explanatoryMemorandum);
            memorandumBinder.setBean(explanatoryMemorandum);
        }
    }

    private void populateLegalTextData(DocumentVO legalTextVo) {
        if (legalTextVo == null) {
            legalTextBlock.setVisible(false);
        } else {
            legalTextBlock.setVisible(true);
            legalTextBlock.setData(legalTextVo);
            legalTextBinder.setBean(legalTextVo);

            annexesLayout.removeAllComponents();
            if (legalTextVo.getChildDocuments().size() > 0) {
                legalTextVo.getChildDocuments().forEach(annex -> addAnnex(annex));
            } else {
                Label noAnnexMessage = new Label(messageHelper.getMessage("collection.message.no.annex"));
                noAnnexMessage.addStyleName("sub-block");
                annexesLayout.addComponent(noAnnexMessage);
            }
        }
    }

    private void populateCollaborators(List<CollaboratorVO> collaborators) {
        collaboratorsComponent.populateData(collaborators);
    }

    private List<CollaboratorVO> getCollaboratorVOs(Function<String, User> userConverter, Map<String, String> collaborators) {
        return Collections.unmodifiableList(collaborators.entrySet().stream()
                .map(entry -> createCollaboratorVO(entry.getKey(), entry.getValue(), userConverter))
                .filter(option -> option.isPresent())
                .map(option -> option.get())
                .collect(Collectors.toList()));
    }

    private Optional<CollaboratorVO> createCollaboratorVO(String login, String authority, Function<String, User> converter) {
        try {
            return Optional.of(new CollaboratorVO(new UserVO(converter.apply(login)), authorityMapHelper.getRoleFromListOfRoles(authority)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void proposeUsers(List<UserVO> users) {
        eventBus.post(new SearchUserResponse(users));
    }

    @Override
    public void openCreateMilestoneWindow() {
        MilestoneWindow createMilestoneWindow = new MilestoneWindow(messageHelper, eventBus);
        UI.getCurrent().addWindow(createMilestoneWindow);
        createMilestoneWindow.center();
        createMilestoneWindow.focus();
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
        return messageHelper.getMessage("collection.caption.document.lastupdated",
                dataFormat.format(vo.getUpdatedOn()),
                new UserLoginDisplayConverter(userHelper).convertToPresentation(vo.getUpdatedBy(), null, null));
    }

    public void setDownloadStreamResource(Resource downloadResource) {
        fileDownloader.setFileDownloadResource(downloadResource);
    }

    private void populateUserCoEditionInfo(Label userCoEditionLabel, DocumentVO documentVO, List<CoEditionVO> coEditionVos, User user) {
        userCoEditionLabel.setIcon(null);
        userCoEditionLabel.setDescription("");
        userCoEditionLabel.removeStyleName("leos-user-coedition-self-user");
        coEditionVos.stream()
                .filter((x) -> (InfoType.ELEMENT_INFO.equals(x.getInfoType()) || InfoType.TOC_INFO.equals(x.getInfoType())) && x.getDocumentId().equals(documentVO.getVersionSeriesId()))
                .sorted(Comparator.comparing(CoEditionVO::getUserName).thenComparingLong(CoEditionVO::getEditionTime))
                .forEach(x -> {
                    StringBuilder userDescription = new StringBuilder();
                    if (!x.getUserLoginName().equals(user.getLogin())) {
                        userDescription.append("<a class=\"leos-user-coedition-lync\" href=\"")
                            .append(StringUtils.isEmpty(x.getUserEmail()) ? "" : (coEditionSipEnabled ? new StringBuilder("sip:").append(x.getUserEmail().replaceFirst("@.*", "@" + coEditionSipDomain)).toString()
                                    : new StringBuilder("mailto:").append(x.getUserEmail()).toString()))
                            .append("\">").append(x.getUserName()).append(" (").append(StringUtils.isEmpty(x.getEntity()) ? "-" : x.getEntity())
                            .append(")</a>");
                    } else {
                        userDescription.append(x.getUserName()).append(" (").append(StringUtils.isEmpty(x.getEntity()) ? "-" : x.getEntity()).append(")");
                    }
                    userCoEditionLabel.setDescription(
                            userCoEditionLabel.getDescription() +
                                    messageHelper.getMessage("coedition.tooltip.message", userDescription, dataFormat.format(new Date(x.getEditionTime()))) +
                                    "<br>",
                            ContentMode.HTML);
                });
        if (!userCoEditionLabel.getDescription().isEmpty()) {
            userCoEditionLabel.setIcon(VaadinIcons.USER);
            if (!userCoEditionLabel.getDescription().contains("href=\"")) {
                userCoEditionLabel.addStyleName("leos-user-coedition-self-user");
            }
        }
    }
    
    @Override
    public void updateUserCoEditionInfo(List<CoEditionVO> coEditionVos, User user) {
        this.getUI().access(() -> {
            // Update memorandum user CoEdition information
            populateUserCoEditionInfo(memorandumUserCoEdition, (DocumentVO) memorandumBlock.getData(), coEditionVos, user);
            
            // Update legal text user CoEdition information
            populateUserCoEditionInfo(legalTextUserCoEdition, (DocumentVO) legalTextBlock.getData(), coEditionVos, user);
            
            // Update annexes user CoEdition information
            Iterator<Component> annexesIterator = annexesLayout.iterator();
            while (annexesIterator.hasNext()) {
                Component annexComponent = annexesIterator.next();
                if (annexComponent instanceof AnnexBlockComponent) {
                    ((AnnexBlockComponent)annexComponent).updateUserCoEditionInfo(coEditionVos, user);
                }
            }
        });
    }
}