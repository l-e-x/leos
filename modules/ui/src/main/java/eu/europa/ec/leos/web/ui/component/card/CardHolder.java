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
package eu.europa.ec.leos.web.ui.component.card;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Table;
import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

import java.util.Arrays;

public class CardHolder<CARD extends Card, CARD_DATA> extends CustomComponent {

    private Table cards;
    private MessageHelper messageHelper;
    private LanguageHelper langHelper;
    private EventBus eventBus;
    private Class<CARD> cardClass;
    private static final String SINGLE_COLUMN_NAME = "CARD";

    public CardHolder(MessageHelper messageHelper, LanguageHelper langHelper, EventBus eventBus, Class<CARD> cardClass) {
        this.messageHelper = messageHelper;
        this.langHelper = langHelper;
        this.eventBus = eventBus;
        this.cardClass = cardClass;
        init();
    }

    private void init() {
        cards = new Table();
        cards.setSelectable(false);
        cards.setColumnHeaderMode(Table.ColumnHeaderMode.HIDDEN);
        cards.setSizeFull();
        cards.setStyleName("leos-card-holder");
        cards.addGeneratedColumn(SINGLE_COLUMN_NAME, new CardFactory<CARD>(messageHelper, langHelper, eventBus, cardClass));
        cards.setVisibleColumns(SINGLE_COLUMN_NAME);
        setCompositionRoot(cards);
        setSizeFull();
    }

    public void setContainer(BeanContainer<String, CARD_DATA> container) {
        cards.setContainerDataSource(container, Arrays.asList(SINGLE_COLUMN_NAME));
    }

    public BeanContainer<String, CARD_DATA> getContainer() {
        return (BeanContainer<String, CARD_DATA>) cards.getContainerDataSource();
    }
}
