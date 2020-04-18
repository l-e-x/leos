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
package eu.europa.ec.leos.usecases.document;

import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.Annex;
import eu.europa.ec.leos.domain.cmis.metadata.AnnexMetadata;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.MetadataVO;
import eu.europa.ec.leos.services.document.AnnexService;
import eu.europa.ec.leos.services.document.SecurityService;
import eu.europa.ec.leos.services.store.TemplateService;
import io.atlassian.fugue.Option;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class AnnexContext {

    private static final Logger LOG = LoggerFactory.getLogger(AnnexContext.class);

    private final TemplateService templateService;
    private final AnnexService annexService;
    private final SecurityService securityService;

    private LeosPackage leosPackage;
    private Annex annex = null;
    private int index;
    private String purpose = null;
    private String type = null;
    private String template = null;
    private String annexId = null;
    private Map<String, String> collaborators = null;

    private DocumentVO annexDocument;
    private final Map<ContextAction, String> actionMsgMap;
    private String annexNumber;
    private String versionComment;
    private String milestoneComment;

    public AnnexContext(
            TemplateService templateService,
            AnnexService annexService,
            SecurityService securityService) {
        this.templateService = templateService;
        this.annexService = annexService;
        this.securityService = securityService;
        this.actionMsgMap = new HashMap<>();
    }

    public void useTemplate(String template) {
        Validate.notNull(template, "Template name is required!");

        this.annex = (Annex) templateService.getTemplate(template);
        Validate.notNull(annex, "Template not found! [name=%s]", template);
        this.template = template;
        LOG.trace("Using {} template... [id={}, name={}]", annex.getCategory(), annex.getId(), annex.getName());
    }

    public void useActionMessageMap(Map<ContextAction, String> messages) {
        Validate.notNull(messages, "Action message map is required!");

        actionMsgMap.putAll(messages);
    }

    public void usePackage(LeosPackage leosPackage) {
        Validate.notNull(leosPackage, "Annex package is required!");
        LOG.trace("Using Annex package... [id={}, path={}]", leosPackage.getId(), leosPackage.getPath());
        this.leosPackage = leosPackage;
    }

    public void usePurpose(String purpose) {
        Validate.notNull(purpose, "Annex purpose is required!");
        LOG.trace("Using Annex purpose... [purpose={}]", purpose);
        this.purpose = purpose;
    }
    
    public void useType(String type) {
        Validate.notNull(type, "Annex type is required!");
        LOG.trace("Using Annex type... [type={}]", type);
        this.type = type;
    }

    public void usePackageTemplate(String template) {
        Validate.notNull(template, "template is required!");
        LOG.trace("Using template... [template={}]", template);
        this.template = template;
    }
    
    public void useIndex(int index) {
        Validate.notNull(index, "Annex index is required!");
        LOG.trace("Using Annex index... [index={}]", index);
        this.index = index;
    }

    public void useAnnexId(String annexId) {
        Validate.notNull(annexId, "Annex 'annexId' is required!");
        LOG.trace("Using AnnexId'... [annexId={}]", annexId);
        this.annexId = annexId;
    }

    public void useDocument(DocumentVO document) {
        Validate.notNull(document, "Annex document is required!");
        annexDocument = document;
    }

    public void useCollaborators(Map<String, String> collaborators) {
        Validate.notNull(collaborators, "Annex 'collaborators' are required!");
        LOG.trace("Using collaborators'... [collaborators={}]", collaborators);
        this.collaborators = Collections.unmodifiableMap(collaborators);
    }

    public void useAnnexNumber(String annexNumber) {
        Validate.notNull(annexNumber, "Annex Number is required!");
        this.annexNumber = annexNumber;
    }

    public void useVersionComment(String comment) {
        Validate.notNull(comment, "Version comment is required!");
        this.versionComment = comment;
    }

    public void useMilestoneComment(String milestoneComment) {
        Validate.notNull(milestoneComment, "milestoneComment is required!");
        this.milestoneComment = milestoneComment;
    }
    
    public void useActionMessage(ContextAction action, String actionMsg) {
        Validate.notNull(actionMsg, "Action message is required!");
        Validate.notNull(action, "Context Action not found! [name=%s]", action);

        LOG.trace("Using action message... [action={}, name={}]", action, actionMsg);
        actionMsgMap.put(action, actionMsg);
    }

    public Annex executeCreateAnnex() {
        LOG.trace("Executing 'Create Annex' use case...");

        Validate.notNull(leosPackage, "Annex package is required!");
        Validate.notNull(annex, "Annex template is required!");
        Validate.notNull(collaborators, "Annex collaborators are required!");
        Validate.notNull(annexNumber, "Annex number is required");

        Option<AnnexMetadata> metadataOption = annex.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Annex metadata is required!");

        Validate.notNull(purpose, "Annex purpose is required!");
        Validate.notNull(type, "Annex type is required!");
        AnnexMetadata metadata = metadataOption.get().withPurpose(purpose).withIndex(index).withNumber(annexNumber).withType(type).withTemplate(template);

        annex = annexService.createAnnex(annex.getId(), leosPackage.getPath(), metadata, actionMsgMap.get(ContextAction.ANNEX_METADATA_UPDATED), null);
        annex = securityService.updateCollaborators(annex.getId(), collaborators, Annex.class);

        return annexService.createVersion(annex.getId(), VersionType.INTERMEDIATE, actionMsgMap.get(ContextAction.DOCUMENT_CREATED));
    }

    public Annex executeImportAnnex() {
        LOG.trace("Executing 'Import Annex' use case...");

        Validate.notNull(leosPackage, "Annex package is required!");
        Validate.notNull(annex, "Annex template is required!");
        Validate.notNull(collaborators, "Annex collaborators are required!");
        Validate.notNull(annexNumber, "Annex number is required");

        Option<AnnexMetadata> metadataOption = annex.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Annex metadata is required!");

        MetadataVO annexMeta = annexDocument.getMetadata();

        Validate.notNull(purpose, "Annex purpose is required!");
        Validate.notNull(type, "Annex type is required!");
        AnnexMetadata metadata = metadataOption.get().withPurpose(purpose).withIndex(index).withNumber(annexNumber).withTitle(annexMeta.getTitle()).withType(type).withTemplate(template);

        annex = annexService.createAnnexFromContent(leosPackage.getPath(), metadata, actionMsgMap.get(ContextAction.ANNEX_BLOCK_UPDATED),
                annexDocument.getSource());
        annex = securityService.updateCollaborators(annex.getId(), collaborators, Annex.class);

        return annexService.createVersion(annex.getId(), VersionType.INTERMEDIATE, actionMsgMap.get(ContextAction.DOCUMENT_CREATED));
    }

    public void executeUpdateAnnexMetadata() {
        LOG.trace("Executing 'Update annex metadata' use case...");
        Validate.notNull(purpose, "Annex purpose is required!");
        Validate.notNull(annexId, "Annex id is required!");

        annex = annexService.findAnnex(annexId);
        Option<AnnexMetadata> metadataOption = annex.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Annex metadata is required!");

        // Updating only purpose at this time. other metadata needs to be set, if needed
        AnnexMetadata annexMetadata = metadataOption.get().withPurpose(purpose);
        annexService.updateAnnex(annex, annexMetadata, VersionType.MINOR, actionMsgMap.get(ContextAction.METADATA_UPDATED));
    }

    public void executeUpdateAnnexIndex() {
        LOG.trace("Executing 'Update annex index' use case...");
        Validate.notNull(annexId, "Annex id is required!");
        Validate.notNull(index, "Annex index is required!");
        Validate.notNull(annexNumber, "Annex number is required");
        Validate.notNull(actionMsgMap, "Action Map is required");
        annex = annexService.findAnnex(annexId);
        Option<AnnexMetadata> metadataOption = annex.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Annex metadata is required!");
        AnnexMetadata metadata = metadataOption.get();
        AnnexMetadata annexMetadata = metadata.withIndex(index).withNumber(annexNumber);
        annex = annexService.updateAnnex(annex, annexMetadata, VersionType.MINOR, actionMsgMap.get(ContextAction.ANNEX_METADATA_UPDATED));
    }

    public void executeUpdateAnnexStructure() {
        byte[] xmlContent = getContent(annex); //Use the content from template
        annex = annexService.findAnnex(annexId); //Get the existing annex document
        
        Option<AnnexMetadata> metadataOption = annex.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Annex metadata is required!");
        AnnexMetadata metadata = metadataOption.get();
        AnnexMetadata annexMetadata = metadata.withPurpose(metadata.getPurpose()).
                    withType(metadata.getType()).withTitle(metadata.getTitle()).withTemplate(template).
                    withDocVersion(metadata.getDocVersion()).withDocTemplate(template);
        
        annex = annexService.updateAnnexWithMetadata(annex, xmlContent, annexMetadata, VersionType.INTERMEDIATE, actionMsgMap.get(ContextAction.ANNEX_STRUCTURE_UPDATED));
    }
    
    private byte[] getContent(Annex annex) {
        final Content content = annex.getContent().getOrError(() -> "Annex content is required!");
        return content.getSource().getBytes();
    }
    
    public void executeCreateMilestone() {
        annex = annexService.findAnnex(annexId);
        List<String> milestoneComments = annex.getMilestoneComments();
        milestoneComments.add(milestoneComment);
        if (annex.getVersionType().equals(VersionType.MAJOR)) {
            annex = annexService.updateAnnexWithMilestoneComments(annex.getId(), milestoneComments);
            LOG.info("Major version {} already present. Updated only milestoneComment for [annex={}]", annex.getVersionLabel(), annex.getId());
        } else {
            annex = annexService.updateAnnexWithMilestoneComments(annex, milestoneComments, VersionType.MAJOR, versionComment);
            LOG.info("Created major version {} for [annex={}]", annex.getVersionLabel(), annex.getId());
        }
    }

    public String getUpdatedAnnexId() {
        return annex.getId();
    }
}
