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
package eu.europa.ec.leos.web.ui.component;

import com.google.common.eventbus.EventBus;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.server.BrowserWindowOpener;
import com.vaadin.server.ClientConnector;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockLevel;
import eu.europa.ec.leos.web.event.component.SplitPositionEvent;
import eu.europa.ec.leos.web.event.view.document.DocumentUpdatedEvent;
import eu.europa.ec.leos.web.event.view.document.LegalTextCommentToggleEvent;
import eu.europa.ec.leos.web.event.view.document.RefreshDocumentEvent;
import eu.europa.ec.leos.web.event.window.ShowVersionsEvent;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.extension.*;
import eu.europa.ec.leos.web.ui.screen.ViewSettings;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.teemu.VaadinIcons;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@StyleSheet({"theme://css/bill_xml.css" + LeosCacheToken.TOKEN})
public class LegalTextComponent extends CustomComponent {
    private static final long serialVersionUID = -7268025691934327898L;
    private static final Logger LOG = LoggerFactory.getLogger(LegalTextComponent.class);
    private static final String LEOS_RELATIVE_FULL_WDT = "100%";

    private EventBus eventBus;
    private MessageHelper messageHelper;
    private ViewSettings viewSettings;
    private Label docContent;
    private Button textRefreshNote;
    private BrowserWindowOpener htmlPreviewOpener;
    private BrowserWindowOpener pdfPreviewOpener;

    @SuppressWarnings("serial")
    public LegalTextComponent(final EventBus eventBus, final MessageHelper messageHelper, ViewSettings viewSettings) {

        this.eventBus = eventBus;
        this.messageHelper = messageHelper;
        this.viewSettings = viewSettings;

        setSizeFull();
        VerticalLayout legalTextlayout = new VerticalLayout();
        legalTextlayout.setSizeFull();
        // create toolbar
        legalTextlayout.addComponent(buildLegalTextToolbar());

        // create content
        final Component textContent = buildLegalTextContent();
        legalTextlayout.addComponent(textContent);
        legalTextlayout.setExpandRatio(textContent, 1.0f);
        setCompositionRoot(legalTextlayout);
    }

    private Component buildLegalTextToolbar() {
        LOG.debug("Building Legal Text toolbar...");

        // create text toolbar layout
        final HorizontalLayout toolsLayout = new HorizontalLayout();
        toolsLayout.setId("legalTextToolbar");
        toolsLayout.setStyleName("leos-viewdoc-docbar");
        toolsLayout.setSpacing(true);

        // set toolbar style
        toolsLayout.setWidth(LEOS_RELATIVE_FULL_WDT);
        toolsLayout.addStyleName("leos-viewdoc-padbar-both");

        // create legal text slider button
        final Button legalTextRightSlideButton = legalTextRightSliderButton();
        toolsLayout.addComponent(legalTextRightSlideButton);
        toolsLayout.setComponentAlignment(legalTextRightSlideButton, Alignment.MIDDLE_LEFT);

        // create preview buttons
        if (viewSettings.isPreviewEnabled()) {
            final Button previewHtmlButton = legalTextPreviewHtmlButton();
            toolsLayout.addComponent(previewHtmlButton);
            toolsLayout.setComponentAlignment(previewHtmlButton, Alignment.MIDDLE_LEFT);
            final Button previewPdfButton = legalTextPreviewPdfButton();
            toolsLayout.addComponent(previewPdfButton);
            toolsLayout.setComponentAlignment(previewPdfButton, Alignment.MIDDLE_LEFT);
        }

        // add compare button
        if (viewSettings.isCompareEnabled()) {
            final Button compareVersionsButton = buildVersionCompareButton();
            toolsLayout.addComponent(compareVersionsButton);
            toolsLayout.setComponentAlignment(compareVersionsButton, Alignment.MIDDLE_LEFT);
        }
        // create legal text slider button
        final Button legalTextLeftSlideButton = legalTextLeftSliderButton();

        // create toolbar spacer label to push components to the sides
        final Label toolbarLabel = new Label("&nbsp;", ContentMode.HTML);
        toolbarLabel.setSizeUndefined();
        toolsLayout.addComponent(toolbarLabel);
        toolsLayout.setComponentAlignment(toolbarLabel, Alignment.MIDDLE_LEFT);
        // toolbar spacer label will use all available space
        toolsLayout.setExpandRatio(toolbarLabel, 1.0f);

        // create blank Note Button
        textRefreshNote = legalTextNote();
        toolsLayout.addComponent(textRefreshNote);
        toolsLayout.setComponentAlignment(textRefreshNote, Alignment.MIDDLE_RIGHT);

        if(!viewSettings.isSideCommentsEnabled()) {
            // create comments button
            final Button commentsButton = legalTextCommentsButton();
            toolsLayout.addComponent(commentsButton);
            toolsLayout.setComponentAlignment(commentsButton, Alignment.MIDDLE_RIGHT);
        }

        // create text refresh button
        final Button textRefreshButton = legalTextRefreshButton();
        toolsLayout.addComponent(textRefreshButton);
        toolsLayout.setComponentAlignment(textRefreshButton, Alignment.MIDDLE_RIGHT);

        // add toc expand button last
        toolsLayout.addComponent(legalTextLeftSlideButton);
        toolsLayout.setComponentAlignment(legalTextLeftSlideButton, Alignment.MIDDLE_RIGHT);

        return toolsLayout;
    }

    // create legal text slider button
    private Button legalTextLeftSliderButton() {
        VaadinIcons legalTextSliderIcon = VaadinIcons.CARET_SQUARE_LEFT_O;

        final Button legalTextSliderButton = new Button();
        legalTextSliderButton.setIcon(legalTextSliderIcon);
        legalTextSliderButton.setData(SplitPositionEvent.MoveDirection.LEFT);
        legalTextSliderButton.setStyleName("link");
        legalTextSliderButton.addStyleName("leos-toolbar-button");
        legalTextSliderButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                eventBus.post(new SplitPositionEvent((SplitPositionEvent.MoveDirection) event.getButton().getData(), LegalTextComponent.this));
            }
        });

        return legalTextSliderButton;
    }

    // create legal text slider button
    private Button legalTextRightSliderButton() {
        VaadinIcons legalTextSliderIcon = VaadinIcons.CARET_SQUARE_RIGHT_O;

        final Button legalTextSliderButton = new Button();
        legalTextSliderButton.setIcon(legalTextSliderIcon);
        legalTextSliderButton.setData(SplitPositionEvent.MoveDirection.RIGHT);
        legalTextSliderButton.setStyleName("link");
        legalTextSliderButton.addStyleName("leos-toolbar-button");
        legalTextSliderButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                eventBus.post(new SplitPositionEvent((SplitPositionEvent.MoveDirection) event.getButton().getData(), LegalTextComponent.this));
            }
        });

        return legalTextSliderButton;
    }

    // Create preview PDF button
    private Button legalTextPreviewPdfButton() {
        final Button previewPDFButton = new Button();
        previewPDFButton.setId("previewPDF");
        previewPDFButton.setDescription(messageHelper.getMessage("document.tab.legal.button.pdfPreview.tooltip"));

        previewPDFButton.setIcon(FontAwesome.FILE_PDF_O);
        previewPDFButton.setStyleName("link");
        previewPDFButton.addStyleName("leos-toolbar-button");

        BrowserWindowOpener pdfPrevOpener = new BrowserWindowOpener("");
        pdfPrevOpener.setFeatures(LeosTheme.LEOS_PREVIEW_WINDOW_FEATURES);
        pdfPrevOpener.extend(previewPDFButton);
        setPdfPreviewOpener(pdfPrevOpener);

        return previewPDFButton;
    }

    // Create preview HTML button
    private Button legalTextPreviewHtmlButton() {
        final Button previewHTMLButton = new Button();
        previewHTMLButton.setId("previewHTML");
        previewHTMLButton.setDescription(messageHelper.getMessage("document.tab.legal.button.htmlPreview.tooltip"));

        previewHTMLButton.setIcon(FontAwesome.FILE_CODE_O);
        previewHTMLButton.setStyleName("link");
        previewHTMLButton.addStyleName("leos-toolbar-button");

        BrowserWindowOpener htmlPrevOpener = new BrowserWindowOpener("");
        htmlPrevOpener.setFeatures(LeosTheme.LEOS_PREVIEW_WINDOW_FEATURES);
        htmlPrevOpener.extend(previewHTMLButton);
        setHtmlPreviewOpener(htmlPrevOpener);

        return previewHTMLButton;
    }

    // create text refresh button
    private Button legalTextRefreshButton() {
        final Button textRefreshButton = new Button();
        textRefreshButton.setId("refreshDocument");
        textRefreshButton.setHtmlContentAllowed(true);
        textRefreshButton.setIcon(VaadinIcons.REFRESH);
        textRefreshButton.setStyleName("link");
        textRefreshButton.addStyleName("leos-dimmed-button");
        textRefreshButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 3714441703159576377L;

            @Override
            public void buttonClick(ClickEvent event) {
                eventBus.post(new RefreshDocumentEvent());
                eventBus.post(new DocumentUpdatedEvent()); //Document might be updated.
            }
        });

        return textRefreshButton;
    }

    // create comments button
    private Button legalTextCommentsButton() {
        final Button commentsButton = new Button();
        commentsButton.setId("documentComments");
        commentsButton.setDescription(messageHelper.getMessage("document.tab.legal.button.comments.tip.show"));
        commentsButton.setIcon(VaadinIcons.COMMENT_ELLIPSIS_O);
        commentsButton.setStyleName("link");
        commentsButton.setData(false);
        commentsButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                commentsButton.setData(! (Boolean)commentsButton.getData());
                if((Boolean)commentsButton.getData()) {
                    commentsButton.setDescription(messageHelper.getMessage("document.tab.legal.button.comments.tip.hide"));
                    commentsButton.setIcon(VaadinIcons.COMMENT_ELLIPSIS);
                }
                else{
                    commentsButton.setDescription(messageHelper.getMessage("document.tab.legal.button.comments.tip.show"));
                    commentsButton.setIcon(VaadinIcons.COMMENT_ELLIPSIS_O);
                }
                eventBus.post(new LegalTextCommentToggleEvent());
            }
        });

        return commentsButton;
    }

    // create text refresh button
    private Button legalTextNote() {
        final Button textRefreshNote = new Button();
        textRefreshNote.setId("refreshDocumentNote");
        textRefreshNote.setHtmlContentAllowed(true);
        textRefreshNote.setStyleName("link");
        textRefreshNote.addStyleName("leos-toolbar-button");
        textRefreshNote.setCaption(messageHelper.getMessage("document.request.refresh.msg"));
        textRefreshNote.setIcon(LeosTheme.LEOS_INFO_YELLOW_16);
        textRefreshNote.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 3714441703159576377L;

            @Override
            public void buttonClick(ClickEvent event) {
                eventBus.post(new RefreshDocumentEvent());
                eventBus.post(new DocumentUpdatedEvent()); //Document might be updated.
            }
        });
        return textRefreshNote;
    }

    private Button buildVersionCompareButton() {
        // create version diff button
        final Button diffButton = new Button();
        diffButton.setId("compareDocVersionButton");
        diffButton.setDescription(messageHelper.getMessage("document.tab.legal.button.versioncompare.tooltip"));
        diffButton.setIcon(VaadinIcons.COPY_O);
        diffButton.setStyleName("link");
        diffButton.addStyleName("leos-toolbar-button");
        diffButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent clickEvent) {
                LOG.debug("compare Versions Button clicked...");
                eventBus.post(new ShowVersionsEvent());
            }
        });

        return diffButton;
    }

    private Component buildLegalTextContent() {
        LOG.debug("Building Legal Text content...");

        // create placeholder to display Legal Text content
        docContent = new Label();
        docContent.setContentMode(ContentMode.HTML);
        docContent.setSizeFull();
        docContent.setStyleName("leos-viewdoc-content");
        docContent.setId("leos-doc-content");

        MathJaxExtension mathJax = new MathJaxExtension();
        mathJax.extend(docContent);

        SliderPinsExtension spe = new SliderPinsExtension(eventBus, getSelectorStyleMap());
        spe.extend(docContent);
        
        LeosEditorExtension leosEditor = new LeosEditorExtension(eventBus);
        leosEditor.extend(docContent);

        if (viewSettings.isSideCommentsEnabled()) {
            SideCommentsExtension sideComments = new SideCommentsExtension(eventBus);
            sideComments.extend(docContent);
            
            SuggestionExtension suggestions = new SuggestionExtension(eventBus);
            suggestions.extend(docContent);
        }
        else{
            LegalTextCommentsExtension ltce= new LegalTextCommentsExtension(eventBus);
            ltce.extend(docContent);

            ActionManagerExtension actionManager = new ActionManagerExtension();
            actionManager.extend(docContent);
        }
        
        return docContent;
    }
    private Map<String, String> getSelectorStyleMap(){
        Map<String, String> selectorStyleMap = new HashMap<>();
        selectorStyleMap.put("popup","pin-popup-side-bar");
        return selectorStyleMap;
    }

    public void populateContent(String docContentText) {
        /* KLUGE: In order to force the update of the docContent on the client side
         * the unique seed is added on every docContent update, please note markDirty
         * method did not work, this was the only solution worked.*/
        String seed = "<div style='display:none' >" +
                new Date().getTime() +
                "</div>";
        docContent.setValue(docContentText + seed);
        textRefreshNote.setVisible(false);
    }

    public void setDocumentPreviewURLs(String documentId, String pdfURL, String htmlURL) {
        ClientConnector buttonPdfConnector = null;
        ClientConnector buttonHtmlConnector = null;

        BrowserWindowOpener pdfOpener = this.getPdfPreviewOpener();
        if (pdfOpener != null) {
            buttonPdfConnector = pdfOpener.getParent();
            buttonPdfConnector.removeExtension(pdfOpener);
        }
        pdfOpener = new BrowserWindowOpener(pdfURL);
        pdfOpener.setFeatures(LeosTheme.LEOS_PREVIEW_WINDOW_FEATURES);
        pdfOpener.setWindowName("PreviewPDF_" + documentId);
        pdfOpener.extend((Button) buttonPdfConnector);
        this.setPdfPreviewOpener(pdfOpener);

        BrowserWindowOpener htmlOpener = this.getHtmlPreviewOpener();
        if (htmlOpener != null) {
            buttonHtmlConnector = htmlOpener.getParent();
            buttonHtmlConnector.removeExtension(htmlOpener);
        }
        htmlOpener = new BrowserWindowOpener(htmlURL);
        htmlOpener.setFeatures(LeosTheme.LEOS_PREVIEW_WINDOW_FEATURES);
        htmlOpener.setWindowName("PreviewHTML_" + documentId);
        htmlOpener.extend((Button) buttonHtmlConnector);
        this.setHtmlPreviewOpener(htmlOpener);
    }

    protected BrowserWindowOpener getHtmlPreviewOpener() {
        return htmlPreviewOpener;
    }

    protected void setHtmlPreviewOpener(BrowserWindowOpener htmlPreviewOpener) {
        this.htmlPreviewOpener = htmlPreviewOpener;
    }

    protected BrowserWindowOpener getPdfPreviewOpener() {
        return pdfPreviewOpener;
    }

    protected void setPdfPreviewOpener(BrowserWindowOpener pdfPreviewOpener) {
        this.pdfPreviewOpener = pdfPreviewOpener;
    }

    public void updateLocks(LockActionInfo lockActionInfo) {
        // update the bar with message to refresh for all other user except the usr who did the update
        if (!VaadinSession.getCurrent().getSession().getId().equals(lockActionInfo.getLock().getSessionId())) {
            if (LockActionInfo.Operation.RELEASE.equals(lockActionInfo.getOperation()) &&
                    (LockLevel.DOCUMENT_LOCK.equals(lockActionInfo.getLock().getLockLevel()) ||
                            LockLevel.ELEMENT_LOCK.equals(lockActionInfo.getLock().getLockLevel()))) {
                textRefreshNote.setVisible(true);
            }
        }
    }
}