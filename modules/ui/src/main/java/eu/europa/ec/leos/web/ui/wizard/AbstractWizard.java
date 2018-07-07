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
package eu.europa.ec.leos.web.ui.wizard;

import eu.europa.ec.leos.web.support.i18n.MessageHelper;

import com.google.common.eventbus.EventBus;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.server.ErrorMessage;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import eu.europa.ec.leos.web.ui.window.AbstractWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWizard extends AbstractWindow {

    private static final long serialVersionUID = -2378510368394982905L;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractWizard.class);

    private static final String DEFAULT_WIZARD_CAPTION_FORMAT = "%s - %s (%d/%d)";

    private VerticalLayout wizardBodyLayout;
    private Label stepDescriptionLabel;
    private int currentStep;

    private Button previousButton;
    private Button nextButton;
    private Button finishButton;

    private List<WizardStep> stepList = new ArrayList<WizardStep>();

    public AbstractWizard(MessageHelper messageHelper, EventBus eventBus) {
        super(messageHelper, eventBus);
        LOG.trace("Initializing wizard...");
        // set default window size
        setWidth("650px");
        setHeight("450px");

        buildWizardBody();
        setBodyComponent(wizardBodyLayout);
        addWizardButtons();
    }

    protected void registerWizardStep(WizardStep step) {
        if (step != null) {
            if (!stepList.contains(step)) {
                stepList.add(step);
                LOG.trace("Registered wizard step: {}={}", stepList.indexOf(step), step.getStepTitle());
            } else {
                LOG.trace("Wizard step already registered: {}={}", stepList.indexOf(step), step.getStepTitle());
            }
        } else {
            LOG.trace("Ignored the registration of a NULL wizard step!");
        }
    }

    protected void clearStepList() {
        stepList.clear();
    }

    protected void setWizardStep(int newStep) {

        // reset step index
        LOG.trace("Wizard has {} steps: currentStep={} moving to step={}", stepList.size(), currentStep, newStep);

        // reset wizard body
        wizardBodyLayout.removeAllComponents();
        if (newStep >= 0) {
            WizardStep step = stepList.get(newStep);
            LOG.trace("Setting wizard step: {}={}", newStep, step.getStepTitle());
            Component comp = step.getComponent();

            wizardBodyLayout.addComponent(comp);
            wizardBodyLayout.setExpandRatio(comp, 1);
            wizardBodyLayout.addComponent(buildStepDescription());
            // set wizard caption
            String wizardCaption = String.format(DEFAULT_WIZARD_CAPTION_FORMAT, getWizardTitle(), step.getStepTitle(), newStep + 1, stepList.size());
            LOG.trace("Setting wizard caption: {}", wizardCaption);
            setCaption(wizardCaption);

            // set wizard step description
            String stepDesc = String.format(messageHelper.getMessage("wizard.step.desc"), newStep + 1, stepList.size(), step.getStepDescription());
            LOG.trace("Setting step description: {}", stepDesc);
            stepDescriptionLabel.setValue(stepDesc);
        }
        currentStep = newStep;
        updateButtonStates();
        clearButtonErrors();
    }

    protected abstract String getWizardTitle();

    protected boolean hasCloseButton() {
        return false;
    }

    private Component buildWizardBody() {
        // create body layout
        wizardBodyLayout = new VerticalLayout();

        wizardBodyLayout.setMargin(false);
        wizardBodyLayout.setSpacing(false);
        wizardBodyLayout.setSizeFull();

        return wizardBodyLayout;
    }

    private void addWizardButtons() {

        addButton(buildCancelButton());
        addButton(buildFinishButton());
        addButton(buildForwardButton());
        addButton(buildBackwardButton());

    }

    private HorizontalLayout buildStepDescription() {
        // build the step description
        HorizontalLayout stepDescriptionLayout = new HorizontalLayout();

        // set margins and spacing
        stepDescriptionLayout.setMargin(new MarginInfo(false, true, false, true));
        stepDescriptionLayout.setSpacing(true);

        // layout will use all available space
        stepDescriptionLayout.setWidth(100, Unit.PERCENTAGE);

        // step description label
        Component stepDescLabel = buildStepDescriptionLabel();
        stepDescriptionLayout.addComponent(stepDescLabel);
        stepDescriptionLayout.setComponentAlignment(stepDescLabel, Alignment.MIDDLE_LEFT);

        // step description label will expand and take available extra space
        stepDescriptionLayout.setExpandRatio(stepDescLabel, 1.0f);
        return stepDescriptionLayout;
    }

    private Component buildStepDescriptionLabel() {
        stepDescriptionLabel = new Label();
        stepDescriptionLabel.setContentMode(ContentMode.HTML);
        return stepDescriptionLabel;
    }

    private Button buildCancelButton() {
        Button button = new Button();
        button.setCaption(messageHelper.getMessage("leos.button.cancel"));
        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                LOG.trace("Wizard cancel button was clicked!");
                close();
            }
        });
        return button;
    }

    private Button buildButton(final boolean forward, String caption) {
        final Button button = new Button();
        button.setCaption(caption);
        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                clearButtonErrors();
                if (forward) {
                    LOG.trace("Wizard forward button was clicked!");
                    if (stepList.get(currentStep).validateState()) {
                        setWizardStep(currentStep + 1);
                    } else {
                        button.setComponentError(new UserError(messageHelper.getMessage("wizard.error"), AbstractErrorMessage.ContentMode.TEXT,
                                ErrorMessage.ErrorLevel.WARNING));
                    }
                } else {
                    LOG.trace("Wizard backward button was clicked!");
                    setWizardStep(currentStep - 1);
                }
            }
        });
        button.setEnabled(false);
        return button;
    }

    private Button buildBackwardButton() {
        previousButton = buildButton(false, messageHelper.getMessage("leos.button.previous"));
        return previousButton;
    }

    private Button buildForwardButton() {
        nextButton = buildButton(true, messageHelper.getMessage("leos.button.next"));
        return nextButton;
    }

    private Button buildFinishButton() {
        finishButton = new Button();
        finishButton.setCaption(messageHelper.getMessage("leos.button.finish"));
        finishButton.addStyleName("primary");
        finishButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                LOG.trace("Wizard finish button was clicked!");
                clearButtonErrors();
                if (stepList.get(currentStep).validateState()) {
                    boolean isSuccess = handleFinishAction(stepList);
                    if (isSuccess) {
                        close();
                    } else {
                        finishButton.setComponentError(new UserError(messageHelper.getMessage("wizard.error.finish"),
                                AbstractErrorMessage.ContentMode.TEXT, ErrorMessage.ErrorLevel.WARNING));
                    }
                } else {
                    finishButton.setComponentError(new UserError(messageHelper.getMessage("wizard.error"), AbstractErrorMessage.ContentMode.TEXT,
                            ErrorMessage.ErrorLevel.WARNING));
                }
            }
        });
        finishButton.setEnabled(false);
        return finishButton;
    }

    private void updateButtonStates() {
        previousButton.setEnabled(stepList.size() > 0 && currentStep > 0);
        nextButton.setEnabled(stepList.size() > 0 && (currentStep + 1) < stepList.size());
        finishButton.setEnabled(stepList.get(currentStep).canFinish());
    }

    private void clearButtonErrors() {
        previousButton.setComponentError(null);
        nextButton.setComponentError(null);
        finishButton.setComponentError(null);
    }

    /**
     * @param stepList
     * @return true if the finish action was successful, false otherwise
     */
    protected abstract boolean handleFinishAction(List<WizardStep> stepList);
}
