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
package eu.europa.ec.leos.ui.extension.dndscroll;

import com.vaadin.shared.ui.grid.DropMode;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.components.grid.TreeGridDropTarget;

public class TreeGridScrollDropTargetExtension<T> extends TreeGridDropTarget<T> {
    private static final long serialVersionUID = -4576985340387021372L;

    public TreeGridScrollDropTargetExtension(TreeGrid<T> target, DropMode dropMode) {
        super(target, dropMode);
    }
}
