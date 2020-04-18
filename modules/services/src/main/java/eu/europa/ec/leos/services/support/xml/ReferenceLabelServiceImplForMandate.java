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

import com.ximpleware.XMLModifier;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.services.support.xml.ref.Ref;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.util.Arrays;

import static eu.europa.ec.leos.services.support.xml.VTDUtils.toByteArray;

@Service
@Instance(instances = {InstanceType.COUNCIL})
public class ReferenceLabelServiceImplForMandate extends ReferenceLabelServiceImpl {

    private String getDirection(String attr) {
        if (attr.equals(VTDUtils.LEOS_SOFT_MOVE_FROM)) {
            return "from";
        }
        else if (attr.equals(VTDUtils.LEOS_SOFT_MOVE_TO)) {
            return "to";
        }
        return "";
    }

    /**
     * Generates the soft move label. Example: MOVED from Article 1(1), point (a)
     *
     * @return: returns the label if ref is valid or an error code if not.
     */
    @Override
    public Result<String> generateSoftmoveLabel(Ref ref, String sourceRefId, XMLModifier xmlModifier, String attr, String sourceDocumentRef) throws Exception {
        Result<String> labelResult = generateLabel(Arrays.asList(ref), toByteArray(xmlModifier));
        
        if (labelResult.isOk()) {
            String label = Jsoup.parse(labelResult.get()).text();
            labelResult = new Result<String> (messageHelper.getMessage("softmove.prefix." + getDirection(attr), new String[] {label}) ,null);
        }
        return labelResult;
    }

}
