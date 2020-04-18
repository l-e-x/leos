package eu.europa.ec.leos.cmis.support;

import eu.europa.ec.leos.cmis.extensions.LeosMetadataExtensions;
import eu.europa.ec.leos.cmis.mapping.CmisProperties;
import eu.europa.ec.leos.domain.cmis.metadata.LeosMetadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

public final class RepositoryUtil {
    
    private RepositoryUtil() {
    }
    
    public static Map<String, ?> updateDocumentProperties(LeosMetadata metadata) {
        Map<String, Object> properties = new HashMap<>();
        properties.putAll(updateMilestoneCommentsProperties(emptyList()));
        properties.putAll(LeosMetadataExtensions.toCmisProperties(metadata));
        return properties;
    }
    
    public static Map<String, List<String>> updateMilestoneCommentsProperties(List<String> milestoneComments) {
        Map<String, List<String>> result = new HashMap<>();
        result.put(CmisProperties.MILESTONE_COMMENTS.getId(), milestoneComments);
        return result;
    }
}
