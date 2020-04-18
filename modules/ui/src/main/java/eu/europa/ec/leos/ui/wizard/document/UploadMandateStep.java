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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;

import eu.europa.ec.leos.domain.vo.ValidationVO;
import eu.europa.ec.leos.ui.wizard.ErrorResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.ErrorVO;
import eu.europa.ec.leos.ui.wizard.WizardStep;
import eu.europa.ec.leos.web.event.view.repository.*;
import eu.europa.ec.leos.i18n.MessageHelper;

class UploadMandateStep extends CustomComponent implements WizardStep, Upload.StartedListener,
        Upload.FailedListener, Upload.SucceededListener,
        Upload.FinishedListener, Upload.ChangeListener {

    private static final long serialVersionUID = 6601694410770223043L;
    private static final Logger LOG = LoggerFactory.getLogger(UploadMandateStep.class);

    private VerticalLayout mainLayout;
    private FormLayout resultUploadLayout;

    private MessageHelper messageHelper;
    private EventBus eventBus;
    private final DocumentVO document;
    private int originalPollInterval = -1;
    // Uploader
    private Upload upload;
    private Label fileName;
    private Label fileValid;
    
    private File file;

    // mime types accepted separated by ","
    private static final String MIME_TYPES_ALLOWED = "application/octet-stream";
    // extension types accepted separated by ,
    private static final String EXT_TYPES_ALLOWED = ".leg";

    UploadMandateStep(DocumentVO document, MessageHelper messageHelper, EventBus eventBus) {
        this.eventBus = eventBus;
        this.document = document;
        this.messageHelper = messageHelper;
        initLayout();
        buildUploadContent();
        buildResultUploadContent();
    }

    @Override
    public void attach() {
        super.attach();
        eventBus.register(this);
        LOG.trace("UploadDocumentStep attached.");
        originalPollInterval = getUI().getPollInterval();
        getUI().setPollInterval(1000);
    }

    @Override
    public void detach() {
        getUI().setPollInterval(originalPollInterval);
        super.detach();
        eventBus.unregister(this);
        LOG.trace("UploadDocumentStep detached.");
    }

    @Override
    public String getStepTitle() {
        return messageHelper.getMessage("wizard.document.upload.template.title");
    }

    @Override
    public String getStepDescription() {
        return messageHelper.getMessage("wizard.document.upload.template.desc");
    }

    @Override
    public Component getComponent() {
        return this;
    }

    private void initLayout() {
        setSizeFull();
        mainLayout = new VerticalLayout();
        setCompositionRoot(mainLayout);
        mainLayout.setSizeFull();
        mainLayout.setMargin(new MarginInfo(true, true, false, true));
        mainLayout.setSpacing(true);
        mainLayout.setStyleName("upload-wizard");
    }

    private void buildUploadContent() {
        HorizontalLayout uploadLayout = new HorizontalLayout();
        uploadLayout.setMargin(false);

        Label fileSelect = new Label();
        fileSelect.setCaption(messageHelper.getMessage("wizard.document.upload.caption"));
        fileSelect.setCaptionAsHtml(true);
        uploadLayout.addComponent(fileSelect);

        upload = new Upload();
        upload.setReceiver(
                new Upload.Receiver() {
                    @Override
                    public OutputStream receiveUpload(String filename,
                            String mimeType) {
                        // Create and return a file output stream
                        OutputStream outputFile;
                        try {
                            file = File.createTempFile(filename,
                                    filename.contains(".") ? filename.substring(filename.lastIndexOf("."), filename.length()) : "");
                            if (!file.exists()) {
                                file.createNewFile();
                            }
                            outputFile = new FileOutputStream(file);
                        } catch (IOException e) {
                            LOG.error("Error uploading file", e);
                            return null;
                        }
                        return outputFile;
                    }
                }

        );
        upload.addChangeListener(this);
        upload.addStartedListener(this);
        upload.addFailedListener(this);
        upload.addFinishedListener(this);
        upload.addSucceededListener(this);
        upload.setImmediateMode(true);
        JavaScript.getCurrent().execute("document.querySelectorAll('.upload-wizard .gwt-FileUpload')[0].setAttribute('accept', '" + EXT_TYPES_ALLOWED + "')");
        uploadLayout.addComponent(upload);

        ProgressBarComponent progressBar = new ProgressBarComponent();
        upload.addProgressListener(progressBar);
        upload.addStartedListener(progressBar);
        uploadLayout.addComponent(progressBar);
        uploadLayout.setComponentAlignment(progressBar, Alignment.MIDDLE_CENTER);

        mainLayout.addComponent(uploadLayout);
        mainLayout.setExpandRatio(uploadLayout, 0.2f);
    }

    private void buildResultUploadContent() {
        Panel resultUploadPanel = new Panel();
        resultUploadPanel.setSizeFull();

        resultUploadLayout = new FormLayout();
        resultUploadLayout.setMargin(true);
        resultUploadLayout.setVisible(false);

        fileName = new Label();
        fileName.setCaption(messageHelper.getMessage("wizard.document.upload.fileName.caption"));
        fileName.setWidth(100, Unit.PERCENTAGE);
        resultUploadLayout.addComponent(fileName);

        fileValid = new Label();
        fileValid.setCaption(messageHelper.getMessage("wizard.document.upload.valid.caption"));
        fileValid.setContentMode(ContentMode.HTML);
        resultUploadLayout.addComponent(fileValid);

        resultUploadPanel.setContent(resultUploadLayout);
        resultUploadPanel.getContent().setSizeUndefined();
        mainLayout.addComponent(resultUploadPanel);
        mainLayout.setExpandRatio(resultUploadPanel, 0.8f);
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public void filenameChanged(Upload.ChangeEvent event) {
    }

    @Override
    public void uploadStarted(final Upload.StartedEvent event) {
        String contentType = event.getMIMEType();
        String extensionType = event.getFilename().contains(".")
                ? event.getFilename().substring(event.getFilename().lastIndexOf("."), event.getFilename().length())
                : "";
        if (Stream.of(MIME_TYPES_ALLOWED).noneMatch(x -> x.equalsIgnoreCase(contentType)) ||
                Stream.of(EXT_TYPES_ALLOWED).noneMatch(x -> x.equalsIgnoreCase(extensionType))) {
            StringBuilder errors = new StringBuilder();
            errors.append(VaadinIcons.CLOSE.getHtml());
            errors.append(messageHelper.getMessage("wizard.document.upload.error.type", EXT_TYPES_ALLOWED));
            fileValid.setValue(errors.toString());
            fileValid.addStyleName("file-invalid");
            resultUploadLayout.setVisible(true);
            upload.interruptUpload();
        } else {
            resultUploadLayout.setVisible(false);
        }
    }

    @Override
    public void uploadSucceeded(final Upload.SucceededEvent event) {
        fileName.setValue(event.getFilename());
        getProposalFromFile(file, document);
        postProcessingMandate(document);
        validateProposal(document);
    }

    @Override
    public void uploadFailed(final Upload.FailedEvent event) {
        fileName.setValue(messageHelper.getMessage("wizard.document.upload.error.file"));
    }

    @Override
    public void uploadFinished(final Upload.FinishedEvent event) {
        resultUploadLayout.setVisible(true);
    }

    private void postProcessingMandate(DocumentVO document) {
        eventBus.post(new PostProcessingMandateEvent(document));
    }

    private void validateProposal(DocumentVO document) {
        eventBus.post(new ValidateProposalEvent(document));
    }

    @Subscribe
    void showPostProcessingResult(ShowPostProcessingMandateEvent event) {
        Result<String> postProcessingResult = event.getResult();
        if (postProcessingResult.isError()) {
            fileName.setValue(messageHelper.getMessage("wizard.document.upload.error.post.processing"));
        }
    }

    @Subscribe
    void showValidationResult(ShowProposalValidationEvent event) {
        ValidationVO validationResult = event.getResult();
        if (validationResult.hasErrors()) {
                // procces the errors messages
                StringBuilder errorHtml = new StringBuilder();
                errorHtml.append(VaadinIcons.CLOSE.getHtml());
                for (ErrorVO error : validationResult.getErrors()) {
                    errorHtml.append(messageHelper.getMessage(ErrorResolver.valueOf(error.getErrorCode().name()).getErrorMessageKey(), error.getObjects()));
                }
                fileValid.setValue(errorHtml.toString());
                fileValid.addStyleName("file-invalid");
        } else {
            fileValid.setValue(VaadinIcons.CHECK.getHtml());
            fileValid.addStyleName("file-valid");
        }

    }

    private void getProposalFromFile(File file, final DocumentVO document) {
        eventBus.post(new FetchProposalFromFileEvent(file, document));
    }

    @Override
    public boolean validateState() {
        return fileValid.getValue().equalsIgnoreCase(VaadinIcons.CHECK.getHtml());
    }

}
