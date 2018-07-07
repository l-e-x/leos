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
package eu.europa.ec.leos.web.ui.component;

import com.google.common.eventbus.EventBus;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.declarative.Design;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.data.util.ObjectProperty;
import com.vaadin.v7.ui.Label;
import com.vaadin.v7.ui.VerticalLayout;
import eu.europa.ec.leos.web.event.view.annex.OpenAnnexEvent;
import eu.europa.ec.leos.web.event.view.proposal.DeleteAnnexRequest;
import eu.europa.ec.leos.web.event.view.proposal.SaveAnnexMetaDataRequest;
import eu.europa.ec.leos.web.model.DocumentVO;
import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.converter.LangCodeToDescriptionConverter;
import eu.europa.ec.leos.web.ui.converter.UserLoginDisplayConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Date;

@SpringComponent
@Scope("prototype")
@DesignRoot("AnnexBlockDesign.html")
public class AnnexBlockComponent extends VerticalLayout {
    public static SimpleDateFormat dataFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    private static final long serialVersionUID = 1133841334809202933L;

    protected HeadingComponent heading;
    protected Label titleCaption;
    protected EditBoxComponent title;
    protected Button openButton;
    protected Label language;
    protected Label lastUpdated;
    protected Button moveUpButton;
    protected Button moveDownButton;

    private MessageHelper messageHelper;
    private LanguageHelper languageHelper;
    private EventBus eventBus;
    private UserHelper userHelper;

    @Autowired
    public AnnexBlockComponent(LanguageHelper languageHelper, MessageHelper messageHelper, EventBus eventBus, UserHelper userHelper) {
        this.languageHelper = languageHelper;
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.userHelper = userHelper;
        Design.read(this);
    }

    @PostConstruct
    private void init() {
        addStyleName("annex-block");
        language.setConverter(new LangCodeToDescriptionConverter(languageHelper));

        titleCaption.setCaption(messageHelper.getMessage("proposal.block.caption.annex.title"));
        title.setInputPrompt(messageHelper.getMessage("proposal.block.annex.title.prompt"));
        openButton.setCaption(messageHelper.getMessage("leos.button.open"));//using same caption as of card
        language.setCaption(messageHelper.getMessage("proposal.caption.language"));
        heading.setCaption(messageHelper.getMessage("proposal.block.caption.annex"));

        heading.addRightButton(createDeleteAnnexButton());
        openButton.addClickListener(event -> openAnnex());
        title.addValueChangeListener(event -> saveData(event));

        moveUpButton.setDisableOnClick(true);
        moveUpButton.setDescription(messageHelper.getMessage("proposal.block.annex.move.up"));
        moveUpButton.addClickListener(event -> eventBus.post(new MoveAnnexEvent((DocumentVO)this.getData(), MoveAnnexEvent.Direction.UP)));

        moveDownButton.setDisableOnClick(true);
        moveDownButton.setDescription(messageHelper.getMessage("proposal.block.annex.move.down"));
        moveDownButton.addClickListener(event -> eventBus.post(new MoveAnnexEvent((DocumentVO)this.getData(), MoveAnnexEvent.Direction.DOWN)));
    }

    private Button createDeleteAnnexButton() {
        Button button = new Button();
        button.setIcon(FontAwesome.MINUS_CIRCLE);
        button.setDescription(messageHelper.getMessage("proposal.description.button.delete.annex"));
        button.addStyleName("delete-button");
        button.addClickListener(listener -> deleteAnnex());
        return button;
    }

    public void populateData(DocumentVO annex) {
        this.setData(annex);
        heading.setCaption(messageHelper.getMessage("proposal.block.caption.annex", annex.getDocNumber())); //update
        title.setPropertyDataSource(new ObjectProperty(annex.getTitle()));//FIXME remove once LEOS-2226 is fixed(annex.getTitle());
        language.setPropertyDataSource(new ObjectProperty(annex.getLanguage()));
        setLastUpdated(annex.getUpdatedBy(), annex.getUpdatedOn());
    }

    private void openAnnex() {
        eventBus.post(new OpenAnnexEvent((DocumentVO) this.getData()));
    }

    private void deleteAnnex() {
        // TODO Confirm
        eventBus.post(new DeleteAnnexRequest((DocumentVO) this.getData()));
    }

    private void saveData(Property.ValueChangeEvent event) {
        // get original vo and update with latest value and fire save
        DocumentVO annex = ((DocumentVO) this.getData());
        annex.setTitle((String) event.getProperty().getValue());
        eventBus.post(new SaveAnnexMetaDataRequest(annex));
    }

    public void setLastUpdated(String lastUpdatedBy, Date lastUpdatedOn){
        lastUpdated.setValue(messageHelper.getMessage("proposal.caption.document.lastupdated", dataFormat.format(lastUpdatedOn), new UserLoginDisplayConverter(userHelper).convertToPresentation(lastUpdatedBy, null, null)));
    }
}