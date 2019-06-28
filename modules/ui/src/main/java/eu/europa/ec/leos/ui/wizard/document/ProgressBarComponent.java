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
package eu.europa.ec.leos.ui.wizard.document;

import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Upload;

class ProgressBarComponent extends ProgressBar implements Upload.ProgressListener, Upload.StartedListener {

    private static final long serialVersionUID = 6601694410770223043L;

    ProgressBarComponent() {
        super();
        this.setVisible(false);
    }

    @Override
    public void attach() {
        super.attach();
    }

    @Override
    public void detach() {
        super.detach();
    }

    @Override
    public void updateProgress(final long readBytes, final long contentLength) {
        this.setValue(readBytes / (float) contentLength);

    }

    @Override
    public void uploadStarted(final Upload.StartedEvent event) {
        this.setValue(0f);
        this.setVisible(true);
    }
}
