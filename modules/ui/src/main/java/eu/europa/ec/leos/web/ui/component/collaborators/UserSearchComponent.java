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
package eu.europa.ec.leos.web.ui.component.collaborators;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.server.ErrorMessage;
import com.vaadin.v7.data.Validator;
import com.vaadin.v7.data.util.BeanItemContainer;
import com.vaadin.v7.shared.ui.combobox.FilteringMode;
import com.vaadin.v7.ui.AbstractSelect;
import eu.europa.ec.leos.web.event.view.proposal.SearchUserRequest;
import eu.europa.ec.leos.web.event.view.proposal.SearchUserResponse;
import eu.europa.ec.leos.web.model.UserVO;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

class UserSearchComponent extends InputListeningComboBox {
    private static final Logger LOG = LoggerFactory.getLogger(UserSearchComponent.class);

    private List<UserVO> existingUsers;

    private EventBus eventBus;
    private MessageHelper messageHelper;

    UserSearchComponent(MessageHelper messageHelper, EventBus eventBus) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        init();
    }

    @Override
    public void attach() {
        super.attach();
        eventBus.register(this);
    }

    @Override
    public void detach() {
        eventBus.unregister(this);
        super.detach();
    }

    // This methods cleans the drop down and reloads the last response
    @Subscribe
    void updateUserDropDown(SearchUserResponse event) {
        List<UserVO> users = event.getUsers();
        if (validateProposedUsers(users)) {
            users.forEach(getContainerDataSource()::addItem);
        }
    }

    void setExistingUsers(List<UserVO> existingUsers) {
        this.existingUsers = existingUsers;
    }

    private void init() {
        setItemCaptionMode(AbstractSelect.ItemCaptionMode.PROPERTY);
        setItemCaptionPropertyId("name"); // field of User bean

        setInputPrompt(messageHelper.getMessage("collaborator.editor.name.prompt"));
        setDescription(messageHelper.getMessage("collaborator.editor.name.desc"));
        setImmediate(true);
        setNewItemsAllowed(true);
        setNullSelectionAllowed(false);
        setValidationVisible(false);

        BeanItemContainer<UserVO> nameContainer = new BeanItemContainer<>(UserVO.class); // separate container as it is a separate list
        setContainerDataSource(nameContainer);
        setFilteringMode(FilteringMode.OFF);// Default filtering mode MIGHT interfere with constantly changing suggestions
        addInputChangeListener(e -> handleUserInputChange(e.getText()));

        addValidator(value -> {
            UserVO user = (UserVO) value;
            if (user == null || StringUtils.isEmpty(user.getName())) {// Not using validators as valudators are fired at binding time also
                throw new Validator.EmptyValueException(messageHelper.getMessage("collaborator.editor.name.error.empty"));
            } else if (existingUsers.contains(user)) {
                throw new Validator.InvalidValueException(messageHelper.getMessage("collaborator.editor.name.already.present"));
            }
        });
    }

    private void handleUserInputChange(String userInput) {
        // clear the existing suggestions and errors, if any
        resetSearchBox();

        // we can do much more here ie buffering , not firing request if we have already fired a broader search query
        // but optimization needs to be done when functionality is achieved
        int MIN_INPUT_LENGTH = 3;
        if ((userInput != null) && (userInput.length() >= MIN_INPUT_LENGTH)) {
            LOG.debug("New Request for searching users with key:{}", userInput);
            eventBus.post(new SearchUserRequest(userInput));
        }
        else {
            setSearchBoxError("collaborator.editor.name.search.guidance");
        }
    }

    private void resetSearchBox() {
        clearSearchBoxError();
        getContainerDataSource().removeAllItems();
    }

    private void clearSearchBoxError() {
        setComponentError(null);// clears the message
    }

    // clears or sets the error on the search box
    private void setSearchBoxError(String errorMessage) {
        clearSearchBoxError();// to keep only one latest error visible
        if (errorMessage != null) {
            setComponentError(new ErrorMessage() {
                @Override
                public ErrorLevel getErrorLevel() {
                    return ErrorLevel.ERROR;
                }

                @Override
                public String getFormattedHtmlMessage() {
                    return messageHelper.getMessage(errorMessage);
                }
            });
        }
    }

    private boolean validateProposedUsers(List<UserVO> users) {
        boolean isValid = true;
        if (users == null || users.size() <= 0) {
            setSearchBoxError("collaborator.editor.name.not.found");
            isValid = false;
        }
        return isValid;
    }
}

