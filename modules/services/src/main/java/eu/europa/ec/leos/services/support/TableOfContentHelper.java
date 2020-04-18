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
package eu.europa.ec.leos.services.support;

import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.action.SoftActionType;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toc.TocItem;
import eu.europa.ec.leos.vo.toc.TocItemUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.ARTICLE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.AUTHORIAL_NOTE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.CHAPTER;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.INLINE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.PARAGRAPH;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.PART;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.SECTION;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.TBLOCK;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.TITLE;

public class TableOfContentHelper {

    public static final int DEFAULT_CAPTION_MAX_SIZE = 50;
    
    public static final List<String> ELEMENTS_WITHOUT_CONTENT = Collections.unmodifiableList(Arrays.asList(ARTICLE, SECTION, CHAPTER, TITLE, PART));
    private static final String MOVE_LABEL_SPAN_START_TAG = "<span class=\"leos-soft-move-label\">";
    private static final String MOVED_TITLE_SPAN_START_TAG = "<span class=\"leos-soft-move-title\">";
    private static final String SPAN_END_TAG = "</span>";
    private static final String SPACE = " ";
    private static final int MOVED_LABEL_SIZE = MOVED_TITLE_SPAN_START_TAG.length() + SPACE.length() + MOVE_LABEL_SPAN_START_TAG.length() + 2 * SPAN_END_TAG.length();
    private static final List<String> ELEMENTS_TO_REMOVE_FROM_CONTENT = Arrays.asList(INLINE, AUTHORIAL_NOTE);

    private static String getMovedLabel(MessageHelper messageHelper) {
        return MOVE_LABEL_SPAN_START_TAG + messageHelper.getMessage("toc.edit.window.softmove.label") + SPAN_END_TAG;
    }

    private static Boolean shouldAddMoveLabel(TableOfContentItemVO tocItem) {
        return tocItem.isSoftActionRoot() != null && tocItem.isSoftActionRoot()
                && (SoftActionType.MOVE_TO.equals(tocItem.getSoftActionAttr()) || SoftActionType.MOVE_FROM.equals(tocItem.getSoftActionAttr()));
    }

    public static String buildItemCaption(TableOfContentItemVO tocItem, int captionMaxSize, MessageHelper messageHelper) {
        Validate.notNull(tocItem.getTocItem(), "Type should not be null");

        boolean shoudlAddMovedLabel = shouldAddMoveLabel(tocItem);

        StringBuilder itemDescription = tocItem.getTocItem().isItemDescription()
                ? new StringBuilder(getDisplayableTocItem(tocItem.getTocItem(), messageHelper)).append(SPACE)
                : new StringBuilder();

        if (shoudlAddMovedLabel) {
            itemDescription.insert(0, MOVED_TITLE_SPAN_START_TAG + SPACE);
        }

        if (!StringUtils.isEmpty(tocItem.getNumber()) && !StringUtils.isEmpty(tocItem.getHeading())) {
            if(tocItem.getTocItem().getAknTag().value().equalsIgnoreCase(tocItem.getNumber().trim())){
                tocItem.setNumber("#");
            }
            itemDescription.append(tocItem.getNumber());
            if (shoudlAddMovedLabel) {
                itemDescription.append(SPAN_END_TAG).append(getMovedLabel(messageHelper));
            }
            if(TBLOCK.equals(tocItem.getTocItem().getAknTag().name())){
                itemDescription.append(TocItemUtils.CONTENT_SEPARATOR).append(tocItem.getHeading());
            }else{
                itemDescription.append(TocItemUtils.NUM_HEADING_SEPARATOR).append(tocItem.getHeading());
            }
        } else if (!StringUtils.isEmpty(tocItem.getNumber())) {
            SoftActionType softAction = tocItem.getNumSoftActionAttr();

            if(softAction != null){
                if (PARAGRAPH.equals(tocItem.getTocItem().getAknTag().value()) && (SoftActionType.DELETE.equals(softAction))
                        && !SoftActionType.MOVE_TO.equals(tocItem.getSoftActionAttr())) {
                    itemDescription.append("<span class=\"leos-soft-num-removed\">" + tocItem.getNumber() + "</span>");
                } else if (PARAGRAPH.equals(tocItem.getTocItem().getAknTag().value())
                        && SoftActionType.ADD.equals(softAction) && !SoftActionType.MOVE_TO.equals(tocItem.getSoftActionAttr())) {
                    itemDescription.append("<span class=\"leos-soft-num-new\">" + tocItem.getNumber() + "</span>");
                }
                captionMaxSize = captionMaxSize+itemDescription.length();
            } else {
                itemDescription.append(tocItem.getNumber());
                if (shoudlAddMovedLabel) {
                    itemDescription.append(SPAN_END_TAG).append(getMovedLabel(messageHelper));
                }
            }
        } else if (!StringUtils.isEmpty(tocItem.getHeading())) {
            itemDescription.append(tocItem.getHeading());
            if (shoudlAddMovedLabel) {
                itemDescription.append(SPAN_END_TAG).append(getMovedLabel(messageHelper));
            }
        } else if (shoudlAddMovedLabel) {
            itemDescription.append(SPAN_END_TAG).append(getMovedLabel(messageHelper));
        }

        if (tocItem.getTocItem().isContentDisplayed()) {
            itemDescription.append(itemDescription.length() > 0 ? TocItemUtils.CONTENT_SEPARATOR : "").append(removeTag(tocItem.getContent()));
        }

        return StringUtils.abbreviate(itemDescription.toString(), shoudlAddMovedLabel ? captionMaxSize + MOVED_LABEL_SIZE : captionMaxSize);
    }

    public static String getDisplayableTocItem(TocItem tocItem, MessageHelper messageHelper) {
        return messageHelper.getMessage("toc.item.type." + tocItem.getAknTag().value().toLowerCase());
    }
    
    private static String removeTag(String itemContent) {
    	for (String element : ELEMENTS_TO_REMOVE_FROM_CONTENT) {
    		itemContent = itemContent.replaceAll("<" + element + ".*?</" + element + ">", "");
    	}
        itemContent = itemContent.replaceAll("<[^>]+>", "");
        return itemContent.replaceAll("\\s+", " ").trim();
    }
    
    public static String getItemSoftStyle(TableOfContentItemVO tableOfContentItemVO) {
        String retVal = "";
        if (tableOfContentItemVO.getSoftActionAttr() != null) {
            if (tableOfContentItemVO.getSoftActionAttr().equals(SoftActionType.ADD)) {
                retVal = "leos-soft-new";
            }
            else if (tableOfContentItemVO.getSoftActionAttr().equals(SoftActionType.DELETE)) {
                retVal = "leos-soft-removed";
            }
            else if (tableOfContentItemVO.getSoftActionAttr().equals(SoftActionType.MOVE_TO)) {
                retVal = "leos-soft-movedto";
            }
            else if (tableOfContentItemVO.getSoftActionAttr().equals(SoftActionType.MOVE_FROM)) {
                retVal = "leos-soft-movedfrom";
            }
        }
        return retVal;
    }
    
}
