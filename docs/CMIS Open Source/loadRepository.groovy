import org.apache.chemistry.opencmis.client.api.CmisObject
import org.apache.chemistry.opencmis.client.api.ObjectId
import org.apache.chemistry.opencmis.client.api.Session
import org.apache.chemistry.opencmis.commons.PropertyIds
import org.apache.chemistry.opencmis.commons.data.ContentStream
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId
import org.apache.chemistry.opencmis.commons.enums.VersioningState
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl

// NOTE set variables with full path on local file system

// ex. C:/devel/sources/pilot/releases/1.0.0-alpha7-council/CMIS/resources
def resourcesLocalPath = '<TODO>'

// ex. C:/devel/sources/pilot/releases/1.0.0-alpha7-council/CMIS/scripts/repository
def repoPropertiesLocalPath = '<TODO>'

def loader = new LeosLoader(resourcesLocalPath, repoPropertiesLocalPath, session)
loader.loadRepository()

/*-----------------------------------------------------------------------------
    Groovy class LeosLoader adapted from LeosInMemoryServiceFactory.java
-----------------------------------------------------------------------------*/

class LeosLoader {

    static final CMIS_PATH_SEPARATOR = '/'

    static final CMIS_ROOT_FOLDER_PATH = '/'

    static final CMIS_FOLDER_TYPE_ID = BaseTypeId.CMIS_FOLDER.value()

    static final LEOS_CATEGORY_PROP_ID = 'leos:category'

    static final PROPS_KEY_MAPPING = [
            // leos XML properties
            'language'                : 'leos:language',
            'template'                : 'leos:template',
            'title'                   : 'leos:title',
            // leos COMMON metadata properties
            'metadata.docTemplate'    : 'metadata:docTemplate',
            'metadata.docStage'       : 'metadata:docStage',
            'metadata.docType'        : 'metadata:docType',
            'metadata.docPurpose'     : 'metadata:docPurpose',
            'metadata.procedureType'  : 'metadata:procedureType',
            'metadata.actType'        : 'metadata:actType',
            // TODO leos MEMORANDUM specific metadata properties
            // use sub-key metadata.memorandum.<>
            // TODO leos BILL specific metadata properties
            // use sub-key metadata.bill.<>
            // leos ANNEX specific metadata properties
            'metadata.annex.docIndex' : 'annex:docIndex',
            'metadata.annex.docNumber': 'annex:docNumber',
            'metadata.annex.docTitle' : 'annex:docTitle'
    ]

    static final PROPS_TYPE_MAPPING = [
            // only for non-String type properties
            'annex:docIndex': BigInteger.class
    ]

    static final ENABLED = '.enabled'
    static final RESOURCE = '.resource'
    static final FOLDER = '.folder'

    static final REPOSITORY_ID = 'InMemoryServer.RepositoryId'
    static final TYPES_CREATOR_CLASS = 'InMemoryServer.TypesCreatorClass'
    static final LEOS_SECONDARY_TYPES_CREATOR_CLASS = 'eu.europa.ec.leos.cmis.types.LeosSecondaryTypesTypeSystemCreator'

    final String RESOURCES_LOCAL_PATH
    final Map<String, String> PARAMETERS
    final Session SESSION

    LeosLoader(String resourcesLocalPath, String repoPropertiesLocalPath, Session session) {
        this.RESOURCES_LOCAL_PATH = resourcesLocalPath

        def repoPropertiesFilePropsPath = repoPropertiesLocalPath + '/repository_os.properties'
        println "Loading properties file: $repoPropertiesFilePropsPath"

        def properties = new java.util.Properties()

        def propsFile = new File(repoPropertiesFilePropsPath)
        propsFile.withInputStream { s ->
            properties.load(s)
        }

        PARAMETERS = (Map) properties
        SESSION = session
    }

    void loadRepository() {
        String repositoryId = PARAMETERS.get(REPOSITORY_ID)
        println "Loading LEOS CMIS repository... [id=$repositoryId]"
        processConfiguration(PARAMETERS)
        println "...finished!"
    }

    private void processConfiguration(Map<String, String> parameters) {
        println "Processing configuration parameters..."
        Set<String> enabledKeys = filter(parameters.keySet(), null, ENABLED)
        for (String key : enabledKeys) {
            String featureKey = key.substring(0, key.length() - ENABLED.length())
            if (Boolean.parseBoolean(parameters.get(key))) {
                println "LEOS feature [$featureKey] is enabled..."
                processFolders(featureKey, parameters)
                processResources(featureKey, parameters)
            } else {
                println "LEOS feature [$featureKey] is disabled!"
            }
        }
    }

    private void processResources(String featureKey, Map<String, String> parameters) {
        println "Processing resources... [feature=$featureKey]"
        Set<String> resourceKeys = filter(parameters.keySet(), featureKey, RESOURCE)
        for (String key : resourceKeys) {
            String path = parameters.get(key)
            InputStream inStream = loadResource(path)
            if (inStream != null) {
                String documentKey = key.substring(0, key.length() - RESOURCE.length())
                processDocument(documentKey, parameters, inStream)
            } else {
                println "Resource not found! [$path]"
            }
        }
    }

    private void processFolders(String featureKey, Map<String, String> parameters) {
        println "Processing folders... [feature=$featureKey]"
        String repositoryId = parameters.get(REPOSITORY_ID)
        Set<String> folderKeys = filter(parameters.keySet(), featureKey, FOLDER)
        for (String key : folderKeys) {
            String path = parameters.get(key)
            String folderId = createFolder(repositoryId, path)
        }
    }

    private Set<String> filter(Set<String> keys, String prefix, String suffix) {
        Set<String> validKeys = new HashSet<>()
        for (String key : keys) {
            boolean validPrefix = (prefix == null) || key.startsWith(prefix)
            boolean validSuffix = (suffix == null) || key.endsWith(suffix)
            if (validPrefix && validSuffix) {
                validKeys.add(key)
            }
        }
        return validKeys
    }

    private InputStream loadResource(String path) {
        def localResourcePath = RESOURCES_LOCAL_PATH + path
        println "Loading resource: [$localResourcePath]"
        return new FileInputStream(localResourcePath)
    }

    private void processDocument(String documentKey, Map<String, String> parameters, InputStream inStream) {
        println "Processing document... [document=$documentKey]"
        String repositoryId = parameters.get(REPOSITORY_ID)
        String path = parameters.get(documentKey + ".path")
        String name = parameters.get(documentKey + ".name")
        String fileName = parameters.get(documentKey + ".fileName")
        String mimeType = parameters.get(documentKey + ".mimeType")
        String primaryTypeId = parameters.get(documentKey + ".primaryTypeId")
        String category = parameters.get(documentKey + ".category")
        Map<String, String> optionalProps = processOptionalProperties(documentKey, parameters)
        List<String> secondaryTypeIds = processOptionalSecondaryTypes(documentKey, parameters)
        String folderId = createFolder(repositoryId, path)
        createDocument(repositoryId, folderId, name, fileName, mimeType, inStream, primaryTypeId, category, optionalProps, secondaryTypeIds)
    }

    private Map<String, String> processOptionalProperties(String documentKey, Map<String, String> parameters) {
        Map<String, String> props = new HashMap<>()
        for (String propAlias : PROPS_KEY_MAPPING.keySet()) {
            String configKey = documentKey + "." + propAlias
            if (parameters.containsKey(configKey)) {
                props.put(PROPS_KEY_MAPPING.get(propAlias), parameters.get(configKey))
            }
        }
        return props
    }

    private String createFolder(String repositoryId, String path) {
        String folderPath = (!CMIS_ROOT_FOLDER_PATH.equals(path) && path.endsWith(CMIS_PATH_SEPARATOR)) ?
                path.substring(0, path.length() - CMIS_PATH_SEPARATOR.length()) : path
        String folderId = findObjectId(repositoryId, folderPath)
        if (folderId != null) {
            println "Folder exists: $folderId => $folderPath"
        } else {
            int sepIndex = folderPath.lastIndexOf(CMIS_PATH_SEPARATOR)
            String folderName = folderPath.substring(sepIndex + 1, folderPath.length())
            String parentPath = folderPath.substring(0, sepIndex + 1)
            String parentId = createFolder(repositoryId, parentPath)

            Map<String, Object> properties = [
                    (PropertyIds.OBJECT_TYPE_ID): CMIS_FOLDER_TYPE_ID,
                    (PropertyIds.NAME)          : folderName
            ]

            ObjectId parentObjectId = SESSION.createObjectId(parentId)

            folderId = SESSION.createFolder(properties, parentObjectId).getId()
            println "Folder created: $folderId => $folderPath"
        }
        return folderId
    }

    private void createDocument(String repositoryId, String folderId, String name,
                                String fileName, String mimeType, InputStream inStream,
                                String primaryTypeId, String category, Map<String, String> optionalProps,
                                List<String> secondaryTypeIds) {
        println "Creating document... [type=$primaryTypeId, category=$category, name=$name]"

        Map<String, Object> properties = [
                (PropertyIds.OBJECT_TYPE_ID): primaryTypeId,
                (PropertyIds.NAME)          : name,
                (LEOS_CATEGORY_PROP_ID)     : category
        ]

        for (Map.Entry<String, String> entry : optionalProps.entrySet()) {
            String propKey = entry.getKey()
            String propValue = entry.getValue()
            if (PROPS_TYPE_MAPPING.containsKey(propKey)) {
                Class propType = PROPS_TYPE_MAPPING.get(propKey)
                if ((propType != null) && BigInteger.class.isAssignableFrom(propType)) {
                    properties.put(propKey, new BigInteger(propValue))
                } else {
                    println "Unable to handle optional property type! [key=$propKey, type=$propType]"
                }
            } else {
                properties.put(propKey, propValue)
            }
        }

        properties.put(PropertyIds.SECONDARY_OBJECT_TYPE_IDS, secondaryTypeIds)
        ContentStream cs = new ContentStreamImpl(fileName, null, mimeType, inStream)
        ObjectId parentObjectId = SESSION.createObjectId(folderId)
        String id = SESSION.createDocument(properties, parentObjectId, cs, VersioningState.MAJOR)
        println "Document created: $id => $name"
    }

    private String findObjectId(String repositoryId, String objectPath) {
        try {
            CmisObject cmisObject = SESSION.getObjectByPath(objectPath)
            return cmisObject.getId()
        } catch (CmisObjectNotFoundException ex) {
            return null
        }
    }

    private List<String> processOptionalSecondaryTypes(String documentKey, Map<String, String> parameters) {
        final String typesCreatorClassName = parameters.get(TYPES_CREATOR_CLASS)
        final boolean enabled = LEOS_SECONDARY_TYPES_CREATOR_CLASS.equals(typesCreatorClassName)
        return enabled ? split(parameters.get(documentKey + ".secondaryTypeIds"), ";") : Collections.<String> emptyList()
    }

    private List<String> split(String input, String separator) {
        List<String> values = Collections.emptyList()
        if ((input != null) && (separator != null)) {
            StringTokenizer tokenizer = new StringTokenizer(input, separator, false)
            if (tokenizer.countTokens() > 0) {
                values = new ArrayList<>(tokenizer.countTokens())
                while (tokenizer.hasMoreTokens()) {
                    values.add(tokenizer.nextToken())
                }
            }
        }
        return values
    }
}
