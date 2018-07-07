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

import com.google.common.base.Stopwatch;
import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.data.util.AbstractBeanContainer;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/** this class knows how to interact with vaadin container*/
public class DataController<CARD_DATA> {

    private CardHolder cardHolder;
    private static final Logger LOG = LoggerFactory.getLogger(DataController.class);

    public DataController(CardHolder cardHolder, Class<CARD_DATA> dataClass,String beanKey) {
        this.cardHolder = cardHolder;
        injectContainerInHolder(dataClass, beanKey);
    }

    private void injectContainerInHolder(Class dataClass, String beanKey) {
        BeanContainer<String, CARD_DATA> vaadinContainer = new BeanContainer<>(dataClass);
        vaadinContainer.setBeanIdProperty(beanKey);
        cardHolder.setContainer(vaadinContainer);
    }

    public BeanContainer<String, CARD_DATA> getVaadinContainer() {
        return cardHolder.getContainer();
    }

    public CARD_DATA getCard(String beanKey) {
        return getVaadinContainer().getItem(beanKey).getBean();
    }

    /* this method triggers the event chain for container*/
    public void addCard(CARD_DATA cardData) {
        getVaadinContainer().addBean(cardData);
    }

    public void addAll(List<CARD_DATA> cardData) {
        if (cardData != null) {
            getVaadinContainer().addAll(cardData);
        }
    }

    public void remove(CARD_DATA cardData) {
        getVaadinContainer().removeItem(cardData);
    }

    public void removeAll() {
        getVaadinContainer().removeAllItems();
    }

    public void updateAll(List<CARD_DATA> cardData) {
        // save active filters
        Collection<Container.Filter> activeFilters =  new ArrayList<>();
        activeFilters.addAll(getVaadinContainer().getContainerFilters());
        getVaadinContainer().removeAllContainerFilters();

        AbstractBeanContainer.BeanIdResolver beanIdResolver = getVaadinContainer().getBeanIdResolver();

        //getItem Ids for items and find the removed items
        List<String> itemExistingIds = new ArrayList<>();
        itemExistingIds.addAll(getVaadinContainer().getItemIds());

        Map<String, CARD_DATA> newItemMap=new HashMap<>();
        for(CARD_DATA newCard:cardData) {
           newItemMap.put((String) beanIdResolver.getIdForBean(newCard), newCard);
        }

        //update or remove existing items
        for(String oldItemId:itemExistingIds){
            if(! newItemMap.containsKey(oldItemId)){
                getVaadinContainer().removeItem(oldItemId);
            }
            else{
                udpateCard(newItemMap.get(oldItemId));
            }
        }

        // add remaining items
        for(CARD_DATA newCard:cardData) {
            String itemId = (String) beanIdResolver.getIdForBean(newCard);
            if (!itemExistingIds.contains(itemId)) {
                getVaadinContainer().addItem(itemId, newCard);
            }
        }

        // reapply filters
        for (Container.Filter filter : activeFilters) {
            getVaadinContainer().addContainerFilter(filter);
        }
    }

    public void udpateCard(CARD_DATA cardData) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        String itemId = getVaadinContainer().getBeanIdResolver().getIdForBean(cardData);
        BeanItem<CARD_DATA> newItem = new BeanItem<>(cardData);
        BeanItem<CARD_DATA> existingItem = getVaadinContainer().getItem(itemId);

        if (existingItem != null) {// TODO check performance
            for (Object propertyId : existingItem.getItemPropertyIds()) {
                Property oldProperty = existingItem.getItemProperty(propertyId);
                Object oldPropertyValue = existingItem.getItemProperty(propertyId).getValue();
                Object newPropertyValue = newItem.getItemProperty(propertyId).getValue();
                if (oldPropertyValue == null || !oldPropertyValue.equals(newPropertyValue)) {
                    oldProperty.setValue(newPropertyValue);
                }
            }
        }
        LOG.debug("total time taken to update card:{}", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    public void udpateProperty(String beanKey, String propertyKey, Object updatedValue) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Property property = getVaadinContainer().getContainerProperty(beanKey, propertyKey);
        if (property != null && (property.getValue() == null || !property.getValue().equals(updatedValue))) {
            property.setValue(updatedValue);
            LOG.debug("Property set in:{}", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    public void sortCards(Object[] sortByFields, boolean[] ordering) {
        getVaadinContainer().sort(sortByFields, ordering);
    }

    public void removeAllFilters() {
        getVaadinContainer().removeAllContainerFilters();
    }

    public void filterCards(Container.Filter filter) {
        getVaadinContainer().removeAllContainerFilters();
        getVaadinContainer().addContainerFilter(filter);
    }
}
