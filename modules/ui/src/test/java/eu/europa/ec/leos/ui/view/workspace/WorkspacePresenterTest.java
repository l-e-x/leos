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

import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.Content.Source;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.document.Proposal;
import eu.europa.ec.leos.domain.cmis.metadata.ProposalMetadata;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.document.ProposalService;
import eu.europa.ec.leos.services.store.TemplateService;
import eu.europa.ec.leos.services.store.WorkspaceService;
import eu.europa.ec.leos.test.support.model.ModelHelper;
import eu.europa.ec.leos.test.support.web.presenter.LeosPresenterTest;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.view.repository.SelectDocumentEvent;
import eu.europa.ec.leos.web.support.UuidHelper;
import eu.europa.ec.leos.web.ui.navigation.Target;
import io.atlassian.fugue.Option;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class WorkspacePresenterTest extends LeosPresenterTest {

    private static final String PRESENTER_ID = "ab51f419-c6b0-45cb-82ed-77a61099b58f";

    private SecurityContext securityContext;

    private User contextUser;

    private UuidHelper uuidHelper;

    @Mock
    private WorkspaceScreen workspaceScreen;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private ProposalService proposalService;

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private WorkspacePresenter repositoryPresenter;

    @Before
    public void setup() {
        contextUser = ModelHelper.buildUser(45L, "login", "name");
        securityContext = mock(SecurityContext.class);
        when(securityContext.getUser()).thenReturn(contextUser);
        uuidHelper = mock(UuidHelper.class);
        when(uuidHelper.getRandomUUID()).thenReturn(PRESENTER_ID);
        super.setup();
    }

    @Test
    public void testEnterRepositoryView() throws Exception {
        Content content = mock(Content.class);
        Source source = mock(Source.class);

        when(source.getBytes()).thenReturn(new byte[]{'1'});
        when(content.getSource()).thenReturn(source);
        String proposalId = "878";

        List<Proposal> documents = new ArrayList<>();
        ProposalMetadata proposalMetadata = new ProposalMetadata("", "REGULATION for EC", "", "PR-00.xml", "EN", "", "proposal-id", "", "0.1.0");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Proposal leosProposal = new Proposal(proposalId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", "", VersionType.MAJOR, true,
                "REGULATION for EC", collaborators, Arrays.asList(""), "login", Instant.now(),
                Option.some(content), Option.some(proposalMetadata));
        documents.add(leosProposal);
        when(workspaceService.findDocuments(eq(Proposal.class), eq(false), anyInt(), anyInt(), any()))
                .thenReturn(documents.stream());
        when(workspaceService.findDocumentCount(eq(Proposal.class), any()))
                .thenReturn(documents.size());
        when(templateService.getTemplatesCatalog())
                .thenReturn(Collections.emptyList());

        when(proposalService.findProposal(proposalId)).thenReturn(leosProposal);

        // DO THE ACTUAL CALL
        repositoryPresenter.enter();

        verify(workspaceScreen, times(1)).setDataFunctions(any(), any());
        verify(workspaceScreen, times(1)).intializeFiltersWithData(anyList());

        verifyNoMoreInteractions(workspaceService, workspaceScreen);
    }

    @Test
    public void testNavigateToView() {

        String docId = "123";

        // DO THE ACTUAL CALL
        repositoryPresenter.navigateToView(new SelectDocumentEvent(docId, LeosCategory.BILL));

        verify(eventBus).post(argThat(Matchers.<NavigationRequestEvent>hasProperty("target", equalTo(Target.LEGALTEXT))));
    }
}