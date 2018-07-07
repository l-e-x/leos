package org.apache.chemistry.opencmis.inmemory.server;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.Constants;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.inmemory.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;

public class LeosServiceFactoryImpl extends InMemoryServiceFactoryImpl {

    private static final Logger LOG = LoggerFactory.getLogger(LeosServiceFactoryImpl.class.getName());

    @Override
    public void init(Map<String, String> parameters) {
        super.init(parameters);
        loadInitialContent(parameters);
    }

    private void loadInitialContent(Map<String, String> parameters) {
        LOG.info("Loading initial content into LEOS repository...");

        try {
            LOG.info("Loading configuration parameters...");
            String repositoryId = parameters.get(ConfigConstants.REPOSITORY_ID);
            String folderTypeId = parameters.get(ConfigConstants.FILLER_FOLDER_TYPE_ID);
            String documentTypeId = parameters.get(ConfigConstants.FILLER_DOCUMENT_TYPE_ID);
            String mimeType = parameters.get(ConfigConstants.CONTENT_KIND);

            LOG.info("Getting repository service...");
            CmisService cmisService = getService(new LeosCallContext());
            RepositoryInfo repoInfo = cmisService.getRepositoryInfo(repositoryId, null);
            String rootFolderId = repoInfo.getRootFolderId();

            if (Boolean.parseBoolean(parameters.get("leos.samples.enabled"))) {
                LOG.info("LEOS samples are enabled!");
                String samplesPath = parameters.get("leos.samples.path");
                String paramKey = "leos.samples.files";
                String folderId = createPath(cmisService, repositoryId, rootFolderId, folderTypeId, samplesPath);
                createFiles(cmisService, repositoryId, folderId, samplesPath, documentTypeId, mimeType, parameters, paramKey, true, true);
            } else {
                LOG.info("LEOS samples are disabled!");
            }

            if (Boolean.parseBoolean(parameters.get("leos.templates.enabled"))) {
                LOG.info("LEOS templates are enabled!");
                String templatesPath = parameters.get("leos.templates.path");
                String paramKey = "leos.templates.files";
                String folderId = createPath(cmisService, repositoryId, rootFolderId, folderTypeId, templatesPath);
                createFiles(cmisService, repositoryId, folderId, templatesPath, documentTypeId, mimeType, parameters, paramKey, false, false);
            } else {
                LOG.info("LEOS templates are disabled!");
            }

            if (Boolean.parseBoolean(parameters.get("leos.workspaces.enabled"))) {
                LOG.info("LEOS workspaces are enabled!");
                String workspacesPath = parameters.get("leos.workspaces.path");
                String paramKey = "leos.workspaces.files";
                String folderId = createPath(cmisService, repositoryId, rootFolderId, folderTypeId, workspacesPath);
                createFiles(cmisService, repositoryId, folderId, workspacesPath, documentTypeId, mimeType, parameters, paramKey, false, false);
            } else {
                LOG.info("LEOS workspaces are disabled!");
            }
        } catch (Exception ex) {
            LOG.error("Unable to load initial repository content!", ex);
            throw new RuntimeException(ex);
        }
    }

    private String createPath(CmisService cmisService, String repositoryId, String rootFolderId, String folderTypeId, String path) {
        LOG.info("Creating path: {}", path);
        String parentPath = "/";
        String parentObjectId = rootFolderId;
        StringTokenizer pathTokenizer = new StringTokenizer(path, "/", false);

        while (pathTokenizer.hasMoreTokens()) {
            String folderName = pathTokenizer.nextToken();
            String folderPath = parentPath + folderName;
            String folderId = findObjectId(cmisService, repositoryId, folderPath);

            if (folderId != null) {
                LOG.info("Folder already exists: {} => {}", folderId, folderPath);
            } else {
                LOG.info("Creating folder: {}", folderName);

                PropertiesImpl props = new PropertiesImpl();
                props.addProperty(new PropertyIdImpl(PropertyIds.OBJECT_TYPE_ID, folderTypeId));
                props.addProperty(new PropertyStringImpl(PropertyIds.NAME, folderName));

                folderId = cmisService.createFolder(repositoryId, props, parentObjectId, null, null, null, null);
            }

            parentPath = folderPath + "/";
            parentObjectId = folderId;
        }

        return parentObjectId;
    }

    private void createFiles(CmisService cmisService, String repositoryId, String folderId, String folderPath, String documentTypeId, String mimeType, Map<String, String> parameters, String paramKey, boolean renameFiles, boolean setContributors) {
        LOG.info("Creating files...");
        String files = parameters.get(paramKey);
        StringTokenizer filesTokenizer = new StringTokenizer(files, ";", false);
        ClassLoader classLoader = getClass().getClassLoader();
        ArrayList<String> contributors = new ArrayList<String>();

        if(setContributors) {
            String contributorsString = parameters.get("leos.samples.contributorIds");
            StringTokenizer contributorTokens = new StringTokenizer(contributorsString, ";", false);

            while (contributorTokens.hasMoreTokens()) {
                String contributorId = contributorTokens.nextToken();
                contributors.add(contributorId);
            }
        }

        while (filesTokenizer.hasMoreTokens()) {
            String fileName = filesTokenizer.nextToken();
            LOG.info("Creating document: {} [{}]", fileName, mimeType);

            String filePath = folderPath + fileName;
            InputStream is = classLoader.getResourceAsStream(filePath);
            ContentStream cs = new ContentStreamImpl(fileName, null, mimeType, is);

            String documentName = renameFiles ? parameters.get(paramKey + "." + fileName) : fileName;
            LOG.info("Document name: {}", documentName);

            PropertiesImpl props = new PropertiesImpl();
            props.addProperty(new PropertyIdImpl(PropertyIds.OBJECT_TYPE_ID, documentTypeId));
            props.addProperty(new PropertyStringImpl(PropertyIds.NAME, documentName));
            String TITLE= "leos:title";
            String TEMPLATE= "leos:template";
            String LANGUAGE ="leos:language";
            String STAGE ="leos:stage";
            String SYSTEM ="leos:system";
            String AUTHOR_ID="leos:authorId";
            String AUTHOR_NAME="leos:authorName";
            String CONTRIBUTOR_IDS ="leos:contributorIds";
            String CONTRIBUTOR_NAMES ="leos:contributorNames";

            props.addProperty(new PropertyStringImpl(TITLE, documentName));
            props.addProperty(new PropertyStringImpl(LANGUAGE, "EN"));
            props.addProperty(new PropertyStringImpl(TEMPLATE, "SJ-016"));
            props.addProperty(new PropertyStringImpl(CONTRIBUTOR_IDS, contributors));
            props.addProperty(new PropertyStringImpl(CONTRIBUTOR_NAMES, contributors));
            props.addProperty(new PropertyStringImpl(AUTHOR_ID, "LEOS"));
            props.addProperty(new PropertyStringImpl(AUTHOR_NAME, "LEOS"));

            //TODO find a way to do cleaner impl
            if(documentName.startsWith("FEEDBACK")||documentName.startsWith("Feedback")){
                props.addProperty(new PropertyStringImpl(STAGE, "REVIEW"));
                props.addProperty(new PropertyStringImpl(SYSTEM, "CISNET"));
            }
            else if(documentName.startsWith("Edition")||documentName.startsWith("EDITION")){
                props.addProperty(new PropertyStringImpl(STAGE, "EDIT"));
                props.addProperty(new PropertyStringImpl(SYSTEM, "CISNET"));
            }
            else  {
                props.addProperty(new PropertyStringImpl(STAGE, "DRAFT"));
                props.addProperty(new PropertyStringImpl(SYSTEM, "LEOS"));
            }
            
            cmisService.createDocument(repositoryId, props, folderId, cs, null, null, null, null, null);
        }
    }

    private String findObjectId(CmisService cmisService, String repositoryId, String objectPath) {
        String objectId = null;
        try {
            ObjectData objectData = cmisService.getObjectByPath(repositoryId, objectPath, null, false,
                    IncludeRelationships.NONE, Constants.RENDITION_NONE, false, false, null);
            objectId = (objectData != null) ? objectData.getId() : null;
        } catch (CmisObjectNotFoundException ex) {
            // do nothing
        }
        return objectId;
    }

    private class LeosCallContext implements CallContext {

        public String get(String key) {
            return null;
        }

        public String getBinding() {
            return null;
        }

        public boolean isObjectInfoRequired() {
            return false;
        }

        public CmisVersion getCmisVersion() {
            return CmisVersion.CMIS_1_1;
        }

        public String getRepositoryId() {
            return "LEOS";
        }

        public String getLocale() {
            return null;
        }

        public BigInteger getOffset() {
            return null;
        }

        public BigInteger getLength() {
            return null;
        }

        public String getPassword() {
            return null;
        }

        public String getUsername() {
            return "LEOS";
        }

        public File getTempDirectory() {
            return null;
        }

        public boolean encryptTempFiles() {
            return true;
        }

        public int getMemoryThreshold() {
            return 4 * 1024 * 1024;
        }

        public long getMaxContentSize() {
            return 4 * 1024 * 1024 * 1024;
        }
    }
}
