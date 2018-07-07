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
package eu.europa.ec.leos.web.ui.screen;

import eu.europa.ec.leos.web.ui.component.LegalTextComponent;
import eu.europa.ec.leos.web.ui.component.toc.TableOfContentComponent;
import eu.europa.ec.leos.web.ui.screen.document.ColumnPosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ViewSettings {

    protected boolean previewEnabled = true;
    protected boolean compareEnabled = true;
    protected boolean tocEditEnabled = true;
    protected boolean sideCommentsEnabled = false;

    protected List<Class> viewComponents = new ArrayList<Class>(Arrays.asList(LegalTextComponent.class, TableOfContentComponent.class));

    public boolean isPreviewEnabled() {
        return previewEnabled;
    }

    public boolean isCompareEnabled() {
        return compareEnabled;
    }

    public boolean isTocEditEnabled() {
        return tocEditEnabled;
    }

    public boolean isSideCommentsEnabled() {
        return sideCommentsEnabled;
    }

    public List<Class> getViewComponents() {
        return viewComponents;
    }

    public void setComponentColumnPosition(ColumnPosition position, Class component){
            switch (position) {
                case OFF:
                    viewComponents.remove(component);
                    break;
                case FIRST:
                    viewComponents.add(0, component);
                    break;
                case LAST:
                    viewComponents.add(component);
                    break;
            }
    }

    public int getComponentColumnPosition(Class component) {
        return viewComponents.indexOf(component);
    }

    public float getDefaultSplitterPosition(int indexSplitter) {
        float splitterSize=0f;
        switch(viewComponents.size()){
            case 1:
                splitterSize=0f;
                break;
            case 2:
                splitterSize = (viewComponents.contains(TableOfContentComponent.class))
                        ?(viewComponents.indexOf(TableOfContentComponent.class)==0)
                        ? (100f - 80f)
                        :(80f)
                        :50f;
                break;
            case 3:
                //there are two splitter
                if (viewComponents.indexOf(TableOfContentComponent.class) == 2) {
                    splitterSize = (indexSplitter == 0) ? 50f : 88f;
                } else if (viewComponents.indexOf(TableOfContentComponent.class) == 0) {
                    splitterSize = (indexSplitter == 0) ? 100f - 88f : 50f;
                } else if (viewComponents.indexOf(TableOfContentComponent.class) == 1) {
                    splitterSize = (indexSplitter == 0) ? 100 * 44f / (100 - 44f) : 44f; // 44/12/44
                }
                break;
        }//end switch
        return splitterSize;
    }
}
