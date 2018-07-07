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
package eu.europa.ec.leos.web.ui.extension;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.data.Property;
import com.vaadin.server.ClientConnector;
import com.vaadin.ui.JavaScriptFunction;
import com.vaadin.ui.Label;
import elemental.json.JsonArray;
import elemental.json.JsonException;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.support.web.GuiUtility;
import eu.europa.ec.leos.vo.UserVO;
import eu.europa.ec.leos.web.event.view.feedback.SetUserEvent;
import eu.europa.ec.leos.web.event.view.feedback.DeleteCommentEvent;
import eu.europa.ec.leos.web.event.view.feedback.InsertCommentEvent;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@StyleSheet({"vaadin://../lib/side-comments/css/side-comments.css"+ LeosCacheToken.TOKEN,
             "vaadin://../lib/side-comments/css/themes/default-theme.css"+ LeosCacheToken.TOKEN,
             "vaadin://../assets/css/leos-side-comments.css"+ LeosCacheToken.TOKEN})
@JavaScript({"vaadin://../js/web/sideCommentsConnector.js"+ LeosCacheToken.TOKEN })
public class SideCommentsExtension extends LeosJavaScriptExtension {

    private static final long serialVersionUID = 8992588149385777077L;

    private static final Logger LOG = LoggerFactory.getLogger(SideCommentsExtension.class);

    private EventBus eventBus;

    public SideCommentsExtension(EventBus eventBus) {
        super();
        bindServerActions();
        Validate.notNull(eventBus, "EventBus must not be null!");
        this.eventBus = eventBus;
    }

    public void extend(Label target) {
        super.extend(target);
        // handle target's value change
        target.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                LOG.trace("Target's value changed...");
                markAsDirty();
            }
        });
    }

    @Override
    protected Class<? extends ClientConnector> getSupportedParentType() {
        return Label.class;
    }

    @Override
    protected SideCommentsState getState() {
        return (SideCommentsState) super.getState();
    }

    @Override
    protected SideCommentsState getState(boolean markAsDirty) {
        return (SideCommentsState) super.getState(markAsDirty);
    }

    @Override
    public void attach() {
        super.attach();
        eventBus.register(this);
    }

    @Override
    public void detach() {
        super.detach();
        eventBus.unregister(this);
    }

    @Subscribe
    public void setUser(SetUserEvent event) {
        LOG.trace("Setting User ...");
        getState(false).user = createUser(event.getUser());
    }

    private UserVO createUser(User leosUser){
        return new UserVO(
                leosUser.getLogin(),
                leosUser.getName(),
                GuiUtility.getDG(leosUser.getDepartment().getDepartmentId()));
    }

    private void bindServerActions() {
        addFunction("insertComment", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) throws JsonException {
                LOG.debug("Comment inserted from client-side... {}", arguments.toJson());
                String elementId = arguments.getString(0);
                String commentId = arguments.getString(1);
                String content = arguments.getString(2);
                eventBus.post(new InsertCommentEvent(elementId, commentId, content));
            }
        });
        addFunction("deleteComment", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) throws JsonException {
                LOG.debug("Comment deleted from client-side... {}", arguments.toJson());
                String elementId = arguments.getString(0);
                String commentId = arguments.getString(1);
                //TODO delete only if the user is same???
                eventBus.post(new DeleteCommentEvent(elementId, commentId));
            }
        });
    }
}