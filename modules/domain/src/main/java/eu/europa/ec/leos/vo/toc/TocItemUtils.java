package eu.europa.ec.leos.vo.toc;
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

import java.util.List;
import java.util.stream.Collectors;

public class TocItemUtils {
    
    public static final String NUM_HEADING_SEPARATOR = " - ";
    public static final String CONTENT_SEPARATOR = " ";
    
    public static TocItem getTocItemByName(List<TocItem> tocItems, String tagName) {
        return tocItems.stream()
                .filter(tocItem -> tocItem.getAknTag().value().equalsIgnoreCase(tagName))
                .findFirst()
                .orElse(null);
    }
    
    public static TocItem getTocItemByNameOrThrow(List<TocItem> tocItems, String tagName) {
        return tocItems.stream()
                .filter(tocItem -> tocItem.getAknTag().value().equalsIgnoreCase(tagName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("TocItem '" + tagName + "' not present in the list of Items [" + getTocItemNamesAsList(tocItems) + "]"));
    }
    
    public static List<String> getTocItemNamesAsList(List<TocItem> tocItems) {
        return tocItems.stream()
                .map(tocItem -> tocItem.getAknTag().value())
                .collect(Collectors.toList());
    }
    
    public static NumberingConfig getNumberingByName(List<NumberingConfig> numberingConfigs, NumberingType numType) {
        return numberingConfigs.stream()
                .filter(config -> config.getType().equals(numType)).findFirst()
                .orElseThrow(() -> new IllegalStateException("NumberingType '" + numType + "' not present in the list of NumberingConfigs [" + numberingConfigs + "]"));
    }

    public static NumberingType getNumberingTypeByDepth(NumberingConfig numberingConfig, int depth) {
        return numberingConfig.getLevels().getLevels().stream()
                .filter(level ->  level.getDepth() == depth).findFirst()
                .map(level -> level.getNumberingType())
                .orElseThrow(() -> new IllegalStateException("Depth '" + depth + "' not defined in the NumberingConfig [" + numberingConfig + "]"));
    }

    public static int getDepthByNumberingType(List<NumberingConfig> numberingConfigs, NumberingType numberingType) {
        final NumberingConfig multilevelConfig = numberingConfigs.stream()
                .filter(config -> config.getType() == NumberingType.MULTILEVEL)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("NumberingType '" + NumberingType.MULTILEVEL + "' not defined in the NumberingConfigs [" + numberingConfigs + "]"));
        final int depth = multilevelConfig.getLevels().getLevels().stream().filter(level -> level.getNumberingType() == numberingType)
                .findFirst()
                .map(level -> level.getDepth())
                .orElseThrow(() -> new IllegalStateException("NumberingType '" + numberingType + "' not defined in the NumberingConfig [" + multilevelConfig + "]"));
        return depth;
    }

    public static NumberingConfig getNumberingConfig(List<NumberingConfig> numberConfigs, NumberingType numType) {
        return numberConfigs.stream()
                .filter(numberingConfig -> numberingConfig.getType().equals(numType))
                .findFirst()
                .orElse(null);
    }
    
    public static NumberingConfig getNumberingConfigByTagName(List<TocItem> items, List<NumberingConfig> numberingConfigs, String tagName) {
        TocItem tocItem = getTocItemByName(items, tagName);
        NumberingConfig numberingConfig = getNumberingByName(numberingConfigs, tocItem.getNumberingType());
        return numberingConfig;
    }

}
