/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.web.event.component;

import com.vaadin.ui.Component;

public class SplitPositionEvent {

    public static enum MoveDirection {
        LEFT,
        RIGHT
    }

    private final MoveDirection moveDirection;
    private final Component originatingComponent;

    public SplitPositionEvent(MoveDirection moveDirection, Component originatingComponent) {
        this.moveDirection = moveDirection;
        this.originatingComponent = originatingComponent;

    }

    public MoveDirection getMoveDirection() {
        return moveDirection;
    }

    public Component getOriginatingComponent() {
        return originatingComponent;
    }

}
