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
package eu.europa.ec.leos.ui.component.toc;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.data.TreeData;
import com.vaadin.data.ValueProvider;
import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.DescriptionGenerator;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.StyleGenerator;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.declarative.Design;
import com.vaadin.ui.renderers.HtmlRenderer;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.support.TableOfContentHelper;
import eu.europa.ec.leos.ui.event.StateChangeEvent;
import eu.europa.ec.leos.ui.event.toc.EditTocRequestEvent;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.coedition.InfoType;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.view.document.RefreshDocumentEvent;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.ContentPane;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static eu.europa.ec.leos.services.support.TableOfContentHelper.DEFAULT_CAPTION_MAX_SIZE;

@SpringComponent
@Scope("prototype")
@DesignRoot("TableOfContentDesign.html")
public class TableOfContentComponent extends VerticalLayout implements ContentPane {

    private static final long serialVersionUID = -4752609567267410718L;
    private static final Logger LOG = LoggerFactory.getLogger(TableOfContentComponent.class);
    public static SimpleDateFormat dataFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    private MessageHelper messageHelper;
    private EventBus eventBus;
    protected Label tocLabel;
    protected Button tocExpandCollapseButton;
    protected Button tocEditButton;
    protected Button tocRefreshButton;
    protected Label spacerLabel;
    
    protected Label tocUserCoEdition;
    
    protected TreeGrid<TableOfContentItemVO> tocTree;
    
    private User user;

    public enum TreeAction {
        EXPAND,
        COLLAPSE
    }

    private boolean coEditionSipEnabled;
    private String coEditionSipDomain;

    @Autowired
    public TableOfContentComponent(final MessageHelper messageHelper, final EventBus eventBus, final SecurityContext securityContext, final ConfigurationHelper cfgHelper) {
        LOG.trace("Initializing table of content...");
        Validate.notNull(messageHelper, "MessageHelper must not be null!");
        this.messageHelper = messageHelper;
        Validate.notNull(eventBus, "EventBus must not be null!");
        this.eventBus = eventBus;
        Validate.notNull(securityContext, "SecurityContext must not be null!");
        this.user = securityContext.getUser();
        Validate.notNull(user, "User must not be null!");
        Validate.notNull(cfgHelper, "cfgHelper must not be null!");
        this.coEditionSipEnabled = Boolean.valueOf(cfgHelper.getProperty("leos.coedition.sip.enabled"));
        this.coEditionSipDomain = cfgHelper.getProperty("leos.coedition.sip.domain");

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

    private void expandDefaultTreeNodes(List<TableOfContentItemVO> items) {
        items.forEach(item -> {
            if(item.getType().isExpandedByDefault()){
                tocTree.expand(item);
                if (!item.getChildItems().isEmpty()) {
                    expandDefaultTreeNodes(item.getChildItems());
                }
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
    
    private ValueProvider<TableOfContentItemVO, String> getColumnHtml() {
        return tableOfContentItemVO -> {
            return getColumnItemHtml(tableOfContentItemVO);
            };
    }

    private String getColumnItemHtml(TableOfContentItemVO tableOfContentItemVO) {
        StringBuilder itemHtml = new StringBuilder(StringUtils.stripEnd(TableOfContentHelper.buildItemCaption(tableOfContentItemVO, DEFAULT_CAPTION_MAX_SIZE, messageHelper), null));
        if (!tableOfContentItemVO.getCoEditionVos().isEmpty()) {
            if (tableOfContentItemVO.getCoEditionVos().stream().anyMatch(x -> !x.getUserLoginName().equals(user.getLogin()))) {
                itemHtml.insert(0, "<span class=\"leos-toc-user-coedition Vaadin-Icons\">&#xe80d</span>");
            } else {
                itemHtml.insert(0, "<span class=\"leos-toc-user-coedition leos-toc-user-coedition-self-user Vaadin-Icons\">&#xe80d</span>");
            }
        }
        return itemHtml.toString();
    }

    private StyleGenerator<TableOfContentItemVO> getColumnStyle() {
        return tableOfContentItemVO -> {
                return TableOfContentHelper.getItemSoftStyle(tableOfContentItemVO);
            };
    }

    private DescriptionGenerator<TableOfContentItemVO> getColumnDescription() {
        return tableOfContentItemVO -> {
                return getColumnItemDescription(tableOfContentItemVO);
            };
    }

    private String getColumnItemDescription(TableOfContentItemVO tableOfContentItemVO) {
        StringBuilder itemDescription = new StringBuilder();
        for (CoEditionVO coEditionVO : tableOfContentItemVO.getCoEditionVos()) {
            StringBuilder userDescription = new StringBuilder();
            if (!coEditionVO.getUserLoginName().equals(user.getLogin())) {
                userDescription.append("<a class=\"leos-toc-user-coedition-lync\" href=\"")
                        .append(StringUtils.isEmpty(coEditionVO.getUserEmail()) ? "" : (coEditionSipEnabled ? new StringBuilder("sip:").append(coEditionVO.getUserEmail().replaceFirst("@.*", "@" + coEditionSipDomain)).toString()
                                    : new StringBuilder("mailto:").append(coEditionVO.getUserEmail()).toString()))
                        .append("\">").append(coEditionVO.getUserName()).append(" (").append(StringUtils.isEmpty(coEditionVO.getEntity()) ? "-" : coEditionVO.getEntity())
                        .append(")</a>");
            } else {
                userDescription.append(coEditionVO.getUserName()).append(" (").append(StringUtils.isEmpty(coEditionVO.getEntity()) ? "-" : coEditionVO.getEntity()).append(")");
            }
            itemDescription.append(messageHelper.getMessage("coedition.tooltip.message", userDescription, dataFormat.format(new Date(coEditionVO.getEditionTime()))) + "<br>");
        }
        return itemDescription.toString();
    }
    
    // Build Table of Content tree
    private void buildTocTree() {
        tocTree.setSelectionMode(Grid.SelectionMode.SINGLE);
        tocTree.addColumn(getColumnHtml(), new HtmlRenderer());
        tocTree.setStyleGenerator(getColumnStyle());
        tocTree.setDescriptionGenerator(getColumnDescription(), ContentMode.HTML);
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
        expandDefaultTreeNodes(tocTree.getTreeData().getRootItems()); //expand default items recursively
        tocTree.getDataProvider().refreshAll();
    }

    public void setPermissions(boolean visible) {
        tocEditButton.setVisible(visible);
    }

    @Override
    public float getDefaultPaneWidth(int numberOfFeatures, boolean tocPresent) {
        final float featureWidth;
        switch(numberOfFeatures){
            case 1:
                featureWidth=100f;
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
    
    private void updateItemsUserCoEditionInfo(List<TableOfContentItemVO> tableOfContentItemVOList, List<CoEditionVO> coEditionVos, String presenterId) {
        tableOfContentItemVOList.forEach(tableOfContentItemVO -> {
            tableOfContentItemVO.removeAllUserCoEdition();
            coEditionVos.stream()
                    .filter((x) -> InfoType.ELEMENT_INFO.equals(x.getInfoType()) && x.getElementId().replace("__blockcontainer", "").equals(tableOfContentItemVO.getId()) &&
                            !x.getPresenterId().equals(presenterId))
                    .sorted(Comparator.comparing(CoEditionVO::getUserName).thenComparingLong(CoEditionVO::getEditionTime))
                    .forEach(x -> {
                        tableOfContentItemVO.addUserCoEdition(x);
                    });
            if (!tableOfContentItemVO.getChildItems().isEmpty()) {
                updateItemsUserCoEditionInfo(tableOfContentItemVO.getChildItems(), coEditionVos, presenterId);
            }
        });
    }
    
    private void updateTreeUserCoEditionInfo(List<CoEditionVO> coEditionVos, String presenterId) {
        updateItemsUserCoEditionInfo(tocTree.getTreeData().getRootItems(), coEditionVos, presenterId);
        tocTree.getDataProvider().refreshAll();
    }
    
    private void updateToolbarUserCoEditionInfo(List<CoEditionVO> coEditionVos, String presenterId) {
        tocUserCoEdition.setIcon(null);
        tocUserCoEdition.setDescription("");
        tocUserCoEdition.removeStyleName("leos-toolbar-user-coedition-self-user");
        coEditionVos.stream()
            .filter((x) -> InfoType.TOC_INFO.equals(x.getInfoType()) && !x.getPresenterId().equals(presenterId))
            .sorted(Comparator.comparing(CoEditionVO::getUserName).thenComparingLong(CoEditionVO::getEditionTime))
            .forEach(x -> {
                StringBuilder userDescription = new StringBuilder();
                if (!x.getUserLoginName().equals(user.getLogin())) {
                    userDescription.append("<a class=\"leos-toc-user-coedition-lync\" href=\"")
                            .append(StringUtils.isEmpty(x.getUserEmail()) ? "" : (coEditionSipEnabled ? new StringBuilder("sip:").append(x.getUserEmail().replaceFirst("@.*", "@" + coEditionSipDomain)).toString()
                                    : new StringBuilder("mailto:").append(x.getUserEmail()).toString()))
                            .append("\">").append(x.getUserName()).append(" (").append(StringUtils.isEmpty(x.getEntity()) ? "-" : x.getEntity())
                            .append(")</a>");
                } else {
                    userDescription.append(x.getUserName()).append(" (").append(StringUtils.isEmpty(x.getEntity()) ? "-" : x.getEntity()).append(")");
                }
                tocUserCoEdition.setDescription(
                        tocUserCoEdition.getDescription() +
                            messageHelper.getMessage("coedition.tooltip.message", userDescription, dataFormat.format(new Date(x.getEditionTime()))) +
                            "<br>",
                            ContentMode.HTML);
                });
        if (!tocUserCoEdition.getDescription().isEmpty()) {
            tocUserCoEdition.setIcon(VaadinIcons.USER);
            if (!tocUserCoEdition.getDescription().contains("href=\"")) {
                tocUserCoEdition.addStyleName("leos-toolbar-user-coedition-self-user");
            }
        }
    }
    
    public void updateUserCoEditionInfo(List<CoEditionVO> coEditionVos, String presenterId) {
        updateToolbarUserCoEditionInfo(coEditionVos, presenterId);
        updateTreeUserCoEditionInfo(coEditionVos, presenterId);
    }

    @Override
    public Class getChildClass() {
        return null;
    }
    
}
