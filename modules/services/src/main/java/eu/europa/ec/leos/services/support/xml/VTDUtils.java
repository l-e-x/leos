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
package eu.europa.ec.leos.services.support.xml;

import com.ximpleware.AutoPilot;
import com.ximpleware.ModifyException;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.PilotException;
import com.ximpleware.TranscodeException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XMLModifier;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.model.action.SoftActionType;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.support.ByteArrayBuilder;
import eu.europa.ec.leos.vo.toc.TocItem;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.ARTICLE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.CLAUSE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.HEADING;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.LIST;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.META;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.NUM;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.PARAGRAPH;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.XML_DOC_TYPE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.XML_NAME;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.XML_SHOW_AS;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.XML_TLC_REFERENCE;

public class VTDUtils {

    private static final Logger LOG = LoggerFactory.getLogger(VTDUtils.class);
    
    public static final String XMLID = "xml:id";
    public static final Charset UTF_8 = Charset.forName("UTF-8");
    static final String WHITESPACE = " ";
    static final byte[] HEADING_BYTES = HEADING.getBytes(UTF_8);
    static final byte[] HEADING_START_TAG = "<".concat(HEADING).concat(">").getBytes(UTF_8);
    static final byte[] NUM_BYTES = NUM.getBytes(UTF_8);
    static final byte[] NUM_START_TAG = "<".concat(NUM).concat(">").getBytes(UTF_8);

    public static final String LEOS_ORIGIN_ATTR = "leos:origin";
    public static final String LEOS_DELETABLE_ATTR = "leos:deletable";
    public static final String LEOS_EDITABLE_ATTR = "leos:editable";
    public static final String LEOS_AFFECTED_ATTR = "leos:affected";
    public static final String LEOS_REF_BROKEN_ATTR = "leos:broken";
    public static final String LEOS_DEPTH_ATTR = "leos:depth";
    
    public static final String LEOS_SOFT_ACTION_ATTR = "leos:softaction";
    public static final String LEOS_SOFT_ACTION_ROOT_ATTR = "leos:softactionroot";

    public static final String LEOS_SOFT_USER_ATTR = "leos:softuser";
    public static final String LEOS_SOFT_DATE_ATTR = "leos:softdate";
    public static final String LEOS_SOFT_MOVE_TO = "leos:softmove_to";
    public static final String LEOS_SOFT_MOVE_FROM = "leos:softmove_from";
    public static final String LEOS_SOFT_TRANS_FROM = "leos:softtrans_from";
    public static final String SOFT_MOVE_PLACEHOLDER_ID_PREFIX = "moved_";
    public static final String SOFT_DELETE_PLACEHOLDER_ID_PREFIX = "deleted_";
    public static final String SOFT_TRANSFORM_PLACEHOLDER_ID_PREFIX = "transformed_";
    public static final String TOGGLED_TO_NUM = "toggled_to_num";

    public static final String LEOS_SOFT_MOVED_LABEL_ATTR = "leos:softmove_label";

    public static final String EMPTY_STRING = "";
    public static final String NON_BREAKING_SPACE = "\u00A0";

    static VTDNav setupVTDNav(byte[] xmlContent) {
        return setupVTDNav(xmlContent, true);
    }

    static VTDNav setupVTDNav(byte[] xmlContent, boolean namespaceEnabled) {
        VTDNav vtdNav = null;
        try {
            VTDGen vtdGen = new VTDGen();
            vtdGen.setDoc(xmlContent);
            vtdGen.parse(namespaceEnabled);
            vtdNav = vtdGen.getNav();
        } catch (ParseException parseException) {
            throw new RuntimeException("Exception occured while Vtd generator parsing", parseException);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred during setup of VTDNav", e);
        }
        return vtdNav;
    }

    static byte[] buildNumElement(VTDNav vtdNav, XMLModifier xmlModifier, byte[] numBytes) throws NavException, ModifyException {
        byte[] element;

        if (vtdNav.toElement(VTDNav.FIRST_CHILD, NUM)) {
            byte[] numTag = getStartTag(vtdNav);
            element = XmlHelper.buildTag(numTag, NUM_BYTES, numBytes);
            xmlModifier.remove();
        } else {
            // build num if not exists
            element = XmlHelper.buildTag(NUM_START_TAG, NUM_BYTES, numBytes);
        }
        return element;
    }

    static XMLModifier setupXMLModifier(VTDNav vtdNav, String tagName, String idAttributeValue) {

        XMLModifier xmlModifier;
        try {
            xmlModifier = new XMLModifier();
            xmlModifier.bind(vtdNav);
            AutoPilot autoPilot = new AutoPilot(vtdNav);

            autoPilot.selectElement(tagName);
            while (autoPilot.iterate()) {
                int attIndex = vtdNav.getAttrVal(XMLID);
                String elementId;
                if (attIndex != -1) {
                    elementId = vtdNav.toString(attIndex);
                    if (idAttributeValue.equals(elementId)) {
                        return xmlModifier;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred during setup of XML Modifier", e);
        }
        return null; // Element not found
    }

    static byte[] toByteArray(XMLModifier xmlModifier) throws ModifyException, TranscodeException, IOException {
        // get the updated XML content
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xmlModifier.output(baos);
        return baos.toByteArray();
    }

    static byte[] getStartTag(VTDNav vtdNav) throws NavException {
        long token = vtdNav.getElementFragment();
        int offsetContent = (int) vtdNav.getContentFragment();
        int offset = (int) token;
        int tagLength = offsetContent != -1 ? (offsetContent - offset) : (int) (token >> 32);
        return vtdNav.getXML().getBytes(offset, tagLength);
    }

    static Map<String, String> getElementAttributes(VTDNav vtdNav, AutoPilot ap) {
        Map<String, String> attrs = new HashMap<>();
        int i;
        try {
            ap.selectAttr("*");
            while ((i = ap.iterateAttr()) != -1) {
                attrs.put(vtdNav.toString(i), vtdNav.toString(i + 1));
            }
        } catch (Exception e) {
            LOG.error("Unexpected error occured while getting elements", e);
        }
        return attrs;
    }
    
    static String getFragmentAsString(VTDNav contentNavigator, long fragmentLocation, boolean removeTags) {
        String fragmentContent = null;
        if (fragmentLocation > -1) {
            int offSet = (int) fragmentLocation;
            int length = (int) (fragmentLocation >> 32);
            byte[] elementContent = contentNavigator.getXML().getBytes(offSet, length);
            fragmentContent = new String(elementContent, UTF_8);

            if (removeTags) {
                // remove all tags and replace multiple space occurrences with a single space
                fragmentContent = fragmentContent.replaceAll("<[^>]+>", EMPTY_STRING);
                fragmentContent = fragmentContent.replaceAll("\\s+", " ").trim();
            }
        }
        return fragmentContent;
    }

    static String extractNumber(String numberStr) {
        if(numberStr!= null) {
            return (numberStr.contains(WHITESPACE)) ?
                    numberStr.substring(numberStr.indexOf(WHITESPACE) + 1, numberStr.length()) : numberStr;
        }
        return null;
    }
    
    static boolean navigateToElementByNameAndId(String tagName, String idAttributeValue, VTDNav vtdNav) throws NavException {
        AutoPilot autoPilot = new AutoPilot(vtdNav);

        autoPilot.selectElement(tagName);
        while (autoPilot.iterate()) {
            if (idAttributeValue == null) {
                return true;
            }
            int attIndex = vtdNav.getAttrVal(XMLID);
            String elementId;
            if (attIndex != -1) {
                elementId = vtdNav.toString(attIndex);
                if (idAttributeValue.equals(elementId)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    static byte[] setAttribute(byte[] xmlContent, String parentTag, List<String> elementTags, String leosAttribute, String value) throws Exception {
        VTDNav vtdNav = setupVTDNav(xmlContent);
        AutoPilot autoPilot = new AutoPilot(vtdNav);
        autoPilot.selectElement(parentTag);
        XMLModifier xmlModifier = new XMLModifier(vtdNav);
        while (autoPilot.iterate()) {
            int currentIndex = vtdNav.getCurrentIndex();
            if (vtdNav.toElement(VTDNav.FIRST_CHILD)) {
                setAttribute(vtdNav, xmlModifier, elementTags, leosAttribute, value);
                while (vtdNav.toElement(VTDNav.NEXT_SIBLING)) {
                    setAttribute(vtdNav, xmlModifier, elementTags, leosAttribute, value);
                }
            }
            vtdNav.recoverNode(currentIndex);
        }
        return toByteArray(xmlModifier);
    }

    static void setAttribute(VTDNav vtdNav, XMLModifier xmlModifier, List<String> elementTags, String leosAttribute, String value) throws Exception {
        String tagName = vtdNav.toString(vtdNav.getCurrentIndex());
        if (tagName.equals(META)) {
            return;
        }
        
        int currentIndex = vtdNav.getCurrentIndex();
        if (elementTags.contains(tagName) || elementTags.isEmpty()) {
            int attIndex = vtdNav.getAttrVal(leosAttribute);
            if (attIndex == -1) {
                xmlModifier.insertAttribute(" ".concat(leosAttribute).concat("=\"").concat(value).concat("\""));
            }
            else {
                xmlModifier.updateToken(attIndex, String.valueOf(value));
            }
        }

        if (vtdNav.toElement(VTDNav.FIRST_CHILD)) {
            setAttribute(vtdNav, xmlModifier, elementTags, leosAttribute, value);
            while (vtdNav.toElement(VTDNav.NEXT_SIBLING)) {
                setAttribute(vtdNav, xmlModifier, elementTags, leosAttribute, value);
            }
        }
        vtdNav.recoverNode(currentIndex);
    }

    public static byte[] updateOriginAttribute(byte[] tag, String value) {
        StringBuilder tagStr = new StringBuilder(new String(tag, UTF_8));
        int origAttrPos = tagStr.indexOf(LEOS_ORIGIN_ATTR);
        if(origAttrPos != -1) {
            int origAttrValPos = tagStr.indexOf("=", origAttrPos) + 2; //pos + 2 to get the next index after ' or "  
            tagStr.replace(origAttrValPos, origAttrValPos + value.length(), value);
        } else {
            int position = tagStr.indexOf(">");
            tagStr.insert(position, insertAttribute(LEOS_ORIGIN_ATTR, value));
        }
        return tagStr.toString().getBytes(UTF_8);
    }

    public static byte[] updateOriginAttribute(byte[] tag, String value, List<String> elementTags) {
        StringBuilder tagStr = new StringBuilder(new String(tag, UTF_8));
        elementTags.forEach(elementTag -> {
            int startIndex = tagStr.indexOf("<" + elementTag);
            int endIndex = startIndex + tagStr.substring(startIndex).indexOf(">") + 1;
            String tagElementStr = tagStr.substring(startIndex, endIndex);
            byte[] modifiedElementTag = updateOriginAttribute(tagElementStr.getBytes(UTF_8), value);
            tagStr.replace(startIndex, endIndex, new String(modifiedElementTag, UTF_8));
        });
        return tagStr.toString().getBytes(UTF_8);
    }

    public static byte[] updateXMLIDAttribute(byte[] tag, String newValue) {
        StringBuilder tagStr = new StringBuilder(new String(tag, UTF_8));
        int origAttrPos = tagStr.indexOf(XMLID);
        if(origAttrPos != -1) {
            int origAttrValPos = tagStr.indexOf("=", origAttrPos) + 2; //pos + 2 to get the next index after ' or "  
            int origAttrValEndPos1 = tagStr.indexOf("\"", origAttrValPos);
            int origAttrValEndPos2 = tagStr.indexOf("'", origAttrValPos);
            int origAttrValEndPos =  (origAttrValEndPos1 == -1 || (origAttrValEndPos2 != -1) && (origAttrValEndPos2 < origAttrValEndPos1)) ? 
                    origAttrValEndPos2 : origAttrValEndPos1;
            tagStr.replace(origAttrValPos, origAttrValEndPos, newValue);
        } else {
            int position = tagStr.indexOf(">");
            tagStr.insert(position, insertAttribute(XMLID, newValue));
        }
        return tagStr.toString().getBytes(UTF_8);
    }

    public static byte[] updateXMLIDAttributesInElementContent(byte[] content, String newValuePrefix, boolean replacePrefix) {
        return updateXMLIDAttributesInElementContent(new ByteArrayBuilder(content), newValuePrefix,replacePrefix).getContent();
    }
    
    public static ByteArrayBuilder updateXMLIDAttributesInElementContent(ByteArrayBuilder content, String newValuePrefix, boolean replacePrefix) {
        StringBuilder contentStr = new StringBuilder(new String(content.getContent(), UTF_8));
        int attrPos = contentStr.indexOf(XMLID);
        while (attrPos != -1) {
            int attrValStartPos = contentStr.indexOf("=", attrPos) + 2; //pos + 2 to get the next index after ' or "
            int attrValEndPos1 = contentStr.indexOf("\"", attrValStartPos);
            int attrValEndPos2 = contentStr.indexOf("'", attrValStartPos);
            int attrValEndPos =  (attrValEndPos1 == -1 || (attrValEndPos2 != -1) && (attrValEndPos2 < attrValEndPos1)) ? 
                    attrValEndPos2 : attrValEndPos1;
            String currentValue = contentStr.substring(attrValStartPos, attrValEndPos);
            if (!currentValue.contains(SOFT_MOVE_PLACEHOLDER_ID_PREFIX) && !currentValue.contains(SOFT_DELETE_PLACEHOLDER_ID_PREFIX)) {
                contentStr.replace(attrValStartPos, attrValStartPos + currentValue.length(), newValuePrefix + currentValue);
            } else if (replacePrefix && currentValue.contains(SOFT_MOVE_PLACEHOLDER_ID_PREFIX)) {
                contentStr.replace(attrValStartPos, attrValStartPos + currentValue.length(), currentValue.replace(SOFT_MOVE_PLACEHOLDER_ID_PREFIX, newValuePrefix));
            } else if (replacePrefix && currentValue.contains(SOFT_DELETE_PLACEHOLDER_ID_PREFIX)) {
                contentStr.replace(attrValStartPos, attrValStartPos + currentValue.length(), currentValue.replace(SOFT_DELETE_PLACEHOLDER_ID_PREFIX, newValuePrefix));
            }

            attrPos = contentStr.indexOf(XMLID, attrValStartPos);
        }
        return new ByteArrayBuilder(contentStr.toString().getBytes(UTF_8));
    }

    public static byte[] insertAffectedAttribute(byte[] tag,boolean isAffected) {
        StringBuilder tagStr = new StringBuilder(new String(tag, UTF_8));
        int affectedAttrPos = tagStr.indexOf(LEOS_AFFECTED_ATTR);
        if(isAffected && affectedAttrPos == -1) {
            int position = tagStr.indexOf(">");
            tagStr.insert(position, insertAttribute(LEOS_AFFECTED_ATTR, "true"));
        }
        return tagStr.toString().getBytes(UTF_8);
    }

    public static String getOriginAttribute(VTDNav contentNavigator) throws NavException {
        int originAttrIndex = contentNavigator.getAttrVal(LEOS_ORIGIN_ATTR);
        return originAttrIndex != -1 ? contentNavigator.toString(originAttrIndex) : null;
    }

    public static String getNumSoftActionAttribute(VTDNav contentNavigator) throws NavException {
        int originAttrIndex = contentNavigator.getAttrVal(LEOS_SOFT_ACTION_ATTR);
        return originAttrIndex != -1 ? contentNavigator.toString(originAttrIndex) : null;
    }

    
    public static byte[] updateSoftInfo(byte[] tag, SoftActionType action, Boolean isSoftActionRoot, User user, String originAttrValue, String moveId, boolean isUndeleted, TocItem tocItem) {
        if (originAttrValue == null) {
            return tag;
        }
        StringBuilder tagStr = new StringBuilder(new String(tag, UTF_8));
        if (SoftActionType.DELETE.equals(action) || SoftActionType.MOVE_TO.equals(action)) {
            insertOrUpdateAttributeValue(tagStr, LEOS_EDITABLE_ATTR, Boolean.FALSE.toString());
            insertOrUpdateAttributeValue(tagStr, LEOS_DELETABLE_ATTR, Boolean.FALSE.toString());
        }
    
        boolean updatedSoftAction = updateSoftAction(tagStr, action, isUndeleted);
    
        insertOrUpdateAttributeValue(tagStr, LEOS_SOFT_ACTION_ROOT_ATTR, isSoftActionRoot);
    
        if (isUndeleted) {
            restoreOldId(tagStr);
            if (tocItem.getAknTag().value() != ARTICLE) {
                insertOrUpdateAttributeValue(tagStr, LEOS_EDITABLE_ATTR, null);
                insertOrUpdateAttributeValue(tagStr, LEOS_DELETABLE_ATTR, null);
            }
            
            if(tocItem.getAknTag().value() == CLAUSE){
                insertOrUpdateAttributeValue(tagStr, LEOS_EDITABLE_ATTR, true);
                insertOrUpdateAttributeValue(tagStr, LEOS_DELETABLE_ATTR, true);
            }
        }
        
        if (updatedSoftAction) {
            if (SoftActionType.MOVE_TO.equals(action)) {
                insertOrUpdateAttributeValue(tagStr, LEOS_SOFT_MOVE_TO, moveId);
                insertOrUpdateAttributeValue(tagStr, LEOS_SOFT_MOVE_FROM, null);
            } else if (SoftActionType.MOVE_FROM.equals(action)) {
                insertOrUpdateAttributeValue(tagStr, LEOS_SOFT_MOVE_FROM, moveId);
                insertOrUpdateAttributeValue(tagStr, LEOS_SOFT_MOVE_TO, null);
            } else {
                insertOrUpdateAttributeValue(tagStr, LEOS_SOFT_MOVED_LABEL_ATTR, null);
                insertOrUpdateAttributeValue(tagStr, LEOS_SOFT_MOVE_FROM, null);
                insertOrUpdateAttributeValue(tagStr, LEOS_SOFT_MOVE_TO, null);
            }
            
            if (action != null) {
                insertOrUpdateAttributeValue(tagStr, LEOS_SOFT_USER_ATTR, user != null ? user.getLogin() : null);
                try {
                    String softActionDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()).toXMLFormat();
                    insertOrUpdateAttributeValue(tagStr, LEOS_SOFT_DATE_ATTR, softActionDate);
                } catch (DatatypeConfigurationException e) {
                    insertOrUpdateAttributeValue(tagStr, LEOS_SOFT_DATE_ATTR, null);
                }
            } else {
                insertOrUpdateAttributeValue(tagStr, LEOS_SOFT_DATE_ATTR, null);
                insertOrUpdateAttributeValue(tagStr, LEOS_SOFT_USER_ATTR, null);
            }
        }
        
        return tagStr.toString().getBytes(UTF_8);
    }
    
    protected static StringBuilder restoreOldId(StringBuilder tagStr) {
        int idAttrPos = tagStr.indexOf(XMLID+"=\"deleted_");
        if (idAttrPos != -1) {
            int idValStartPos = tagStr.indexOf("=", idAttrPos) + 2;
            int idValEndPos = tagStr.indexOf("\"", idValStartPos);
            String currentIdVal = tagStr.substring(idValStartPos, idValEndPos);
            String restoredIdVal = currentIdVal.replace("deleted_", EMPTY_STRING);
            tagStr.replace(idValStartPos, idValStartPos + currentIdVal.length(), restoredIdVal);
        }
        return tagStr;
    }
    
    private static boolean updateSoftAction(StringBuilder tagStr, SoftActionType action, boolean isUndeleted) {
        Boolean changeSoftAction = Boolean.TRUE;
        int softAttrPos = tagStr.indexOf(LEOS_SOFT_ACTION_ATTR);
        if(softAttrPos != -1) {
            int softAttrValStartPos = tagStr.indexOf("=", softAttrPos) + 2;
            int softAttrValEndPos = tagStr.indexOf("\"", softAttrValStartPos);
            String currentSoftAttrVal = tagStr.substring(softAttrValStartPos, softAttrValEndPos);
            
            // change actual softInfo only if: is not DEL, nor MOVE_TO, not selected, OR if is UNDelete operation
            changeSoftAction = (!SoftActionType.DELETE.getSoftAction().equals(currentSoftAttrVal)
                                && !SoftActionType.MOVE_TO.getSoftAction().equals(currentSoftAttrVal)
                                &&  !(action != null && action.getSoftAction().equals(currentSoftAttrVal)))
                                || isUndeleted;

            if (changeSoftAction) {
                if (action != null) {
                    tagStr.replace(softAttrValStartPos, softAttrValEndPos,  action.getSoftAction());
                } else {
                    tagStr.replace(softAttrPos, softAttrValEndPos+1, EMPTY_STRING);
                }
            }
        }
        else {
            int position = tagStr.indexOf(">");
            if (action != null && position != -1) {
                tagStr.insert(position, insertAttribute(LEOS_SOFT_ACTION_ATTR, action.getSoftAction()));
            }
        }

        return changeSoftAction;
    }
    
    public static SoftActionType getSoftActionAttribute(VTDNav contentNavigator) throws NavException {
        int softActionAttrIndex = contentNavigator.getAttrVal(LEOS_SOFT_ACTION_ATTR);
        String softActionAttr = softActionAttrIndex != -1 ? contentNavigator.toString(softActionAttrIndex) : null;
        return !StringUtils.isEmpty(softActionAttr) ? SoftActionType.of(softActionAttr) : null;
    }

    public static Boolean getSoftActionRootAttribute(VTDNav contentNavigator) throws NavException {
        int softUserAttrIndex = contentNavigator.getAttrVal(LEOS_SOFT_ACTION_ROOT_ATTR);
        return softUserAttrIndex != -1 ? Boolean.valueOf(contentNavigator.toString(softUserAttrIndex)) : null;
    }

    public static String getSoftUserAttribute(VTDNav contentNavigator) throws NavException {
        int softUserAttrIndex = contentNavigator.getAttrVal(LEOS_SOFT_USER_ATTR);
        return softUserAttrIndex != -1 ? contentNavigator.toString(softUserAttrIndex) : null;
    }

    public static String getSoftMovedFromAttribute(VTDNav contentNavigator) throws NavException {
        int softUserAttrIndex = contentNavigator.getAttrVal(LEOS_SOFT_MOVE_FROM);
        return softUserAttrIndex != -1 ? contentNavigator.toString(softUserAttrIndex) : null;
    }

    public static String getSoftMovedToAttribute(VTDNav contentNavigator) throws NavException {
        int softUserAttrIndex = contentNavigator.getAttrVal(LEOS_SOFT_MOVE_TO);
        return softUserAttrIndex != -1 ? contentNavigator.toString(softUserAttrIndex) : null;
    }

    public static GregorianCalendar getSoftDateAttribute(VTDNav contentNavigator) throws NavException {
        int softDateAttrIndex = contentNavigator.getAttrVal(LEOS_SOFT_DATE_ATTR);
        String softDateAttr = softDateAttrIndex != -1 ? contentNavigator.toString(softDateAttrIndex) : null;
        return convertStringDateToCalendar(softDateAttr);
    }

    public static byte[] updateSoftTransFromAttribute(byte[] tag, String newValue) {
        StringBuilder tagStr = new StringBuilder(new String(tag, UTF_8));
        insertOrUpdateAttributeValue(tagStr, LEOS_SOFT_TRANS_FROM, newValue);
        return tagStr.toString().getBytes(UTF_8);
    }

    private static GregorianCalendar convertStringDateToCalendar(String strDate) {
        try {
            GregorianCalendar gregorianCalendar =(GregorianCalendar) GregorianCalendar.getInstance();
            gregorianCalendar.setTime( new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(strDate));
            return gregorianCalendar;
        } catch (Exception ex) {
            return null;
        }
    }

    public static String insertAttribute(String attrTag, Object attrVal) {
        return attrVal != null ? (" ").concat(attrTag).concat("=\"").concat(attrVal.toString()).concat("\"") : EMPTY_STRING;
    }

    public static StringBuilder insertOrUpdateAttributeValue(StringBuilder tagStr, String attrName, Object attrValue) {
        if (tagStr != null && attrName != null) {
            int attrPos = tagStr.indexOf(attrName);
            if (attrPos != -1) {
                int attrValStartPos = tagStr.indexOf("=", attrPos) + 2;
                int attrValEndPos = tagStr.indexOf("\"", attrValStartPos);
                if (attrValue != null) {
                    tagStr.replace(attrValStartPos, attrValEndPos, attrValue.toString());
                } else {
                    tagStr.replace(attrPos, attrValEndPos+1, EMPTY_STRING);
                }
            } else {
                int position = tagStr.indexOf(">");
                if (position >= 0) {
                    tagStr.insert(position, insertAttribute(attrName, attrValue));
                }
            }
        }
        return tagStr;
    }

    public static StringBuilder removeAttribute(StringBuilder tagStr, String leosAttr) {
        if (tagStr != null && leosAttr != null) {
            int editableAttrPos = tagStr.indexOf(leosAttr);
            if (editableAttrPos != -1) {
                int editableAttrValStartPos = tagStr.indexOf("=", editableAttrPos) + 2;
                int editableAttrValEndPos = tagStr.indexOf("\"", editableAttrValStartPos) + 1;
                tagStr.delete(editableAttrPos - 1, editableAttrValEndPos);
            }
        }
        return tagStr;
    }

    public static void removeAttribute(VTDNav vtdNav, XMLModifier xmlModifier, String leosAttr) throws PilotException, NavException, ModifyException {
        int currentIndex = vtdNav.getCurrentIndex();
        if (vtdNav.getAttrVal(leosAttr) != -1) {
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.selectAttr(leosAttr);
            xmlModifier.removeAttribute(autoPilot.iterateAttr());
        }
        vtdNav.recoverNode(currentIndex);
    }

    public static boolean toBeSkippedForNumbering(VTDNav vtdNav) {

        try {
           int attIndex = vtdNav.getAttrVal(LEOS_SOFT_ACTION_ATTR);
           if (attIndex != -1) {
                String elementActionType = vtdNav.toString(attIndex);
                if (elementActionType.equals(SoftActionType.MOVE_TO.getSoftAction()) || elementActionType.equals(SoftActionType.DELETE.getSoftAction())) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static int getPointDepth(VTDNav vtdNav, int pointDepth) throws NavException {
        int currentIndex = vtdNav.getCurrentIndex();
        while (true) {
            if (vtdNav.toElement(VTDNav.PARENT)) {
                if (vtdNav.matchElement(LIST)) {
                    pointDepth++;
                } else if (vtdNav.matchElement(PARAGRAPH)) {
                    break;
                }
            } else {
                break;
            }
        }
        vtdNav.recoverNode(currentIndex);
        return pointDepth;
    }
    
    public static String getDocType(byte[] xmlContent) {
        String docType = null;
        try {
            VTDNav vtdNav = VTDUtils.setupVTDNav(xmlContent);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.selectElement(XML_TLC_REFERENCE);
            while (autoPilot.iterate()) {
                int attIndex = vtdNav.getAttrVal(XML_NAME);
                if (attIndex != -1) {
                    String name = vtdNav.toString(attIndex);
                    if (XML_DOC_TYPE.equals(name)) {
                        attIndex = vtdNav.getAttrVal(XML_SHOW_AS);
                        if (attIndex != -1) {
                            docType = vtdNav.toString(attIndex);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Unexpected error occurred while calculating getDocType {}", e.getMessage());
            throw new IllegalStateException("Unexpected error occurred while calculating getDocType", e);
        }
        return docType;
    }
}
