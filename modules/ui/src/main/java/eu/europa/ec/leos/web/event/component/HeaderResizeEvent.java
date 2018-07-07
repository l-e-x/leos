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

import eu.europa.ec.leos.web.ui.component.layout.Header;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;

public class HeaderResizeEvent {

    private Header.Action action;

    public HeaderResizeEvent(@Nonnull Header.Action action) {
        Validate.notNull(action, "The header resize action must not be null!");
        this.action = action;
    }

    public @Nonnull Header.Action getAction() {
        return action;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HeaderResizeEvent{");
        sb.append("action=").append(action);
        sb.append('}');
        return sb.toString();
    }
}
