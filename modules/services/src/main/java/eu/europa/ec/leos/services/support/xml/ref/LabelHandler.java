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
package eu.europa.ec.leos.services.support.xml.ref;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.ARTICLE;

abstract public class LabelHandler {
    protected final String THIS_REF = "this";
    
    abstract public boolean canProcess(List<TreeNode> refs);
    
    abstract public void process(List<TreeNode> refs, List<TreeNode> mrefCommonNodes, TreeNode sourceNode, StringBuffer label, Locale locale, boolean withAnchor);

    abstract public int getOrder();

    protected String createAnchor(TreeNode ref, Locale locale, boolean withAnchor) {
        final String rv;
        if(withAnchor) {
            rv = String.format("<ref xml:id=\"%s\" href=\"%s\" documentref=\"%s\">%s</ref>",
                    ref.getRefId(),
                    ref.getIdentifier(),
                    ref.getDocumentRef(),
                    NumFormatter.formattedNum(ref, locale));
        } else {
            rv = NumFormatter.formattedNum(ref, locale);
        }
        
        return rv;
    }
    protected final boolean contains(List<TreeNode> node, Function<TreeNode, Object> valueGetter, Object value) {
        for (TreeNode treeNode : node) {
            if (value.equals(valueGetter.apply(treeNode))) {
                return true;
            }
        }
        return false;
    }
    protected final List<TreeNode> seperateNodesOfMaxDepth(List<TreeNode> pendingNodes) {
        List<TreeNode> nodes = new ArrayList<>();
        if (pendingNodes.size() > 0) {
            TreeNode maxDepthNode = pendingNodes.get(0);
            for (TreeNode node : pendingNodes) {
                if (node.getDepth() > maxDepthNode.getDepth()) {
                    maxDepthNode = node;
                }
            }
            for (TreeNode node : pendingNodes) {
                if (node.getDepth() == maxDepthNode.getDepth()) {
                    nodes.add(node);
                }
            }
        }
        return nodes;
    }
    
    protected boolean inThisArticle(List<TreeNode> mrefCommonNodes) {
        return mrefCommonNodes.size() > 0 && ARTICLE.equals(mrefCommonNodes.get(0).getType());
    }

    public void addPreffix(StringBuffer label, String docType) {
    }

}
