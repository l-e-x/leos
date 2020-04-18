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
package eu.europa.ec.leos.web.ui.component;

import com.google.common.eventbus.EventBus;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.data.Binder;
import com.vaadin.data.ReadOnlyHasValue;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.declarative.Design;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.ui.event.view.collection.DeleteAnnexRequest;
import eu.europa.ec.leos.ui.event.view.collection.SaveAnnexMetaDataRequest;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.coedition.InfoType;
import eu.europa.ec.leos.web.event.view.annex.OpenAnnexEvent;
import eu.europa.ec.leos.i18n.LanguageHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.converter.LangCodeToDescriptionV8Converter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@SpringComponent
@Scope("prototype")
@DesignRoot("AnnexBlockDesign.html")
public class AnnexBlockComponent extends VerticalLayout {
    public static SimpleDateFormat dataFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    private static final long serialVersionUID = 1133841334809202933L;
    private static final int TITLE_MAX_LEGTH  = 2000;

    protected HeadingComponent heading;
    protected Label titleCaption;
    protected EditBoxComponent title;
    protected Label annexUserCoEdition;
    protected Button openButton;
    protected Label language;
    protected Label lastUpdated;
    protected Button moveUpButton;
    protected Button moveDownButton;

    private MessageHelper messageHelper;
    private EventBus eventBus;
    private UserHelper userHelper;
    private SecurityContext securityContext;
    private LangCodeToDescriptionV8Converter langConverter;

    private boolean enableSave;
    private Binder<DocumentVO> annexBinder;

    @Value("${leos.coedition.sip.enabled}")
    private boolean coEditionSipEnabled;

    @Value("${leos.coedition.sip.domain}")
    private String coEditionSipDomain;

    @Autowired
    public AnnexBlockComponent(LanguageHelper languageHelper, MessageHelper messageHelper, EventBus eventBus, UserHelper userHelper,
            SecurityContext securityContext) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.userHelper = userHelper;
        this.securityContext = securityContext;
        this.langConverter = new LangCodeToDescriptionV8Converter(languageHelper);
        Design.read(this);
    }

    @PostConstruct
    private void init() {
        addStyleName("annex-block");

        annexBinder = new Binder<>();
        annexBinder.forField(new ReadOnlyHasValue<>(language::setValue))
                .withConverter(langConverter)
                .bind(DocumentVO::getLanguage, DocumentVO::setLanguage);
        annexBinder.forField(title).bind(DocumentVO::getTitle, DocumentVO::setTitle);

        titleCaption.setCaption(messageHelper.getMessage("collection.block.caption.annex.title"));
        title.setPlaceholder(messageHelper.getMessage("collection.block.annex.title.prompt"));
        openButton.setCaption(messageHelper.getMessage("leos.button.open"));// using same caption as of card
        language.setCaption(messageHelper.getMessage("collection.caption.language"));
        heading.setCaption(messageHelper.getMessage("collection.block.caption.annex"));

        heading.addRightButton(createDeleteAnnexButton());
        openButton.addClickListener(event -> openAnnex());
        title.addValueChangeListener(event -> saveData());

        moveUpButton.setDisableOnClick(true);
        moveUpButton.setDescription(messageHelper.getMessage("collection.block.annex.move.up"));
        moveUpButton.addClickListener(event -> eventBus.post(new MoveAnnexEvent((DocumentVO) this.getData(), MoveAnnexEvent.Direction.UP)));

        moveDownButton.setDisableOnClick(true);
        moveDownButton.setDescription(messageHelper.getMessage("collection.block.annex.move.down"));
        moveDownButton.addClickListener(event -> eventBus.post(new MoveAnnexEvent((DocumentVO) this.getData(), MoveAnnexEvent.Direction.DOWN)));
    }

    private Button createDeleteAnnexButton() {
        Button button = new Button();
        button.setIcon(VaadinIcons.MINUS_CIRCLE);
        button.setDescription(messageHelper.getMessage("collection.description.button.delete.annex"));
        button.addStyleName("delete-button");
        button.addClickListener(listener -> deleteAnnex());
        return button;
    }

    public void populateData(DocumentVO annex) {
    	enableSave = false; // To avoid triggering save on load of data
        resetBasedOnPermissions(annex);
        this.setData(annex);
        annexBinder.setBean(annex);
        heading.setCaption(messageHelper.getMessage("collection.block.caption.annex", annex.getDocNumber())); // update
        setLastUpdated(annex.getUpdatedBy(), annex.getUpdatedOn());
        enableSave = true;
        title.setTitleMaxSize(TITLE_MAX_LEGTH);
    }

    private void resetBasedOnPermissions(DocumentVO annexVO) {
        boolean enableUpdate = securityContext.hasPermission(annexVO, LeosPermission.CAN_UPDATE);
        heading.getRightButton().setVisible(enableUpdate);
        moveUpButton.setVisible(enableUpdate);
        moveDownButton.setVisible(enableUpdate);
        title.setEnabled(enableUpdate);
    }

    private void openAnnex() {
        eventBus.post(new OpenAnnexEvent((DocumentVO) this.getData()));
    }

    private void deleteAnnex() {
        // TODO Confirm
        eventBus.post(new DeleteAnnexRequest((DocumentVO) this.getData()));
    }

    private void saveData() {
    	if (enableSave) {
	        // get original vo and update with latest value and fire save
	        DocumentVO annex = ((DocumentVO) this.getData());
	        annex.setTitle(title.getValue());
	        eventBus.post(new SaveAnnexMetaDataRequest(annex));
    	}
    }

    public void setLastUpdated(String lastUpdatedBy, Date lastUpdatedOn) {
        lastUpdated.setValue(messageHelper.getMessage("collection.caption.document.lastupdated", dataFormat.format(lastUpdatedOn),
                userHelper.convertToPresentation(lastUpdatedBy)));
    }

    public void updateUserCoEditionInfo(List<CoEditionVO> coEditionVos, User user) {
        // Update annex user CoEdition information
        annexUserCoEdition.setIcon(null);
        annexUserCoEdition.setDescription("");
        annexUserCoEdition.removeStyleName("leos-user-coedition-self-user");
        DocumentVO annexVo = (DocumentVO) this.getData();
        coEditionVos.stream()
                .filter((x) -> (InfoType.ELEMENT_INFO.equals(x.getInfoType()) || InfoType.TOC_INFO.equals(x.getInfoType())) && x.getDocumentId().equals(annexVo.getVersionSeriesId()))
                .sorted(Comparator.comparing(CoEditionVO::getUserName).thenComparingLong(CoEditionVO::getEditionTime))
                .forEach(x -> {
                    StringBuilder userDescription = new StringBuilder();
                    if (!x.getUserLoginName().equals(user.getLogin())) {
                        userDescription.append("<a class=\"leos-user-coedition-lync\" href=\"")
                            .append(StringUtils.isEmpty(x.getUserEmail()) ? "" : (coEditionSipEnabled ? new StringBuilder("sip:").append(x.getUserEmail().replaceFirst("@.*", "@" + coEditionSipDomain)).toString()
                                    : new StringBuilder("mailto:").append(x.getUserEmail()).toString()))
                            .append("\">").append(x.getUserName()).append(" (").append(StringUtils.isEmpty(x.getEntity()) ? "-" : x.getEntity())
                            .append(")</a>");
                    } else {
                        userDescription.append(x.getUserName()).append(" (").append(StringUtils.isEmpty(x.getEntity()) ? "-" : x.getEntity()).append(")");
                    }
                    annexUserCoEdition.setDescription(
                            annexUserCoEdition.getDescription() +
                                    messageHelper.getMessage("coedition.tooltip.message", userDescription, dataFormat.format(new Date(x.getEditionTime()))) +
                                    "<br>",
                            ContentMode.HTML);
                });
        if (!annexUserCoEdition.getDescription().isEmpty()) {
            annexUserCoEdition.setIcon(VaadinIcons.USER);
            if (!annexUserCoEdition.getDescription().contains("href=\"")) {
                annexUserCoEdition.addStyleName("leos-user-coedition-self-user");
            }
        }
    }
}