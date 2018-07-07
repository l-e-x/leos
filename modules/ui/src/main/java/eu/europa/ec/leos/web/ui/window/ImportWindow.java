/*
 * Copyright 2017 European Commission
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
package eu.europa.ec.leos.web.ui.window;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.ui.extension.ImportElementExtension;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.model.DocType;
import eu.europa.ec.leos.web.model.SearchCriteriaVO;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class ImportWindow extends AbstractWindow {

    private static final long serialVersionUID = -7318940290911926353L;
    private EventBus eventBus;
    public static final String ARTICLE = "article";
    public static final String RECITAL = "recital";
    
    private Label content;
    private Button importBtn;
    private Label importInfo;
    private Button selectAllArticles;
    private Button selectAllRecitals;
    private Binder<SearchCriteriaVO> searchCriteriaBinder;

    public ImportWindow(MessageHelper messageHelper, EventBus eventBus) {
        super(messageHelper, eventBus);
        this.eventBus = eventBus;
        setCaption(messageHelper.getMessage("document.importer.window.title"));
        prepareWindow();
    }

    @Override
    public void attach() {
        super.attach();
        eventBus.register(this);
    }

    @Override
    public void detach() {
        super.detach();
        eventBus.unregister(this);
    }

    public void prepareWindow() {
        setWidth("27.5cm");
        setHeight(95, Unit.PERCENTAGE);
        
        VerticalLayout windowLayout = new VerticalLayout();
        windowLayout.setSizeFull();
        windowLayout.setMargin(new MarginInfo(false, false, false, true));
        setBodyComponent(windowLayout);
        
        windowLayout.addComponent(buildSearchLayout());
        
        Label infoLabel = new Label(messageHelper.getMessage("document.import.element.message"), ContentMode.HTML);
        windowLayout.addComponent(infoLabel);

        final Component textContent = buildContentLayout();
        windowLayout.addComponent(textContent);
        windowLayout.setExpandRatio(textContent, 1.0f);

        //add components to window layout
        selectAllRecitals = buildSelectAllButton(RECITAL);
        addComponentOnLeft(selectAllRecitals);
        selectAllRecitals.setCaption(messageHelper.getMessage("document.import.selectAll.recitals"));
        
        selectAllArticles = buildSelectAllButton(ARTICLE);
        addComponentOnLeft(selectAllArticles);
        selectAllArticles.setCaption(messageHelper.getMessage("document.import.selectAll.articles"));
        
        buildImportInfoLabel();
        buildImportButton();
    }
    
    private Button buildSelectAllButton(String type) {
        Button selectAll = new Button();
        selectAll.setEnabled(false);
        selectAll.addStyleName("leos-import-selectAll");
        selectAll.setData(Boolean.FALSE);
        selectAll.addClickListener(event -> {
            if((boolean) selectAll.getData()) {
                selectAll.removeStyleName("leos-import-selectAll-active");
                selectAll.setData(Boolean.FALSE);
            } else {
                selectAll.addStyleName("leos-import-selectAll-active");
                selectAll.setData(Boolean.TRUE);
            }
            eventBus.post(new SelectAllElementRequestEvent((boolean) selectAll.getData(), type));
        });
        return selectAll;
    }

    private void buildImportInfoLabel() {
        importInfo = new Label();
        importInfo.setContentMode(ContentMode.HTML);
        importInfo.setValue("&nbsp;");
        addComponentAtPosition(importInfo, 1);
    }
    
    private void buildImportButton() {
        importBtn = new Button();
        importBtn.addStyleName("primary");
        importBtn.setCaption(messageHelper.getMessage("document.importer.button.import"));
        importBtn.setStyleName("primary");
        importBtn.setCaptionAsHtml(true);
        importBtn.setEnabled(false);
        addComponentAtPosition(importBtn, 2);
        
        importBtn.addClickListener(event -> {
            eventBus.post(new SelectedElementRequestEvent());
        });
    }

    private HorizontalLayout buildSearchLayout() {
        HorizontalLayout searchLayout = new HorizontalLayout();
        searchLayout.setWidth(100, Unit.PERCENTAGE);
        searchLayout.setMargin(new MarginInfo(false, true, true, false));

        searchCriteriaBinder = new Binder<>();

        NativeSelect<DocType> type = new NativeSelect<>(messageHelper.getMessage("document.importer.type.caption"), Arrays.asList(DocType.values()));
        type.setEmptySelectionAllowed(false);
        type.setRequiredIndicatorVisible(true);
        type.setSelectedItem(DocType.REGULATION); //default

        searchCriteriaBinder.forField(type)
                .asRequired(messageHelper.getMessage("document.importer.type.required.error"))
                .bind(SearchCriteriaVO::getType, SearchCriteriaVO::setType);

        List<String> yearsList = new ArrayList<String>();
        for (int years = Calendar.getInstance().get(Calendar.YEAR); years >= 1980; years--) {
            yearsList.add(String.valueOf(years));
        }

        NativeSelect<String> year = new NativeSelect<>(messageHelper.getMessage("document.importer.year.caption"), yearsList);
        year.setEmptySelectionAllowed(false);
        year.setRequiredIndicatorVisible(true);
        year.setSelectedItem(yearsList.get(0)); //default
        
        searchCriteriaBinder.forField(year)
                .asRequired(messageHelper.getMessage("document.importer.year.required.error"))
                .bind(SearchCriteriaVO::getYear, SearchCriteriaVO::setYear);

        String regex = "^\\d+$"; // regex to look for digits only
        TextField number = new TextField(messageHelper.getMessage("document.importer.number.caption"));
        number.setRequiredIndicatorVisible(true);
        searchCriteriaBinder.forField(number)
                .asRequired(messageHelper.getMessage("document.importer.number.required.error"))
                .withValidator(new RegexpValidator(messageHelper.getMessage("document.importer.number.format.error"), regex))
                .bind(SearchCriteriaVO::getNumber, SearchCriteriaVO::setNumber);

        searchLayout.addComponent(type);
        searchLayout.addComponent(year);
        searchLayout.addComponent(number);
        
        SearchCriteriaVO searchCriteriaBean = new SearchCriteriaVO(type.getValue(), year.getValue(), number.getValue());
        searchCriteriaBinder.readBean(searchCriteriaBean);

        Button searchButton = new Button(messageHelper.getMessage("document.importer.button.search"));
        searchButton.addStyleName("primary");
        searchButton.setDisableOnClick(true);
        
        searchButton.addClickListener(event -> {
            if (searchCriteriaBinder.validate().isOk()) {
                try {
                    searchCriteriaBinder.writeBean(searchCriteriaBean);
                    eventBus.post(new SearchActRequestEvent(searchCriteriaBean));
                    //The search criteria needs to be stored for import to keep the consistent
                    //searched data in case of search and import
                    importBtn.setData(searchCriteriaBean);
                    event.getButton().setEnabled(true);
                    reset();
                } catch (ValidationException e) {
                    e.printStackTrace();
                }
            }
        });
        searchLayout.addComponent(searchButton);
        searchLayout.setComponentAlignment(searchButton, Alignment.BOTTOM_CENTER);

        Label info = new Label(VaadinIcons.INFO_CIRCLE.getHtml(), ContentMode.HTML);
        info.setDescription(messageHelper.getMessage("document.importer.search.info"), ContentMode.HTML);
        info.addStyleName("leos-import-search-info-icon");
        searchLayout.addComponent(info);
        searchLayout.setComponentAlignment(info, Alignment.BOTTOM_LEFT);

        return searchLayout;
    }

    private Component buildContentLayout() {
        // create placeholder to display imported content
        content = new Label();
        content.setContentMode(ContentMode.HTML);
        content.setSizeFull();
        content.setStyleName("leos-import-content");
        
        new ImportElementExtension(eventBus, content);//content extension
        
        return content;
    }

    @Subscribe
    public void getSearchActResponse(SearchActResponseEvent event) {
        String document = event.getDocument();
        Boolean isEnabled = document == null ? false : true;
        content.setValue(document);
        content.addStyleName(LeosCategory.BILL.name().toLowerCase());
        selectAllArticles.setEnabled(isEnabled);
        selectAllRecitals.setEnabled(isEnabled);
    }
    
    @Subscribe
    public void receiveSelectedElement(SelectedElementResponseEvent event) {
        SearchCriteriaVO searchBean = (SearchCriteriaVO) importBtn.getData();
        eventBus.post(new ImportElementRequestEvent(searchBean, event.getElementIds()));
    }
    
    @Subscribe
    public void enableImportButton(ImportSelectionChangeEvent event) {
        int count = event.getCount();
        if(count > 0) {
            importBtn.setEnabled(true);
            importInfo.setValue(messageHelper.getMessage("document.import.selected.element", count));
        } else {
            importBtn.setEnabled(false);
            importInfo.setValue(null);
        }
    }
    
    private void reset() {
        importBtn.setEnabled(false);
        importInfo.setValue(null);
        selectAllArticles.removeStyleName("leos-import-selectAll-active");
        selectAllRecitals.removeStyleName("leos-import-selectAll-active");
        selectAllArticles.setData(Boolean.FALSE);
        selectAllRecitals.setData(Boolean.FALSE);
    }
}
