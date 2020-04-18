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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ZipPackageUtil {
    private static Logger LOG = LoggerFactory.getLogger(ZipPackageUtil.class);

    public static File zipFiles(String zipFileName, Map<String, Object> contentToZip) throws IOException {
        String fileExtension = "." + FilenameUtils.getExtension(zipFileName);
        String fileName = FilenameUtils.getBaseName(zipFileName);
        File zipFile = null;
        ZipOutputStream zipOutputStream = null;
        try {
            zipFile = File.createTempFile(fileName, fileExtension);
            zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));
            addContentToOutputStream(zipOutputStream, contentToZip);
            return zipFile;
        } catch (IOException e) {
            LOG.error("Error creating zip package: {}", e.getMessage());
            if (zipFile != null && zipFile.exists()) {
                zipFile.delete();
            }
            throw new IOException(e.getMessage());
        } finally {
            if (zipOutputStream != null) {
                zipOutputStream.close();
            }
        }
    }

    public static byte[] zipByteArray(Map<String, Object> contentToZip) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(bos)) {
            addContentToOutputStream(zos, contentToZip);
            zos.close();
            return bos.toByteArray();
        }
    }

    private static void addContentToOutputStream(ZipOutputStream zipOutputStream, Map<String, Object> contentToZip) throws IOException {
        for (Map.Entry<String, Object> entry : contentToZip.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof File) {
                File fileValue = (File) value;
                ZipEntry ze = new ZipEntry(key);
                zipOutputStream.putNextEntry(ze);
                FileInputStream fileInputStream = new FileInputStream(fileValue);
                IOUtils.copy(fileInputStream, zipOutputStream);
                fileInputStream.close();
                zipOutputStream.closeEntry();
            } else if (value instanceof ByteArrayOutputStream) {
                ByteArrayOutputStream byteArrayOutputStreamValue = (ByteArrayOutputStream) value;
                ZipEntry ze = new ZipEntry(key);
                zipOutputStream.putNextEntry(ze);
                zipOutputStream.write(byteArrayOutputStreamValue.toByteArray());
                zipOutputStream.closeEntry();
                byteArrayOutputStreamValue.close();
            } else if (value instanceof String) {
                String stringValue = (String) value;
                ZipEntry ze = new ZipEntry(key);
                zipOutputStream.putNextEntry(ze);
                zipOutputStream.write(stringValue.getBytes(UTF_8));
                zipOutputStream.closeEntry();
            } else if (value instanceof byte[]) {
                byte[] byteArrayValue = (byte[]) value;
                ZipEntry ze = new ZipEntry(key);
                zipOutputStream.putNextEntry(ze);
                zipOutputStream.write(byteArrayValue);
                zipOutputStream.closeEntry();
            }
        }
    }

    public static Map<String, Object> unzipByteArray(byte[] zipppedData) throws IOException {
        Map<String, Object> unzippedFiles = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipppedData))) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    continue;
                }
                unzippedFiles.put(ze.getName(), IOUtils.toByteArray(zis));
            }
            zis.closeEntry();
        }
        return unzippedFiles;
    }

    public static Map<String, Object> unzipFiles(File file, String unzipPath) {
        Map<String, Object> unzippedFiles = new HashMap<>();
        String tempDir = System.getProperty("java.io.tmpdir");
        String outputFolder = tempDir + unzipPath + file.getName() + "_" + System.currentTimeMillis();
        // create output directory is not exists
        File folder = new File(outputFolder);
        if (!folder.exists()) {
            folder.mkdir();
        }
        // get the zip file content with try-with-resources
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file)))  {
            // get the zipped file list entry
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);
                // create all non exists folders
                // else you will hit FileNotFoundException for compressed folder
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                IOUtils.copy(zis, fos);
                fos.close();
                unzippedFiles.put(newFile.getName(), newFile);
            }
            // closeEntry should not be required. In the next step the stream will be closed.
            // close will be done by the try-with-resources block
        } catch (IOException ex) {
            LOG.error("Error unzipping the file {} : {}", file.getName(), ex.getMessage());
        }
        return unzippedFiles;
    }
}
