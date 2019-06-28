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
package eu.europa.ec.leos.services.content.toc;

import eu.europa.ec.leos.vo.toctype.TocItemType;

import java.util.List;
import java.util.Map;

public class ProposalTocRulesMap {
    
    public Map<TocItemType, List<TocItemType>> proposalTocRules;
    public List<TocItemType> arabicNumberingElements;
    public List<TocItemType> romanNumberingElements;
    public static final String ARABIC_REGEX = "\\d+$|#"; // look for digits only and symbol #
    public static final String ROMAN_REGEX = "M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$|#";
    
    public void setProposalTocRules(Map<TocItemType, List<TocItemType>> proposalTocRules) {
        this.proposalTocRules = proposalTocRules;
    }
    
    public Map<TocItemType, List<TocItemType>> getDefaultTableOfContentRules() {
        return proposalTocRules;
    }
    
    public List<TocItemType> getArabicNumberingElements() {
        return arabicNumberingElements;
    }
    
    public void setArabicNumberingElements(List<TocItemType> arabicNumberingElements) {
        this.arabicNumberingElements = arabicNumberingElements;
    }
    
    public List<TocItemType> getRomanNumberingElements() {
        return romanNumberingElements;
    }
    
    public void setRomanNumberingElements(List<TocItemType> romanNumberingElements) {
        this.romanNumberingElements = romanNumberingElements;
    }
}
