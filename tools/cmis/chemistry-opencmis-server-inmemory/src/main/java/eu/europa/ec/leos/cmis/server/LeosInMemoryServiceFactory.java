package eu.europa.ec.leos.cmis.server;

import eu.europa.ec.leos.cmis.LeosCallContext;
import eu.europa.ec.leos.cmis.types.LeosSecondaryTypesTypeSystemCreator;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.Constants;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.*;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.inmemory.ConfigConstants;
import org.apache.chemistry.opencmis.inmemory.server.InMemoryServiceFactoryImpl;
import org.apache.chemistry.opencmis.inmemory.storedobj.api.Filing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.*;

public class LeosInMemoryServiceFactory extends InMemoryServiceFactoryImpl {

    private static final Logger LOG = LoggerFactory.getLogger(LeosInMemoryServiceFactory.class.getName());

    private static final String CMIS_ROOT_FOLDER_PATH = Filing.PATH_SEPARATOR;
    private static final String CMIS_FOLDER_TYPE_ID = BaseTypeId.CMIS_FOLDER.value();

    private static final String LEOS_CATEGORY_PROP_ID = "leos:category";

    private static final Map<String, String> PROPS_KEY_MAPPING = new HashMap<>();
    static {
        // leos XML properties
        PROPS_KEY_MAPPING.put("language", "leos:language");
        PROPS_KEY_MAPPING.put("template", "leos:template");
        PROPS_KEY_MAPPING.put("title", "leos:title");
        PROPS_KEY_MAPPING.put("collaborators", "leos:collaborators");
        PROPS_KEY_MAPPING.put("milestoneComments", "leos:milestoneComments");
        PROPS_KEY_MAPPING.put("initialCreatedBy", "leos:initialCreatedBy");
        PROPS_KEY_MAPPING.put("initialCreationDate", "leos:initialCreationDate");
        // leos COMMON metadata properties
        PROPS_KEY_MAPPING.put("metadata.docTemplate", "metadata:docTemplate");
        PROPS_KEY_MAPPING.put("metadata.docStage", "metadata:docStage");
        PROPS_KEY_MAPPING.put("metadata.docType", "metadata:docType");
        PROPS_KEY_MAPPING.put("metadata.docPurpose", "metadata:docPurpose");
        PROPS_KEY_MAPPING.put("metadata.ref", "metadata:ref");
        // TODO leos MEMORANDUM specific metadata properties
        // use sub-key metadata.memorandum.<>
        // TODO leos BILL specific metadata properties
        // use sub-key metadata.bill.<>
        // leos ANNEX specific metadata properties
        PROPS_KEY_MAPPING.put("metadata.annex.docIndex", "annex:docIndex");
        PROPS_KEY_MAPPING.put("metadata.annex.docNumber", "annex:docNumber");
        PROPS_KEY_MAPPING.put("metadata.annex.docTitle", "annex:docTitle");
    }

    private static final Map<String, Class> PROPS_TYPE_MAPPING = new HashMap<>();
    static {
        // only for non-String type properties
        PROPS_TYPE_MAPPING.put("annex:docIndex", BigInteger.class);
        PROPS_TYPE_MAPPING.put("leos:collaborators", List.class);
        PROPS_TYPE_MAPPING.put("leos:milestoneComments", List.class);
        PROPS_TYPE_MAPPING.put("leos:initialCreationDate", GregorianCalendar.class);
    }

    private static final String ENABLED = ".enabled";
    private static final String RESOURCE = ".resource";
    private static final String FOLDER = ".folder";

    private CmisService cmisService = null;

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
        loadRepository(parameters);
    }

    private void loadRepository(Map<String, String> parameters) {
        LOG.info("Loading LEOS CMIS repository...");
        String repositoryId = parameters.get(ConfigConstants.REPOSITORY_ID);

        LOG.debug("Initiating repository [id={}] CMIS service...", repositoryId);
        cmisService = getService(new LeosCallContext(parameters));

        processConfiguration(parameters);
    }

    private void processConfiguration(Map<String, String> parameters) {
        LOG.debug("Processing configuration parameters...");
        Set<String> enabledKeys = filter(parameters.keySet(), null, ENABLED);
        for (String key : enabledKeys) {
            String featureKey = key.substring(0, key.length() - ENABLED.length());
            if (Boolean.parseBoolean(parameters.get(key))) {
                LOG.info("LEOS feature [{}] is enabled...", featureKey);
                processFolders(featureKey, parameters);
                processResources(featureKey, parameters);
            } else {
                LOG.info("LEOS feature [{}] is disabled!", featureKey);
            }
        }
    }

    private void processResources(String featureKey, Map<String, String> parameters) {
        LOG.debug("Processing resources... [feature={}]", featureKey);
        Set<String> resourceKeys = filter(parameters.keySet(), featureKey, RESOURCE);
        for (String key : resourceKeys) {
            String path = parameters.get(key);
            InputStream inStream = loadResource(path);
            if (inStream != null) {
                String documentKey = key.substring(0, key.length() - RESOURCE.length());
                processDocument(documentKey, parameters, inStream);
            } else {
                LOG.warn("Resource not found! [{}]", path);
            }
        }
    }

    private void processFolders(String featureKey, Map<String, String> parameters) {
        LOG.debug("Processing folders... [feature={}]", featureKey);
        String repositoryId = parameters.get(ConfigConstants.REPOSITORY_ID);
        Set<String> folderKeys = filter(parameters.keySet(), featureKey, FOLDER);
        for (String key : folderKeys) {
            String path = parameters.get(key);
            String folderId = createFolder(repositoryId, path);
        }
    }

    private Set<String> filter(Set<String> keys, String prefix, String suffix) {
        Set<String> validKeys = new HashSet<>();
        for (String key : keys) {
            boolean validPrefix = (prefix == null) || key.startsWith(prefix);
            boolean validSuffix = (suffix == null) || key.endsWith(suffix);
            if (validPrefix && validSuffix) {
                validKeys.add(key);
            }
        }
        return validKeys;
    }

    private InputStream loadResource(String path) {
        LOG.trace("Loading resource: [{}]", path);
        ClassLoader classLoader = getClass().getClassLoader();
        return classLoader.getResourceAsStream(path);
    }

    private void processDocument(String documentKey, Map<String, String> parameters, InputStream inStream) {
        LOG.debug("Processing document... [document={}]", documentKey);
        String repositoryId = parameters.get(ConfigConstants.REPOSITORY_ID);
        String path = parameters.get(documentKey + ".path");
        String name = parameters.get(documentKey + ".name");
        String fileName = parameters.get(documentKey + ".fileName");
        String mimeType = parameters.get(documentKey + ".mimeType");
        String primaryTypeId = parameters.get(documentKey + ".primaryTypeId");
        String category = parameters.get(documentKey + ".category");
        Map<String, String> optionalProps = processOptionalProperties(documentKey, parameters);
        List<String> secondaryTypeIds = processOptionalSecondaryTypes(documentKey, parameters);
        String folderId = createFolder(repositoryId, path);
        createDocument(repositoryId, folderId, name, fileName, mimeType, inStream, primaryTypeId, category, optionalProps, secondaryTypeIds);
    }

    private Map<String, String> processOptionalProperties(String documentKey, Map<String, String> parameters) {
        Map<String, String> props = new HashMap<>();
        for (String propAlias : PROPS_KEY_MAPPING.keySet()) {
            String configKey = documentKey + "." + propAlias;
            if (parameters.containsKey(configKey)) {
                props.put(PROPS_KEY_MAPPING.get(propAlias), parameters.get(configKey));
            }
        }
        return props;
    }

    private String createFolder(String repositoryId, String path) {
        String folderPath = (!CMIS_ROOT_FOLDER_PATH.equals(path) && path.endsWith(Filing.PATH_SEPARATOR)) ?
                            path.substring(0, path.length() - Filing.PATH_SEPARATOR.length()) : path;
        String folderId = findObjectId(repositoryId, folderPath);
        if (folderId != null) {
            LOG.trace("Folder exists: {} => {}", folderId, folderPath);
        } else {
            int sepIndex = folderPath.lastIndexOf(Filing.PATH_SEPARATOR);
            String folderName = folderPath.substring(sepIndex + 1, folderPath.length());
            String parentPath = folderPath.substring(0, sepIndex + 1);
            String parentId = createFolder(repositoryId, parentPath);
            PropertiesImpl props = new PropertiesImpl();
            props.addProperty(new PropertyIdImpl(PropertyIds.OBJECT_TYPE_ID, CMIS_FOLDER_TYPE_ID));
            props.addProperty(new PropertyStringImpl(PropertyIds.NAME, folderName));
            folderId = cmisService.createFolder(repositoryId, props, parentId, null, null, null, null);
            LOG.trace("Folder created: {} => {}", folderId, folderPath);
        }
        return folderId;
    }

    private void createDocument(String repositoryId, String folderId, String name,
                                String fileName, String mimeType, InputStream inStream,
                                String primaryTypeId, String category, Map<String, String> optionalProps,
                                List<String> secondaryTypeIds) {
        LOG.trace("Creating document... [type={}, category={}, name={}]", primaryTypeId, category, name);
        PropertiesImpl props = new PropertiesImpl();
        props.addProperty(new PropertyIdImpl(PropertyIds.OBJECT_TYPE_ID, primaryTypeId));
        props.addProperty(new PropertyStringImpl(PropertyIds.NAME, name));
        props.addProperty(new PropertyStringImpl(LEOS_CATEGORY_PROP_ID, category));

        for (Map.Entry<String, String> entry : optionalProps.entrySet()) {
            String propKey = entry.getKey();
            String propValue = entry.getValue();
            if (PROPS_TYPE_MAPPING.containsKey(propKey)) {
                Class propType = PROPS_TYPE_MAPPING.get(propKey);
                if ((propType != null) && BigInteger.class.isAssignableFrom(propType)) {
                    props.addProperty(new PropertyIntegerImpl(propKey, new BigInteger(propValue)));
                } else if ((propType != null) && List.class.isAssignableFrom(propType)) {
                    List<String> values = split(propValue,";");
                    props.addProperty(new PropertyStringImpl(propKey, values));
                } else {
                    LOG.warn("Unable to handle optional property type! [key={}, type={}]", propKey, propType);
                }
            } else {
                props.addProperty(new PropertyStringImpl(propKey, propValue));
            }
        }

        props.addProperty(new PropertyIdImpl(PropertyIds.SECONDARY_OBJECT_TYPE_IDS, secondaryTypeIds));
        ContentStream cs = new ContentStreamImpl(fileName, null, mimeType, inStream);
        String id = cmisService.createDocument(repositoryId, props, folderId, cs, null, null, null, null, null);
        LOG.trace("Document created: {} => {}", id, name);
    }

    private String findObjectId(String repositoryId, String objectPath) {
        try {
            ObjectData objectData = cmisService.getObjectByPath(repositoryId, objectPath, null, false,
                                    IncludeRelationships.NONE, Constants.RENDITION_NONE, false, false, null);
            return objectData.getId();
        } catch (CmisObjectNotFoundException ex) {
            return null;
        }
    }

    private List<String> processOptionalSecondaryTypes(String documentKey, Map<String, String> parameters) {
        final String typesCreatorClassName = parameters.get("InMemoryServer.TypesCreatorClass");
        final boolean enabled = LeosSecondaryTypesTypeSystemCreator.class.getName().equals(typesCreatorClassName);
        return enabled ? split(parameters.get(documentKey + ".secondaryTypeIds"), ";") : Collections.<String>emptyList();
    }

    private List<String> split(String input, String separator) {
        List<String> values = Collections.emptyList();
        if ((input != null) && (separator != null)) {
             StringTokenizer tokenizer = new StringTokenizer(input, separator, false);
             if (tokenizer.countTokens() > 0) {
                 values = new ArrayList<>(tokenizer.countTokens());
                 while (tokenizer.hasMoreTokens()) {
                     values.add(tokenizer.nextToken());
                 }
             }
         }
        return values;
    }
}
