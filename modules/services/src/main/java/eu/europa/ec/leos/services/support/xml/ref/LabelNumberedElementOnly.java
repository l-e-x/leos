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

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LabelNumberedElementOnly extends LabelHandler {

    @Override
    public boolean process(List<TreeNode> refs, List<TreeNode> mrefCommonNodes, StringBuffer label) {
        for (TreeNode ref : refs) {
            if (NumFormatter.anyUnnumberedParent(ref)) {
                return false;//break and let other rules handle 
            }
        }

        Map<TreeNode, Deque<String>> buffers = new HashMap<>();
        refs.forEach(ref -> buffers.put(ref, new ArrayDeque<>()));

        List<TreeNode> pendingNodes = new ArrayList<>();
        pendingNodes.addAll(refs);
        Collections.reverse(pendingNodes);//reverse as we want to start from last

        List<TreeNode> toBeTreatedNodes = null;
        boolean AND = true;

        do {
            toBeTreatedNodes = seperateNodesOfMaxDepth(pendingNodes);

            for (int i = 0; i < toBeTreatedNodes.size(); i++) {
                TreeNode node = toBeTreatedNodes.get(i);
                if (node.getChildren().isEmpty() && refs.contains(node)) {
                    if (mrefCommonNodes.contains(node) && toBeTreatedNodes.size()==1) {
                        buffers.get(node).push(StringUtils.capitalize(CURRENT) + " " + node.getType());
                    }
                    else {
                        buffers.get(node).push(createAnchor(node));
                    }
                } else if (!mrefCommonNodes.contains(node)) {
                    buffers.get(node).push(NumFormatter.formattedNum(node));
                }
            }

            if (toBeTreatedNodes.size() > 1) {
                for (int i = 0; i < toBeTreatedNodes.size(); i++) {
                    TreeNode iNode = toBeTreatedNodes.get(i);
                    for (int j = i + 1; j < toBeTreatedNodes.size(); ) {
                        TreeNode jNode = toBeTreatedNodes.get(j);

                        if (iNode.getParent().equals(jNode.getParent())) {
                            buffers.get(iNode).push(AND ? " and " : ", ");
                            buffers.get(jNode).forEach(item -> buffers.get(iNode).push(item));

                            buffers.remove(jNode);
                            toBeTreatedNodes.remove(jNode);
                            pendingNodes.remove(jNode);

                            AND = false;
                        } else {
                            j++;
                        }
                    }
                }
            }

            for (TreeNode node : toBeTreatedNodes) {
                // "Article" reference should not be displayed when we referred sth in the same article
                if (!mrefCommonNodes.contains(node) && node.getType().equals("article")) {
                    buffers.get(node).push(StringUtils.capitalize(node.getType()) + " ");
                }
            }

            //assign buffers to parent
            for (TreeNode node : toBeTreatedNodes) {
                if (node.getParent() == null || "article".equals(node.getType())) {
                    Deque<String> buffer = buffers.get(node);
                    buffers.remove(node);
                    buffer.forEach(item -> label.append(item));
                    pendingNodes.remove(node);
                }
                else if (node.getParent() != null) {
                    Deque<String> buffer = buffers.get(node);
                    buffers.remove(node);
                    int index = pendingNodes.indexOf(node);
                    pendingNodes.remove(node);

                    buffers.put(node.getParent(), buffer);
                    pendingNodes.add(index, node.getParent());
                }
              
            }

        }
        while (pendingNodes.size() > 0 && !contains(toBeTreatedNodes, TreeNode::getType, "article"));

        return true;
    }

    @Override
    public int getOrder() {
        return 5;
    }
}
