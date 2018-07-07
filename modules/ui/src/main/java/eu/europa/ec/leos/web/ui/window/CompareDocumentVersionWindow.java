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
import com.vaadin.data.Property;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.web.event.view.document.CompareVersionEvent;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.extension.MathJaxExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

@com.vaadin.annotations.JavaScript({"vaadin://../js/web/legacy/versionCompare.js" + LeosCacheToken.TOKEN})
public class CompareDocumentVersionWindow extends AbstractWindow {

    private static final long serialVersionUID = 638705322911001557L;

    private static final Logger LOG = LoggerFactory.getLogger(CompareDocumentVersionWindow.class);

    private LeosDocumentProperties firstItem;
    private LeosDocumentProperties secondItem;
    private HashMap<Integer, Object> htmlCompareResult = new HashMap<Integer, Object>();
    private Panel diffPanel;
    private int diffMode = 1;

    final static int SINGLE_COLUMN_MODE=1;
    final static int TWO_COLUMN_MODE=2;

    final static String SINGLE_COLUMN_WIDTH="27.0cm";
    final static String TWO_COLUMN_WIDTH="33.0cm";

    public CompareDocumentVersionWindow(MessageHelper messageHelper, final EventBus eventBus, 
            LeosDocumentProperties firstItem, LeosDocumentProperties secondItem) {

        super(messageHelper, eventBus);
        setCaption(messageHelper.getMessage("document.versions.caption.window", firstItem.getVersionLabel(), secondItem.getVersionLabel()));
        this.firstItem = firstItem;
        this.secondItem = secondItem;
        prepareWindow();
    }

    public void prepareWindow() {
        
        setWidth(SINGLE_COLUMN_WIDTH); //default setting for single column mode
        setHeight("90%");

        VerticalLayout windowLayout = new VerticalLayout();
        windowLayout.setSizeFull();
        windowLayout.setMargin(true);
        setBodyComponent(windowLayout);

        fillLayout(windowLayout);
    }

    private void fillLayout(VerticalLayout windowLayout) {
        Panel headerPanel =new Panel();//added a new panel for header as jerky scrollbar behaviour was observed LEOS-1415
        VerticalLayout header=new VerticalLayout();
        header.addComponent(buildDiffModeSelectionArea());
        header.setSpacing(true);
        header.addComponent(buildComparisonHeaderArea(firstItem, secondItem));
        headerPanel.setContent(header);
        windowLayout.addComponent(headerPanel);

        diffPanel = new Panel();
        diffPanel.setSizeFull();
        
        windowLayout.addComponent(diffPanel);
        windowLayout.setExpandRatio(diffPanel, 1f);

        //get the default comparision.
        diffMode=SINGLE_COLUMN_MODE;
        eventBus.post(new CompareVersionEvent(firstItem,secondItem, SINGLE_COLUMN_MODE));
    }

    private Component buildDiffModeSelectionArea() {

        HorizontalLayout diffModeSelectionArea = new HorizontalLayout();
        diffModeSelectionArea.setSpacing(true);
        diffModeSelectionArea.setWidth("100%");
        Component headerLabel=buildHeaderLabel();
        diffModeSelectionArea.addComponent(headerLabel);
        diffModeSelectionArea.setExpandRatio(headerLabel, 1f);

        final OptionGroup singleColumnOption = new OptionGroup();
        singleColumnOption.addItem(0);
        singleColumnOption.setItemCaption(0,  messageHelper.getMessage("leos.version.singlecolumn"));
        singleColumnOption.setImmediate(true);
        diffModeSelectionArea.addComponent(singleColumnOption);

        final OptionGroup twoColumnOption = new OptionGroup();
        twoColumnOption.addItem(0);
        twoColumnOption.setItemCaption(0, messageHelper.getMessage("leos.version.twocolumn"));
        twoColumnOption.setImmediate(true);
        diffModeSelectionArea.addComponent(twoColumnOption);

        if (diffMode == SINGLE_COLUMN_MODE) {
            singleColumnOption.setValue(0);
        } else {
            twoColumnOption.setValue(0);
        }

        singleColumnOption.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                if (event.getProperty().getValue() != null) {
                    twoColumnOption.setValue(null);
                    diffMode = SINGLE_COLUMN_MODE;
                    setWidth(SINGLE_COLUMN_WIDTH);//contracting the window for two column layout

                    if(htmlCompareResult.get(SINGLE_COLUMN_MODE)!=null ){
                        diffPanel.setContent(buildComparisonResultArea());
                        }
                    else {
                        eventBus.post(new CompareVersionEvent(firstItem,secondItem, SINGLE_COLUMN_MODE));
                    }
                }
            }
        });
        twoColumnOption.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                if (event.getProperty().getValue() != null) {
                    singleColumnOption.setValue(null);
                    diffMode = TWO_COLUMN_MODE;
                    setWidth(TWO_COLUMN_WIDTH); //expanding the window for two column layout

                    if(htmlCompareResult.get(TWO_COLUMN_MODE)!=null){
                        diffPanel.setContent(buildComparisonResultArea());
                    }
                    else {
                        eventBus.post(new CompareVersionEvent(firstItem,secondItem, TWO_COLUMN_MODE));
                    }
                }
            }
        });

        return diffModeSelectionArea;
    }

    private Component buildHeaderLabel() {
        return new Label(messageHelper.getMessage("leos.version.comparision"), ContentMode.HTML);
    }

    private Component buildComparisonResultArea() {

        HorizontalLayout contentCompareResult = new HorizontalLayout();
        contentCompareResult.setSpacing(true);

        if (diffMode == SINGLE_COLUMN_MODE && htmlCompareResult.get(SINGLE_COLUMN_MODE)!=null) {
            Label docDiff = new Label((String) htmlCompareResult.get(SINGLE_COLUMN_MODE),ContentMode.HTML);
            (new MathJaxExtension()).extend(docDiff);

            contentCompareResult.setWidth("100%");
            contentCompareResult.addStyleName("leos-single-column-layout");
            contentCompareResult.addComponent(docDiff);

        } else if (htmlCompareResult.get(TWO_COLUMN_MODE)!=null){
            Label leftSide = new Label(((String[]) htmlCompareResult.get(TWO_COLUMN_MODE))[0], ContentMode.HTML);
            (new MathJaxExtension()).extend(leftSide);
            leftSide.setSizeFull();
            leftSide.setPrimaryStyleName("leos-two-column-compare");

            Label rightSide = new Label(((String[]) htmlCompareResult.get(TWO_COLUMN_MODE))[1], ContentMode.HTML);
            (new MathJaxExtension()).extend(rightSide);
            rightSide.setSizeFull();
            rightSide.setPrimaryStyleName("leos-two-column-compare");

            contentCompareResult.addComponent(leftSide);
            contentCompareResult.setExpandRatio(leftSide, 1.0f);
            contentCompareResult.addComponent(rightSide);
            contentCompareResult.setExpandRatio(rightSide, 1.0f);
            contentCompareResult.setSizeUndefined();
            contentCompareResult.setPrimaryStyleName("leos-two-column-layout");

            //this implementation binds as well a JS call to align the modified elements
            JavaScript.getCurrent().execute("versionCompare.alignModifiedElements();");			
        }
        return contentCompareResult;
    }

    private Component buildComparisonHeaderArea(LeosDocumentProperties firstItem, LeosDocumentProperties secondItem) {
        
        HorizontalLayout comparisonHeaderArea = new HorizontalLayout();
        comparisonHeaderArea.setWidth("100%");

        Component itemHeader = buildItemHeader(firstItem);
        comparisonHeaderArea.addComponent(itemHeader);
        comparisonHeaderArea.setExpandRatio(itemHeader, 1.0f);

        itemHeader = buildItemHeader(secondItem);
        comparisonHeaderArea.addComponent(itemHeader);
        comparisonHeaderArea.setExpandRatio(itemHeader, 1.0f);
        
        return comparisonHeaderArea;
    }

    private VerticalLayout buildItemHeader(LeosDocumentProperties componentCompareItem) {
        VerticalLayout verticalLayout = new VerticalLayout();

        //add the first line
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setMargin(new MarginInfo(false, true, false, true));
        horizontalLayout.setSpacing(true);
        horizontalLayout.setWidth("100%");
        horizontalLayout.addComponent(new Label( messageHelper.getMessage("document.versions.caption.author" , componentCompareItem.getUpdatedBy()), ContentMode.HTML));
        horizontalLayout.addComponent(new Label( messageHelper.getMessage("document.versions.caption.date",  componentCompareItem.getUpdatedOn()), ContentMode.HTML));
        horizontalLayout.addComponent(new Label( messageHelper.getMessage("document.versions.caption.revision", componentCompareItem.getVersionLabel()), ContentMode.HTML));
        verticalLayout.addComponent(horizontalLayout);

        //add the second line
        horizontalLayout = new HorizontalLayout();
        horizontalLayout.setWidth("100%");
        horizontalLayout.setMargin(new MarginInfo(false, true, false, true));
        horizontalLayout.setSpacing(true);
        Label commentsLabel = new Label(messageHelper.getMessage("document.versions.caption.comments"), ContentMode.HTML);
        commentsLabel.setSizeUndefined();
        horizontalLayout.addComponent(commentsLabel);
        Label commentsContent = new Label(componentCompareItem.getVersionComment()!=null? messageHelper.getMessage(componentCompareItem.getVersionComment()):"");
        commentsContent.setSizeUndefined();
        horizontalLayout.addComponent(commentsContent);
        horizontalLayout.setExpandRatio(commentsContent, 1.0f);
        verticalLayout.addComponent(horizontalLayout);

        return verticalLayout;
    }

    public void setComparisonContent(HashMap<Integer, Object> htmlCompareResult ){
        this.htmlCompareResult=htmlCompareResult;
        diffPanel.setContent(buildComparisonResultArea());
        this.center();
    }
}
