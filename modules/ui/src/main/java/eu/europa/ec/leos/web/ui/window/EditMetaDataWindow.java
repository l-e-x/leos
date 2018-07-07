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
package eu.europa.ec.leos.web.ui.window;

import com.google.common.eventbus.EventBus;

import eu.europa.ec.leos.vo.MetaDataVO;
import eu.europa.ec.leos.web.event.window.CloseMetadataEditorEvent;
import eu.europa.ec.leos.web.event.window.SaveMetaDataRequestEvent;
import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.MetaDataFormComponent;

public class EditMetaDataWindow extends AbstractEditWindow {

    private static final long serialVersionUID = -240120852946095116L;

    private MetaDataFormComponent metaDataFormComponent;

    public EditMetaDataWindow(MessageHelper messageHelper, LanguageHelper languageHelper, EventBus eventBus, MetaDataVO metaDataVO) {
        super(messageHelper, eventBus);

        setWidth(650, Unit.PIXELS);//same as create doc wiz size
        setHeight(450, Unit.PIXELS);
        setCaption(messageHelper.getMessage("window.metadata.edit.caption"));

        metaDataFormComponent = new MetaDataFormComponent(messageHelper, languageHelper, metaDataVO);
        setBodyComponent(metaDataFormComponent);
    }

    @Override
    protected void onSave() {
        if (metaDataFormComponent.isValid()) {
            eventBus.post(new SaveMetaDataRequestEvent(metaDataFormComponent.getMetaDataValues()));
        }
    }
    
    @Override 
    public void close() {
	   	super.close();
	    eventBus.post(new CloseMetadataEditorEvent());
    }

}
