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
package eu.europa.ec.leos.usecases.document;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Annex;
import eu.europa.ec.leos.domain.document.LeosMetadata;
import eu.europa.ec.leos.domain.document.LeosMetadata.AnnexMetadata;
import eu.europa.ec.leos.domain.document.LeosPackage;
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

@Component
@Scope("prototype")
public class AnnexContext {

    private static final Logger LOG = LoggerFactory.getLogger(AnnexContext.class);

    private final TemplateService templateService;
    private final AnnexService annexService;
    private final SecurityService securityService;

    private LeosPackage leosPackage;
    private Annex template;
    private int index;
    private String purpose = null;
    private String annexId = null;
    private Map<String, LeosAuthority> collaborators = null;

    private DocumentVO annexDocument;
    private final Map<ContextAction, String> actionMsgMap;
    private String annexNumber;

    public AnnexContext(
            TemplateService templateService,
            AnnexService annexService,
            SecurityService securityService) {
        this.templateService = templateService;
        this.annexService = annexService;
        this.securityService = securityService;
        this.actionMsgMap = new HashMap<>();
    }

    public void useTemplate(String name) {
        Validate.notNull(name, "Template name is required!");

        // KLUGE temporary workaround
        String annexTemplate = "AN" + name.substring(2);

        template = (Annex) templateService.getTemplate(annexTemplate);
        Validate.notNull(template, "Template not found! [name=%s]", annexTemplate);

        LOG.trace("Using {} template... [id={}, name={}]", template.getCategory(), template.getId(), template.getName());
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
    
    public void useCollaborators(Map<String, LeosAuthority> collaborators) {
        Validate.notNull(collaborators, "Annex 'collaborators' are required!");
        LOG.trace("Using collaborators'... [collaborators={}]", collaborators);
        this.collaborators = Collections.unmodifiableMap(collaborators);
    }

    public void useAnnexNumber(String annexNumber) {
    	Validate.notNull(annexNumber, "Annex Number is required!");
    	this.annexNumber = annexNumber;
    }

    public Annex executeCreateAnnex() {
        LOG.trace("Executing 'Create Annex' use case...");

        Validate.notNull(leosPackage, "Annex package is required!");
        Validate.notNull(template, "Annex template is required!");
        Validate.notNull(collaborators, "Annex collaborators are required!");
        Validate.notNull(annexNumber,"Annex number is required");

        Option<AnnexMetadata> metadataOption = template.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Annex metadata is required!");

        Validate.notNull(purpose, "Annex purpose is required!");
        AnnexMetadata metadata = metadataOption.get().withPurpose(purpose).withIndex(index).withNumber(annexNumber);

        Annex annex = annexService.createAnnex(template.getId(), leosPackage.getPath(), metadata,actionMsgMap.get(ContextAction.ANNEX_METADATA_UPDATED), null);
        annex = securityService.updateCollaborators(annex.getId(), collaborators, Annex.class);

        return annex;
    }

    public Annex executeImportAnnex() {
        LOG.trace("Executing 'Import Annex' use case...");

        Validate.notNull(leosPackage, "Annex package is required!");
        Validate.notNull(template, "Annex template is required!");
        Validate.notNull(collaborators, "Annex collaborators are required!");
        Validate.notNull(annexNumber,"Annex number is required");

        Option<AnnexMetadata> metadataOption = template.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Annex metadata is required!");

        MetadataVO annexMeta = annexDocument.getMetadata();

        Validate.notNull(purpose, "Annex purpose is required!");
        AnnexMetadata metadata = metadataOption.get().withPurpose(purpose).withIndex(index).withNumber(annexNumber).withTitle(annexMeta.getTitle());
        
        Annex annex = annexService.createAnnex(template.getId(), leosPackage.getPath(), metadata, actionMsgMap.get(ContextAction.ANNEX_BLOCK_UPDATED), annexDocument.getSource());
        annex = securityService.updateCollaborators(annex.getId(), collaborators, Annex.class);

        return annex;
    }

    public void executeUpdateAnnexMetadata() {
        LOG.trace("Executing 'Update annex metadata' use case...");
        Validate.notNull(purpose, "Annex purpose is required!");
        Validate.notNull(annexId, "Annex id is required!");

        Annex annex = annexService.findAnnex(annexId);
        Option<LeosMetadata.AnnexMetadata> metadataOption = annex.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Annex metadata is required!");

        //Updating only purpose at this time. other metadata needs to be set, if needed
        AnnexMetadata annexMetadata = metadataOption.get().withPurpose(purpose);
        annexService.updateAnnex(annex, annexMetadata,false, actionMsgMap.get(ContextAction.METADATA_UPDATED));
    }

	public void executeUpdateAnnexIndex() {
		LOG.trace("Executing 'Update annex index' use case...");
		Validate.notNull(annexId, "Annex id is required!");
		Validate.notNull(index, "Annex index is required!");
		Validate.notNull(annexNumber,"Annex number is required");
		Validate.notNull(actionMsgMap,"Action Map is required");
		Annex annex = annexService.findAnnex(annexId);
		Option<LeosMetadata.AnnexMetadata> metadataOption = annex.getMetadata();
		Validate.isTrue(metadataOption.isDefined(), "Annex metadata is required!");
		AnnexMetadata metadata = metadataOption.get();
		AnnexMetadata annexMetadata = metadata.withIndex(index).withNumber(annexNumber);
		annexService.updateAnnex(annex, annexMetadata, false, actionMsgMap.get(ContextAction.ANNEX_METADATA_UPDATED));
	}

}
