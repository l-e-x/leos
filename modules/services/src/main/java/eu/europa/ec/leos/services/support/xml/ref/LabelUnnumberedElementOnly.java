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
public class LabelUnnumberedElementOnly extends LabelHandler {

    private final List<String> thisTypes = new ArrayList<String>(Arrays.asList("paragraph", "point"));
    private final String PARAGRAPH = "paragraph";

    @Override
    public boolean process(List<TreeNode> refs, List<TreeNode> mrefCommonNodes, StringBuffer label) {
        for (TreeNode ref : refs) {
            //if one ref is unnumbered then how can second be numbered as we are allowing only siblings
            if (!NumFormatter.anyUnnumberedParent(ref)) {
                return false;//break and let other rules handle 
            }
        }

        Deque<String> sb = new ArrayDeque<>();
        //collect all the ref anchors
        for (int i = 0; i < refs.size(); i++) {
            if (i != 0 && i == refs.size() - 1) {
                sb.push(" and ");
            } else if (i > 0) {
                sb.push(", ");
            }
            sb.push(createAnchor(refs.get(i)));

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
        TreeNode ref = refs.get(0);
        //rest of nodes are supposed to be in one tree :))
        while ((ref.getParent() != null) && (!ARTICLE.equals(ref.getType()))
                // "Article" reference should not be displayed when we referred sth in the same article
                && (!mrefCommonNodes.contains(ref.getParent()) || !ARTICLE.equals(ref.getParent().getType()))) {
            ref = ref.getParent();
            boolean inCurrentArticle = (mrefCommonNodes.contains(ref.getParent()) && ref.getParent().getType().equals(ARTICLE));
            
            if (mrefCommonNodes.contains(ref) && thisTypes.contains(ref.getType())) {
                processOtherNodesLabel(sb, ref, true, inCurrentArticle);
            }
            else {
                processOtherNodesLabel(sb, ref, false, inCurrentArticle);
            }
        }

        while (sb.size() > 0) {
            label.append(sb.removeLast());
        }

        return true;
    }

    private void processOtherNodesLabel(Deque<String> sb, TreeNode ref, boolean isCurrent, boolean inCurrentArticle) {
        if (NumFormatter.isUnnumbered(ref)) {
            sb.push(String.format(" of the %s %s", isCurrent? CURRENT : NumFormatter.formatUnnumbered(ref), ref.getType()));
        } else {
            String oldnum = "";
            if (!ref.getChildren().get(0).getType().equals(ref.getType()) && !isCurrent) {
                resolveType(sb, ref, inCurrentArticle);
            } else {
                oldnum = sb.removeFirst();
            }
            if (!isCurrent) {
                if(ref.getType().equals(ARTICLE) && !NumFormatter.isUnnumbered(ref.getChildren().get(0))) {
                    sb.push(NumFormatter.formattedNum(ref) + NumFormatter.formattedNum(ref.getChildren().get(0)));
                } else if(inCurrentArticle || !ref.getType().equals(PARAGRAPH)) {
                    sb.push(NumFormatter.formattedNum(ref) + oldnum);
                }
            }
            else {
                sb.push(oldnum);
            }
        }
    }

    private void resolveType(Deque<String> sb, TreeNode ref, boolean inCurrentArticle) {
        String format = null;
        switch(ref.getType()) {
            case ARTICLE:
                format = String.format(" of %s ", StringUtils.capitalize(ref.getType()));
                break;
            case PARAGRAPH:
                format = inCurrentArticle ? String.format(" of %s ", ref.getType()) : "";
                break;
            default:
                format = String.format(" of %s ", ref.getType());    
        }
        sb.push(format);
    }
    
    @Override
    public int getOrder() {
        return 10;
    }
}
