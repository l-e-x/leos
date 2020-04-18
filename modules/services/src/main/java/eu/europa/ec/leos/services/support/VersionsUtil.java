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

import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.action.ActionType;
import eu.europa.ec.leos.model.action.CheckinCommentVO;
import eu.europa.ec.leos.model.action.CheckinElement;
import eu.europa.ec.leos.model.action.VersionVO;
import eu.europa.ec.leos.services.document.util.CheckinCommentUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class VersionsUtil {
    
    public static String buildLabel(CheckinCommentVO checkinComment, MessageHelper messageHelper) {
        final String label;
        final CheckinElement checkinElement = checkinComment.getCheckinElement();
        
        if (checkinElement == null) {
            //major save
            label = checkinComment.getTitle();
        } else if (checkinElement.getChildElements() == null || checkinElement.getChildElements().size() == 0) {
            //minor save
            label = messageHelper.getMessage("operation.element." + checkinElement.getActionType().name().toLowerCase(), checkinElement.getElementLabel());
        } else {
            // structural change
            label = buildLabelForStructuralSave(checkinElement.getChildElements(), messageHelper);
        }
        
        return label;
    }
    
    public static String buildLabelForStructuralSave(Set<CheckinElement> items, MessageHelper messageHelper) {
        final StringBuilder label = new StringBuilder();
        
        // Group the items by ActionType. The map will be like:
        //{INSERTED - [(article, ""), (article, ""), (article, ""), (citation, "")]}
        //{UPDATED -  [(article, "Article 3")]}
        //{DELETED -  [(article, "Article 4"), (recital, "second recital")]}
        Map<ActionType, List<CheckinElement>> groupByAction = items.stream().collect(Collectors.groupingBy(CheckinElement::getActionType));
        
        //Build a map of type (ActionType, String), example:  (we skip INSERTED since we do not have still nor id not label for new elements.)
        //(UPDATED -  "Article 3"
        //(DELETED -  "Article 4, second recital"
        Map<ActionType, String> groupByActionStringified = groupByAction
                .entrySet()
                .stream()
                .filter(action -> action.getKey() != ActionType.INSERTED) //not new tocItems
                .collect(Collectors.toMap(Map.Entry::getKey,
                        list -> {
                            final StringBuilder labels = new StringBuilder();
                            list.getValue().stream().forEach(itemVO -> {
                                if (itemVO.getElementLabel() != null) {
                                    labels.append(itemVO.getElementLabel()).append(", ");
                                }
                            });
                            String str = labels.toString().trim();
                            return str.substring(0, str.length() - 1);
                        }));
        
        // Attach labels to the global label
        groupByActionStringified.forEach((actionType, str) -> {
            label.append(messageHelper.getMessage("operation.element." + actionType.name().toLowerCase(), str))
                    .append("; ");
        });
        
        // For inserted elements we group by TagName in order to build the string as:
        // "3 articles inserted, 1 citation inserted" for the following items:
        // {INSERTED - [(article, ""), (article, ""), (article, ""), (citation, "")]}
        List<CheckinElement> insertedElements = groupByAction.get(ActionType.INSERTED);
        if (insertedElements != null) {
            Map<String, Long> groupByTagName = insertedElements.stream()
                    .collect(Collectors.groupingBy(CheckinElement::getElementTagName, Collectors.counting()));

            groupByTagName.entrySet().forEach((entry) -> {
                label.append(messageHelper.getMessage("operation.element.inserted.count", entry.getValue(), entry.getKey().toLowerCase()))
                        .append("; ");
            });
        }
        
        String str = label.toString().trim();
        if (!str.isEmpty() && str.charAt(str.length() - 1) == ';') {
            str = str.substring(0, str.length() - 1);
        }
        
        return str;
    }
    
    public static <D extends XmlDocument> List<VersionVO> buildVersionVO(List<D> versions, MessageHelper messageHelper) {
        List<VersionVO> allVersions = new ArrayList<>();
        versions.forEach(doc -> {
            final String checkinCommentJson;
            if (doc.getMilestoneComments().size() > 0) {
                // Only the first comment is related to the document changes. All other comments, if presents,
                // means that the same document, without being changed, is included in other milestones.
                // Example: Create a Milestone 1. Enter inside Annex, make a change, and then create another Milestone 2.
                // In doc VersionCards is shown only the first Milestone1 and in milestoneComments of the doc are present both comments.
                // We need to show only Milestone 1 comment.
                checkinCommentJson = doc.getMilestoneComments().get(0);
            } else {
                checkinCommentJson = doc.getVersionComment() != null ? doc.getVersionComment() : messageHelper.getMessage("popup.label.revision.nocomment");
            }
            final CheckinCommentVO checkinCommentVO = CheckinCommentUtil.getJavaObjectFromJson(checkinCommentJson);

            VersionVO versionVO = new VersionVO();
            versionVO.setVersionType(doc.getVersionType());
            versionVO.setDocumentId(doc.getId());
            versionVO.setVersionNumber(doc.getVersionLabel());
            versionVO.setCmisVersionNumber(doc.getCmisVersionLabel());
            versionVO.setUpdatedDate(doc.getLastModificationInstant());
            versionVO.setUsername(doc.getLastModifiedBy());
            versionVO.setCheckinCommentVO(checkinCommentVO);
            versionVO.setVersionedReference(doc.getVersionedReference());
            allVersions.add(versionVO);
        });
        return allVersions;
    }
}
