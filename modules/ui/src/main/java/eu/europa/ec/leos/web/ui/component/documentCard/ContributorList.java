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
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;

import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import eu.europa.ec.leos.vo.UserVO;
import eu.europa.ec.leos.web.event.view.repository.RemoveContributorRequest;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.card.ListTypeProperty;

public class ContributorList extends ListTypeProperty<UserVO> {
    private static final long serialVersionUID = 37518407800033L;
    protected MessageHelper messageHelper;
    protected EventBus eventBus;
    private String docId;

    public ContributorList() {
        super();
    }

    public void init(MessageHelper messageHelper, EventBus eventBus, String docId) {
        this.eventBus = eventBus;
        this.messageHelper = messageHelper;
        this.docId = docId;
    }

    @Override
    protected Component createRepresentation(final UserVO userVO) {

        Button btn = new Button();
        btn.setData(userVO.getId());
        btn.setDescription(messageHelper.getMessage("contributor.remove.button.tooltip", userVO.getName()));
        btn.setIcon(FontAwesome.MINUS_CIRCLE);
        btn.setStyleName("icon-only link center icon-red");
        btn.setWidth("100%");
        btn.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                eventBus.post(new RemoveContributorRequest(docId, userVO));
            }
        });

        Label contributorName= new Label(userVO.getName());
        contributorName.setDescription(userVO.getName());

        HorizontalLayout contributorLayout=new HorizontalLayout();
        contributorLayout.setData(userVO.getId());
        contributorLayout.addComponent(btn);
        contributorLayout.addComponent(contributorName);
        return contributorLayout;
    }
}
