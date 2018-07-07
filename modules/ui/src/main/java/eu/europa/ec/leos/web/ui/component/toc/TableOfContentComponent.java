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
package eu.europa.ec.leos.web.ui.component.toc;

import com.google.common.eventbus.EventBus;
import com.vaadin.annotations.JavaScript;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.vo.lock.LockLevel;
import eu.europa.ec.leos.web.event.component.EditTocRequestEvent;
import eu.europa.ec.leos.web.event.component.SplitPositionEvent;
import eu.europa.ec.leos.web.event.view.document.RefreshDocumentEvent;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.converter.TableOfContentItemConverter;
import eu.europa.ec.leos.web.ui.screen.ViewSettings;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.teemu.VaadinIcons;

@JavaScript({"vaadin://../js/web/legacy/navigate.js" + LeosCacheToken.TOKEN})
public class TableOfContentComponent extends CustomComponent {

    private static final long serialVersionUID = -4752609567267410718L;
    private static final Logger LOG = LoggerFactory.getLogger(TableOfContentComponent.class);

    private static final String LEOS_RELATIVE_FULL_WDT = "100%";

    private MessageHelper messageHelper;
    private EventBus eventBus;
    private Tree tocTree;
    private Button tocExpandCollapseButton;
    private Button tocEditButton;
    private ViewSettings viewSettings;
    
    public enum TreeAction {
        EXPAND,
        COLLAPSE
    }

    public TableOfContentComponent(final MessageHelper messageHelper, final EventBus eventBus, ViewSettings viewSettings) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.viewSettings=viewSettings;
        buildToc();
    }

    private void buildToc() {
        LOG.debug("Building Legal Text table of contents...");

        // create Legal Text toc layout
        final VerticalLayout tocLayout = new VerticalLayout();

        // set toclayout as composition root
        setCompositionRoot(tocLayout);

        setSizeFull(); // set full size of the custom component
        tocLayout.setSizeFull(); // set full size of the vertical layout
        // create toc toolbar
        tocLayout.addComponent(buildTocToolbar());

        // create toc tree
        final Component tocTree = buildTocTree();
        tocLayout.addComponent(tocTree);
        // toc tree will expand to use all available space
        tocLayout.setExpandRatio(tocTree, 1.0f);
    }

    private Component buildTocToolbar() {
        LOG.debug("Building Legal Text table of contents toolbar...");

        // create toc toolbar layout
        final HorizontalLayout tocToolsLayout = new HorizontalLayout();
        tocToolsLayout.setWidth(LEOS_RELATIVE_FULL_WDT);
        tocToolsLayout.setStyleName("leos-viewdoc-tocbar");
        tocToolsLayout.setSpacing(true);

        // Toc slide button
        final Button tocSlideButton = tocSlideButton();
        tocToolsLayout.addComponent(tocSlideButton);
        tocToolsLayout.setComponentAlignment(tocSlideButton, Alignment.MIDDLE_LEFT);

        // create toc header label
        final Label tocLabel = new Label(messageHelper.getMessage("toc.title"));
        tocLabel.setSizeUndefined();
        tocToolsLayout.addComponent(tocLabel);
        tocToolsLayout.setComponentAlignment(tocLabel, Alignment.MIDDLE_LEFT);

        // create toc expand/collapse button
        tocExpandCollapseButton = tocExpandCollapseButton();
        tocToolsLayout.addComponent(tocExpandCollapseButton);
        tocToolsLayout.setComponentAlignment(tocExpandCollapseButton, Alignment.MIDDLE_LEFT);

        // create toc edit button
        if(viewSettings.isTocEditEnabled()){
            tocEditButton = tocEditButton();
            tocToolsLayout.addComponent(tocEditButton);
            tocToolsLayout.setComponentAlignment(tocEditButton, Alignment.MIDDLE_LEFT);
        }
        // spacer label will use all available space
        final Label spacerLabel = new Label("&nbsp;", ContentMode.HTML);
        tocToolsLayout.addComponent(spacerLabel);
        tocToolsLayout.setExpandRatio(spacerLabel, 1.0f);

        // create toc refresh button
        final Button tocRefreshButton = tocRefreshButton();
        tocToolsLayout.addComponent(tocRefreshButton);
        tocToolsLayout.setComponentAlignment(tocRefreshButton, Alignment.MIDDLE_RIGHT);

        return tocToolsLayout;
    }

    // Toc slider button for automatic expand/collapse
    private Button tocSlideButton() {
        VaadinIcons tocSliderIcon = VaadinIcons.CARET_SQUARE_RIGHT_O;
        // create toc collapse button
        final Button tocSliderButton = new Button();
        tocSliderButton.setIcon(tocSliderIcon);
        tocSliderButton.setData(SplitPositionEvent.MoveDirection.RIGHT);
        tocSliderButton.setStyleName("link");
        tocSliderButton.addStyleName("leos-toolbar-button");
        tocSliderButton.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = 8812695783793874019L;

            @Override
            public void buttonClick(Button.ClickEvent event) {
                eventBus.post(new SplitPositionEvent((SplitPositionEvent.MoveDirection) event.getButton().getData(),TableOfContentComponent.this));
            }
        });

        return tocSliderButton;
    }

    // create toc expand/collapse button
    private Button tocExpandCollapseButton() {
        final Button tocTreeButton = new Button();
        tocTreeButton.setData(TreeAction.EXPAND);
        tocTreeButton.setDescription(messageHelper.getMessage("toc.expand.button.description"));
        tocTreeButton.setIcon(LeosTheme.LEOS_TOC_TREE_ICON_16);
        tocTreeButton.setStyleName("link");
        tocTreeButton.addStyleName("leos-toolbar-button");
        tocTreeButton.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = -1860677385826130362L;

            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                toggleTreeItems((TreeAction) tocTreeButton.getData());
            }
        });
        return tocTreeButton;
    }

    private void toggleTreeItems(TreeAction treeAction) {
        if (TreeAction.EXPAND.equals(treeAction)) {
            LOG.debug("Expanding the tree...");
            for (Object rootId : tocTree.rootItemIds()) {
                tocTree.expandItemsRecursively(rootId);
            }
            tocExpandCollapseButton.setData(TreeAction.COLLAPSE);
            tocExpandCollapseButton.setDescription(messageHelper.getMessage("toc.collapse.button.description"));
        } else if (TreeAction.COLLAPSE.equals(treeAction)) {
            LOG.debug("Collapsing the tree...");
            for (Object rootId : tocTree.rootItemIds()) {
                tocTree.collapseItemsRecursively(rootId);
            }
            tocExpandCollapseButton.setData(TreeAction.EXPAND);
            tocExpandCollapseButton.setDescription(messageHelper.getMessage("toc.expand.button.description"));
        } else {
            LOG.debug("Ignoring unknown tree control button click action! [action={}]", tocExpandCollapseButton.getData());
        }
    }

    // Toc edit button
    private Button tocEditButton() {
        final Button tocEditButton = new Button();
        tocEditButton.setDescription(messageHelper.getMessage("toc.edit.button.description"));
        tocEditButton.setIcon(LeosTheme.LEOS_TOC_EDIT_ICON_16);
        tocEditButton.setStyleName("link");
        tocEditButton.addStyleName("leos-toolbar-button");
        tocEditButton.addClickListener(new Button.ClickListener() {

            private static final long serialVersionUID = -5569672970887757136L;

            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                eventBus.post(new EditTocRequestEvent());
            }
        });
        return tocEditButton;
    }

    // Toc tree Refresh Button
    private Button tocRefreshButton() {
        final Button tocRefreshButton = new Button();
        tocRefreshButton.setIcon(VaadinIcons.REFRESH);
        tocRefreshButton.setStyleName("link");
        tocRefreshButton.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = -6447526561147453574L;

            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                LOG.debug("Toc refresh button clicked...");
                eventBus.post(new RefreshDocumentEvent());
            }
        });
        return tocRefreshButton;
    }

    // Build Table of Content tree
    private Component buildTocTree() {

        tocTree = new Tree();
        tocTree.addStyleName("leos-viewdoc-toctree");

        // configure toc tree
        tocTree.setImmediate(true);
        tocTree.setItemCaptionPropertyId(TableOfContentItemConverter.CAPTION_PROPERTY);
        tocTree.setItemCaptionMode(AbstractSelect.ItemCaptionMode.PROPERTY);

        // generate toc tree item tooltip for abbreviated captions
        tocTree.setItemDescriptionGenerator(new AbstractSelect.ItemDescriptionGenerator() {

            private static final long serialVersionUID = -7240277780272871964L;

            @Override
            public String generateDescription(Component source, Object itemId, Object propertyId) {
                Item item = ((Tree) source).getItem(itemId);
                String caption = (String) item.getItemProperty(TableOfContentItemConverter.CAPTION_PROPERTY).getValue();
                String description = (String) item.getItemProperty(TableOfContentItemConverter.DESC_PROPERTY).getValue();
                boolean setTooltip = StringUtils.isNotBlank(caption) && description.length() > caption.length() && StringUtils.endsWith(caption, "...");
                return (setTooltip ? description : null);
            }
        });

        // handle tree node selections
        tocTree.addValueChangeListener(new Property.ValueChangeListener() {
            private static final long serialVersionUID = 5989780381988552177L;

            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                Object itemId = event.getProperty().getValue();
                LOG.trace("ToC selection changed: id='{}'", itemId);
                Item item = tocTree.getItem(event.getProperty().getValue());
                if (item != null) {
                    String xmlId = (String) item.getItemProperty(TableOfContentItemConverter.XML_ID_PROPERTY).getValue();
                    LOG.trace("ToC navigating to (xmlId={})...", xmlId);
                    com.vaadin.ui.JavaScript.getCurrent().execute("nav_navigateToContent('" + xmlId + "');");
                }
            }
        });

        // wrap tree in panel to provide scroll bars
        final Panel treePanel = new Panel(tocTree);
        treePanel.setStyleName("leos-viewdoc-toctree-panel");
        treePanel.setSizeFull();

        return treePanel;
    }

    public void setTableOfContent(Container tocContainer) {
        tocTree.setContainerDataSource(tocContainer);
    }
    
    public void updateLocks(LockActionInfo lockActionInfo) {
        if(!viewSettings.isTocEditEnabled()){
                return;
        }
        
        LockData lock= lockActionInfo.getLock();
        String currentSessionId= VaadinSession.getCurrent().getSession().getId();
        boolean tocEnabaled=true;
        for (LockData lockData : lockActionInfo.getCurrentLocks()) {
            if( !currentSessionId.equals(lockData.getSessionId())
                    && (LockLevel.DOCUMENT_LOCK.equals(lockData.getLockLevel())
                        || LockLevel.ELEMENT_LOCK.equals(lockData.getLockLevel()))){
                //if some other session has acquired the element or doc lock.                
                tocEnabaled=false;
                break;
            }
        }
        if(tocEnabaled){
            tocEditButton.setEnabled(true);
            tocEditButton.setDescription(messageHelper.getMessage("toc.edit.button.description"));
        }
        else {
            tocEditButton.setEnabled(false);
            tocEditButton.setDescription(messageHelper.getMessage("toc.edit.button.description.locked.tooltip", lock.getUserName()));

        }
    }
}