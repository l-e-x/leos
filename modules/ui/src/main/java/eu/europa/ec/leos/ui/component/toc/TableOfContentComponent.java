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
package eu.europa.ec.leos.ui.component.toc;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.data.TreeData;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.*;
import com.vaadin.ui.declarative.Design;
import com.vaadin.ui.renderers.HtmlRenderer;

import eu.europa.ec.leos.ui.event.StateChangeEvent;
import eu.europa.ec.leos.ui.event.toc.EditTocRequestEvent;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.view.document.RefreshDocumentEvent;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.ContentPane;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import java.util.List;
import java.util.Optional;

@SpringComponent
@Scope("prototype")
@DesignRoot("TableOfContentDesign.html")
public class TableOfContentComponent extends VerticalLayout implements ContentPane {

    private static final long serialVersionUID = -4752609567267410718L;
    private static final Logger LOG = LoggerFactory.getLogger(TableOfContentComponent.class);

    private MessageHelper messageHelper;
    private EventBus eventBus;
    
    protected Label tocLabel;
    protected Button tocExpandCollapseButton;
    protected Button tocEditButton;
    protected Button tocRefreshButton;
    protected Label spacerLabel;
    
    protected TreeGrid<TableOfContentItemVO> tocTree;

    public enum TreeAction {
        EXPAND,
        COLLAPSE
    }
    
    @Autowired
    public TableOfContentComponent(final MessageHelper messageHelper, final EventBus eventBus) {
        LOG.trace("Initializing table of content...");
        Validate.notNull(messageHelper, "MessageHelper must not be null!");
        this.messageHelper = messageHelper;
        Validate.notNull(eventBus, "EventBus must not be null!");
        this.eventBus = eventBus;
        
        Design.read(this);
        buildToc();
    }

    @Override
    public void attach() {
        super.attach();
        eventBus.register(this);
    }

    @Override
    public void detach() {
        super.detach();
        eventBus.unregister(this);
    }

    private void buildToc() {
        LOG.debug("Building table of contents...");

        buildTocToolbar();
        
        // create toc tree
        buildTocTree();
    }

    private void buildTocToolbar() {
        LOG.debug("Building table of contents toolbar...");

        // create toc header label
        tocLabel.setValue(messageHelper.getMessage("toc.title"));
        tocLabel.setSizeUndefined();

        // create toc expand/collapse button
        tocExpandCollapseButton();

        // create toc edit button
        tocEditButton();

        // spacer label will use all available space
        spacerLabel.setContentMode(ContentMode.HTML);
        
        // create toc refresh button
        tocRefreshButton();
    }

    // create toc expand/collapse button
    private void tocExpandCollapseButton() {
        tocExpandCollapseButton.setData(TreeAction.EXPAND);
        tocExpandCollapseButton.setDescription(messageHelper.getMessage("toc.expand.button.description"));
        tocExpandCollapseButton.setIcon(LeosTheme.LEOS_TOC_TREE_ICON_16);
        tocExpandCollapseButton.addClickListener(event -> {
           toggleTreeItems((TreeAction) tocExpandCollapseButton.getData());
        });
    }

    private void toggleTreeItems(TreeAction treeAction) {
        if (TreeAction.EXPAND.equals(treeAction)) {
            LOG.debug("Expanding the tree...");
            expandTree(tocTree.getTreeData().getRootItems()); //expand items recursively
            tocExpandCollapseButton.setData(TreeAction.COLLAPSE);
            tocExpandCollapseButton.setDescription(messageHelper.getMessage("toc.collapse.button.description"));
        } else if (TreeAction.COLLAPSE.equals(treeAction)) {
            LOG.debug("Collapsing the tree...");
            tocTree.collapse(tocTree.getTreeData().getRootItems());
            tocExpandCollapseButton.setData(TreeAction.EXPAND);
            tocExpandCollapseButton.setDescription(messageHelper.getMessage("toc.expand.button.description"));
        } else {
            LOG.debug("Ignoring unknown tree control button click action! [action={}]", tocExpandCollapseButton.getData());
        }
    }

    private void expandTree(List<TableOfContentItemVO> items) {
        items.forEach(item -> {
            tocTree.expand(item);
            if (!item.getChildItems().isEmpty()) {
                expandTree(item.getChildItems());
            }
        });
    }

    // Toc edit button
    private void tocEditButton() {
        tocEditButton.setVisible(true);
        tocEditButton.setDescription(messageHelper.getMessage("toc.edit.button.description"));
        tocEditButton.setIcon(LeosTheme.LEOS_TOC_EDIT_ICON_16);
        tocEditButton.addClickListener(event -> {
           eventBus.post(new EditTocRequestEvent());
        });
    }

    // Toc tree Refresh Button
    private void tocRefreshButton() {
        tocRefreshButton.addClickListener(event -> {
            LOG.debug("Toc refresh button clicked...");
            eventBus.post(new RefreshDocumentEvent());
        });
    }

    // Build Table of Content tree
    private void buildTocTree() {
        tocTree.setSelectionMode(Grid.SelectionMode.SINGLE);
        tocTree.addColumn(tableOfContentItemVO -> {
            String iconHtml = "";
            if(tableOfContentItemVO.areChildrenAllowed() ) {
                iconHtml = VaadinIcons.FOLDER_O.getHtml();
            }
            return iconHtml + " " + TableOfContentItemConverter.buildItemCaption(tableOfContentItemVO,
                    TableOfContentItemConverter.DEFAULT_CAPTION_MAX_SIZE, messageHelper);
            }, new HtmlRenderer());
        
        tocTree.removeHeaderRow(tocTree.getDefaultHeaderRow());

        // handle tree node selections
        tocTree.addSelectionListener(new SelectionListener<TableOfContentItemVO>(){
            private static final long serialVersionUID = 5989780381988552177L;
            @Override
            public void selectionChange(SelectionEvent<TableOfContentItemVO> event) {
                Optional<TableOfContentItemVO> itemId = event.getFirstSelectedItem();
                LOG.trace("ToC selection changed: id='{}'", itemId);
                itemId.ifPresent(tableOfContentItemVO -> {
                    String id = tableOfContentItemVO.getId();
                    LOG.trace("ToC navigating to (id={})...", id);
                    com.vaadin.ui.JavaScript.getCurrent().execute("LEOS.scrollTo('" + id + "');");
                });
            }
        });
    }

    public void setTableOfContent(TreeData<TableOfContentItemVO> newTocData) {
        TreeData<TableOfContentItemVO> tocData = tocTree.getTreeData();
        tocData.removeItem(null);//remove all old data
        tocData.addItems(newTocData.getRootItems(), TableOfContentItemVO::getChildItemsView);//add all new data
        expandTree(tocTree.getTreeData().getRootItems()); //expand items recursively
        tocTree.getDataProvider().refreshAll();
    }

    public void setPermissions(boolean visible) {
        tocEditButton.setVisible(visible);
    }

    @Override
    public float getDefaultPaneWidth(int numberOfFeatures) {
        float featureWidth=0f;
        switch(numberOfFeatures){
            case 1:
                featureWidth=100f;
                break;
            case 2:
                featureWidth=20f;
                break;
            default:
                featureWidth = 20f;
                break;
        }//end switch
        return featureWidth;
    }

    @Subscribe
    public void handleElementState(StateChangeEvent event) {
        if (event.getState() != null) {
            tocEditButton.setEnabled(event.getState().isState());
            tocRefreshButton.setEnabled(event.getState().isState());
        }
    }
}
