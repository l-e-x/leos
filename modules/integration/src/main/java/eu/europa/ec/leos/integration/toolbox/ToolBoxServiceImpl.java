/*
 * Copyright 2017 European Commission
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
package eu.europa.ec.leos.integration.toolbox;

import eu.europa.ec.leos.integration.ToolBoxService;
import eu.europa.ec.leos.integration.toolbox.jaxb.beans.ToolboxManifest;
import eu.europa.ec.leos.integration.toolbox.wsdl.*;
import eu.europa.ec.leos.integration.utils.zip.ZipPackageUtil;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Service
class ToolBoxServiceImpl implements ToolBoxService {
    private static Logger LOG = LoggerFactory.getLogger(ToolBoxServiceImpl.class);

    @Value("#{integrationProperties['leos.export.url']}")
    private String toolBoxWebServiceUrl;

    @Value("#{integrationProperties['leos.export.username']}")
    private String toolBoxUserName;

    @Value("#{integrationProperties['leos.export.mimetype']}")
    private String mimeTypeContent;

    private static final QName SERVICE_NAME = new QName("http://ec.europa.eu/digit/toolboxcode/v3.0", "ToolboxCoDe");

    public String createJob(File legisWritePackage, String zipPackageName, String mailAddress) throws IOException, JAXBException {
        Validate.notNull(legisWritePackage);
        Validate.notNull(mailAddress);

        LOG.trace("Creating Tool Box job using mail address {}", mailAddress);

        CreateJobParameters parameters = new CreateJobParameters();
        ArrayOfJobParameter jobParameters = new ArrayOfJobParameter();
        
        JobParameter jobParameter = new JobParameter();
        // Setting destination mail
        jobParameter.setName("destination");
        jobParameter.setValue("ms:" + mailAddress);
        jobParameters.getJobParameter().add(jobParameter);

        parameters.setParameters(jobParameters);

        // Setting file content and name
        ArrayOfFileContent arrayOfFileContent = new ArrayOfFileContent();
        FileContent fileContent = new FileContent();
        File filePackage = null;
        try {
            filePackage = createZipWsPayload(legisWritePackage, zipPackageName + ".zip");
            fileContent.setFileName(filePackage.getName());
            fileContent.setFileBody(Files.readAllBytes(filePackage.toPath()));
        }
        finally {
            if (filePackage != null && filePackage.exists()) {
                filePackage.delete();
            }
        }
        arrayOfFileContent.getFileContent().add(fileContent);
        parameters.setFiles(arrayOfFileContent);
        
        ToolboxCoDe service;
        String jobId;
        try {
            service = new ToolboxCoDe(new URL(toolBoxWebServiceUrl), SERVICE_NAME);
            ToolboxCoDeSoap client = service.getToolboxCoDeSoap();
            jobId = client.createJob(toolBoxUserName, parameters); // create a job with the given user and the created parameters
        } catch (SOAPFaultException sfe) {
            LOG.error("Webservice error occurred in method createJob() while sending job to ToolBox: {}", sfe.getMessage());
            throw new WebServiceException(sfe.getMessage());
        } catch (WebServiceException wse) {
            LOG.error("Webservice error occurred in method createJob() while sending job to ToolBox: {}", wse.getCause().getCause().getCause().getMessage());
            throw new WebServiceException(wse.getCause().getCause().getCause().getMessage());
        } catch (Exception ex) {
            LOG.error("Unexpected error occurred in method createJob() while sending job to ToolBox: {}", ex.getMessage());
            throw ex;
        }
        
        return jobId;
    }

    public String getJobResult(String jobId) throws MalformedURLException {
        ToolboxCoDe service = new ToolboxCoDe(new URL(toolBoxWebServiceUrl), SERVICE_NAME);
        ToolboxCoDeSoap client = service.getToolboxCoDeSoap();
        return client.getJobResult(jobId, WebServiceJobDataType.CONVERTED_FILES).getStatusMessage(); // get job result with the given job id 
    }

    File createZipWsPayload(File legisWritePackage, String zipPackageName)  throws IOException, JAXBException {
        Map<String, Object> contentToZip = new HashMap<String, Object>();
        ByteArrayOutputStream manifestFileContent = null;
        try {
            manifestFileContent = createManifestFile(legisWritePackage.getName());
            contentToZip.put("manifest.xml", manifestFileContent);

            contentToZip.put("mimetype", mimeTypeContent);

            contentToZip.put(legisWritePackage.getName(), legisWritePackage);

            return ZipPackageUtil.zipFiles(zipPackageName, contentToZip);
        }
        finally {
            if (manifestFileContent != null) {
                manifestFileContent.close();
            }
        }
    }

    private ByteArrayOutputStream createManifestFile(String leosPackageName) throws JAXBException {
        ToolboxManifest toolboxManifest = new ToolboxManifest();

        ToolboxManifest.Files files = new ToolboxManifest.Files();
        ToolboxManifest.Files.File file = new ToolboxManifest.Files.File();
        file.setCreated("false");
        file.setPurged("false");
        file.setId("File01");
        file.setValue(leosPackageName);
        files.setFile(file);
        toolboxManifest.setFiles(files);

        ToolboxManifest.Actions.Action action = new ToolboxManifest.Actions.Action();
        action.setId("Action01");
        action.setName("AkomaNtosoToLegisWrite");

        ToolboxManifest.Actions.Action.ActionData.Data data = new ToolboxManifest.Actions.Action.ActionData.Data();
        data.setKey("inputfile");
        data.setValue("File01");
        ToolboxManifest.Actions.Action.ActionData actionData = new ToolboxManifest.Actions.Action.ActionData();
        actionData.setData(data);
        action.setActionData(actionData);
        ToolboxManifest.Actions actions = new ToolboxManifest.Actions();
        actions.setAction(action);
        toolboxManifest.setActions(actions);

        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ToolboxManifest.class );
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true );
            jaxbMarshaller.marshal(toolboxManifest, byteOutputStream);
        } catch (JAXBException e) {
            LOG.error("Error while creating manifest xml file {}", e.getMessage());
            throw e;
        }
        return byteOutputStream;
    }
}
