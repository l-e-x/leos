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
package eu.europa.ec.leos.services.coedition.handler;

import eu.europa.ec.leos.services.coedition.repository.EditionInfoRepository;
import eu.europa.ec.leos.vo.coedition.CoEditionActionInfo;
import eu.europa.ec.leos.vo.coedition.CoEditionActionInfo.Operation;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.coedition.InfoType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component()
public class CoEditionInfoHandler implements InfoHandler {

    @Autowired
    private EditionInfoRepository editionInfoRepository;

    @Override
    public CoEditionActionInfo storeInfo(CoEditionVO coEditionVo) {
        String docId = coEditionVo.getDocumentId();
        CoEditionVO addedInfo = editionInfoRepository.store(coEditionVo);

        return new CoEditionActionInfo(addedInfo != null, Operation.STORE, addedInfo,
                editionInfoRepository.getCurrentEditInfo(docId));
    }

    @Override
    public CoEditionActionInfo removeInfo(CoEditionVO coEditionVo) {
        String docId = coEditionVo.getDocumentId();
        CoEditionVO removedInfo = editionInfoRepository.removeInfo(coEditionVo);

        return new CoEditionActionInfo(removedInfo != null, Operation.REMOVE, removedInfo,
                editionInfoRepository.getCurrentEditInfo(docId));
    }

    @Override
    public CoEditionActionInfo checkIfInfoExists(CoEditionVO coEditionVo) {
        String docId = coEditionVo.getDocumentId();
        String elementId = coEditionVo.getElementId();

        List<CoEditionVO> editInfos = editionInfoRepository.getCurrentEditInfo(docId);

        for (CoEditionVO existingInfo : editInfos) {
            if (existingInfo.getDocumentId().equalsIgnoreCase(docId) && existingInfo.getPresenterId().equalsIgnoreCase(coEditionVo.getPresenterId()) &&
                    (InfoType.DOCUMENT_INFO.equals(coEditionVo.getInfoType()) ||
                            (InfoType.TOC_INFO.equals(coEditionVo.getInfoType()) && existingInfo.getInfoType().equals(coEditionVo.getInfoType())) ||
                            (InfoType.ELEMENT_INFO.equals(existingInfo.getInfoType()) && existingInfo.getElementId().equalsIgnoreCase(elementId)))) {

                return new CoEditionActionInfo(true, Operation.EXISTS, existingInfo,
                        editionInfoRepository.getCurrentEditInfo(docId));
            }
        }
        return new CoEditionActionInfo(false, Operation.EXISTS, coEditionVo, editionInfoRepository.getCurrentEditInfo(docId));
    }

    @Override
    public CoEditionActionInfo checkIfInfoExists(String sessionId) {
        Optional<CoEditionVO> existingInfo = editionInfoRepository.getAllEditInfo().stream()
                .filter((x) -> x.getSessionId().equals(sessionId)).findFirst();
        if (existingInfo.isPresent()) {
            return new CoEditionActionInfo(true, Operation.EXISTS, existingInfo.get(),
                    editionInfoRepository.getCurrentEditInfo(existingInfo.get().getDocumentId()));
        }
        return new CoEditionActionInfo(false, Operation.EXISTS, null, null);
    }

    @Override
    public List<CoEditionVO> getAllEditInfo() {
        return editionInfoRepository.getAllEditInfo();
    }

    @Override
    public List<CoEditionVO> getCurrentEditInfo(String docId) {
        return editionInfoRepository.getCurrentEditInfo(docId);
    }

}
