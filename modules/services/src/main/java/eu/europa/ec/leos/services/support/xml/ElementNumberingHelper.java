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

import com.ximpleware.*;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.action.SoftActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static eu.europa.ec.leos.services.support.xml.VTDUtils.*;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.*;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.nCopies;

@Component
public class ElementNumberingHelper {

     private static final String LEOS_ORIGIN_ATTR_EC = "ec";
        private static final Logger LOG = LoggerFactory.getLogger(ElementNumberingHelper.class);

        private boolean isDefaultEditable = false;
        private boolean isNameSpaceEnabled = true;


        byte[] renumberElements(String element, String elementNumber , Object data ) throws NavException, ModifyException, TranscodeException, IOException{

            VTDNav vtdNav = createVTDNav(data, element);
            XMLModifier xmlModifier = null;
            try {
                xmlModifier = new XMLModifier(vtdNav);
            } catch (ModifyException e) {
                throw new RuntimeException("Unable to perform the renumber "+ element +" operation", e);
            }
            return renumberElements(element, vtdNav, xmlModifier, elementNumber);

        }
        private byte[] renumberElements(String element, VTDNav vtdNav, XMLModifier xmlModifier, String elementNumber) throws NavException, ModifyException, TranscodeException, IOException {

            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.selectElement(element);
            char alphaNumber = 'a';
            List<Integer> indexList = new ArrayList<Integer>();
            boolean foundProposalElement = false;
            int parentIndex = getParentIndex(vtdNav, element);

            if (!isProposalElement(vtdNav) && (element == PARAGRAPH || element == POINT)) {
                while (autoPilot.iterate()) {
                    switch (element) {
                        case PARAGRAPH:
                            updatePointNumbersDefault(vtdNav, xmlModifier);
                            if (hasAffectedAttribute(vtdNav, xmlModifier)) {
                                if (vtdNav.toElement(VTDNav.FIRST_CHILD, LIST)) {
                                    renumberElements(POINT, vtdNav, xmlModifier, "");
                                }
                            }
                            break;
                        case POINT:
                            updatePointNumbersDefault(vtdNav, xmlModifier);
                            if (hasAffectedAttribute(vtdNav, xmlModifier)) {
                                if (vtdNav.toElement(VTDNav.FIRST_CHILD, LIST)) {
                                    renumberElements(POINT, vtdNav, xmlModifier, "");
                                }
                            }
                            break;
                    }
                }
            } else {
                while (autoPilot.iterate()) {
                    int tempIndex = getParentIndex(vtdNav, element);
                    if (tempIndex != parentIndex && (element == PARAGRAPH || element == POINT)) {
                        continue;
                    }

                    if (foundProposalElement || isElementOriginEC(vtdNav)) {
                        if (isElementOriginEC(vtdNav)) {
                            foundProposalElement = true;
                            alphaNumber = 'a';
                            //set the series number for manual numbering of Articles
                            int index = vtdNav.getCurrentIndex();
                            if (vtdNav.toElement(VTDNav.FIRST_CHILD, NUM)) {
                                long contentFragment = vtdNav.getContentFragment();
                                elementNumber = new String(vtdNav.getXML().getBytes((int) contentFragment, (int) (contentFragment >> 32)));
                            }
                            vtdNav.recoverNode(index);
                            //Check if elemetns added to proposal elements
                            if (hasAffectedAttribute(vtdNav, xmlModifier) || hasNumToggledAttribute(vtdNav, xmlModifier)) {
                                switch (element) {
                                    case ARTICLE:
                                            renumberElements(PARAGRAPH, vtdNav, xmlModifier, "");
                                        break;
                                    case PARAGRAPH:
                                        if (vtdNav.toElement(VTDNav.FIRST_CHILD, LIST)) {
                                            updateMandatePointsNumbering(vtdNav, xmlModifier);
                                            renumberElements(POINT, vtdNav, xmlModifier, "");
                                        }
                                        break;
                                    case POINT:
                                        if (vtdNav.toElement(VTDNav.FIRST_CHILD, LIST)) {
                                            updateMandatePointsNumbering(vtdNav, xmlModifier);
                                            renumberElements(POINT, vtdNav, xmlModifier, "");
                                        }
                                        break;
                                }
                            }
                        } else { //adding alpha number series to added article
                            switch (element) {
                                case ARTICLE:
                                    alphaNumber = updateArticleNumbers(elementNumber, vtdNav, xmlModifier, alphaNumber, false);
                                    if (hasAffectedAttribute(vtdNav, xmlModifier) || hasNumToggledAttribute(vtdNav, xmlModifier)) {
                                        updateParagraphNumbersDefault(vtdNav, xmlModifier);
                                        renumberElements(PARAGRAPH, vtdNav, xmlModifier, "");
                                    }
                                    break;
                                case PARAGRAPH:
                                    alphaNumber = updateParagraphNumbers(elementNumber, vtdNav, xmlModifier, alphaNumber, false);
                                    if (hasAffectedAttribute(vtdNav, xmlModifier)) {
                                        if (vtdNav.toElement(VTDNav.FIRST_CHILD, LIST)) {
                                            updateMandatePointsNumbering(vtdNav, xmlModifier);
                                            renumberElements(POINT, vtdNav, xmlModifier, "");
                                        }
                                    }
                                    break;
                                case POINT:
                                    alphaNumber = updatePointNumbers("", elementNumber, vtdNav, xmlModifier, alphaNumber, false);
                                    if (hasAffectedAttribute(vtdNav, xmlModifier)) {
                                        if (vtdNav.toElement(VTDNav.FIRST_CHILD, LIST)) {
                                            updateMandatePointsNumbering(vtdNav, xmlModifier);
                                            renumberElements(POINT, vtdNav, xmlModifier, "");
                                        }
                                    }
                                    break;
                                case RECITAL:
                                    alphaNumber = updateRecitalNumbers(elementNumber, vtdNav, xmlModifier, alphaNumber, false);
                                    break;
                            }
                        }
                    } else {
                        indexList.add(vtdNav.getCurrentIndex());
                    }
                }
                //-ve numbering
                long negativeNumber = -(indexList.size());
                //For Points -ve numbering get the type of sequence
                String numSeq = getNegativeSidePointsNumSequence(element, vtdNav, indexList);

                for (int negativeNumIndex : indexList) {
                    vtdNav.recoverNode(negativeNumIndex);
                    switch (element) {
                        case ARTICLE:
                            updateArticleNumbers("Article " + negativeNumber++, vtdNav, xmlModifier, Character.MIN_VALUE, true);
                            if (hasAffectedAttribute(vtdNav, xmlModifier) || hasNumToggledAttribute(vtdNav, xmlModifier)) {
                                updateParagraphNumbersDefault(vtdNav, xmlModifier);
                                renumberElements(PARAGRAPH, vtdNav, xmlModifier, "");
                            }
                            break;
                        case PARAGRAPH:
                            updateParagraphNumbers("" + negativeNumber++, vtdNav, xmlModifier, Character.MIN_VALUE, true);
                            pointNumProcessing(vtdNav, xmlModifier);
                            break;
                        case POINT:
                            updatePointNumbers(numSeq, "" + negativeNumber++, vtdNav, xmlModifier, alphaNumber, true);
                            pointNumProcessing(vtdNav, xmlModifier);
                            break;
                        case RECITAL:
                            updateRecitalNumbers("" + negativeNumber++, vtdNav, xmlModifier, Character.MIN_VALUE, true);
                            break;
                    }
                }
            }
            return toByteArray(xmlModifier);
        }

        private void pointNumProcessing(VTDNav vtdNav, XMLModifier xmlModifier) throws NavException, ModifyException, TranscodeException, IOException {
            if (hasAffectedAttribute(vtdNav, xmlModifier)) {
                if (vtdNav.toElement(VTDNav.FIRST_CHILD, LIST)) {
                    int currentIndex = vtdNav.getCurrentIndex();
                    renumberPoints(vtdNav, xmlModifier);
                    vtdNav.recoverNode(currentIndex);
                    renumberElements(POINT, vtdNav, xmlModifier, "");
                }
            }
        }

        private void updateMandatePointsNumbering(VTDNav vtdNav, XMLModifier xmlModifier)
                    throws NavException, ModifyException {
                if(!isProposalElement(vtdNav)) {
                    int currentIndex = vtdNav.getCurrentIndex();
                    renumberPoints(vtdNav, xmlModifier);
                    vtdNav.recoverNode(currentIndex);
                }
            }

        private boolean isProposalElement(VTDNav vtdNav) throws NavException {
            boolean isProposalElement = false;
            int attributeIndex = vtdNav.getAttrVal(LEOS_ORIGIN_ATTR);
            if (attributeIndex != -1 && vtdNav.toNormalizedString(attributeIndex).equals(LEOS_ORIGIN_ATTR_EC)) {
                isProposalElement = true;
            }
            return isProposalElement;
        }

        private String getNegativeSidePointsNumSequence(String element, VTDNav vtdNav, List<Integer> indexList) throws NavException {
            String seqType = null;
            if (element.equals(POINT) && indexList.size() > 0) {
                vtdNav.recoverNode(indexList.get(indexList.size() - 1));
                if (vtdNav.toElement(VTDNav.NEXT_SIBLING)) {
                    if (vtdNav.toElement(VTDNav.FIRST_CHILD, NUM)) {
                        seqType = vtdNav.toString((int) vtdNav.getContentFragment(), (int) (vtdNav.getContentFragment() >> 32));
                    }
                }
            }
            return seqType;
        }

        private byte[] updatePointNumbersDefault(VTDNav vtdNav, XMLModifier xmlModifier)
                throws NavException, ModifyException, IOException, TranscodeException {
            LOG.info("points default numbering");
            int currentIndex = vtdNav.getCurrentIndex();
            if (vtdNav.toElement(VTDNav.FIRST_CHILD, LIST)) {
                renumberPoints(vtdNav, xmlModifier);
                vtdNav.recoverNode(currentIndex);
            }
            return toByteArray(xmlModifier);
        }

        private void renumberPoints(VTDNav vtdNav, XMLModifier xmlModifier) throws NavException, PilotException, ModifyException {
            long number = 1L;
            int parentIndex = getParentIndex(vtdNav, POINT);
            String seqType = getPointNumSequence(vtdNav);
            if (vtdNav.toElement(VTDNav.FIRST_CHILD, POINT)) {
                do {
                    int tempIndex = getParentIndex(vtdNav, POINT);
                    if (tempIndex != parentIndex) {
                        break;
                    }

                    // check if it has num element
                    String num = getPointNumber((int) number++, seqType);
                    byte[] elementeNum = seqType == "-" ? num.getBytes(UTF_8) : ("(" + num + ")").getBytes(UTF_8);
                    int index = vtdNav.getCurrentIndex();
                    elementeNum = buildNumElement(vtdNav, xmlModifier, elementeNum);
                    vtdNav.recoverNode(index);
                    xmlModifier.insertAfterHead(elementeNum);
                }
                while (vtdNav.toElement(VTDNav.NEXT_SIBLING));
            }
        }

        private int getParentIndex(VTDNav vtdNav, String element) throws NavException {
            int index = vtdNav.getCurrentIndex();
            AutoPilot ap = new AutoPilot(vtdNav);
            ap.selectElement(element);
            int parentIndex = 0;
            while (ap.iterate()) {
                if (vtdNav.toElement(VTDNav.PARENT)) {
                    parentIndex = vtdNav.getCurrentIndex();
                    break;
                }
            }
            vtdNav.recoverNode(index);
            return parentIndex;
        }


        private String getPointNumSequence(VTDNav vtdNav) throws NavException {
            int index = vtdNav.getCurrentIndex();
            int pointDepth = 1;
            String pointSequence = "#";
            boolean isNumSequencePresent = false;
            //If points are already there than need to check the num sequence of first point

                if(vtdNav.toElement(VTDNav.FIRST_CHILD,POINT)){
                    if(vtdNav.toElement(VTDNav.FIRST_CHILD,NUM)){
                        String numSeq = vtdNav.toString((int)vtdNav.getContentFragment(),(int)(vtdNav.getContentFragment() >> 32));
                        if(numSeq == "(a)"  || numSeq == "(i)" || numSeq == "-" || numSeq == "(1)" ){
                            pointSequence = numSeq;
                            isNumSequencePresent = true;
                        }
                    }

                }
            vtdNav.recoverNode(index);
            //If there are no num sequence is present then have to check for level of points
            if(!isNumSequencePresent){
                while (true) {
                    if (vtdNav.toElement(VTDNav.PARENT)) {
                        if (vtdNav.matchElement(LIST)) {
                            pointDepth++;
                        } else if (vtdNav.matchElement(PARAGRAPH)) {
                            break;
                        }
                    }
                }
                if (pointDepth == 1) {
                    pointSequence = "(a)";
                } else if (pointDepth == 2) {
                    pointSequence = "(1)";
                } else if (pointDepth == 3) {
                    pointSequence = "(i)";
                } else {
                    pointSequence = "-";
                }
            }
            vtdNav.recoverNode(index);
            return pointSequence;
        }


        private byte[] updateParagraphNumbersDefault(VTDNav vtdNav, XMLModifier xmlModifier)
                throws NavException, ModifyException, IOException, TranscodeException {
            int currentIndex = vtdNav.getCurrentIndex();
            long number = 1L;
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.selectElement(PARAGRAPH);
            while (autoPilot.iterate()) {
                if(isNumElementExists(vtdNav)){
                byte[] elementeNum = ((number++) + ".").getBytes(UTF_8);
                int index = vtdNav.getCurrentIndex();
                elementeNum = buildNumElement(vtdNav, xmlModifier, elementeNum);
                vtdNav.recoverNode(index);
                xmlModifier.insertAfterHead(elementeNum);
                }
            }
            vtdNav.recoverNode(currentIndex);
            return toByteArray(xmlModifier);
        }

        private char updateArticleNumbers(String elementNumber, VTDNav vtdNav, XMLModifier xmlModifier, char alphaNumber, boolean onNegativeSide)
                throws NavException, ModifyException {
            byte[] articleNum;
            if (onNegativeSide) {
                articleNum = elementNumber.getBytes(UTF_8);
            } else {
                articleNum = (elementNumber + (alphaNumber <= 'z' ? alphaNumber++ : '#')).getBytes(UTF_8);
            }
            updateNumElement(vtdNav, xmlModifier, articleNum);
            return alphaNumber;
        }

        private char updateRecitalNumbers(String elementNumber, VTDNav vtdNav, XMLModifier xmlModifier, char alphaNumber, boolean onNegativeSide)
                throws NavException, ModifyException {
            byte[] recitalNum;
            if (onNegativeSide) {
                recitalNum = ("(" + elementNumber + ")").getBytes(UTF_8);
            } else {
                recitalNum = (elementNumber.replace(')', (alphaNumber <= 'z' ? alphaNumber++ : '#')) + ")").getBytes(UTF_8);
            }
            updateNumElement(vtdNav, xmlModifier, recitalNum);
            return alphaNumber;
        }

        private char updateParagraphNumbers(String elementNumber, VTDNav vtdNav, XMLModifier xmlModifier, char alphaNumber, boolean onNegativeSide) throws NavException, ModifyException{
            byte[] paraNum;
            if (onNegativeSide) {
                paraNum = (elementNumber + ".").getBytes(UTF_8);
            } else {
                paraNum = (elementNumber.replace('.', (alphaNumber <= 'z' ? alphaNumber++ : '#')) + ".").getBytes(UTF_8);
            }
            if (isNumElementExists(vtdNav)) {
                updateNumElement(vtdNav, xmlModifier, paraNum);
            }
            return alphaNumber;
        }

        private char updatePointNumbers(String seqType, String elementNumber, VTDNav vtdNav, XMLModifier xmlModifier, char alphaNumber, boolean onNegativeSide)
                throws NavException, ModifyException {
            String pointNum;
            if (onNegativeSide && seqType != null) {
                String num = getPointNumber(Math.abs(Integer.parseInt(elementNumber)), seqType);
                pointNum = ("(" + (num == "-" ? num : "-" + num + ")"));
            } else {
                if(elementNumber != null && elementNumber != "") {
                    pointNum = (elementNumber.replace(')', (alphaNumber <= 'z' ? alphaNumber++ : '#')) + ")");
                    if(elementNumber.indexOf("-") != -1) {
                        pointNum = pointNum.substring(0, pointNum.length() -1);
                    }
                } else {
                    pointNum = ("(" + (alphaNumber <= 'z' ? alphaNumber++ : '#') + ")");
                }
            }
            updateNumElement(vtdNav, xmlModifier, pointNum.getBytes(UTF_8));
            return alphaNumber;
        }

        private String getPointNumber(int number, String seqType) {
            String pointNum = "#";
            if (seqType != null) {
                switch (seqType) {
                    case "(i)":
                        pointNum = join("", nCopies(number, "i"))
                                .replace("iiiii", "v")
                                .replace("iiii", "iv")
                                .replace("vv", "x")
                                .replace("viv", "ix")
                                .replace("xxxxx", "l")
                                .replace("xxxx", "xl")
                                .replace("ll", "c")
                                .replace("lxl", "xc")
                                .replace("ccccc", "d")
                                .replace("cccc", "cd")
                                .replace("dd", "m")
                                .replace("dcd", "cm");
                        break;
                    case "(a)":
                        pointNum = number > 0 && number < 27 ? String.valueOf((char) (number + 96)) : "#";
                        break;
                    case "-":
                        pointNum = "-";
                        break;
                    case "(1)":
                        pointNum = "" + number;
                        break;
                }
            }
            return pointNum;
        }

        private void updateNumElement(VTDNav vtdNav, XMLModifier xmlModifier, byte[] elementNum) throws NavException, ModifyException {
            byte[] element;
            int currentIndex = vtdNav.getCurrentIndex();
            element = buildNumElement(vtdNav, xmlModifier, elementNum);
            vtdNav.recoverNode(currentIndex);
            xmlModifier.insertAfterHead(element);
        }

        private boolean isElementOriginEC(VTDNav vtdNav) throws NavException {
            int currentIndex = vtdNav.getCurrentIndex();
            boolean isOriginEC = false;
            if (vtdNav.toElement(VTDNav.FIRST_CHILD, NUM)) {
                int attributeIndex = vtdNav.getAttrVal(LEOS_ORIGIN_ATTR);
                if (attributeIndex != -1 && vtdNav.toNormalizedString(attributeIndex).equals(LEOS_ORIGIN_ATTR_EC)) {
                    isOriginEC = true;
                }
            } else {
                int attributeIndex = vtdNav.getAttrVal(LEOS_ORIGIN_ATTR);
                if (attributeIndex != -1 && vtdNav.toNormalizedString(attributeIndex).equals(LEOS_ORIGIN_ATTR_EC)) {
                    isOriginEC = true;
                }
            }
            vtdNav.recoverNode(currentIndex);
            return isOriginEC;
        }

        private boolean hasAffectedAttribute(VTDNav vtdNav, XMLModifier xmlModifier)
                throws NavException, ModifyException {
            int index = vtdNav.getCurrentIndex();
            boolean flag;
            int attributeIndex =vtdNav.getAttrVal(LEOS_AFFECTED_ATTR);

            flag = setFlagAndRemoveAttribute(vtdNav, xmlModifier, LEOS_AFFECTED_ATTR, attributeIndex);

            vtdNav.recoverNode(index);
            return flag;
        }

        private boolean hasNumToggledAttribute(VTDNav vtdNav, XMLModifier xmlModifier) throws NavException, ModifyException {

            int index = vtdNav.getCurrentIndex();
            boolean flag = this.isDefaultEditable;
            int attributeIndex;
            if (vtdNav.toElement(VTDNav.FIRST_CHILD, PARAGRAPH)) {
                do {
                    int paraIndex = vtdNav.getCurrentIndex();
                    if (vtdNav.toElement(VTDNav.FIRST_CHILD, NUM)) {
                        attributeIndex = vtdNav.getAttrVal(TOGGLED_TO_NUM);
                        if(!flag){
                            flag = setFlagAndRemoveAttribute(vtdNav, xmlModifier, TOGGLED_TO_NUM, attributeIndex);
                        }else{
                            setFlagAndRemoveAttribute(vtdNav, xmlModifier, TOGGLED_TO_NUM, attributeIndex);
                        }
                    }
                    vtdNav.recoverNode(paraIndex);
                } while (vtdNav.toElement(VTDNav.NEXT_SIBLING, PARAGRAPH));
            }

            vtdNav.recoverNode(index);
            return flag;

        }

        private boolean setFlagAndRemoveAttribute(VTDNav vtdNav, XMLModifier xmlModifier, String affectedAttribute, int attributeIndex) throws NavException, ModifyException {
            boolean flag = false;
            if (attributeIndex != -1 && vtdNav.toNormalizedString(attributeIndex).equals("true")) {
                flag = true;
                AutoPilot ap = new AutoPilot(vtdNav);
                ap.selectAttr(affectedAttribute);
                int attrIndex = ap.iterateAttr();
                xmlModifier.removeAttribute(attrIndex);
            }
            return flag;
        }


        private boolean isNumElementExists(VTDNav vtdNav) throws NavException {
            int index = vtdNav.getCurrentIndex();
            boolean exists = false;
            if (vtdNav.toElement(VTDNav.FIRST_CHILD, NUM)) {
                //check if Num is soft deleted
                exists = true;
                int attributeIndex = vtdNav.getAttrVal(LEOS_SOFT_ACTION_ATTR);
                if (attributeIndex != -1 && vtdNav.toNormalizedString(attributeIndex).equals(SoftActionType.DELETE.getSoftAction())) {
                    exists = false;
                }
            }
            vtdNav.recoverNode(index);
            return exists;
        }

        private VTDNav createVTDNav(Object data, String element) {
            switch (element) {
            case ARTICLE :
                try {
                    if(data instanceof String) {

                        return setupVTDNav(((String)data).getBytes(UTF_8), isNameSpaceEnabled);

                    }else if(data instanceof byte[]) {
                        return setupVTDNav((byte[])data, isNameSpaceEnabled);
                    }
                } catch (NavException e) {
                    throw new RuntimeException("Unable to perform the renumberArticles operation", e);
                }
                break;
            case RECITAL:
                try {
                    if(data instanceof String) {

                        return setupVTDNav(((String)data).getBytes(UTF_8));

                    }else if(data instanceof byte[]) {
                        return setupVTDNav((byte[])data);
                    }
                } catch (NavException e) {
                    throw new RuntimeException("Unable to perform the renumberRecitals operation", e);
                }
                break;
            }
            return null;

        }

        byte[] renumberElements(String element, String elementNumber , byte[] xmlContent, MessageHelper messageHelper) {
            this.isNameSpaceEnabled = false;
            VTDNav vtdNav = createVTDNav(xmlContent, element);
            long number = 1L;
            byte[] updatedElement;
            XMLModifier xmlModifier=null;
            try {
                xmlModifier = new XMLModifier(vtdNav);
                AutoPilot autoPilot = new AutoPilot(vtdNav);
                autoPilot.selectElement(element);
                byte[] elementeNum =  null;
                String recitalNum = null;
                while (autoPilot.iterate()) {
                    if(element.equals(ARTICLE)) {
                        elementeNum = messageHelper
                                .getMessage("legaltext.article.num", new Object[] { (number++) })
                                .getBytes(UTF_8);
                    } else {
                        recitalNum = "(" + number++ + ")";
                        elementeNum = recitalNum.getBytes(UTF_8);
                    }
                    int currentIndex = vtdNav.getCurrentIndex();
                    updatedElement = buildNumElement(vtdNav, xmlModifier, elementeNum);
                    vtdNav.recoverNode(currentIndex);
                    xmlModifier.insertAfterHead(updatedElement);

                }
                this.isNameSpaceEnabled = true;
                return toByteArray(xmlModifier);
            } catch (Exception e) {
                throw new RuntimeException("Unable to perform the renumberArticles operation", e);
            }

        }

        void setImportAticleDefaultProperties() {
            this.isDefaultEditable = true;
            this.isNameSpaceEnabled = false;

        };

        void resetImportAticleDefaultProperties() {
            this.isDefaultEditable = false;
            this.isNameSpaceEnabled = true;
        }

}
