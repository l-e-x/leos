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
public class LabelHigherOrderElementsOnly extends LabelHandler {

    private List<String> higherOrderElements = Arrays.asList("part", "section", "title", "chapter");

    @Override
    public boolean process(List<TreeNode> refs, List<TreeNode> mrefCommonNodes, StringBuffer label) {
        //if all refs are higher than article generate and leave
        for (TreeNode ref : refs) {
            if (!higherOrderElements.contains(ref.getType())) {
                return false;//break and let other rules handle 
            }
        }

        Map<TreeNode, Deque<String>> buffers = new HashMap<>();
        refs.forEach(ref -> buffers.put(ref, new ArrayDeque<String>()));

        List<TreeNode> pendingNodes = new ArrayList<>();
        pendingNodes.addAll(refs);
        Collections.reverse(pendingNodes);//reverse as we want to start from last

        List<TreeNode> toBeTreatedNodes;
        do {
            toBeTreatedNodes = seperateNodesOfMaxDepth(pendingNodes);

            for (int i = 0; i < toBeTreatedNodes.size(); i++) {
                TreeNode node = toBeTreatedNodes.get(i);
                if (!mrefCommonNodes.contains(node) || toBeTreatedNodes.size() > 1) {
                    if (node.getChildren().isEmpty()) {
                        buffers.get(node).push(createAnchor(node));
                    } else if (node.getNum() != null) {
                        buffers.get(node).push(NumFormatter.formattedNum(node) + " ");
                    }
                }
            }

            if (toBeTreatedNodes.size() > 1) {
                for (int i = 0; i < toBeTreatedNodes.size(); i++) {
                    TreeNode iNode = toBeTreatedNodes.get(i);
                    boolean AND = true;
                    for (int j = i + 1; j < toBeTreatedNodes.size(); ) {
                        TreeNode jNode = toBeTreatedNodes.get(j);

                        if (iNode.getParent().equals(jNode.getParent())
                                || (iNode.getType().equals("article") && jNode.getType().equals("article"))) {
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
                if (higherOrderElements.contains(node.getType())) {
                    if (!mrefCommonNodes.contains(node)) {
                        buffers.get(node).push(String.format("%s ", StringUtils.capitalize(node.getType())));
                    }
                    else {
                        buffers.get(node).push(String.format("%s %s ", StringUtils.capitalize(CURRENT), node.getType()));
                    }
                }
            }

            //assign buffers to parent
            for (TreeNode node : toBeTreatedNodes) {
                if ((node.getParent() == null) || (toBeTreatedNodes.size() == 1 && mrefCommonNodes.contains(toBeTreatedNodes.get(0)))) {
                    Deque<String> buffer = buffers.get(node);
                    buffers.remove(node);
                    buffer.forEach(item -> label.append(item));
                    pendingNodes.remove(node);
                } else if (node.getParent() != null) {
                    Deque<String> buffer = buffers.get(node);
                    buffers.remove(node);
                    int index = pendingNodes.indexOf(node);
                    pendingNodes.remove(node);

                    buffers.put(node.getParent(), buffer);
                    pendingNodes.add(index, node.getParent());
                } 
            }
        }
        while ((pendingNodes.size() > 0) && (toBeTreatedNodes.size() > 1 || !mrefCommonNodes.contains(toBeTreatedNodes.get(0))));

        return true;
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
