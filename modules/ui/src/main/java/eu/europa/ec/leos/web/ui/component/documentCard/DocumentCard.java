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
package eu.europa.ec.leos.web.ui.component.documentCard;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.PopupView;
import eu.europa.ec.leos.model.content.LeosDocumentProperties.Stage;
import eu.europa.ec.leos.model.user.Permission;
import eu.europa.ec.leos.web.event.view.repository.DeleteDocumentRequest;
import eu.europa.ec.leos.web.event.view.repository.EditContributorRequest;
import eu.europa.ec.leos.web.event.view.repository.SelectDocumentEvent;
import eu.europa.ec.leos.web.event.view.repository.StageChangeRequest;
import eu.europa.ec.leos.web.model.DocumentVO;
import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.card.Card;
import eu.europa.ec.leos.web.ui.converter.StageIconConverter;
import eu.europa.ec.leos.web.ui.converter.UserDisplayConverter;

import java.util.List;

public class DocumentCard extends DocumentCardDesign implements Card {
    private static final long serialVersionUID = 9091707850925912485L;

    public static final String KEY = "leosId";
    private MessageHelper messageHelper;
    private LanguageHelper langHelper;
    private EventBus eventBus;
    private Item itemDataSource;
    private List<Permission> permissions;

    public DocumentCard() {
        super();
    }

    @Override
    public DocumentCard init(Object... args) {
        if (args.length < 3) throw new IllegalArgumentException("Incorrect number of arguments");
        this.messageHelper = (MessageHelper) args[0];
        this.langHelper = (LanguageHelper) args[1];
        this.eventBus = (EventBus) args[2];
        initializeStaticData();
        initializeEventListeners();
        return this;
    }

    private void initializeStaticData() {
        lockButton.addStyleName("hidden");
        createdBy.setConverter(new UserDisplayConverter());
        language.setConverter(new LangCodeToDescriptionConverter(langHelper));
        docIcon.setContentMode(ContentMode.HTML);
        docIcon.setConverter(new StageIconConverter());
        stage.init(messageHelper, eventBus);
        createdCaption.setValue(messageHelper.getMessage("repository.caption.created"));
        updatedCaption.setValue(messageHelper.getMessage("repository.caption.updated"));
        language.setCaption(messageHelper.getMessage("card.caption.language"));
        addContributorButton.setCaption(messageHelper.getMessage("card.button.caption.addcontributor"));
        openButton.setCaption(messageHelper.getMessage("card.button.caption.open"));
        deleteButton.setCaption(messageHelper.getMessage("card.button.caption.delete"));
    }

    private void initializeEventListeners() {
        openButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                eventBus.post(new SelectDocumentEvent((String) itemDataSource.getItemProperty(KEY).getValue(),
                        (Stage)stage.getPropertyDataSource().getValue()));
            }
        });
        addContributorButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                eventBus.post(new EditContributorRequest((DocumentVO) ((BeanItem) itemDataSource).getBean()));
            }
        });

        deleteButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                eventBus.post(new DeleteDocumentRequest((String) itemDataSource.getItemProperty(KEY).getValue(),
                                                        (String) itemDataSource.getItemProperty("title").getValue()));

                    }
        });

        stage.addPopupVisibilityListener(new PopupView.PopupVisibilityListener() {
            @Override
            public final void popupVisibilityChange(final PopupView.PopupVisibilityEvent event) {
                Stage updatedStage = stage.getSelectedStage();
                Stage originalStage=(Stage)itemDataSource.getItemProperty("stage").getValue();
                String documentKey=(String) itemDataSource.getItemProperty(KEY).getValue();

                if( !event.isPopupVisible() && !originalStage.equals(updatedStage)) {
                    eventBus.post(new StageChangeRequest(documentKey, updatedStage));
                }
            }
        });
    }

    @Override
    public void setItemDataSource(Item item) {
        // Info: following doesnt work on labels :(FieldGroup.bindMemberFields(this);
        itemDataSource = item;
        template.setPropertyDataSource(item.getItemProperty("template"));
        stage.setPropertyDataSource(item.getItemProperty("stage"));
        docIcon.setPropertyDataSource(item.getItemProperty("stage"));
        title.setPropertyDataSource(item.getItemProperty("title"));
        language.setPropertyDataSource(item.getItemProperty("language"));
        createdBy.setPropertyDataSource(item.getItemProperty("author"));
        createdOn.setPropertyDataSource(item.getItemProperty("createdOn"));
        updatedBy.setPropertyDataSource(item.getItemProperty("updatedBy"));
        updatedOn.setPropertyDataSource(item.getItemProperty("updatedOn"));
        lockButton.setPropertyDataSource(item.getItemProperty("msgForUser"));
        permissions = (List<Permission>)item.getItemProperty("permissions").getValue();
        // contributorList only reacts to change in list members.. not to any data change inside list member
        if (permissions.contains(Permission.GRANT)) {
            contibutorList.init(messageHelper, eventBus, (String) item.getItemProperty(KEY).getValue());
            contibutorList.setPropertyDataSource(item.getItemProperty("contributors"));
        }
        // set data bsaed on values.these values are static and will not be changed once card is created
        addStyleName(item.getItemProperty("stage").getValue().toString().toLowerCase());
        applySecurity();
    }

    private void applySecurity() {
        if (!permissions.contains(Permission.DELETE)) deleteButton.setVisible(false);
        if (!permissions.contains(Permission.GRANT)) addContributorButton.setVisible(false);
        if (!permissions.contains(Permission.GRANT)) contibutorList.setVisible(false);
    }

    @Override
    public Item getItemDataSource() {
        return itemDataSource;
    }

}
