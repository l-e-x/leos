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
import com.vaadin.data.Item;
import com.vaadin.ui.Table;
import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

public class CardFactory<CARD extends Card> implements Table.ColumnGenerator {
    private static final long serialVersionUID = 8610707033723162234L;
    private MessageHelper messageHelper;
    private EventBus eventBus;
    private LanguageHelper langHelper;
    private Class cardClass;

    public CardFactory(MessageHelper messageHelper, LanguageHelper langHelper, EventBus eventBus, Class cardClass) {
        super();
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.langHelper=langHelper;
        this.cardClass = cardClass;
    }

    @Override public Object generateCell(Table source, Object itemId, Object columnId) {
        Item item = source.getItem(itemId);
        CARD card;
        try {
            card = (CARD) cardClass.newInstance();

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        card.init(messageHelper, langHelper, eventBus);
        card.setItemDataSource(item);
        return card;
    }
}
