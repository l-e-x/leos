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

package eu.europa.ec.leos.services.controllers;

import com.google.common.eventbus.EventBus;
import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import eu.europa.ec.leos.domain.cmis.document.LegDocument;
import eu.europa.ec.leos.model.event.MilestoneUpdatedEvent;
import eu.europa.ec.leos.security.AuthClient;
import eu.europa.ec.leos.security.TokenService;
import eu.europa.ec.leos.services.compare.ContentComparatorContext;
import eu.europa.ec.leos.services.compare.ContentComparatorService;
import eu.europa.ec.leos.services.content.processor.TransformationService;
import eu.europa.ec.leos.services.export.ExportOptions;
import eu.europa.ec.leos.services.export.ExportService;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.vo.token.JsonTokenReponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;

import static eu.europa.ec.leos.services.compare.ContentComparatorService.*;

@RestController
public class LeosApiController {

    private static final Logger LOG = LoggerFactory.getLogger(LeosApiController.class);

    @Autowired
    private PackageService packageService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TransformationService transformationService;

    @Autowired
    private ContentComparatorService comparatorService;

    @Autowired
    private EventBus leosApplicationEventBus;

    @Autowired
    private ExportService exportService;

    private final int SINGLE_COLUMN_MODE = 1;
    private final int TWO_COLUMN_MODE = 2;
    private static final String GRANT_TYPE = "grant-type";
    private static final String BEARER_GRANT_TYPE = "jwt-bearer";
    private static final String BEARER_PARAMETER = "assertion";

    @RequestMapping(value = "/token", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getToken(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
    
        final String grantType = request.getHeader(GRANT_TYPE);
        if (!StringUtils.isEmpty(grantType) && grantType.contains(BEARER_GRANT_TYPE)) {
            String token = request.getHeader(BEARER_PARAMETER);
            AuthClient authClient = tokenService.validateClientByJwtToken(token);
            if (authClient.isVerified()) {
                LOG.debug("Client '{}' correctly validated with jwt-bearer token provided", authClient.getName());
                JsonTokenReponse jsonToken = new JsonTokenReponse(tokenService.getAccessToken(), "jwt", System.currentTimeMillis() + 3600000, null, null);
                LOG.debug("Created accessToken for the Client '{}", authClient.getName());
                return new ResponseEntity<>(jsonToken, HttpStatus.OK);
            } else {
                LOG.warn("Authorization failed! A client is asking for an accessToken, but the provided '{}' token is not valid!", BEARER_GRANT_TYPE);
                return new ResponseEntity<>("Wrong jwt-bearer token!", HttpStatus.FORBIDDEN);
            }
        } else {
            LOG.warn("Authorization failed! Wrong Headers: '{}' is missing or contains a wrong value", GRANT_TYPE);
        }
    
        return new ResponseEntity<>("Wrong Headers!", HttpStatus.FORBIDDEN);
    }
    
    @RequestMapping(value = "/secured/compare", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResponseEntity<Object> compareContents(HttpServletRequest request, @RequestParam("mode") int mode,
            @RequestParam("firstContent") MultipartFile firstContent, @RequestParam("secondContent") MultipartFile secondContent) {
        if ((mode != SINGLE_COLUMN_MODE) && (mode != TWO_COLUMN_MODE)) {
            return new ResponseEntity<>("Mode value has to be 1(single column mode) or 2(two column mode)", HttpStatus.BAD_REQUEST);
        }
        try {
            String contextPath = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString();
            String baseContextPath = contextPath.substring(0, StringUtils.ordinalIndexOf(contextPath, "/", 4));
            String firstContentHtml = transformationService
                    .formatToHtml(new ByteArrayInputStream(firstContent.getBytes()), baseContextPath, null)
                    .replaceAll("(?i)(href|onClick)=\".*?\"", "");
            String secondContentHtml = transformationService
                    .formatToHtml(new ByteArrayInputStream(secondContent.getBytes()), baseContextPath, null)
                    .replaceAll("(?i)(href|onClick)=\".*?\"", "");
            if (mode == SINGLE_COLUMN_MODE) {
                return new ResponseEntity<>(new String[]{comparatorService.compareContents(new ContentComparatorContext.Builder(firstContentHtml, secondContentHtml)
                        .withAttrName(ATTR_NAME)
                        .withRemovedValue(CONTENT_REMOVED_CLASS)
                        .withAddedValue(CONTENT_ADDED_CLASS)
                        .build())}, HttpStatus.OK);
            }
            return new ResponseEntity<>(comparatorService.twoColumnsCompareContents(new ContentComparatorContext.Builder(firstContentHtml, secondContentHtml).build()), HttpStatus.OK);
        } catch (Exception ex) {
            LOG.error("Error occurred while comparing contents", ex.getMessage());
            return new ResponseEntity<>("Error occurred while comparing contents: ", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/secured/search/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getProposalsForUser(@PathVariable("userId") String userId) {
        try {
            return new ResponseEntity<>(packageService.getLegDocumentDetailsByUserId(userId).toArray(), HttpStatus.OK);
        } catch (Exception ex) {
            LOG.error("Exception occurred in search "+ ex.getMessage());
            return new ResponseEntity<>("Error Occurred while getting the Leg Document for user " + userId, HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/secured/searchlegfile/{legFileId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getLegFile(@PathVariable("legFileId") String legFileId) {
        boolean isStatusUpdated = false;
        LeosLegStatus currentStatus = null;
        try {
            LegDocument legDocument = packageService.findLegDocumentById(legFileId);
            currentStatus = legDocument.getStatus();
            if (!(currentStatus == LeosLegStatus.IN_PREPARATION || currentStatus == LeosLegStatus.FILE_ERROR)) {
                byte[] file = legDocument.getContent().get().getSource().getBytes();
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Disposition", "attachment; filename=" + legDocument.getName());
                headers.setContentLength(file.length);
                LegDocument updatedLegDocument = packageService.updateLegDocument(legFileId, LeosLegStatus.SENT_TO_CONSULTATION);
                leosApplicationEventBus.post(new MilestoneUpdatedEvent(updatedLegDocument));
                isStatusUpdated = true;
                return new ResponseEntity<>(file, headers, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Leg file with Id" + legFileId + " in status " + currentStatus, HttpStatus.NOT_FOUND);
            }
        } catch (Exception ex) {
            // in case of any exception reverting to current status
            if (isStatusUpdated) {
                LegDocument updatedLegDocument = packageService.updateLegDocument(legFileId, currentStatus);
                leosApplicationEventBus.post(new MilestoneUpdatedEvent(updatedLegDocument));
            }
            LOG.error("Exception occurred in downloading leg file "+ ex.getMessage());
            return new ResponseEntity<>("Error Occurred while sending the leg file  for Leg File Id " +
                    legFileId, HttpStatus.NOT_FOUND);
        }

    }
    
    @RequestMapping(value = "/secured/renditionfromleg", method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getPdfFromLegFile(@RequestParam("legFile") MultipartFile legFile, @RequestParam("type") String type) {
        File legFileTemp = null;
        try {
            final ExportOptions exportOptions;
            switch (type.toLowerCase()) {
                case "pdf":
                    exportOptions = ExportOptions.TO_PDF_LW;
                    break;
                case "lw":
                    exportOptions = ExportOptions.TO_WORD_LW;
                    break;
                default:
                    throw new IllegalArgumentException("Wrong value on parameter type '" + type + "'");
            }

            //create a temporary file with the bytes arrived as input
            legFileTemp = File.createTempFile("tmp_", ".leg");
            FileUtils.writeByteArrayToFile(legFileTemp, legFile.getBytes());

            byte[] renditionFile = exportService.exportToToolboxCoDe(legFileTemp, exportOptions);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Disposition", "attachment; filename=" + "TOOLBOX_RESULT_" + System.currentTimeMillis());
            headers.setContentLength(renditionFile.length);

            LOG.info("Returning zip file of {} bytes containing renditions to the external caller." + renditionFile.length);
            return new ResponseEntity<>(renditionFile, headers, HttpStatus.OK);
        } catch (Exception e) {
            String errMsg = "Error occurred while creating rendition file: " + e.getMessage();
            LOG.error(errMsg, e);
            return new ResponseEntity<>(errMsg, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally{
            if (legFileTemp != null && legFileTemp.exists()) {
                legFileTemp.delete();
            }
        }
    }
    
    
    
    /** The following methods are temporarily. They will be removed once ISC group fully adapt to the new security changes.
     * From now on our endpoint will be accessible in a secure way as:
     * - /api/secured/endpointName
     *
     * The only endpoint not secured is /token which can be accessed
     * - /api/token
     *
     * The following *_compatibility methods will keep the code working for the ongoing calls:
     * - /secured-api/method
     * */
    @RequestMapping(value = "/compare", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResponseEntity<Object> compareContents_compatibility(HttpServletRequest request, @RequestParam("mode") int mode,
               @RequestParam("firstContent") MultipartFile firstContent, @RequestParam("secondContent") MultipartFile secondContent){
        return compareContents(request, mode, firstContent, secondContent);
    }
    
    @RequestMapping(value = "/search/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getProposalsForUser_compatibility(@PathVariable("userId") String userId) {
        return getProposalsForUser(userId);
    }
    
    @RequestMapping(value = "/searchlegfile/{legFileId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getLegFile_compatibility(@PathVariable("legFileId") String legFileId) {
        return getLegFile(legFileId);
    }
    
    @RequestMapping(value = "/renditionfromleg", method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getPdfFromLegFile_compatibility(@RequestParam("legFile") MultipartFile legFile, @RequestParam("type") String type) {
        return getPdfFromLegFile(legFile, type);
    }
    
}

