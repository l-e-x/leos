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
package eu.europa.ec.leos.web.ui.component.card;

import com.google.common.eventbus.EventBus;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.declarative.Design;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.ui.Label;
import com.vaadin.v7.ui.VerticalLayout;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.web.event.view.repository.SelectDocumentEvent;
import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.converter.LangCodeToDescriptionConverter;
import eu.europa.ec.leos.web.ui.converter.UserLoginDisplayConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;

@SpringComponent
@Scope("prototype")
@SuppressWarnings("serial")
@DesignRoot("ProposalCardDesign.html")
public class ProposalCard extends VerticalLayout implements Card{
    protected Label title;
    protected Label language;
    protected Label createdBy;
    protected Label createdOn;
    protected Label updatedBy;
    protected Label updatedOn;
    protected Label createdCaption;
    protected Label updatedCaption;

    protected VerticalLayout actions;
    protected Button openButton;

    private MessageHelper messageHelper;
    private LanguageHelper langHelper;
    private UserHelper userHelper;
    private EventBus eventBus;
    private Item itemDataSource;

    public static final String KEY = "id";  // KLUGE temporary hack for compatibility with new domain model

    @Autowired
    public ProposalCard(UserHelper userHelper, MessageHelper messageHelper, LanguageHelper langHelper, EventBus eventBus) {
        this.messageHelper = messageHelper;
        this.langHelper = langHelper;
        this.eventBus = eventBus;
        this.userHelper = userHelper;

        Design.read(this);
    }

    @PostConstruct
    private void init() {
        initializeStaticData();
        initializeEventListeners();
    }

    private void initializeStaticData() {
        // FIXME createdBy & updatedBy must use the same converter
        // KLUGE temporary hack for compatibility with new domain model
        // createdBy.setConverter(new UserDisplayConverter());
        language.setConverter(new LangCodeToDescriptionConverter(langHelper));
        createdCaption.setValue(messageHelper.getMessage("repository.caption.created"));
        updatedCaption.setValue(messageHelper.getMessage("repository.caption.updated"));
        language.setCaption(messageHelper.getMessage("card.caption.language"));
        openButton.setCaption(messageHelper.getMessage("leos.button.open"));
    }

    private void initializeEventListeners() {
        openButton.addClickListener(event -> {
            // KLUGE temporary hack for compatibility with new domain model
            eventBus.post(new SelectDocumentEvent((String) itemDataSource.getItemProperty(KEY).getValue(), LeosCategory.PROPOSAL));
        });
    }

    @Override
    public void setItemDataSource(Item item) {
        // Info: following doesnt work on labels :(FieldGroup.bindMemberFields(this);
        itemDataSource = item;
        title.setPropertyDataSource(item.getItemProperty("title"));
        language.setPropertyDataSource(item.getItemProperty("language"));
        createdBy.setConverter(new UserLoginDisplayConverter(userHelper));
        createdBy.setPropertyDataSource(item.getItemProperty("createdBy"));
        createdOn.setPropertyDataSource(item.getItemProperty("createdOn"));
        updatedBy.setConverter(new UserLoginDisplayConverter(userHelper));
        updatedBy.setPropertyDataSource(item.getItemProperty("updatedBy"));
        updatedOn.setPropertyDataSource(item.getItemProperty("updatedOn"));

        addStyleName("proposal");
    }

    @Override
    public Item getItemDataSource() {
        return itemDataSource;
    }
}