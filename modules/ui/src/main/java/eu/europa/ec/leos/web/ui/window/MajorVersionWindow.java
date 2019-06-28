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
import com.vaadin.ui.VerticalLayout;
import eu.europa.ec.leos.web.event.view.document.SaveMajorVersionEvent;
import eu.europa.ec.leos.i18n.MessageHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MajorVersionWindow extends AbstractWindow {

    private static final long serialVersionUID = -7318940290911926353L;
    private static final Logger LOG = LoggerFactory.getLogger(MajorVersionWindow.class);
    
    private TextArea commentArea;
    Binder<CommentVO> commentsBinder;
    
    public MajorVersionWindow(MessageHelper messageHelper, EventBus eventBus) {
        super(messageHelper, eventBus);
        setCaption(messageHelper.getMessage("document.major.version.window.title"));
        prepareWindow();
    }

    public void prepareWindow() {
        setWidth("420px");
        setHeight("300px");
        
        VerticalLayout windowLayout = new VerticalLayout();
        windowLayout.setSizeFull();
        windowLayout.setMargin(false);
        windowLayout.setSpacing(false);
        setBodyComponent(windowLayout);

        buildLayout(windowLayout);
        
        Button saveButon = buildSaveButton();
        addButton(saveButon);
    }

    private void buildLayout(VerticalLayout windowLayout) {
        commentArea =  new TextArea(messageHelper.getMessage("document.major.version.comment.caption"));
        commentArea.setRows(5);
        commentArea.setWidth("400px");
        commentArea.setRequiredIndicatorVisible(true);
        
        commentsBinder = new Binder<>();
        commentsBinder.forField(commentArea).asRequired(messageHelper.getMessage("document.major.version.validation.error"))
        .withValidator(val -> !StringUtils.isBlank(val), messageHelper.getMessage("document.major.version.validation.empty.space.error"))
        .bind(CommentVO::getComment, CommentVO::setComment);
        
        CommentVO commentsVO = new CommentVO(commentArea.getValue());
        commentsBinder.setBean(commentsVO);
        
        windowLayout.addComponent(commentArea);
        
        addFocusListener(event -> {
        	commentArea.focus();
        });
    }

    private Button buildSaveButton() {
        // create save Button
        Button saveButton = new Button(messageHelper.getMessage("document.major.version.button.save"));
        saveButton.addStyleName("primary");
        saveButton.addClickListener(event -> {
            if (commentsBinder.validate().isOk()) {
                String savedComment = commentsBinder.getBean().getComment();
                LOG.debug("Saved as Major Version with Comments - {}", savedComment.trim());
                eventBus.post(new SaveMajorVersionEvent(savedComment.trim(), true));
                close();
            }
        });
        return saveButton;
    }
    
    class CommentVO {
        private String comment;

        public CommentVO(String comment) {
            this.comment = comment;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}
