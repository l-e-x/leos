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

import com.google.common.base.Stopwatch;
import com.ximpleware.*;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TreeHelper {
    private static final Logger LOG = LoggerFactory.getLogger(TreeHelper.class);

    private static List<String> NOT_SIGNIFICANT_NODES = Arrays.asList("bill", "content", "list", "preface", "preamble", "body", "mainBody", "recitals");

    public static TreeNode createTree(VTDNav vtdNav, TreeNode root, List<Ref> refs) throws Exception {
        Validate.isTrue(refs != null && !refs.isEmpty(), "refs can not be empty");
        Stopwatch watch = Stopwatch.createStarted();

        for (Ref ref : refs) {
            AutoPilot ap = new AutoPilot(vtdNav);
            try {
                ap.selectXPath(String.format("//*[@%s = '%s']", "GUID", ref.getHref()));
                if (ap.evalXPath() == -1) {
                    //probably it is broken reference
                    LOG.debug("Element with id: {} does not exists. Skipping", ref.getHref());
                    continue;
                }

                // This block finds all ancestors till some already exists in tree
                TreeNode parent = null;
                String tagName;
                do {
                    tagName = vtdNav.toString(vtdNav.getCurrentIndex());
                    if (NOT_SIGNIFICANT_NODES.contains(tagName)) {
                        continue;
                    }
                    //find if any ancestor of current node exists in tree. If it does, break and attach all children to this node in tree. 
                    else if ((parent = find(root, TreeNode::getVtdIndex, vtdNav.getCurrentIndex())) != null) {
                        break;
                    }
                    vtdNav.push();
                }
                while (vtdNav.toElement(VTDNav.PARENT) && !"akomaNtoso".equals(tagName));

                //this block creates node subtree and attaches them in tree 
                int depth;
                while (vtdNav.pop()) {
                    depth = (parent == null) ? 0 : parent.getDepth() + 1;
                    TreeNode currentNode = createNode(vtdNav, parent, depth);

                    //if tree root is not assigned
                    if (root == null) {
                        root = currentNode;
                    }

                    //if this is leaf, set ref
                    if (currentNode.getIdentifier() != null && currentNode.getIdentifier().equals(ref.getHref())) {
                        currentNode.setRefId(ref.getId());
                    }
                    //add at appropriate place
                    if (parent != null) {
                        parent.addChildren(currentNode);
                        parent.getChildren().sort(Comparator.comparingInt(TreeNode::getSiblingNumber));
                    }

                    parent = currentNode;
                }
            } catch (Exception e) {
                LOG.error(String.format("Not able to retrieve ancestors ids for given element id: %s", ref.getHref()), e);
                throw e;
            }
        }

        LOG.trace("Retrieved ancestors ids in {} ms", watch.elapsed(TimeUnit.MILLISECONDS));
        return root;
    }

    static TreeNode find(TreeNode start, Function<TreeNode, Object> condition, Object value) {
        if (start == null) {
            return null;
        }
        Object nodeValue = condition.apply(start);
        if ((nodeValue == null && value == null) ||
                (nodeValue != null && nodeValue.equals(value))) {
            return start;
        }
        for (TreeNode child : start.getChildren()) {
            TreeNode result = find(child, condition, value);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public static List<TreeNode> getLeaves(TreeNode start) {
        List<TreeNode> leaves = new ArrayList<>();
        if (start == null) {
            return leaves;
        }
        if (start.getChildren().isEmpty()) {
            leaves.add(start);
        } else {
            for (TreeNode child : start.getChildren()) {
                leaves.addAll(getLeaves(child));
            }
        }
        return leaves;
    }

    private static final Pattern idPattern = Pattern.compile("\\s(GUID)(\\s)*=(\\s)*\"(.+?)\"");

    static TreeNode createNode(VTDNav vtdNav, TreeNode parent, int depth) throws Exception {
        try {
            int currentIndex = vtdNav.getCurrentIndex();
            int tokenType = vtdNav.getTokenType(currentIndex);

            String tagContent = getTagContent(vtdNav);
            String tagName = null;
            String tagId = null;
            if (tokenType == VTDNav.TOKEN_STARTING_TAG) {
                tagName = vtdNav.toString(currentIndex);
                Matcher idMatcher = idPattern.matcher(tagContent);
                tagId = idMatcher.find() ? idMatcher.group(4) : null;
            } else if (tokenType == VTDNav.TOKEN_CHARACTER_DATA) {//if textNode
                tagName = "text";
            }

            //find num 
            String numValue = findNum(vtdNav, tagName);
            int childSeq = findSeq(vtdNav, tagName);
            return new TreeNode(tagName, depth, childSeq, tagId, numValue, vtdNav.getCurrentIndex(), parent);
        } catch (Exception ex) {
            LOG.error("Unexpected error. Consuming and continuing!!", ex);
            throw ex;
        }
    }

    public static List<TreeNode> findCommonAncestor(VTDNav vtdNav, String mrefLocationId, TreeNode root) {
        List<TreeNode> nodes = new ArrayList<TreeNode>();
        
        List<String> ancestorsIdsOfMrefParent;
        try {
            ancestorsIdsOfMrefParent = getAncestorsIdsForElementId(vtdNav, mrefLocationId);
        } catch (NavException e) {
            return null;
        }
        if (ancestorsIdsOfMrefParent == null || ancestorsIdsOfMrefParent.isEmpty()) {
            return nodes;
        }

        for(String ancestorIdOfMrefParent: ancestorsIdsOfMrefParent) {
            TreeNode node = find(root, TreeNode::getIdentifier, ancestorIdOfMrefParent);
            if (node != null) {
                nodes.add(node);
            }
        }

        return nodes;
    }

    private static List<String> getAncestorsIdsForElementId(VTDNav vtdNav, String idAttributeValue) throws NavException {
        LinkedList<String> ancestorsIds = new LinkedList<String>();
        if (idAttributeValue == null) {
            return ancestorsIds;
        }

        int currentIndex = vtdNav.getCurrentIndex();
        try {
            AutoPilot ap = new AutoPilot(vtdNav);
            ap.selectXPath("//*[@GUID = '" + idAttributeValue + "']");
            if (ap.evalXPath() != -1) {
                ancestorsIds.add(idAttributeValue);
                /* Skip current element */
                if (!vtdNav.toElement(VTDNav.PARENT)) {
                    return ancestorsIds;
                }
                do {
                    ap.selectAttr("GUID");
                    int i = -1;
                    String idValue = "";
                    if ((i = ap.iterateAttr()) != -1) {
                        // Get the value of the 'id' attribute
                        idValue = vtdNav.toRawString(i + 1);
                        ancestorsIds.addFirst(idValue);
                    }
                }
                while (vtdNav.toElement(VTDNav.PARENT));
            }
            
        } catch (Exception e) {
            LOG.warn("Error while getting ancestors ids", e);
            return null;
        } finally {
            vtdNav.recoverNode(currentIndex);
        }
        return ancestorsIds;
    }

    private static int findSeq(VTDNav vtdNav, String type) {
        vtdNav.push();
        int childSeq = 0;
        try {
            do {
                if (type == null || type.equals(vtdNav.toString(vtdNav.getCurrentIndex()))) {
                    //this considers elements of same type only.
                    childSeq++;
                }
            } while (vtdNav.toElement(VTDNav.PREV_SIBLING));
        } catch (Exception e) {
            LOG.error("Error while getting child seq content", e);
        } finally {
            vtdNav.pop();
        }
        return childSeq;
    }

    private static String findNum(VTDNav contentNavigator, String tagName) {
        contentNavigator.push();
        try {
            if (contentNavigator.toElement(VTDNav.FIRST_CHILD)) {
                do {
                    String childTag = contentNavigator.toString(contentNavigator.getCurrentIndex());
                    if ("num".equalsIgnoreCase(childTag)) {
                        //get content
                        return parseNum(getContent(contentNavigator));
                    }
                } while (contentNavigator.toElement(VTDNav.NEXT_SIBLING));
            }
        } catch (Exception e) {
            LOG.error("Error while getting num content", e);
        } finally {
            contentNavigator.pop();
        }
        return null;
    }

    private static String parseNum(String xmlNum) {
        //remove type if part/section/
        String[] s = xmlNum.split(" ", 2);
        String num = s.length > 1 ? s[1] : s[0];
        //clean spaces, .,(), etc
        return num.replaceAll("[\\s+|\\(|\\)|\\.]", "");
    }

    private static String getTagContent(VTDNav vtdNav) throws Exception {
        int offset = (int) vtdNav.getElementFragment();
        long token = (long) vtdNav.getContentFragment();
        int offsetContent = (int) token;
        String tagContent = (offsetContent > 0)
                ? new String(vtdNav.getXML().getBytes(offset, (offsetContent - offset)))
                : null;
        return tagContent;
    }

    private static String getContent(VTDNav vtdNav) throws Exception {
        long token = vtdNav.getContentFragment();
        int offsetContent = (int) token;
        int length = (int) (token >> 32);
        String tagContent = (offsetContent > 0)
                ? new String(vtdNav.getXML().getBytes(offsetContent, length), Charset.forName("UTF-8"))
                : null;
        return tagContent;
    }
}
