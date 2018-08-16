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

import com.ibm.icu.text.RuleBasedNumberFormat;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

class NumFormatter {
    static private final List<String> unNumberedItems = Arrays.asList("paragraph", "subparagraph", "sub-point", "point", "indent");

    static String formattedNum(TreeNode node) {
        switch (node.getType()) {
            case "part":
            case "title":
            case "chapter":
            case "section":
            case "article":
                return node.getNum();
            default:
                return (isUnnumbered(node)) ? formatUnnumbered(node) : formatNumbered(node);
        }
    }

    private static String formatNumbered(TreeNode node) {
        return String.format("(%s)", node.getNum());
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
        if("article".equals(node.getType()) || node.getParent() == null){
            return false;
        }
        else{
            return isUnnumbered(node)
                    || anyUnnumberedParent(node.getParent());
        }        
    }
    
    static String formatUnnumbered(TreeNode node) {
        RuleBasedNumberFormat numberFormat = new RuleBasedNumberFormat(new Locale("en"), RuleBasedNumberFormat.SPELLOUT);
        numberFormat.setDefaultRuleSet("%spellout-ordinal");

        return numberFormat.format(node.getSiblingNumber());
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
}