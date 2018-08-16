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
package eu.europa.ec.leos.web.ui.component.card;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.CustomComponent;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.data.util.BeanContainer;
import com.vaadin.v7.ui.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;

@SpringComponent
@ViewScope
public class CardHolder<CARD extends Card, CARD_DATA> extends CustomComponent implements Table.ColumnGenerator {

    private Table cards;
    private Class<CARD> cardClass;
    private static final String SINGLE_COLUMN_NAME = "CARD";
    private WebApplicationContext webApplicationContext;

    @Autowired
    public CardHolder(WebApplicationContext webApplicationContext) {
        super();
        this.webApplicationContext = webApplicationContext;
    }

    public void setCardType(Class<CARD> cardClass) {
        this.cardClass = cardClass;
        cards = new Table();
        cards.setSelectable(false);
        cards.setColumnHeaderMode(Table.ColumnHeaderMode.HIDDEN);
        cards.setSizeFull();
        cards.setStyleName("leos-card-holder");
        cards.addGeneratedColumn(SINGLE_COLUMN_NAME, this);
        cards.setVisibleColumns(SINGLE_COLUMN_NAME);
        setCompositionRoot(cards);
    }

    public void setContainer(BeanContainer<String, CARD_DATA> container) {
        cards.setContainerDataSource(container, Arrays.asList(SINGLE_COLUMN_NAME));
    }

    public BeanContainer<String, CARD_DATA> getContainer() {
        return (BeanContainer<String, CARD_DATA>) cards.getContainerDataSource();
    }

    @Override
    public Object generateCell(Table source, Object itemId, Object columnId) {
        if (cardClass == null) {
            throw new IllegalStateException("Card type is not available to create card!!");
        }
        Item item = source.getItem(itemId);
        CARD card = (CARD) webApplicationContext.getBean(cardClass);

        card.setItemDataSource(item);
        return card;
    }
}
