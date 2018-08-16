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
package eu.europa.ec.leos.ui.view.repository;

import com.google.common.base.Stopwatch;
import com.google.common.eventbus.EventBus;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import com.vaadin.ui.declarative.Design;
import com.vaadin.v7.data.util.converter.Converter;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.v7.ui.TextField;
import com.vaadin.v7.ui.VerticalLayout;
import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.domain.vo.ValidationVO;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.ui.model.RepositoryType;
import eu.europa.ec.leos.ui.wizard.document.CreateDocumentWizard;
import eu.europa.ec.leos.ui.wizard.document.UploadDocumentWizard;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.web.event.view.repository.*;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.card.CardHolder;
import eu.europa.ec.leos.web.ui.component.card.DataController;
import eu.europa.ec.leos.web.ui.component.card.DocumentCard;
import eu.europa.ec.leos.web.ui.component.card.ProposalCard;
import eu.europa.ec.leos.web.ui.converter.ActTypeToDescriptionConverter;
import eu.europa.ec.leos.web.ui.converter.LangCodeToDescriptionConverter;
import eu.europa.ec.leos.web.ui.converter.ProcedureTypeToDescriptionConverter;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@DesignRoot("RepositoryScreenDesign.html")
abstract class RepositoryScreenImpl extends HorizontalLayout implements RepositoryScreen {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(RepositoryScreenImpl.class);

    protected VerticalLayout filterArea;
    protected Button refreshButton;
    protected Button selectDisplayButton;
    protected Button createDocumentButton;
    protected Button uploadDocumentButton;
    protected Button sortAscButton;
    protected Button sortDescButton;
    protected TextField searchBox;
    protected CardHolder cardHolder;

    protected final SecurityContext securityContext;
    protected final EventBus eventBus;
    protected final MessageHelper messageHelper;
    protected final LanguageHelper langHelper;

    protected DataController dataController;

    RepositoryScreenImpl(SecurityContext securityContext, EventBus eventBus,
                         MessageHelper messageHelper, LanguageHelper langHelper) {
        LOG.trace("Initializing repository screen...");
        Validate.notNull(securityContext, "SecurityContext must not be null!");
        this.securityContext = securityContext;
        Validate.notNull(eventBus, "EventBus must not be null!");
        this.eventBus = eventBus;
        Validate.notNull(messageHelper, "MessageHelper must not be null!");
        this.messageHelper = messageHelper;
        Validate.notNull(langHelper, "LanguageHelper must not be null!");
        this.langHelper = langHelper;

        Design.read(this);
        initStaticData();
        initListeners();

        eventBus.register(this);
    }

    @Override
    public void attach() {
        super.attach();
        LOG.trace("Attaching repository screen...");
    }

    @Override
    public void detach() {
        super.detach();
        LOG.trace("Detaching repository screen...");
    }

    @Override
    public void populateData(final List<DocumentVO> documents) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        dataController.updateAll(documents);
        defaultSort();
        LOG.trace("time taken in setting document list :{} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Override
    public void showUploadDocumentWizard() {
        LOG.debug("Showing the upload document wizard...");
        UploadDocumentWizard uploadDocumentWizard = new UploadDocumentWizard(messageHelper, langHelper, eventBus);
        UI.getCurrent().addWindow(uploadDocumentWizard);
        uploadDocumentWizard.focus();
    }

    private void initStaticData() {
        refreshButton.setDisableOnClick(true);
        refreshButton.setDescription(messageHelper.getMessage("repository.refresh.button.tooltip"));

        sortAscButton.setDescription(messageHelper.getMessage("repository.filter.tooltip.sortGroup"));
        sortDescButton.setDescription(messageHelper.getMessage("repository.filter.tooltip.sortGroup"));

        uploadDocumentButton.setCaption(messageHelper.getMessage("repository.upload.document"));
        uploadDocumentButton.setDescription(messageHelper.getMessage("repository.upload.document.tooltip"));
        
        selectDisplayButton.setDisableOnClick(true);
    }
    
    private void initListeners() {
        refreshButton.addClickListener(clickEvent -> {
            eventBus.post(new RefreshDisplayedListEvent());
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
                    resetButton(sourceButton.equals(sortAscButton) ? sortDescButton : sortAscButton);

                    doTitleSort(sourceButton.equals(sortAscButton));
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
        sortAscButton.addClickListener(bc);
        sortDescButton.addClickListener(bc);

        uploadDocumentButton.addClickListener(clickEvent -> this.showUploadDocumentWizard());

        selectDisplayButton.addClickListener(event -> {
            Button sourceButton = event.getButton();
            if (RepositoryType.PROPOSALS.equals(sourceButton.getData())) {// Proposals are open, changing to Document
                    eventBus.post(new ChangeDisplayTypeEvent(RepositoryType.DOCUMENTS));
                }
                else {// Documents are open, changing to Proposals
                    eventBus.post(new ChangeDisplayTypeEvent(RepositoryType.PROPOSALS));
                }
                if (!sourceButton.isEnabled()) {
                    sourceButton.setEnabled(true);
                }
            });
    }

    @Override
    public void setRepositoryType(RepositoryType repositoryType) {
        Class cardType = RepositoryType.PROPOSALS.equals(repositoryType) ? ProposalCard.class : DocumentCard.class;
        String cardKey = RepositoryType.PROPOSALS.equals(repositoryType) ? ProposalCard.KEY : DocumentCard.KEY;
        Class dataType = DocumentVO.class;

        cardHolder.setCardType(cardType);
        dataController = new DataController<>(cardHolder, dataType, cardKey);
        RepositoryFilters filters = new RepositoryFilters<>(messageHelper, langHelper, eventBus,
                filterArea,
                searchBox,
                dataController.getVaadinContainer(),
                getFilterMap(repositoryType),
                securityContext.getUser());

        changeDisplaySelectorButton(repositoryType);
    }

    @Override
    public void showValidationResult(ValidationVO result) {
        eventBus.post(new ShowProposalValidationEvent(result));
    }

    @Override
    public void showPostProcessingResult(Result result) {
        eventBus.post(new ShowPostProcessingMandateEvent(result));
    }

    private void changeDisplaySelectorButton(RepositoryType targetRepositoryType) {
        switch (targetRepositoryType) {
            case DOCUMENTS:
                selectDisplayButton.setData(RepositoryType.DOCUMENTS);
                selectDisplayButton.setIcon(VaadinIcons.FOLDER_OPEN);
                selectDisplayButton.setDescription(messageHelper.getMessage("repository.display.button.tooltip.documents"));
                break;
            case PROPOSALS:
                selectDisplayButton.setData(RepositoryType.PROPOSALS);
                selectDisplayButton.setIcon(VaadinIcons.FOLDER);
                selectDisplayButton.setDescription(messageHelper.getMessage("repository.display.button.tooltip.proposals"));
                break;
        }
    }

    private Map<String, Converter> getFilterMap(RepositoryType repositoryType) {
        Map<String, Converter> filterPropertyToConverterMap = new LinkedHashMap<>();
        if (RepositoryType.DOCUMENTS.equals(repositoryType)) {
            filterPropertyToConverterMap.put("template", null);
            filterPropertyToConverterMap.put("language", new LangCodeToDescriptionConverter(langHelper));
        }
        else if (RepositoryType.PROPOSALS.equals(repositoryType)) {
            filterPropertyToConverterMap.put("procedureType", new ProcedureTypeToDescriptionConverter(messageHelper));
            filterPropertyToConverterMap.put("actType", new ActTypeToDescriptionConverter(messageHelper));
            filterPropertyToConverterMap.put("language", new LangCodeToDescriptionConverter(langHelper));
        }
        return filterPropertyToConverterMap;
    }

    protected void defaultSort() {
        dataController.sortCards(new Object[]{"updatedOn"}, new boolean[]{false});
    }
}
