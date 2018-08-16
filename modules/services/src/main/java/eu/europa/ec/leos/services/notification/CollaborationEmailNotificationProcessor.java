/*
 * Copyright 2018 European Commission
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
package eu.europa.ec.leos.services.notification;

import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Proposal;
import eu.europa.ec.leos.domain.document.LeosMetadata.ProposalMetadata;
import eu.europa.ec.leos.model.notification.EmailNotification;
import eu.europa.ec.leos.model.notification.collaborators.CollaboratorEmailNotification;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.document.ProposalService;
import eu.europa.ec.leos.services.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.StringJoiner;

@Component
public class CollaborationEmailNotificationProcessor implements EmailNotificationProcessor<CollaboratorEmailNotification> {

    @Autowired
    @Qualifier("emailsMessageSource")
    private MessageSource emailsMessageSource;

    private final ProposalService proposalService;
    private final UserService userService;
    private final FreemarkerNotificationProcessor processor;

    @Autowired
    public CollaborationEmailNotificationProcessor(ProposalService proposalService, UserService userService, FreemarkerNotificationProcessor processor) {
        this.proposalService = proposalService;
        this.userService = userService;
        this.processor = processor;
    }

    @Override
    public boolean canProcess(EmailNotification emailNotification) {
        if (CollaboratorEmailNotification.class.isAssignableFrom(emailNotification.getClass())) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public void process(Locale language, CollaboratorEmailNotification emailNotification) {
        buildEmailBody(language, emailNotification);
        buildEmailSubject(language, emailNotification);
    }

    private void buildEmailBody(Locale language, CollaboratorEmailNotification collaborationEmailNotification) {
        User collaborator = collaborationEmailNotification.getRecipient();
        Proposal proposal = proposalService.findProposal(collaborationEmailNotification.getDocumentId());
        collaborationEmailNotification.setLeosAuthorityName(emailsMessageSource
                .getMessage("notification.collaborator.leosAuthority." + collaborationEmailNotification.getLeosAuthority().name(), null, language));

        collaborationEmailNotification.setTitle(getProposalTitle(proposal));
        collaborationEmailNotification.setOwners(buildCollaboratorList(proposal, collaborator, LeosAuthority.OWNER));
        collaborationEmailNotification.setContributors(buildCollaboratorList(proposal, collaborator, LeosAuthority.CONTRIBUTOR));
        collaborationEmailNotification.setReviewers(buildCollaboratorList(proposal, collaborator, LeosAuthority.REVIEWER));
        collaborationEmailNotification.setEmailBody(processor.processTemplate(collaborationEmailNotification));
    }

    private void buildEmailSubject(Locale language, CollaboratorEmailNotification collaborationEmailNotification) {
        String proposalId = collaborationEmailNotification.getDocumentId();
        Proposal proposal = proposalService.findProposal(proposalId);
        String title = getProposalTitle(proposal);
        String entity = collaborationEmailNotification.getRecipient().getEntity();
        String role = emailsMessageSource
                .getMessage("notification.collaborator.leosAuthority." + collaborationEmailNotification.getLeosAuthority().name(), null, language);
        collaborationEmailNotification.setLeosAuthorityName(role);
        
        collaborationEmailNotification
                .setEmailSubject(emailsMessageSource.getMessage(collaborationEmailNotification.getEmailSubjectKey(), new Object[]{role, entity, title}, language));
    }

    private String buildCollaboratorList(Proposal proposal, User collaborator, LeosAuthority collaboratorAuthority) {
        StringJoiner collaborators = new StringJoiner(", ");
        proposal.getCollaborators().forEach((login, auth) -> {
            User user = userService.getUser(login);
            if (auth == collaboratorAuthority) {
                collaborators.add(user.getName());
            }
        });
        return collaborators.toString();
    }

    private String getProposalTitle(Proposal proposal) {
        ProposalMetadata proposalMetadata = proposal.getMetadata().get();
        StringBuilder proposalTitle = new StringBuilder(proposalMetadata.getStage()).append(" ");
        proposalTitle.append(proposalMetadata.getType()).append(" ");
        proposalTitle.append(proposalMetadata.getPurpose()).append(" ");
        return proposalTitle.toString();
    }
}
