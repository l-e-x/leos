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

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.ARTICLE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.CHAPTER;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.PART;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.SECTION;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.TITLE;

@Component
public class LabelHigherOrderElementsOnly extends LabelHandler {

    private List<String> NODES_TO_CONSIDER = Arrays.asList(PART, SECTION, TITLE, CHAPTER);

    @Override
    public boolean canProcess(List<TreeNode> refs) {
        boolean canProcess = refs.stream()
                        .allMatch(ref -> NODES_TO_CONSIDER.contains(ref.getType()));
        return canProcess;
    }

    public void addPreffix(StringBuffer label, String docType) {
        if (!StringUtils.isEmpty(docType)) {
            label.append(docType);
            label.append(", ");
        }
    }

    @Override
    public void process(List<TreeNode> refs, List<TreeNode> mrefCommonNodes, TreeNode sourceNode, StringBuffer label, Locale locale, boolean withAnchor) {
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
                        buffers.get(node).push(createAnchor(node, locale, withAnchor));
                    } else if (node.getNum() != null) {
                        buffers.get(node).push(String.format("%s, ", NumFormatter.formattedNum(node, locale)));
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
                                || (iNode.getType().equals(ARTICLE) && jNode.getType().equals(ARTICLE))) {
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
                if (NODES_TO_CONSIDER.contains(node.getType())) {
                    if (!mrefCommonNodes.contains(node)) {
                        buffers.get(node).push(String.format("%s ", StringUtils.capitalize(node.getType())));
                    }
                    else {
                        buffers.get(node).push(String.format("%s %s ", StringUtils.capitalize(THIS_REF), node.getType()));
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
    }

    @Override
    public int getOrder() {
        return 2;
    }

}
