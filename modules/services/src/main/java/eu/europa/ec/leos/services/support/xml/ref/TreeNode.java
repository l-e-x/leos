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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.INDENT;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.SUBPOINT;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.SUBPOINT_LABEL;

//TreeNode representation to be used for reference tree
public class TreeNode {
    private String type;        //tag name to used as type of tag
    private int depth;          //depth in xml tree
    private int siblingNumber;  //children order in xml
    private String identifier;  //xml Id of the node
    private String refId;       //xml Id of the node
    private String num;         //num associated with TreeNode in XML
    private int vtdIndex;       //vtd index of node
    private TreeNode parent;    //parent in TreeNode
    private String documentRef;
    private List<TreeNode> children = new ArrayList<>();

    //Children of this node in tree(only refs)
    public TreeNode(String type, int depth, int siblingNumber, String identifier, String num, int vtdIndex, TreeNode parent, String documentRef) {
        this.depth = depth;
        this.siblingNumber = siblingNumber;
        this.identifier = identifier;
        this.num = num;
        this.type = getDecoratedType(type, num);
        this.vtdIndex = vtdIndex;
        this.parent = parent;
        this.documentRef = documentRef;
    }

    public String getType() {
        return type;
    }

    public int getDepth() {
        return depth;
    }

    public int getSiblingNumber() {
        return siblingNumber;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getNum() {
        return num;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public int getVtdIndex() {
        return vtdIndex;
    }

    public TreeNode getParent() {
        return parent;
    }

    public List<TreeNode> getChildren() {
        return children;//Intentionally returning the ref
    }

    public void setParent(TreeNode parent) {
        this.parent = parent;
    }

    public void addChildren(TreeNode child) {
        this.children.add(child);
    }

    public String getDocumentRef() {
        return documentRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TreeNode)) return false;
        TreeNode node = (TreeNode) o;
        return vtdIndex == node.vtdIndex &&
                Objects.equals(type, node.type) &&
                Objects.equals(identifier, node.identifier) &&
                Objects.equals(num, node.num) &&
                Objects.equals(documentRef, node.documentRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, identifier, num, vtdIndex, documentRef);
    }

    private String getDecoratedType(String type, String num) {
        if("-".equals(num)) {
            type = INDENT;
        } else if(type.equals(SUBPOINT)) {
            type = SUBPOINT_LABEL;
        }
        return type;
    }
}