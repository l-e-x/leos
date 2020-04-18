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
package eu.europa.ec.leos.web.ui.window;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.Binder;
import com.vaadin.ui.Button;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.action.CheckinCommentVO;
import eu.europa.ec.leos.services.document.util.CheckinCommentUtil;
import eu.europa.ec.leos.web.event.view.document.SaveIntermediateVersionEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IntermediateVersionWindow extends AbstractWindow {

    private static final long serialVersionUID = -7318940290911926353L;
    private static final Logger LOG = LoggerFactory.getLogger(IntermediateVersionWindow.class);
    
    private TextField titleField;
    private TextArea descriptionArea;
    private Binder<CheckinCommentVO> commentsBinder;
    
    public IntermediateVersionWindow(MessageHelper messageHelper, EventBus eventBus) {
        super(messageHelper, eventBus);
        setCaption(messageHelper.getMessage("document.major.version.window.title"));
        prepareWindow();
    }

    public void prepareWindow() {
        setWidth("420px");
        setHeight("280px");
        
        VerticalLayout windowLayout = new VerticalLayout();
        windowLayout.setSizeFull();
        windowLayout.setMargin(false);
        windowLayout.setSpacing(true);
        setBodyComponent(windowLayout);

        buildLayout(windowLayout);
        
        Button saveButon = buildSaveButton();
        addButton(saveButon);
    }

    private void buildLayout(VerticalLayout windowLayout) {
        titleField =  new TextField(messageHelper.getMessage("document.major.version.title.caption"));
        titleField.setPlaceholder(messageHelper.getMessage("document.major.version.title.placeholder"));
        titleField.setWidth("400px");
        titleField.setRequiredIndicatorVisible(true);
        
        descriptionArea =  new TextArea(messageHelper.getMessage("document.major.version.description.caption"));
        descriptionArea.setPlaceholder(messageHelper.getMessage("document.major.version.description.placeholder"));
        descriptionArea.setRows(5);
        descriptionArea.setWidth("400px");
        
        commentsBinder = new Binder<>();
        commentsBinder.forField(titleField)
                .asRequired(messageHelper.getMessage("document.major.version.validation.error"))
                .withValidator(val -> !StringUtils.isBlank(val), messageHelper.getMessage("document.major.version.validation.empty.space.error"))
                .bind(CheckinCommentVO::getTitle, CheckinCommentVO::setTitle);
        
        
        CheckinCommentVO checkinCommentVO = new CheckinCommentVO(titleField.getValue(), descriptionArea.getValue());
        commentsBinder.setBean(checkinCommentVO);
        
        windowLayout.addComponent(titleField);
        windowLayout.addComponent(descriptionArea);
        windowLayout.setExpandRatio(descriptionArea, 1);
        
        addFocusListener(event -> {
            titleField.focus();
        });
    }

    private Button buildSaveButton() {
        // create save Button
        Button saveButton = new Button(messageHelper.getMessage("leos.button.save"));
        saveButton.addStyleName("primary");
        saveButton.addClickListener(event -> {
            if (commentsBinder.validate().isOk()) {
                CheckinCommentVO checkinCommentVO = commentsBinder.getBean();
                checkinCommentVO.setDescription(descriptionArea.getValue());
                final String checkinCommentJson = CheckinCommentUtil.getJsonObject(checkinCommentVO);
                LOG.debug("Saved as Major Version with checkinCommentVO - {}", checkinCommentJson);
                eventBus.post(new SaveIntermediateVersionEvent(checkinCommentJson, VersionType.INTERMEDIATE));
                close();
            }
        });
        return saveButton;
    }
    
    
}
