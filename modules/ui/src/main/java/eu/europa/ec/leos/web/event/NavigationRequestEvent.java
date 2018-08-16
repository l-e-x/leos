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
package eu.europa.ec.leos.web.event;

import java.util.Arrays;

import javax.annotation.Nonnull;

import eu.europa.ec.leos.web.ui.navigation.Target;
import org.apache.commons.lang3.Validate;

public class NavigationRequestEvent {

    private Target target;
    private String[] parameters;

    public NavigationRequestEvent(@Nonnull Target target, String... parameters) {
        Validate.notNull(target, "The navigation target must not be null!");

        this.target = target;
        this.parameters = parameters;
    }

    public @Nonnull Target getTarget() {
        return target;
    }

    public String[] getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return String.format("NavigationRequestEvent{target='%s', parameters=%s}", target, Arrays.toString(parameters));
    }
}
