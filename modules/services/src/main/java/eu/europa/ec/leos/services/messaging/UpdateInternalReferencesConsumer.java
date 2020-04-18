package eu.europa.ec.leos.services.messaging;

import com.google.common.eventbus.EventBus;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.model.event.DocumentUpdatedByCoEditorEvent;
import eu.europa.ec.leos.model.messaging.UpdateInternalReferencesMessage;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.messaging.conf.Base64Serializer;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.services.store.WorkspaceService;
import eu.europa.ec.leos.services.store.XmlDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static eu.europa.ec.leos.services.messaging.conf.JmsDestinations.QUEUE_UPDATE_INTERNAL_REFERENCE;

@Component
public class UpdateInternalReferencesConsumer {

    private static final Logger logger = LoggerFactory.getLogger(UpdateInternalReferencesConsumer.class);
    private static final List<LeosCategory> DOCUMENTS_TO_DISCONSIDER = Arrays.asList(LeosCategory.PROPOSAL, LeosCategory.MEMORANDUM);
    private final PackageService packageService;
    private final XmlDocumentService xmlDocumentService;
    private final WorkspaceService workspaceService;
    private final EventBus leosApplicationEventBus;
    
    public UpdateInternalReferencesConsumer(PackageService packageService,
                                            WorkspaceService workspaceService,
                                            EventBus leosApplicationEventBus,
                                            XmlDocumentService xmlDocumentService) {
        this.packageService = packageService;
        this.workspaceService = workspaceService;
        this.leosApplicationEventBus = leosApplicationEventBus;
        this.xmlDocumentService = xmlDocumentService;
    }

    @JmsListener(destination = QUEUE_UPDATE_INTERNAL_REFERENCE, subscription = "updateInternalReferences", containerFactory = "jmsListenerContainerFactory")
    public void updateInternalReferences(@Payload UpdateInternalReferencesMessage message, @Header String authcontext) {
        logger.info("Processing internal references for document {}", message.getDocumentRef());
        detachAuthenticationContext(authcontext);
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        LeosPackage leosPackage = packageService.findPackageByDocumentId(message.getDocumentId());
        List<XmlDocument> documents = packageService.findDocumentsByPackagePath(leosPackage.getPath(), XmlDocument.class, false);
        for (XmlDocument document : documents) {
            String ref = document.getMetadata().get().getRef();
            boolean isDifferentDocument = !ref.equals(message.getDocumentRef());
            boolean canProcess = DOCUMENTS_TO_DISCONSIDER.stream().noneMatch(p -> document.getMetadata().get().getCategory().equals(p));
            if (isDifferentDocument && canProcess) {
                try {
                    XmlDocument xmlDocument = workspaceService.findDocumentById(document.getId(), XmlDocument.class);
                    boolean updated = xmlDocumentService.updateInternalReferences(xmlDocument);
                    logger.debug("updateInternalReferences processed for {}, isXmlChanged {}: ", ref, updated);

                    if (updated) {
                        leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, xmlDocument.getVersionSeriesId(), message.getPresenterId()));
                    }
                } catch (Exception e) {
                    logger.error("Error occurred calling updateInternalRef() for doc {}", ref, e);
                }
            }
        }
    }

    private void detachAuthenticationContext(String authcontext) {
        Authentication authentication = (Authentication) Base64Serializer.deserialize(authcontext);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

}
