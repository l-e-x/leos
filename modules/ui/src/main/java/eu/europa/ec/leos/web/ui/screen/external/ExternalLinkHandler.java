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
package eu.europa.ec.leos.web.ui.screen.external;

import com.google.common.eventbus.EventBus;
import com.vaadin.navigator.ViewChangeListener;
import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.services.content.DocumentService;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.view.DocumentView;
import eu.europa.ec.leos.web.view.FeedbackView;
import eu.europa.ec.leos.web.view.RepositoryView;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;

/** this solution is a last ditch attempt to avoid the issue of
 * "wrong presenters registered in case a view is initialized from enter method of another view"
 * this should be reconsidered for refactoring when refactoring  presenter-view architecture */
@Scope("session")
@Component
public class ExternalLinkHandler implements ViewChangeListener {

    public static final String EXTERNAL_LINK = "external";

    @Autowired
    protected DocumentService documentService;

    @Autowired
    protected HttpSession session;

    @Autowired
    protected EventBus eventBus;

    @Override
    public boolean beforeViewChange(ViewChangeEvent event) {
        if (EXTERNAL_LINK.equals(event.getViewName())) {
            openView(getParameters(event));
            return false;// terminate the flow for external view
        }
        return true;// pass through
    }

    @Override
    public void afterViewChange(ViewChangeEvent event) {
        // for external Link/view, do nothing as code should never reach here
        // for other views, pass through
    }

    private void openView(String[] params) {
        LeosDocument leosDocument = null;
        String documentId = null;

        if (params.length > 0 && params[0] != null) {
            documentId = params[0];
            try {
                leosDocument = documentService.getDocument(documentId);
            }
            catch(Exception e){
                //document not found
            }
        }

        if (leosDocument != null) {
            handleDocumentFound(leosDocument);
        } else {
            handleDocumentNotFound(documentId);
        }
    }

    // document not found or present
    private void handleDocumentNotFound(String documentId) {
        eventBus.post(new NavigationRequestEvent(RepositoryView.VIEW_ID));
        eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "document.not.found", documentId));
    }

    private void handleDocumentFound(LeosDocument leosDocument) {
        session.setAttribute(SessionAttribute.DOCUMENT_ID.name(), leosDocument.getLeosId());
        switch (leosDocument.getStage()) {
            case DRAFT:
            case EDIT:
                eventBus.post(new NavigationRequestEvent(DocumentView.VIEW_ID));
                break;
            case FEEDBACK:
                eventBus.post(new NavigationRequestEvent(FeedbackView.VIEW_ID));
                break;
        }
    }

    private String[] getParameters(ViewChangeListener.ViewChangeEvent event) {
        String[] parameters = new String[]{};
        if (StringUtils.isNotBlank(event.getParameters())) {
            parameters = event.getParameters().split("/");
        }
        return parameters;
    }
}
