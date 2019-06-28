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
package eu.europa.ec.leos.services.export;

import eu.europa.ec.leos.domain.cmis.LeosCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportResource {
    private final LeosCategory leosCategory;
    private String resourceId;
    private int docNumber = 0;
    private Map<String, String> componentsIdsMap;
    private List<ExportResource> childResources;
    private ExportOptions exportOptions;

    public ExportResource(LeosCategory leosCategory) {
        setChildResources(new ArrayList());
        this.componentsIdsMap = new HashMap();
        this.leosCategory = leosCategory;
    }

    /**
     * @param componentName the component name
     * @return the reference of a tag which name equals to componentName
     */
    public String getComponentId(String componentName) {
        return componentsIdsMap.get(componentName);
    }

    /**
     * @param componentsIdsMap the componentsIdsMap to set
     */
    public void setComponentsIdsMap(Map<String, String> componentsIdsMap) {
        this.componentsIdsMap = componentsIdsMap;
    }

    /**
     * @return the childResources
     */
    public List<ExportResource> getChildResources() {
        return childResources;
    }

    /**
     * @param childResources the childResources to set
     */
    public void setChildResources(List<ExportResource> childResources) {
        this.childResources = childResources;
    }

    /**
     * @param childResource the childResource to add
     */
    public void addChildResource(ExportResource childResource) {
        this.childResources.add(childResource);
    }

    /**
     * @return the resourceId
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * @param resourceId the resourceId to set
     */
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * @return the leosCategory
     */
    public LeosCategory getLeosCategory() {
        return leosCategory;
    }

    public List<ExportResource> getChildResources(String categoryName) {
        List<ExportResource> exportResources = new ArrayList();
        childResources.forEach(childResource -> {
            if (childResource.leosCategory.name().equalsIgnoreCase(categoryName)) {
                exportResources.add(childResource);
            }
        });
        return exportResources;
    }

    public ExportResource getChildResource(String categoryName) {
        for (ExportResource childResource : childResources) {
            if (childResource.leosCategory.name().equalsIgnoreCase(categoryName)) {
                return childResource;
            }
        }
        return null;
    }

    /**
     * @return the docNumber
     */
    public int getDocNumber() {
        return docNumber;
    }

    /**
     * @param docNumber the docNumber to set
     */
    public void setDocNumber(int docNumber) {
        this.docNumber = docNumber;
    }

    public ExportOptions getExportOptions() {
        return exportOptions;
    }

    public void setExportOptions(ExportOptions exportOptions) {
        this.exportOptions = exportOptions;
    }
}
