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

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.data.provider.CallbackDataProvider;
import com.vaadin.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.*;
import com.vaadin.ui.declarative.Design;
import eu.europa.ec.leos.domain.cmis.document.Proposal;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.domain.vo.ValidationVO;
import eu.europa.ec.leos.i18n.LanguageHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.filter.QueryFilter;
import eu.europa.ec.leos.security.LeosPermissionAuthorityMapHelper;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.ui.view.TriFunction;
import eu.europa.ec.leos.ui.wizard.document.UploadDocumentWizard;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.web.event.view.repository.RefreshDisplayedListEvent;
import eu.europa.ec.leos.web.event.view.repository.ShowPostProcessingMandateEvent;
import eu.europa.ec.leos.web.event.view.repository.ShowProposalValidationEvent;
import eu.europa.ec.leos.web.support.user.UserHelper;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@DesignRoot("WorkspaceScreenDesign.html")
abstract class WorkspaceScreenImpl extends HorizontalLayout implements WorkspaceScreen {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceScreenImpl.class);
    protected final SecurityContext securityContext;
    protected final EventBus eventBus;
    protected final MessageHelper messageHelper;
    protected final LanguageHelper langHelper;
    protected final UserHelper userHelper;
    protected final LeosPermissionAuthorityMapHelper authorityMapHelper;
    protected WorkspaceOptions workspaceOptions;
    protected Label filterCaption;
    protected Button resetButton;
    protected Button refreshButton;
    protected Button createDocumentButton;
    protected Button uploadDocumentButton;
    protected Button sortAscButton;
    protected Button sortDescButton;
    protected TextField searchBox;
    protected VerticalLayout documentHolder;
    private CallbackDataProvider<Proposal, QueryFilter> dataProvider;
    private TriFunction<Integer, Integer, QueryFilter, Stream<Proposal>> dataFn;
    private Function<QueryFilter, Integer> countFn;

    WorkspaceScreenImpl(SecurityContext securityContext, EventBus eventBus,
                        MessageHelper messageHelper, LanguageHelper langHelper, UserHelper userHelper, LeosPermissionAuthorityMapHelper authorityMapHelper) {
        LOG.trace("Initializing repository screen...");
        Validate.notNull(securityContext, "SecurityContext must not be null!");
        this.securityContext = securityContext;
        Validate.notNull(eventBus, "EventBus must not be null!");
        this.eventBus = eventBus;
        Validate.notNull(messageHelper, "MessageHelper must not be null!");
        this.messageHelper = messageHelper;
        Validate.notNull(langHelper, "LanguageHelper must not be null!");
        this.langHelper = langHelper;
        Validate.notNull(userHelper, "UserHelper must not be null!");
        this.userHelper = userHelper;
        this.authorityMapHelper = authorityMapHelper;

        Design.read(this);
        initStaticData();
        initListeners();
    }

    @Override
    public void attach() {
        eventBus.register(this);
        super.attach();
        LOG.trace("Attaching repository screen...");
    }

    @Override
    public void detach() {
        super.detach();
        eventBus.unregister(this);
        LOG.trace("Detaching repository screen...");
    }

    @Override
    public void setDataFunctions(TriFunction<Integer, Integer, QueryFilter, Stream<Proposal>> dataFn,
                                 Function<QueryFilter, Integer> countFn) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        this.dataFn = dataFn;
        this.countFn = countFn;

        // For use with some external filter, e.g. a search form
        ConfigurableFilterDataProvider<Proposal, Void, QueryFilter>
                everythingConfigurable = dataProvider.withConfigurableFilter();
        everythingConfigurable.setFilter(workspaceOptions.getQueryFilter());

        LOG.trace("time taken in setting document list :{} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Override
    public void intializeFiltersWithData(List<CatalogItem> catalogItems) {
        workspaceOptions.initializeSearchBox(searchBox);// handing over control to workspace Options
        workspaceOptions.intializeOptions(catalogItems);
    }

    @Override
    public void showUploadDocumentWizard() {
        LOG.debug("Showing the upload document wizard...");
        UploadDocumentWizard uploadDocumentWizard = new UploadDocumentWizard(messageHelper, langHelper, eventBus);
        UI.getCurrent().addWindow(uploadDocumentWizard);
        uploadDocumentWizard.focus();
    }

    private void initStaticData() {
        filterCaption.setValue(messageHelper.getMessage("repository.caption.filters"));
        resetButton.setCaption(messageHelper.getMessage("repository.caption.filters.reset"));
        resetButton.setDescription(messageHelper.getMessage("repository.caption.filters.reset"));

        refreshButton.setDisableOnClick(true);
        refreshButton.setDescription(messageHelper.getMessage("repository.refresh.button.tooltip"));

        sortAscButton.setDescription(messageHelper.getMessage("repository.filter.tooltip.sortGroup"));
        sortDescButton.setDescription(messageHelper.getMessage("repository.filter.tooltip.sortGroup"));

        uploadDocumentButton.setCaption(messageHelper.getMessage("repository.upload.document"));
        uploadDocumentButton.setDescription(messageHelper.getMessage("repository.upload.document.tooltip"));

        initDocumentsHolder();
    }

    private void initDocumentsHolder() {
        dataProvider =
                DataProvider.fromFilteringCallbacks(
                        query -> {
                            QueryFilter filter = workspaceOptions.getQueryFilter();
                            LOG.debug("dataFn requested from offset:{},next:{}", query.getOffset(), query.getLimit());

                            return dataFn.apply(
                                    query.getOffset(),
                                    query.getLimit(),
                                    filter
                            );
                        },
                        query -> {
                            QueryFilter filter = workspaceOptions.getQueryFilter();

                            int i = countFn.apply(filter);
                            LOG.debug("countFn requested {}", i);
                            return i;
                        }
                );

        Grid<Proposal> grid = new Grid<>();
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        Grid.Column column = grid.addComponentColumn(proposal -> {
            ProposalCard card = new ProposalCard(proposal, userHelper, messageHelper, langHelper, eventBus);
            card.setHeightUndefined();
            return card;
        });
        grid.setDefaultHeaderRow(null);
        grid.setDataProvider(dataProvider);
        grid.setWidth("100%");
        grid.setBodyRowHeight(135);//px
        grid.setHeightMode(HeightMode.UNDEFINED);
        while (grid.getHeaderRowCount() > 0) {
            grid.removeHeaderRow(0);
        }

        documentHolder.addComponentsAndExpand(grid);
    }

    private void initListeners() {
        refreshButton.addClickListener(clickEvent -> {
            refreshData();
            if (!refreshButton.isEnabled()) {
                refreshButton.setEnabled(true);
            }
        });

        Button.ClickListener bc = new Button.ClickListener() {
            final static String ENABLED_STYLE = "enabled";

            @Override
            public void buttonClick(Button.ClickEvent event) {
                Button sourceButton = event.getButton();
                if (sourceButton.getData() == null) {// first click
                    sourceButton.addStyleName(ENABLED_STYLE);
                    sourceButton.setData(sourceButton.getId());
                    resetSortButton(sourceButton.equals(sortAscButton) ? sortDescButton : sortAscButton);
                    workspaceOptions.setTitleSortOrder(sourceButton.equals(sortAscButton));
                } else {// already sorted then remove the sort
                    workspaceOptions.setTitleSortOrder(false);
                    resetSortButton(sourceButton);
                }
            }

            private void resetSortButton(Button button) {
                button.setData(null);
                button.removeStyleName(ENABLED_STYLE);
            }
        };
        sortAscButton.addClickListener(bc);
        sortDescButton.addClickListener(bc);
        resetButton.addClickListener(workspaceOptions::resetFilters);
        uploadDocumentButton.addClickListener(clickEvent -> this.showUploadDocumentWizard());
    }

    @Override
    public void refreshData() {
        dataProvider.refreshAll();
    }

    @Subscribe
    public void refreshData(RefreshDisplayedListEvent event) {
        dataProvider.refreshAll();
    }

    @Override
    public void showValidationResult(ValidationVO result) {
        eventBus.post(new ShowProposalValidationEvent(result));
    }

    @Override
    public void showPostProcessingResult(Result result) {
        eventBus.post(new ShowPostProcessingMandateEvent(result));
    }
}
