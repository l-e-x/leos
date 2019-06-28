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
package eu.europa.ec.leos.vo.coedition;

import java.util.List;

public class CoEditionActionInfo {

    public static enum Operation {
        STORE("store"),
        REMOVE("remove"),
        EXISTS("exists");
        
        private final String value;
        
        Operation(String value) {
            this.value=value;
        }
        public String getValue(){
            return value;
        }
    }
    
    private final boolean isSucessful;
    private final Operation operation;
    
    private final CoEditionVO coEditionVo;
    private final List<CoEditionVO> coEditionVos;//current/remaining list of locks after operation.
    
    public CoEditionActionInfo(boolean isSucessful, Operation operation, CoEditionVO coEditionVo, List<CoEditionVO> coEditionVos) {
        this.isSucessful = isSucessful;
        this.operation=operation;
        this.coEditionVo = coEditionVo;
        this.coEditionVos = coEditionVos;
    }

    public boolean sucesss() {
        return isSucessful;
    }

    public Operation getOperation() {
        return operation;
    }
    
    public CoEditionVO getInfo() {
        return coEditionVo;
    }
    
    public List<CoEditionVO> getCoEditionVos() {
        return coEditionVos;
    }
}
