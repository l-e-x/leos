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
package eu.europa.ec.leos.ui.window.toc;

import com.vaadin.shared.ui.grid.GridClientRpc;
import com.vaadin.shared.ui.grid.ScrollDestination;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.components.grid.GridSelectionModel;

public class MultiSelectTreeGrid<T> extends TreeGrid<T> {
    private static final long serialVersionUID = 1462333925534793763L;

    @Override
    public void setSelectionModel(GridSelectionModel<T> model) {
     super.setSelectionModel(model);
    }
    
    
    @Override
    public void scrollTo(int row) throws IllegalArgumentException {
        getRpcProxy(GridClientRpc.class).scrollToRow(row, ScrollDestination.ANY);
    }
}
