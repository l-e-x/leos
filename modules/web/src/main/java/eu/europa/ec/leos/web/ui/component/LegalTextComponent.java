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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.server.*;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import elemental.json.JsonArray;
import elemental.json.JsonException;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockLevel;
import eu.europa.ec.leos.web.event.component.SplitPositionEvent;
import eu.europa.ec.leos.web.event.component.TocPositionEvent;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.event.window.ShowVersionsEvent;
import eu.europa.ec.leos.web.model.ViewSettings;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.screen.document.TocPosition;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.teemu.VaadinIcons;

@StyleSheet({"vaadin://themes/leos/css/bill_xml.css"+ LeosCacheToken.TOKEN})
@JavaScript({"vaadin://js/web/util/legaltext.js" + LeosCacheToken.TOKEN})
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

    public LegalTextComponent(final EventBus eventBus,final MessageHelper messageHelper, ViewSettings viewSettings) {

        this.eventBus = eventBus;
        this.messageHelper = messageHelper;
        this.viewSettings = viewSettings;
        VerticalLayout legalTextlayout = new VerticalLayout();
        setCompositionRoot(legalTextlayout);

        setSizeFull();
        legalTextlayout.setSizeFull();
        // create toolbar
        legalTextlayout.addComponent(buildLegalTextToolbar());

        // create content
        final Component content = buildLegalTextContent();
        legalTextlayout.addComponent(content);
        // content will expand to use all available space
        legalTextlayout.setExpandRatio(content, 1.0f);
        bindJavaScriptActions();
    }

    private Component buildLegalTextToolbar() {
        LOG.debug("Building Legal Text toolbar...");

        // create text toolbar layout
        final HorizontalLayout toolsLayout = new HorizontalLayout();
        toolsLayout.setStyleName("leos-viewdoc-docbar");
        toolsLayout.setSpacing(true);

        // set toolbar style
        toolsLayout.setWidth(LEOS_RELATIVE_FULL_WDT);
        toolsLayout.addStyleName("leos-viewdoc-padbar-both");


        // create preview buttons
        if(viewSettings.isPreviewEnabled()){
            final Button previewHtmlButton = legalTextPreviewHtmlButton();
            toolsLayout.addComponent(previewHtmlButton);
            toolsLayout.setComponentAlignment(previewHtmlButton, Alignment.MIDDLE_LEFT);
            final Button previewPdfButton = legalTextPreviewPdfButton();
            toolsLayout.addComponent(previewPdfButton);
            toolsLayout.setComponentAlignment(previewPdfButton, Alignment.MIDDLE_LEFT );
        }

        //add compare button
        if(viewSettings.isCompareEnabled()){
            final Button compareVersionsButton = buildVersionCompareButton();
            toolsLayout.addComponent(compareVersionsButton);
            toolsLayout.setComponentAlignment(compareVersionsButton, Alignment.MIDDLE_LEFT );
        }
        // create legal text slider button
        final Button legalTextSlideButton = legalTextSliderButton();

        // create toolbar spacer label to push components to the sides
        final Label toolbarLabel = new Label("&nbsp;", ContentMode.HTML);
        toolbarLabel.setSizeUndefined();
        toolsLayout.addComponent(toolbarLabel);
        toolsLayout.setComponentAlignment(toolbarLabel, Alignment.MIDDLE_LEFT);
        // toolbar spacer label will use all available space
        toolsLayout.setExpandRatio(toolbarLabel, 1.0f);

        //create blank Note Button
        textRefreshNote =legalTextNote();
        toolsLayout.addComponent(textRefreshNote);
        toolsLayout.setComponentAlignment(textRefreshNote, Alignment.MIDDLE_RIGHT);

        // create text refresh button
        final Button textRefreshButton = legalTextRefreshButton();
        toolsLayout.addComponent(textRefreshButton);
        toolsLayout.setComponentAlignment(textRefreshButton, Alignment.MIDDLE_RIGHT);

        // add toc expand button last
        toolsLayout.addComponent(legalTextSlideButton);
        toolsLayout.setComponentAlignment(legalTextSlideButton, Alignment.MIDDLE_RIGHT);

        // Legal text toolbar icon updater
        Object lttIconPositionUpdater = new Object() {
            @Subscribe
            public void updateLegalTextSliderIcon(TocPositionEvent event) {
                toolsLayout.removeComponent(legalTextSlideButton);
                if (event.getTocPosition().equals(TocPosition.RIGHT)) {
                    toolsLayout.addComponent(legalTextSlideButton, 2);
                    toolsLayout.setComponentAlignment(legalTextSlideButton, Alignment.MIDDLE_RIGHT);
                } else {
                    toolsLayout.addComponentAsFirst(legalTextSlideButton);
                    toolsLayout.setComponentAlignment(legalTextSlideButton, Alignment.MIDDLE_LEFT);
                }
            }
        };
        eventBus.register(lttIconPositionUpdater);

        return toolsLayout;
    }

    // create legal text slider button
    private Button legalTextSliderButton() {
        VaadinIcons legalTextSliderIcon = VaadinIcons.CARET_SQUARE_LEFT_O;

        final Button legalTextSliderButton = new Button();
        legalTextSliderButton.setIcon(legalTextSliderIcon);
        legalTextSliderButton.setData(SplitPositionEvent.MoveDirection.LEFT);
        legalTextSliderButton.setStyleName("link");
        legalTextSliderButton.addStyleName("leos-toolbar-button");
        legalTextSliderButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                eventBus.post(new SplitPositionEvent((SplitPositionEvent.MoveDirection) event.getButton().getData()));
            }
        });
        Object tocPositionUpdateHandler = new Object() {
            @Subscribe
            public void updateLegalTextSliderIcon(TocPositionEvent event) {
                if (event.getTocPosition().equals(TocPosition.RIGHT)) {
                    legalTextSliderButton.setIcon(VaadinIcons.CARET_SQUARE_LEFT_O);
                    legalTextSliderButton.setData(SplitPositionEvent.MoveDirection.LEFT);
                } else {
                    legalTextSliderButton.setIcon(VaadinIcons.CARET_SQUARE_RIGHT_O);
                    legalTextSliderButton.setData(SplitPositionEvent.MoveDirection.RIGHT);
                }
            }
        };
        eventBus.register(tocPositionUpdateHandler);
        return legalTextSliderButton;
    }

    //Create preview PDF button
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

    //Create preview HTML button
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
            }
        });

        return textRefreshButton;
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

        // create content layout
        final VerticalLayout textContentLayout = new VerticalLayout();
        textContentLayout.setSizeUndefined();
        textContentLayout.addStyleName("leos-relative-full-wdth");

        // create placeholder to display Legal Text content
        docContent = new Label();
        docContent.setContentMode(ContentMode.HTML);
        docContent.setSizeFull();
        docContent.setStyleName("leos-viewdoc-content");

        return docContent;
    }

    public void populateContent(String docContentText) {
        docContent.setValue(docContentText);
        textRefreshNote.setVisible(false);

    }

    private void bindJavaScriptActions() {
        com.vaadin.ui.JavaScript.getCurrent().addFunction("leg_editArticle",
                new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) throws JsonException {
                String articleId = arguments.getString(0);
                eventBus.post(new EditArticleRequestEvent(articleId));
            }
        });

        com.vaadin.ui.JavaScript.getCurrent().addFunction("leg_editCitations",
                new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) throws JsonException {
                String citationsId = arguments.getString(0);
                eventBus.post(new EditCitationsRequestEvent(citationsId));
            }
        });

        com.vaadin.ui.JavaScript.getCurrent().addFunction("leg_editRecitals",
                new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) throws JsonException {
                String recitalsId = arguments.getString(0);
                eventBus.post(new EditRecitalsRequestEvent(recitalsId));
            }
        });

        com.vaadin.ui.JavaScript.getCurrent().addFunction("leg_deleteArticle",
                new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) throws JsonException {
                String articleId = arguments.getString(0);

                eventBus.post(new DeleteArticleRequestEvent(articleId));
            }
        });

        com.vaadin.ui.JavaScript.getCurrent().addFunction("leg_insertArticleAfter",
                new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) throws JsonException {
                String articleId = arguments.getString(0);

                eventBus.post(new InsertArticleRequestEvent(articleId, InsertArticleRequestEvent.POSITION.AFTER));
            }
        });
        com.vaadin.ui.JavaScript.getCurrent().addFunction("leg_insertArticleBefore",
                new JavaScriptFunction() {
            @Override
            public void call(JsonArray arguments) throws JsonException {
                String articleId = arguments.getString(0);

                eventBus.post(new InsertArticleRequestEvent(articleId, InsertArticleRequestEvent.POSITION.BEFORE));
            }
        });
    }

    public void setDocumentPreviewURLs(String documentId, String pdfURL, String htmlURL){
        ClientConnector buttonPdfConnector =null;
        ClientConnector buttonHtmlConnector =null;

        BrowserWindowOpener pdfOpener = this.getPdfPreviewOpener();
        if(pdfOpener != null){
            buttonPdfConnector= pdfOpener.getParent();
            buttonPdfConnector.removeExtension(pdfOpener);
        }
        pdfOpener = new BrowserWindowOpener(pdfURL);
        pdfOpener.setFeatures(LeosTheme.LEOS_PREVIEW_WINDOW_FEATURES);
        pdfOpener.setWindowName("PreviewPDF_" + documentId);
        pdfOpener.extend((Button)buttonPdfConnector);
        this.setPdfPreviewOpener(pdfOpener);

        BrowserWindowOpener htmlOpener = this.getHtmlPreviewOpener();
        if(htmlOpener != null){
            buttonHtmlConnector= htmlOpener.getParent();
            buttonHtmlConnector.removeExtension(htmlOpener);
        }
        htmlOpener = new BrowserWindowOpener(htmlURL);
        htmlOpener.setFeatures(LeosTheme.LEOS_PREVIEW_WINDOW_FEATURES);
        htmlOpener.setWindowName("PreviewHTML_" +documentId );
        htmlOpener.extend((Button)buttonHtmlConnector);
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

    public void updateLocks(LockActionInfo lockActionInfo){
        // update the bar with message to refresh for all other user except the usr who did the update
        if(! VaadinSession.getCurrent().getSession().getId().equals(lockActionInfo.getLock().getSessionId())){
            if (LockActionInfo.Operation.RELEASE.equals(lockActionInfo.getOperation())
                    && (LockLevel.DOCUMENT_LOCK.equals(lockActionInfo.getLock().getLockLevel())||LockLevel.ELEMENT_LOCK.equals(lockActionInfo.getLock().getLockLevel())) ){
                textRefreshNote.setVisible(true);
            }
        }
    }
}
