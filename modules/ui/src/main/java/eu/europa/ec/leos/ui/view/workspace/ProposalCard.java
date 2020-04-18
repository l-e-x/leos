/*
 * Copyright 2019 European Commission
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
package eu.europa.ec.leos.ui.view.workspace;

import com.google.common.eventbus.EventBus;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.declarative.Design;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.document.Proposal;
import eu.europa.ec.leos.i18n.LanguageHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.web.event.view.repository.SelectDocumentEvent;
import eu.europa.ec.leos.web.support.user.UserHelper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

@DesignRoot("ProposalCardDesign.html")
public class ProposalCard extends VerticalLayout {
    private static final long serialVersionUID = -3728040115432884863L;

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
    private Proposal proposal;

    @Autowired
    public ProposalCard(Proposal proposal,
                        UserHelper userHelper,
                        MessageHelper messageHelper,
                        LanguageHelper langHelper,
                        EventBus eventBus) {
        this.messageHelper = messageHelper;
        this.langHelper = langHelper;
        this.eventBus = eventBus;
        this.userHelper = userHelper;

        Design.read(this);
        this.proposal = proposal;

        init();
        setData(proposal);
    }

    private void init() {
        initializeStaticData();
        initializeEventListeners();
    }

    private void initializeStaticData() {
        createdCaption.setValue(messageHelper.getMessage("repository.caption.created"));
        updatedCaption.setValue(messageHelper.getMessage("repository.caption.updated"));
        language.setCaption(messageHelper.getMessage("card.caption.language"));
        openButton.setCaption(messageHelper.getMessage("leos.button.open"));
    }

    private void initializeEventListeners() {
        Runnable clickEvent = () -> eventBus.post(new SelectDocumentEvent(proposal.getMetadata().getOrError(() -> "Proposal metadata is not available!").getRef(), LeosCategory.PROPOSAL));
        addLayoutClickListener(event -> clickEvent.run());
        openButton.addClickListener(event -> clickEvent.run());
    }

    public void setData(Proposal item) {
        // Info: following doesnt work on labels :(FieldGroup.bindMemberFields(this);
        title.setValue(proposal.getTitle());
        language.setValue(langHelper.getLanguageDescription(proposal.getMetadata().get().getLanguage()));

        createdBy.setValue(userHelper.convertToPresentation(proposal.getInitialCreatedBy()));
        updatedBy.setValue(userHelper.convertToPresentation(proposal.getLastModifiedBy()));

        createdOn.setValue(Date.from(proposal.getInitialCreationInstant()).toString());
        updatedOn.setValue(Date.from(proposal.getLastModificationInstant()).toString());

        addStyleName("proposal");
    }
}