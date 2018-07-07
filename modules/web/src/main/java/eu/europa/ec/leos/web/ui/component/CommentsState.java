/**
 * Copyright 2015 European Commission
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
package eu.europa.ec.leos.web.ui.component;

import com.vaadin.shared.ui.JavaScriptComponentState;
import eu.europa.ec.leos.vo.UserVO;
import eu.europa.ec.leos.web.model.SectionVO;

import java.util.List;

public class CommentsState extends JavaScriptComponentState {

    private static final long serialVersionUID = -2459202165954689298L;
    public UserVO currentUser;
    public List<SectionVO> existingComments;
    public boolean ready;
}
