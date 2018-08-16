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
package eu.europa.ec.leos.services.support.xml.ref;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

abstract public class LabelHandler {
    protected final String CURRENT = "current";
    protected final String ARTICLE = "article";
    
    abstract public boolean process(List<TreeNode> refs, List<TreeNode> mrefCommonNodes, StringBuffer label);

    abstract public int getOrder();

    protected String createAnchor(TreeNode ref) {
        return String.format("<ref GUID=\"%s\" href=\"%s\">%s</ref>",
                ref.getRefId(),
                ref.getIdentifier(),
                NumFormatter.formattedNum(ref));
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
}
