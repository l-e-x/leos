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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.ARTICLE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.RECITAL;

@Component
public class LabelArticlesOrRecitalsOnly extends LabelHandler {

    private static final List<String> NODES_TO_CONSIDER = Arrays.asList(ARTICLE, RECITAL);
    
    @Override
    public boolean canProcess(List<TreeNode> refs) {
        boolean canProcess = refs.stream()
                .allMatch(ref -> NODES_TO_CONSIDER.contains(ref.getType()));
        return canProcess;
    }
    
    @Override
    public void process(List<TreeNode> refs, List<TreeNode> mrefCommonNodes, TreeNode sourceNode, StringBuffer label, Locale locale, boolean withAnchor) {
        Map<TreeNode, StringBuffer> buffers = new HashMap<>();
        refs.forEach(ref -> buffers.put(ref, new StringBuffer()));

        List<TreeNode> pendingNodes = new ArrayList<>();
        pendingNodes.addAll(refs);
        Collections.reverse(pendingNodes);//reverse as we want to start from last
        boolean AND = true;
        int plural = 1;

        //As only articles are to be considered and all articles are to be considered at same level
        List<TreeNode> toBeTreatedNodes = pendingNodes;

        for (int i = 0; i < toBeTreatedNodes.size(); i++) {
            TreeNode node = toBeTreatedNodes.get(i);
            if (!mrefCommonNodes.contains(node) || toBeTreatedNodes.size() > 1) {
                if (node.getChildren().isEmpty()) {
                    buffers.get(node).insert(0, createAnchor(node, locale, withAnchor));
                } else {
                    buffers.get(node).insert(0, NumFormatter.formattedNum(node, locale));
                }
            }
        }

        if (toBeTreatedNodes.size() > 1) {
            for (int i = 0; i < toBeTreatedNodes.size(); i++) {
                TreeNode iNode = toBeTreatedNodes.get(i);
                for (int j = i + 1; j < toBeTreatedNodes.size(); ) {
                    TreeNode jNode = toBeTreatedNodes.get(j);

                    if (NODES_TO_CONSIDER.contains(iNode.getType()) && NODES_TO_CONSIDER.contains(jNode.getType())) {
                        buffers.get(iNode).insert(0, AND ? " and " : ", ");
                        buffers.get(iNode).insert(0, buffers.get(jNode));
                        plural = 0;
                        
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
            if (!mrefCommonNodes.contains(node) || toBeTreatedNodes.size() > 1) {
                buffers.get(node).insert(0, String.format("%s ", StringUtils.capitalize(NumFormatter.formatPlural(node, plural, locale))));
                plural = 1;
            } else {
                buffers.get(node).insert(0, String.format("%s %s ", StringUtils.capitalize(THIS_REF), node.getType()));
            }
            label.insert(0, buffers.get(node));
        }
    }

    @Override
    public int getOrder() {
        return 3;
    }
}
