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
package eu.europa.ec.leos.services.coedition.repository;

import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class EditionInfoRepositoryImpl implements EditionInfoRepository {

    //Data Structure to store the info
    private Map<String, ArrayList<CoEditionVO>> editInfoMap = new HashMap<>();

    @Override
    public CoEditionVO store(CoEditionVO editionVo ){
        String docId= editionVo.getDocumentId();
        ArrayList<CoEditionVO> editInfoVos = editInfoMap.get(docId) == null ? new ArrayList<>() : editInfoMap.get(docId);
        editInfoMap.put(docId, editInfoVos);
        return editInfoVos.add(editionVo) ? editionVo : null;
    }
    
    @Override
    public CoEditionVO removeInfo(CoEditionVO editionVo){
        String docId= editionVo.getDocumentId();
        boolean removed=false;
        CoEditionVO removedInfo = null;
        ArrayList<CoEditionVO> editInfoVos = editInfoMap.get(docId);

        if (editInfoVos==null)
            return null;

        Iterator<CoEditionVO> iterator = editInfoVos.iterator();
        while (iterator.hasNext()) {
            CoEditionVO exisingInfo = iterator.next();
            if(exisingInfo.equals(editionVo)){
                removedInfo=exisingInfo;
                iterator.remove();
                removed=true;
            }
        }
        if(editInfoVos.isEmpty()){//clean up of map 
            editInfoMap.remove(docId);
        }
        return removed ? removedInfo : null;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public  List<CoEditionVO> getCurrentEditInfo(String docId) {
        return  Collections.unmodifiableList(editInfoMap.get(docId) == null
                ? new ArrayList<>()
                        : (List<CoEditionVO>) editInfoMap.get(docId).clone());
    }

    @Override
    public List<CoEditionVO> getAllEditInfo() {
        Collection<ArrayList<CoEditionVO>> editInfoList = editInfoMap.values();
        ArrayList<CoEditionVO> allInfo = new ArrayList<>();
        for (ArrayList<CoEditionVO> arrEditInfo : editInfoList) {
            allInfo.addAll(arrEditInfo);
        }
        return Collections.unmodifiableList(allInfo);
    }
}
