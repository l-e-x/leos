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
package eu.europa.ec.leos.web.ui.component;

import eu.europa.ec.leos.domain.vo.DocumentVO;

public class MoveAnnexEvent {
    public enum Direction {
        UP,
        DOWN
    }

    private Direction direction;
    private DocumentVO annexVo;

    public MoveAnnexEvent(DocumentVO annexVo, Direction direction) {
        this.direction = direction;
        this.annexVo = annexVo;
    }

    public Direction getDirection() {
        return direction;
    }

    public DocumentVO getAnnexVo() {
        return annexVo;
    }
}
