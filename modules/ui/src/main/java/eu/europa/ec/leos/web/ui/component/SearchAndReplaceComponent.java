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
package eu.europa.ec.leos.web.ui.component;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.Binder;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import eu.europa.ec.leos.web.event.view.document.SearchAndReplaceTextEvent;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

public class SearchAndReplaceComponent extends PopupView {

    private static final long serialVersionUID = -7318940290911926353L;
    
    private MessageHelper messageHelper;
    private EventBus eventBus;
    
    private VerticalLayout popupLayout;
    private TextField search;
    private TextField replace;
    private Binder<SearchBox> searchBoxBinder;
    
    public SearchAndReplaceComponent(MessageHelper messageHelper, EventBus eventBus) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        
        init();
    }
    
    public void init() {
        
        setStyleName("search-and-replace-min");
        setHideOnMouseOut(false);
        setHeight(100, Unit.PERCENTAGE);
        
        buildMaximizedLayout();
        
        PopupView.Content content = createContent(VaadinIcons.SEARCH.getHtml(), popupLayout);
        setContent(content);
    }

    private void buildMaximizedLayout() {
        popupLayout = new VerticalLayout();
        popupLayout.setWidth(400.0f, Unit.PIXELS);
        popupLayout.setSpacing(false);
        popupLayout.setMargin(true);
        popupLayout.setStyleName("search-and-replace-max");
        
        Label popupCaption = new Label(messageHelper.getMessage("document.popup.search.title"), ContentMode.HTML);
        popupLayout.addComponent(popupCaption);
        popupLayout.addComponent( buildFormLayout());
        popupLayout.addComponent(buildBtnLayout());
    }

    private FormLayout buildFormLayout() {
        FormLayout  formLayout = new FormLayout();
        searchBoxBinder = new Binder<>();
        String regex = "([^'\"]*'[^\"]*)|(\"[^']*)|([^'\"]*)"; //regex allows single quote or double quote but not both.
        
        search =  new TextField(messageHelper.getMessage("document.popup.search.caption"));
        search.setWidth(100, Unit.PERCENTAGE);
        search.setRequiredIndicatorVisible(true);
        
        searchBoxBinder.forField(search)
        .asRequired(messageHelper.getMessage("document.popup.search.required.error"))
        .withValidator(new RegexpValidator(messageHelper.getMessage("document.popup.search.invalid.error"), regex))
        .bind(SearchBox :: getSearchText, SearchBox :: setSearchText);
        
        formLayout.addComponent(search);
        
        replace = new TextField(messageHelper.getMessage("document.popup.replace.caption"));
        replace.setWidth(100, Unit.PERCENTAGE);
        replace.setRequiredIndicatorVisible(true);
        
        searchBoxBinder.forField(replace)
        .asRequired(messageHelper.getMessage("document.popup.replace.required.error"))
        .bind(SearchBox :: getReplaceText, SearchBox :: setReplaceText);
        
        formLayout.addComponent(replace);
        
        return formLayout;
    }

    private HorizontalLayout buildBtnLayout() {
        HorizontalLayout btnLayout = new HorizontalLayout();
        btnLayout.setWidth(100, Unit.PERCENTAGE);
        btnLayout.setSpacing(false);

        Label spacer = new Label("&nbsp;", ContentMode.HTML);
        btnLayout.addComponent(spacer);
        btnLayout.setExpandRatio(spacer, 0.5f);
        
        Button replaceAllBtn = buildReplaceAllButton();
        btnLayout.addComponent(replaceAllBtn);
        btnLayout.setComponentAlignment(replaceAllBtn, Alignment.BOTTOM_RIGHT);
        btnLayout.setExpandRatio(replaceAllBtn, 0.25f);
        
        Button cancelBtn = buildCancelButton();
        btnLayout.addComponent(cancelBtn);
        btnLayout.setComponentAlignment(cancelBtn, Alignment.BOTTOM_RIGHT);
        btnLayout.setExpandRatio(cancelBtn, 0.25f);

        return btnLayout;
    }

    private Button buildReplaceAllButton() {
        // create replace all button
        Button replaceAllButton = new Button(messageHelper.getMessage("document.popup.replace.button"));
        replaceAllButton.setStyleName("primary");
        replaceAllButton.addClickListener(event -> {
            clearSearchBoxError();
            if(searchBoxBinder.validate().isOk()) {
                clearSearchBoxError();
                eventBus.post(new SearchAndReplaceTextEvent(search.getValue(), replace.getValue()));
            }
        });
        return replaceAllButton;
    }

    private Button buildCancelButton() {
        //create replace all button
        Button cancelButton = new Button(messageHelper.getMessage("document.popup.cancel.button"));
        cancelButton.setDescription(messageHelper.getMessage("document.popup.cancel.button.description"));
        cancelButton.addClickListener(event -> {
            resetSearchBox();
            setPopupVisible(false);
        });
        return cancelButton;
    }

    private void resetSearchBox() {
        search.clear();
        replace.clear();
        clearSearchBoxError();
    }
    
    private void clearSearchBoxError() {
        search.setComponentError(null);
        replace.setComponentError(null);
    }
    
    class SearchBox {
        private String searchText;
        private String replaceText;
        
        public String getSearchText() {
            return searchText;
        }
        public void setSearchText(String searchText) {
            this.searchText = searchText;
        }
        public String getReplaceText() {
            return replaceText;
        }
        public void setReplaceText(String replaceText) {
            this.replaceText = replaceText;
        }
    }
}
