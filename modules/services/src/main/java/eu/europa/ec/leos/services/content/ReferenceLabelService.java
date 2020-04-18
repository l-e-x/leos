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
package eu.europa.ec.leos.services.content;

import com.ximpleware.XMLModifier;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.services.support.xml.ref.Ref;

import java.util.List;

public interface ReferenceLabelService {
    
    Result<String> generateLabel(List<Ref> refs, byte[] sourceBytes);
    Result<String> generateLabelStringRef(List<String> refs, String sourceDocumentRef, byte[] sourceBytes);
    
    Result<String> generateLabel(List<Ref> refs, String sourceDocumentRef, String sourceRefId, byte[] sourceBytes);
    Result<String> generateLabelStringRef(List<String> refsString, String sourceDocumentRef, String sourceRefId, byte[] sourceBytes);
    
    Result<String> generateLabel(List<Ref> refs, String sourceDocumentRef, String sourceRefId, byte[] sourceBytes, byte[] targetBytes, String targetDocType, boolean withAnchor);
    Result<String> generateLabelStringRef(List<String> refs, String sourceDocumentRef, String sourceRefId, byte[] sourceBytes, String targetDocumentRef, boolean withAnchor);
    
    Result<String> generateSoftmoveLabel(Ref ref, String referenceLocation, XMLModifier xmlModifier, String direction, String documentRefSource) throws Exception;
    
}
