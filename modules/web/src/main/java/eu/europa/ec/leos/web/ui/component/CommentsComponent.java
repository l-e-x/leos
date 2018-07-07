/**
 * Copyright 2015 European Commission
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
package eu.europa.ec.leos.web.ui.component;

import com.google.common.eventbus.EventBus;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.JavaScriptFunction;
import elemental.json.JsonArray;
import elemental.json.JsonException;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.support.web.GuiUtility;
import eu.europa.ec.leos.vo.CommentVO;
import eu.europa.ec.leos.vo.UserVO;
import eu.europa.ec.leos.web.event.view.feedback.DeleteCommentEvent;
import eu.europa.ec.leos.web.event.view.feedback.InsertCommentEvent;
import eu.europa.ec.leos.web.event.view.feedback.LoadExistingCommentsEvent;
import eu.europa.ec.leos.web.event.view.feedback.UpdateCommentEvent;
import eu.europa.ec.leos.web.model.SectionVO;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@StyleSheet({"vaadin://js/web/sideComments/css/leos-side-comments.css"+ LeosCacheToken.TOKEN})
@JavaScript({"vaadin://js/web/sideComments/sideCommentsBootstrap.js"+ LeosCacheToken.TOKEN,"vaadin://js/web/connector/commentsConnector.js"+ LeosCacheToken.TOKEN })
public class CommentsComponent extends AbstractJavaScriptComponent {

    private static final Logger LOG = LoggerFactory.getLogger(CommentsComponent.class);
    private static final long serialVersionUID = 2020774676556550711L;

    private EventBus eventBus;

    public CommentsComponent(EventBus eventBus, MessageHelper messageHelper) {
        this.eventBus=eventBus;
        init();
    }

    private void init(){
        LOG.debug("Initializing...");
        bindJavaScriptActions();
    }

    @Override
    protected CommentsState getState() {
        return (CommentsState) super.getState();
    }

    /** this method sets the data required by SideComments library */
    public void setData(List<CommentVO> comments, User leosUser){
        LOG.debug("Setting data {} comments...",comments.size());
        getState().ready = false;
        getState().existingComments = createSectionVOs(comments);
        getState().currentUser=createUser(leosUser);
        getState().ready = true;//send the info to client
    }

    //create a Json object in format required by SideComments library
    private UserVO createUser(User leosUser){
        return new UserVO(leosUser.getLogin(),GuiUtility.getAvatarUrl(),leosUser.getName(),GuiUtility.getDG(leosUser.getDepartment().getDepartmentId()));
    }

    private void bindJavaScriptActions() {

        com.vaadin.ui.JavaScript.getCurrent().addFunction("vaadin_getExistingComments", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) throws JsonException {
                LOG.debug("received vaadin_getExistingComments call from browser");
                eventBus.post(new LoadExistingCommentsEvent());
            }
        });

        com.vaadin.ui.JavaScript.getCurrent().addFunction("vaadin_addComment", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) throws JsonException {
                String elementId = arguments.getString(0);
                String commentId = arguments.getString(1);
                String content = arguments.getString(2);
                LOG.debug("received vaadin_addComment call from browser");
                eventBus.post(new InsertCommentEvent(elementId, commentId, content));
            }
        });

        com.vaadin.ui.JavaScript.getCurrent().addFunction("vaadin_editComment", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) throws JsonException {
                LOG.debug("received vaadin_editComment call from browser");

                String elementId = arguments.getString(0);
                String commentId = arguments.getString(1);
                String content = arguments.getString(2);
                eventBus.post(new UpdateCommentEvent(elementId,commentId, content));
            }
        });

        com.vaadin.ui.JavaScript.getCurrent().addFunction("vaadin_deleteComment", new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) throws JsonException {
                LOG.debug("received vaadin_deleteComment call from browser");

                String elementId = arguments.getString(0);
                String commentId = arguments.getString(1);
                //TODO delete only if the user is same
                eventBus.post(new DeleteCommentEvent(elementId, commentId));
            }
        });
    }

    private List<SectionVO>  createSectionVOs(List<CommentVO> comments) {
        List<SectionVO> sections = new ArrayList<SectionVO>();

        Map<String, List<CommentVO>> map = getMapOfSections(comments);
        try {
            Iterator<String> elementIterator = map.keySet().iterator();
            while(elementIterator.hasNext()) {
                String sectionId = elementIterator.next();//sectionid means enclosing element id

                //Add comments in array
                List<CommentVO> sectionComments = new ArrayList<CommentVO>();
                for (CommentVO comment:map.get(sectionId)) {
                    comment.setDg(GuiUtility.getDG(comment.getDg()));//Change to GUI representation format
                    comment.setAuthorAvatarUrl(GuiUtility.getAvatarUrl());//Remove when removing the AvatarURL
                    sectionComments.add(comment);
                }

                SectionVO sectionVO = new SectionVO(sectionId,sectionComments);
                sections.add(sectionVO);
            }
        } catch (JsonException e) {
            LOG.debug("Exception ocurred: {}", e);
        }
        return sections;
    }

    private Map<String, List<CommentVO>> getMapOfSections(List<CommentVO>  comments){
        Collections.sort(comments, new Comparator<CommentVO>() {
            @Override
            public int compare(CommentVO o1, CommentVO o2) {
                return o1.getTimestamp().compareTo(o2.getTimestamp());
            }
        });

        Map<String, List<CommentVO>> map = new HashMap<String, List<CommentVO>>();
        List<CommentVO> commentsForElement;
        for (CommentVO comment : comments) {
            commentsForElement = (map.get(comment.getEnclosingElementId()) == null)
                    ? new ArrayList<CommentVO>()
                    : map.get(comment.getEnclosingElementId());
            commentsForElement.add(comment);
            map.put(comment.getEnclosingElementId(), commentsForElement);
        }
        return map;
    }

}
