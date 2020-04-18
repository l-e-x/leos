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
package eu.europa.ec.leos.services.document;

import static eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper.createValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;

import cool.graph.cuid.Cuid;
import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.Proposal;
import eu.europa.ec.leos.domain.cmis.metadata.ProposalMetadata;
import eu.europa.ec.leos.repository.document.ProposalRepository;
import eu.europa.ec.leos.repository.store.PackageRepository;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper;
import eu.europa.ec.leos.services.support.xml.XmlNodeProcessor;
import io.atlassian.fugue.Option;

@Service
public class ProposalServiceImpl implements ProposalService {

    private static final Logger LOG = LoggerFactory.getLogger(ProposalServiceImpl.class);

    private static final String PROPOSAL_NAME_PREFIX = "proposal_";
    private static final String PROPOSAL_DOC_EXTENSION = ".xml";

    private final ProposalRepository proposalRepository;
    private final XmlNodeProcessor xmlNodeProcessor;
    private final XmlContentProcessor xmlContentProcessor;
    private final XmlNodeConfigHelper xmlNodeConfigHelper;
    private final PackageRepository packageRepository;

    ProposalServiceImpl(ProposalRepository proposalRepository,
                        XmlNodeProcessor xmlNodeProcessor,
                        XmlContentProcessor xmlContentProcessor,
                        XmlNodeConfigHelper xmlNodeConfigHelper, PackageRepository packageRepository) {
        this.proposalRepository = proposalRepository;
        this.xmlNodeProcessor = xmlNodeProcessor;
        this.xmlContentProcessor = xmlContentProcessor;
        this.xmlNodeConfigHelper = xmlNodeConfigHelper;
        this.packageRepository = packageRepository;
    }

    @Override
    public Proposal createProposal(String templateId, String path, ProposalMetadata metadata, byte[] content) {
        LOG.trace("Creating Proposal... [templateId={}, path={}, metadata={}]", templateId, path, metadata);
        String name = generateProposalName();
        metadata = metadata.withRef(name.replaceAll(PROPOSAL_DOC_EXTENSION + "$", ""));//FIXME: a better scheme needs to be devised
        Proposal proposal = proposalRepository.createProposal(templateId, path, name, metadata);
        byte[] updatedBytes = updateDataInXml((content == null) ? getContent(proposal) : content, metadata);
        return proposalRepository.updateProposal(proposal.getId(), updatedBytes);
    }

    @Override
    public Proposal createProposalFromContent(String path, ProposalMetadata metadata, byte[] content) {
        LOG.trace("Creating Proposal From Content... [path={}, metadata={}]", path, metadata);
        String name = generateProposalName();
        metadata = metadata.withRef(name.replaceAll(PROPOSAL_DOC_EXTENSION + "$", ""));//FIXME: a better scheme needs to be devised
        return proposalRepository.createProposalFromContent(path, name, metadata, updateDataInXml(content, metadata));
    }

    @Override
    public Proposal findProposal(String id) {
        LOG.trace("Finding Proposal... [id={}]", id);
        return proposalRepository.findProposalById(id, true);
    }

    @Override
    public Proposal updateProposal(Proposal proposal, ProposalMetadata updatedMetadata, VersionType versionType, String comment) {
        LOG.trace("Updating Proposal... [id={}, metadata={}, versionType={}, comment={}]", proposal.getId(), updatedMetadata, versionType, comment);
        byte[] updatedBytes = updateDataInXml(getContent(proposal), updatedMetadata);
        proposal = proposalRepository.updateProposal(proposal.getId(), updatedMetadata, updatedBytes, versionType, comment);
        return proposal;
    }

    @Override
    public Proposal updateProposal(Proposal proposal, ProposalMetadata metadata) {
        LOG.trace("Updating Proposal... [id={}, metadata={}]", proposal.getId(), metadata);
        return proposalRepository.updateProposal(proposal.getId(), metadata);
    }

    @Override
    public Proposal updateProposalWithMilestoneComments(Proposal proposal, List<String> milestoneComments, VersionType versionType, String comment) {
        LOG.trace("Updating Proposal... [id={}, milestoneComments={}, major={}, comment={}]", proposal.getId(), milestoneComments, versionType, comment);
        final byte[] updatedBytes = getContent(proposal);
        proposal = proposalRepository.updateProposal(proposal.getId(), milestoneComments, updatedBytes, versionType, comment);
        return proposal;
    }

    @Override
    public Proposal updateProposalWithMilestoneComments(String proposalId, List<String> milestoneComments) {
        LOG.trace("Updating Proposal... [id={}, milestoneComments={}]", proposalId, milestoneComments);
        return proposalRepository.updateMilestoneComments(proposalId, milestoneComments);
    }

    @Override
    public Proposal findProposalByPackagePath(String path) {
        LOG.trace("Finding Proposal by package path... [path={}]", path);
        // FIXME can be improved, now we dont fetch ALL docs because it's loaded later the one needed,
        List<Proposal> docs = packageRepository.findDocumentsByPackagePath(path, Proposal.class, false);
        if(!docs.isEmpty()){
            return findProposal(docs.get(0).getId());
        } else {
            return null;
        }
    }

    @Override
    @Async("delegatingSecurityContextAsyncTaskExecutor")
    public void updateProposalAsync(String documentId, String comment) {
        LeosPackage leosPackage = packageRepository.findPackageByDocumentId(documentId);
        Proposal proposal = this.findProposalByPackagePath(leosPackage.getPath());
        if (proposal != null) {
            Option<ProposalMetadata> metadataOption = proposal.getMetadata();
            ProposalMetadata metadata = metadataOption.get();
            if (StringUtils.isEmpty(comment)) {                                // Comment will be stored in cmis:checkinComment property.
                proposalRepository.updateProposal(proposal.getId(), metadata); // This property only can be updated with a document checkout/checkin (creating new version).
            } else {                                                           // Then a new proposal version is created only when a comment is received.
                proposalRepository.updateProposal(proposal.getId(), metadata, getContent(proposal), VersionType.MINOR, comment);
            }
        }
    }

    private byte[] updateDataInXml(final byte[] content, ProposalMetadata dataObject) {
        byte[] updatedBytes = xmlNodeProcessor.setValuesInXml(content, createValueMap(dataObject), xmlNodeConfigHelper.getConfig(dataObject.getCategory()));
        return xmlContentProcessor.doXMLPostProcessing(updatedBytes);
    }

    @Override
    public Proposal addComponentRef(Proposal proposal, String href, LeosCategory leosCategory){
        LOG.trace("Add component in Proposal ... [id={}, href={}, leosCategory={}]", proposal.getId(), href, leosCategory.name());
        Stopwatch stopwatch = Stopwatch.createStarted();

        //create config
        Map<String, String> keyValueMap = new HashMap<>();
        keyValueMap.put(leosCategory.name() + "_href", href);

        //Do the xml update
        byte[] xmlBytes = proposal.getContent().get().getSource().getBytes();
        byte[] updatedBytes = xmlNodeProcessor.setValuesInXml(xmlBytes,
                                keyValueMap,
                                xmlNodeConfigHelper.getProposalComponentsConfig(leosCategory, "href"));
        updatedBytes = xmlContentProcessor.doXMLPostProcessing(updatedBytes);

        //save updated xml
        proposal = proposalRepository.updateProposal(proposal.getId(), updatedBytes);

        LOG.trace("Added component in Proposal ...({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return proposal;
    }

    @Override
    public Proposal removeComponentRef(Proposal proposal, String href, LeosCategory leosCategory){
        LOG.trace("Removing component in Proposal ... [id={}, href={}]", proposal.getId(), href);
        Stopwatch stopwatch = Stopwatch.createStarted();

        byte[] xmlBytes = proposal.getContent().get().getSource().getBytes();
        byte[] updatedBytes = xmlContentProcessor.removeElements(xmlBytes,
                               String.format("//collectionBody/component[@refersTo='#%s']", leosCategory.name().toLowerCase()));

        //save updated xml
        proposal = proposalRepository.updateProposal(proposal.getId(), updatedBytes);
        LOG.trace("Removed component in Proposal ...({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return proposal;
    }

    private String generateProposalName() {
        return PROPOSAL_NAME_PREFIX + Cuid.createCuid() + PROPOSAL_DOC_EXTENSION;
    }

    private byte[] getContent(Proposal proposal) {
        final Content content = proposal.getContent().getOrError(() -> "Proposal content is required!");
        return content.getSource().getBytes();
    }

    @Override
    public Proposal createVersion(String id, VersionType versionType, String comment) {
        LOG.trace("Creating Proposal version... [id={}, versionType={}, comment={}]", id, versionType, comment);
        final Proposal proposal = findProposal(id);
        final ProposalMetadata metadata = proposal.getMetadata().getOrError(() -> "Proposal metadata is required!");
        final Content content = proposal.getContent().getOrError(() -> "Proposal content is required!");
        final byte[] contentBytes = content.getSource().getBytes();
        return proposalRepository.updateProposal(id, metadata, contentBytes, versionType, comment);
    }

    @Override
    public Proposal findProposalByRef(String ref) {
        LOG.trace("Finding Proposal by ref... [ref=" + ref + "]");
        return proposalRepository.findProposalByRef(ref);
    }
}
