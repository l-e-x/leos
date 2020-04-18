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
package eu.europa.ec.leos.services.support.xml;

import com.ximpleware.VTDNav;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.common.ErrorCode;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.i18n.LanguageHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.services.content.ReferenceLabelService;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.services.store.WorkspaceService;
import eu.europa.ec.leos.services.support.xml.ref.LabelHandler;
import eu.europa.ec.leos.services.support.xml.ref.Ref;
import eu.europa.ec.leos.services.support.xml.ref.TreeHelper;
import eu.europa.ec.leos.services.support.xml.ref.TreeNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.ARTICLE;
import static eu.europa.ec.leos.services.support.xml.ref.TreeHelper.createTree;
import static eu.europa.ec.leos.services.support.xml.ref.TreeHelper.findCommonAncestor;
import static eu.europa.ec.leos.services.support.xml.ref.TreeHelper.getLeaves;

abstract class ReferenceLabelServiceImpl implements ReferenceLabelService {
    private static final Logger LOG = LoggerFactory.getLogger(ReferenceLabelServiceImpl.class);
    
    @Autowired
    private LanguageHelper languageHelper;
    @Autowired
    protected MessageHelper messageHelper;
    @Autowired
    protected List<LabelHandler> labelHandlers;
    @Autowired
    protected WorkspaceService workspaceService;
    @Autowired
    protected PackageService packageService;
    
    /**
     * Generates the multi references label.
     * This method is used when you don't need real <code>html</code> ref, instead you want only the label.
     * Example of generated label: Article 1(1), point (a)(1)(i), second indent
     * Overloads: {@link #generateLabel(List, String, String, byte[], byte[], String, boolean)}
     *
     * @param refs              element ids selected by the user
     * @param sourceBytes       bytes of the source document
     * @return: returns the label
     */
    @Override
    public Result<String> generateLabel(List<Ref> refs, byte[] sourceBytes){
        return generateLabel(refs, "", "", sourceBytes, sourceBytes, null, false);
    }
    @Override
    public Result<String> generateLabelStringRef(List<String> refsString, String sourceDocumentRef, byte[] sourceBytes){
        final List refs = buildRefs(refsString, sourceDocumentRef);
        return generateLabel(refs, sourceBytes);
    }
    
    /**
     * Generates the multi references label.
     * Uses <code>documentRef</code> of the <code>refs</code> list to fetch the target document from the repository.
     * Once getting the target data, overloads: {@link #generateLabel(List, String, String, byte[], byte[], String, boolean)}
     *
     * @param refs              element ids selected by the user
     * @param sourceRefId       element id being edited
     * @param sourceDocumentRef document reference of the source document
     * @param sourceBytes       bytes of the source document
     * @return: returns the label if multi ref is valid or an error code otherwise.
     */
    @Override
    public Result<String> generateLabel(List<Ref> refs, String sourceDocumentRef, String sourceRefId, byte[] sourceBytes) {
        // fetch the target only if is different from the source
        XmlDocument targetDocument;
        try {
            targetDocument = refs.stream()
                    .filter(ref -> !ref.getDocumentref().equals(sourceDocumentRef))
                    .findFirst()
                    .map(ref -> workspaceService.findDocumentByRef(ref.getDocumentref(), XmlDocument.class))
                    .orElse(null);
        } catch(Exception e){
            LOG.warn("Error fetching target document. {}", e.getMessage());
            return new Result<>("", ErrorCode.DOCUMENT_REFERENCE_NOT_VALID);
        }
        
        byte[] targetBytes = sourceBytes;
        String targetDocType = "";
        if (targetDocument != null) {
            targetBytes = targetDocument.getContent().get().getSource().getBytes();
            targetDocType = packageService.calculateDocType(targetDocument);
        }
        return generateLabel(refs, sourceDocumentRef, sourceRefId, sourceBytes, targetBytes, targetDocType, true);
    }
    @Override
    public Result<String> generateLabelStringRef(List<String> refsString, String sourceDocumentRef, String sourceRefId, byte[] sourceBytes) {
        final List refs = buildRefs(refsString, sourceDocumentRef);
        return generateLabel(refs, sourceDocumentRef, sourceRefId, sourceBytes);
    }
    
    /**
     * Generates the multi references label composed by the href for navigation purpose.
     * If the selected elements are located in different documents(source!=target), the algorithm add the suffix (@param targetDocType)
     * to the generated label.
     * Example of generated label: Article 1(1), point (a)(1)(i), <ref xml:id="" href="art_1_6FvZwH" documentref="bill_ck67vo25e0004dc9682dmo307">second</ref> indent
     *
     * @param refs              element ids selected by the user
     * @param sourceRefId       element id being edited
     * @param sourceDocumentRef document reference of the source document
     * @param sourceBytes       bytes of the source document
     * @param targetBytes       bytes of the target document
     * @param targetDocType     Prefix for the target document. (Ex: Annex, Regulation, Decision, etc)
     * @param withAnchor        true if the label should be a html anchor for navigation purpose
     *
     * @return: returns the label if multi ref is valid or an error code otherwise.
     */
    @Override
    public Result<String> generateLabel(List<Ref> refs, String sourceDocumentRef, String sourceRefId, byte[] sourceBytes, byte[] targetBytes, String targetDocType, boolean withAnchor) {
        try {
            VTDNav targetVtdNav = VTDUtils.setupVTDNav(targetBytes);
            TreeNode targetTree = createTree(targetVtdNav, null, refs);
            List<TreeNode> targetNodes = getLeaves(targetTree);//in xml order

            //Validate
            boolean valid = RefValidator.validate(targetNodes, refs);
            //If invalid references, mark mref as broken and return
            if (!valid) {
                return new Result<>("", ErrorCode.DOCUMENT_REFERENCE_NOT_VALID);
            }

            VTDNav sourceVtdNav = VTDUtils.setupVTDNav(sourceBytes, true);
            TreeNode sourceTree = createTree(sourceVtdNav, null, Arrays.asList(new Ref("", sourceRefId, sourceDocumentRef)));
            TreeNode sourceNode = TreeHelper.find(sourceTree, TreeNode::getIdentifier, sourceRefId);
    
            List<TreeNode> commonNodes = findCommonAncestor(sourceVtdNav, sourceRefId, targetTree);
            //If Valid, generate, Tree is already sorted
            return new Result<>(createLabel(targetNodes, commonNodes, sourceNode, targetDocType, withAnchor), null);
        } catch (IllegalStateException e) {
            return new Result<>("", ErrorCode.DOCUMENT_REFERENCE_NOT_VALID);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error generation Labels for sourceRefId: " + sourceRefId + " and references: " + refs, e);
        }
    }
    @Override
    public Result<String> generateLabelStringRef(List<String> refsString, String sourceDocumentRef, String sourceRefId, byte[] sourceBytes, String targetDocumentRef, boolean withAnchor) {
        final XmlDocument targetDocument = getTargetDocument(sourceDocumentRef, targetDocumentRef);
        
        byte[] targetBytes = sourceBytes;
        String targetDocType = "";
        if (targetDocument != null) {
            targetBytes = targetDocument.getContent().get().getSource().getBytes();
            targetDocType = packageService.calculateDocType(targetDocument);
        }
        
        final List<Ref> refs = buildRefs(refsString, targetDocumentRef);
        return generateLabel(refs, sourceDocumentRef, sourceRefId, sourceBytes, targetBytes, targetDocType, true);
    }
    
    private XmlDocument getTargetDocument(String sourceDocumentRef, String targetDocumentRef){
        XmlDocument targetDocument = null;
        if(!targetDocumentRef.equals(sourceDocumentRef)){
            targetDocument = workspaceService.findDocumentByRef(targetDocumentRef, XmlDocument.class);
        }
        return targetDocument;
    }
    
    private String createLabel(List<TreeNode> refs, List<TreeNode> mrefCommonNodes, TreeNode sourceNode, String docType, boolean withAnchor) {
        labelHandlers.sort(Comparator.comparingInt(LabelHandler::getOrder));
        StringBuffer accumulator = new StringBuffer();
        for (LabelHandler rule : labelHandlers) {
            if(rule.canProcess(refs)) {
                rule.addPreffix(accumulator, docType);
                rule.process(refs, mrefCommonNodes, sourceNode, accumulator, languageHelper.getCurrentLocale(), withAnchor);
                break;
            }
        }
        return accumulator.toString();
    }
    
    private List<Ref> buildRefs(List<String> refs, String targetDocumentRef) {
        return refs
                .stream()
                .map(r -> {
                    String[] strs = r.split(",");
                    String refId;
                    String refHref;
                    if (strs.length == 0) {
                        throw new RuntimeException("references list is empty");
                    } else if (strs.length == 1) {
                        refId = "";
                        refHref = strs[0];
                    } else {
                        refId = strs[0];
                        refHref = strs[1];
                    }
                    return new Ref(refId, refHref, targetDocumentRef);
                })
                .filter(ref -> !StringUtils.isEmpty(ref.getHref()))
                .collect(Collectors.toList());
    }
    

    static class RefValidator {
        private static final List<BiFunction<List<TreeNode>, List<Ref>, Boolean>> validationRules = new ArrayList<>();

        static {
            validationRules.add(RefValidator::checkEmpty);
            validationRules.add(RefValidator::checkSameParentAndSameType);
            validationRules.add(RefValidator::checkBrokenRefs);
        }

        static boolean validate(List<TreeNode> refs, List<Ref> oldRefs) {
            for (BiFunction<List<TreeNode>, List<Ref>, Boolean> rule : validationRules) {
                if (!rule.apply(refs, oldRefs)) {
                    return false;
                }
            }
            return true;
        }

        private static boolean checkBrokenRefs(List<TreeNode> refs, List<Ref> oldRefs) {
            return (refs.size() == oldRefs.size());
        }

        private static boolean checkEmpty(List<TreeNode> refs, List<Ref> oldRefs) {
            return !refs.isEmpty();
        }

        private static boolean checkSameParentAndSameType(List<TreeNode> refs, List<Ref> oldRefs) {
            TreeNode parent = refs.get(0).getParent();
            String type = refs.get(0).getType();
            int depth = refs.get(0).getDepth();

            for (int i = 1; i < refs.size(); i++) {
                TreeNode ref= refs.get(i);
                if (!ref.getParent().equals(parent)
                        && !(ref.getType().equals(type) && (ref.getDepth() == depth || type.equalsIgnoreCase(ARTICLE)))) {
                    return false;
                }
            }
            return true;
        }
    }
}
