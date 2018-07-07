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

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.OptionGroup;
import eu.europa.ec.leos.vo.UserVO;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import org.apache.commons.lang3.Validate;

public class RoleFilter implements Container.Filter {
    private MessageHelper messageHelper;
    private OptionGroup contributorGroup ;
    private BeanContainer container;
    private static final String OWNER_PROPERTY ="author";
    private static final String CONTRIBUTOR="none";
    private UserVO owner;

    public RoleFilter(MessageHelper messageHelper, BeanContainer container, UserVO owner) {
        Validate.notNull(owner.getId(), "author can not be null");
        this.owner = owner;
        this.messageHelper = messageHelper;
        this.container = container;

        contributorGroup = createOptionGroupForContributors(OWNER_PROPERTY);
    }

    public OptionGroup getOptionGroup(){
        return contributorGroup;
    }


    private OptionGroup createOptionGroupForContributors(String propertyId) {
        OptionGroup contributorGroup = new OptionGroup(messageHelper.getMessage("repository.caption.filters.role"));
        contributorGroup.setData(propertyId); // will serve as ID of the Option Group
        contributorGroup.addStyleName("right");//to move checkbox to right
        contributorGroup.addStyleName(propertyId);
        contributorGroup.setImmediate(true);
        contributorGroup.setMultiSelect(true);
        contributorGroup.setItemCaptionMode(AbstractSelect.ItemCaptionMode.EXPLICIT);
        contributorGroup.setWidth("100%");

        Item ownerItem = contributorGroup.addItem(owner);
        contributorGroup.setItemCaption(owner, messageHelper.getMessage("repository.caption.filters.role.asOwner"));
        contributorGroup.select(owner);

        Item contributorItem = contributorGroup.addItem(CONTRIBUTOR);
        contributorGroup.setItemCaption(CONTRIBUTOR, messageHelper.getMessage("repository.caption.filters.role.asContributor"));
        contributorGroup.select(CONTRIBUTOR);

        return contributorGroup;
    }

    @Override
    public boolean passesFilter(Object itemId, Item item) throws UnsupportedOperationException {
        // check if the item is selected or notitem.getItemProperty(optionGroup.getData()) checkBoxValue.equals(). !
            Property property = item.getItemProperty(OWNER_PROPERTY);
            ///TODO change logic when user access control is done
            if(property != null && owner.equals(property.getValue())){
                if(contributorGroup.isSelected(owner)){
                    return true;
                }
            }
            else if(contributorGroup.isSelected(CONTRIBUTOR)){ // implicit (!ownerName.equalsIgnoreCase((String)property.getValue())
                return true;
            }

        return false;
    }

    @Override
    public boolean appliesToProperty(Object propertyId) {
        return OWNER_PROPERTY.equals(propertyId);
    }

}
