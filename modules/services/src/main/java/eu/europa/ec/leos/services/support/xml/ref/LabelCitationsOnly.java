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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.CITATION;

@Component
public class LabelCitationsOnly extends LabelHandler {

    private static final List<String> NODES_TO_CONSIDER = Arrays.asList(CITATION);
    
    @Override
    public boolean canProcess(List<TreeNode> refs) {
        boolean canProcess = refs.stream()
                .allMatch(ref -> NODES_TO_CONSIDER.contains(ref.getType()));
        return canProcess;
    }
    
    @Override
    public void process(List<TreeNode> refs, List<TreeNode> mrefCommonNodes, TreeNode sourceNode, StringBuffer label, Locale locale, boolean withAnchor) {
        Deque<String> sb = new ArrayDeque<>();
        //collect all the ref anchors
        for (int i = 0; i < refs.size(); i++) {
            if (i != 0 && i == refs.size() - 1) {
                sb.push(" and ");
            } else if (i > 0) {
                sb.push(", ");
            }
            sb.push(createAnchor(refs.get(i), locale, withAnchor));

            if (i == refs.size() - 1) {
                //consolidate here
                StringBuilder stringBuilder = new StringBuilder();
                while (sb.size() > 0) {
                    stringBuilder.append(sb.removeLast());
                }
                sb.push(stringBuilder.toString());
                
                //Now order it.
                String refType = refs.get(i).getType();
                if(NumFormatter.isUnnumbered(refs.get(i))) {
                    sb.push(" " + refType);
                }
                else{
                    sb.addLast(refType+" ");
                }
            }
        }

        while (sb.size() > 0) {
            label.append(sb.removeLast());
        }
    }

    @Override
    public int getOrder() {
        return 4;
    }

}
