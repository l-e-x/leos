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
package eu.europa.ec.leos.web.ui.screen.repository;

import com.google.common.base.Stopwatch;
import com.google.common.eventbus.Subscribe;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import eu.europa.ec.leos.vo.UserVO;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.view.repository.*;
import eu.europa.ec.leos.web.model.DocumentVO;
import eu.europa.ec.leos.web.ui.component.card.CardHolder;
import eu.europa.ec.leos.web.ui.component.card.DataController;
import eu.europa.ec.leos.web.ui.component.documentCard.DocumentCard;
import eu.europa.ec.leos.web.ui.screen.LeosScreen;
import eu.europa.ec.leos.web.ui.window.EditContributorWindow;
import eu.europa.ec.leos.web.ui.wizard.document.CreateDocumentWizard;
import eu.europa.ec.leos.web.view.RepositoryView;
import eu.europa.ec.leos.web.view.subView.FilterSubView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.teemu.VaadinIcons;
import ru.xpoft.vaadin.VaadinView;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Scope("session")
@org.springframework.stereotype.Component(RepositoryView.VIEW_ID)
@VaadinView(RepositoryView.VIEW_ID)
public class RepositoryViewImpl extends LeosScreen implements RepositoryView {

    private static final long serialVersionUID = -5293223956971397143L;
    private static final Logger LOG = LoggerFactory.getLogger(RepositoryViewImpl.class);
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private DataController<DocumentVO> dataController;
    private FilterSubView<DocumentVO> filterSubView;

    @PostConstruct
    private void init() {
        LOG.trace("Initializing {} view...", VIEW_ID);
        setSizeFull();
        setSpacing(true);
        setMargin(true);
        addStyleName("leos-repository");

        com.vaadin.ui.Component documentsGrid = createDocumentGrid();
        filterSubView = new FilterSubView<>(messageHelper, langHelper, eventBus, dataController.getVaadinContainer(), securityContext.getUser());

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setSizeFull();
        horizontalLayout.setSpacing(true);
        addComponent(horizontalLayout);

        horizontalLayout.addComponent(filterSubView.getUIComponent());
        horizontalLayout.setExpandRatio(filterSubView.getUIComponent(), .2f);

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();
        verticalLayout.setSpacing(true);
        horizontalLayout.addComponent(verticalLayout);

        verticalLayout.addComponent(createTopBar());
        verticalLayout.addComponent(documentsGrid);
        verticalLayout.setExpandRatio(documentsGrid, 1f);

        horizontalLayout.setExpandRatio(verticalLayout, 0.8f);
        eventBus.register(this);
    }

    private com.vaadin.ui.Component createTopBar() {
        HorizontalLayout topLayout = new HorizontalLayout();
        topLayout.setWidth("100%");
        topLayout.setSpacing(true);

        Component refreshButton= createRefreshButton();
        topLayout.addComponent(refreshButton);
        topLayout.setComponentAlignment(refreshButton, Alignment.TOP_LEFT);

        Component titleSearchGroup = createTitleSearchGroup();
        topLayout.addComponent(titleSearchGroup);
        topLayout.setExpandRatio(titleSearchGroup, .50f);
        topLayout.setComponentAlignment(titleSearchGroup, Alignment.TOP_CENTER);

        Button createButton = constructCreateDocumentButton();
        topLayout.addComponent(createButton);
        topLayout.setExpandRatio(createButton, .15f);
        topLayout.setComponentAlignment(createButton, Alignment.TOP_RIGHT);
        return topLayout;
    }

    private Component createRefreshButton() {
        final Button refreshButton=new Button();
        refreshButton.setStyleName(ValoTheme.BUTTON_ICON_ONLY);
        refreshButton.setIcon(FontAwesome.REFRESH);
        refreshButton.addStyleName("leos-refresh-button");
        refreshButton.setDescription(messageHelper.getMessage("repository.refresh.button.tooltip"));
        refreshButton.setDisableOnClick(true);

        refreshButton.addClickListener(new Button.ClickListener() {
            @Override public void buttonClick(Button.ClickEvent clickEvent) {
                eventBus.post(new RefreshDocumentListEvent());
                if(!refreshButton.isEnabled()){
                    refreshButton.setEnabled(true);
                }
            }
        });
        return refreshButton;
    }

    private Component createTitleSearchGroup() {
        CssLayout group= new CssLayout();
        group.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
        group.setWidth("100%");

        Component textFilter = filterSubView.getSearchBox();

        final Button sortAsc = new Button();
        sortAsc.setIcon(VaadinIcons.CHEVRON_DOWN);
        sortAsc.setId("ASC");
        sortAsc.setStyleName("group-button");
        sortAsc.setDescription(messageHelper.getMessage("repository.filter.tooltip.sortGroup"));

        final Button sortDesc = new Button();
        sortDesc.setIcon(VaadinIcons.CHEVRON_UP);
        sortDesc.setId("DESC");
        sortDesc.setStyleName("group-button");
        sortDesc.setDescription(messageHelper.getMessage("repository.filter.tooltip.sortGroup"));
        group.addComponent(textFilter);
        group.addComponent(sortAsc);
        group.addComponent(sortDesc);

        Button.ClickListener bc = new Button.ClickListener() {
            final static String ENABLED_STYLE = "enabled";
            @Override
            public void buttonClick(Button.ClickEvent event) {
                Button sourceButton = event.getButton();
                if (sourceButton.getData() == null) {// first click
                    sourceButton.addStyleName(ENABLED_STYLE);
                    sourceButton.setData(sourceButton.getId());
                    resetButton(sourceButton.equals(sortAsc) ? sortDesc : sortAsc);

                    doTitleSort(sourceButton.equals(sortAsc));
                } else {// already sorted then remove the sort
                    resetButton(sourceButton);
                    defaultSort();
                }
            }
            private void doTitleSort(boolean sortOrder) {
                dataController.sortCards(new Object[]{"title"}, new boolean[]{sortOrder});
            }
            private void resetButton(Button button) {
                button.setData(null);
                button.removeStyleName(ENABLED_STYLE);
            }
        };
        sortAsc.addClickListener(bc);
        sortDesc.addClickListener(bc);
        return group;
    }

    private Button constructCreateDocumentButton() {
        Button button = new Button(messageHelper.getMessage("repository.create.document"));
        button.setDescription(messageHelper.getMessage("repository.create.document.tooltip"));
        button.addStyleName(ValoTheme.BUTTON_PRIMARY);

        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                eventBus.post(new DocumentCreateWizardRequestEvent());
            }
        });
        return button;
    }

    private com.vaadin.ui.Component createDocumentGrid() {
        CardHolder<DocumentCard, DocumentVO> documentsGrid = new CardHolder<>(messageHelper, langHelper, eventBus, DocumentCard.class);
        dataController = new DataController<>(documentsGrid, DocumentVO.class, DocumentCard.KEY);// must do before creating filters
        return documentsGrid;
    }

    @Override
    public void showCreateDocumentWizard(List<CatalogItem> templates) {
        LOG.debug("Showing the create document wizard...");
        CreateDocumentWizard createDocumentWizard = new CreateDocumentWizard(templates, messageHelper, langHelper, eventBus);
        UI.getCurrent().addWindow(createDocumentWizard);
        createDocumentWizard.focus();
    }

    @Override
    public void openContributorsWindow(List<UserVO> allUsers, DocumentVO docDetails) {
        LOG.debug("Showing the select contributors window...");
        EditContributorWindow editContributorWindow = new EditContributorWindow( messageHelper, eventBus, allUsers, docDetails );
        UI.getCurrent().addWindow(editContributorWindow);
        editContributorWindow.focus();
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        LOG.debug("Entering {} view...", getViewId());
        eventBus.post(new EnterRepositoryViewEvent());
    }

    @Override
    public void setSampleDocuments(final List<DocumentVO> documents) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        dataController.updateAll(documents);
        defaultSort();
        LOG.trace("time taken in setting document list :{} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Subscribe
    public void showDialogForDelete(final DeleteDocumentRequest event) {
        // ask confirmation before delete
        ConfirmDialog confirmDialog = ConfirmDialog.getFactory().create(
                messageHelper.getMessage("delete.confirmation.title"),
                messageHelper.getMessage("delete.confirmation.message", event.getTitle()),
                messageHelper.getMessage("delete.confirmation.confirm"),
                messageHelper.getMessage("delete.confirmation.cancel"), null);
        confirmDialog.setContentMode(ConfirmDialog.ContentMode.HTML);

        confirmDialog.show(getUI(),
                new ConfirmDialog.Listener() {
                    private static final long serialVersionUID = -1441968814274639L;

                    public void onClose(ConfirmDialog dialog) {
                        if (dialog.isConfirmed()) {
                            eventBus.post(new DeleteDocumentEvent(event.getDocumentId()));
                        }
                    }
                }, true);
    }

    private void defaultSort(){
        dataController.sortCards(new Object[]{"updatedOn"}, new boolean[]{false});
    }

    @Override
    public @Nonnull String getViewId() {
        return VIEW_ID;
    }

    @Override
    public void updateLockInfo(final DocumentVO udpatedDocVO) {
        getUI().access(new Runnable() {
            @Override
            public void run() {
                // TODO fix to hide the implimentation details
                dataController.udpateProperty(udpatedDocVO.getLeosId(), "msgForUser", udpatedDocVO.getMsgForUser());
            }
        });
    }

    public void showDisclaimer() {
        NotificationEvent disclaimer=new NotificationEvent(NotificationEvent.Type.DISCLAIMER,"prototype.disclaimer.description");
        disclaimer.setCaptionKey("prototype.disclaimer.caption");
        eventBus.post(disclaimer);
    }

    @Override
    public void attach() {
        super.attach();
        // enable polling
        setPollingStatus(true);
    }

    @Override
    public void detach() {
        super.detach();
        // disable polling
        setPollingStatus(false);
    }
}
