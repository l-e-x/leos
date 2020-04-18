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

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.CLAUSE;

@Component
public class LabelSimpleNodesOnly extends LabelHandler {
    
    private static final List<String> NODES_TO_CONSIDER = Arrays.asList(CLAUSE);
    
    @Override
    public boolean canProcess(List<TreeNode> refs) {
        boolean canProcess = refs.stream()
                .allMatch(ref -> NODES_TO_CONSIDER.contains(ref.getType()));
        return canProcess;
    }
    
    
    /**
     * This label processor shows only the type name as label, without calculating any numbering.
     */
    @Override
    public void process(List<TreeNode> refs, List<TreeNode> mrefCommonNodes, TreeNode sourceNode, StringBuffer label, Locale locale, boolean withAnchor) {
        refs.stream().forEach(ref -> {
            label.append(ref.getType())
                    .append(", ");
        });

        if(label.substring(label.length()-2, label.length()).equals(", ")){
            label.delete(label.length()-2, label.length());
        }
    }
    
    @Override
    public int getOrder() {
        return 1;
    }
}
