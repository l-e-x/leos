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

import com.google.common.base.Strings;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.RuleBasedNumberFormat;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.ARTICLE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.CHAPTER;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.CITATION;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.INDENT;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.PARAGRAPH;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.PART;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.POINT;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.SECTION;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.SUBPARAGRAPH;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.SUBPOINT_LABEL;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.TITLE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.LEVEL;

class NumFormatter {
    static private final List<String> unNumberedItems = Arrays.asList(CITATION, PARAGRAPH, SUBPARAGRAPH, SUBPOINT_LABEL, POINT, INDENT);

    static String formattedNum(TreeNode node, Locale locale) {
        switch (node.getType()) {
            case PART:
            case TITLE:
            case CHAPTER:
            case SECTION:
            case ARTICLE:
            case LEVEL:
                return node.getNum();
            default:
                return (isUnnumbered(node)) ? formatUnnumbered(node, locale) : formatNumbered(node);
        }
    }

    private static String formatNumbered(TreeNode node) {
        return Strings.isNullOrEmpty(node.getNum()) ? "" : String.format("(%s)", node.getNum());
    }

    static boolean isUnnumbered(TreeNode node) {
        if (unNumberedItems.contains(node.getType())
                && (StringUtils.isEmpty(node.getNum()) || node.getNum().matches("[^a-zA-Z0-9]+"))) {
            return true;
        } else {
            return false;
        }
    }

    static boolean anyUnnumberedParent(TreeNode node) {
        if(ARTICLE.equals(node.getType()) || node.getParent() == null){
            return false;
        }
        else{
            return isUnnumbered(node) ||
                    anyUnnumberedParent(node.getParent());
        }        
    }
    
    static String formatUnnumbered(TreeNode node, Locale locale) {
        RuleBasedNumberFormat numberFormat = new RuleBasedNumberFormat(locale, RuleBasedNumberFormat.SPELLOUT);
        //FIXME: Need to define spellout rules for other than english in files RbnfRulesSet
        //once done, remove if below
        if (locale.getLanguage().equalsIgnoreCase("en")) {
            numberFormat.setDefaultRuleSet("%spellout-ordinal");
        }
        
        return numberFormat.format(node.getSiblingNumber());
    }
    
    static String formatPlural(TreeNode node, int number, Locale locale) {
        return formatPlural(node.getType(), number, locale);
    }

    static String formatPlural(String nodeType, int number, Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle(new ClassPathResource("messages/message").getPath(), locale);
        String pattern = bundle.getString("plural");
        MessageFormat msgFormat = new MessageFormat(pattern, locale);
        return msgFormat.format(new Object[] {nodeType, number});
    }
}