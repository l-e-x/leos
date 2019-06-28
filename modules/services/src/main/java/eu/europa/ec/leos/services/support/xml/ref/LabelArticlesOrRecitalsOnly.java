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

import eu.europa.ec.leos.i18n.LanguageHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LabelArticlesOrRecitalsOnly extends LabelHandler {

    private static final List<String> TOP_LEVEL_NODES = Arrays.asList("article", "recital");
    
    @Override
    public boolean process(List<TreeNode> refs, List<TreeNode> mrefCommonNodes, TreeNode sourceNode, StringBuffer label, Locale locale) {
        //if all refs are higher than article generate and leave
        for (TreeNode ref : refs) {
            if (!TOP_LEVEL_NODES.contains(ref.getType())) {
                return false;//break and let other rules handle 
            }
        }

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
                    buffers.get(node).insert(0, createAnchor(node, locale));
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

                    if (TOP_LEVEL_NODES.contains(iNode.getType()) && TOP_LEVEL_NODES.contains(jNode.getType())) {
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

        return true;
    }

    @Override
    public int getOrder() {
        return 3;
    }
}
