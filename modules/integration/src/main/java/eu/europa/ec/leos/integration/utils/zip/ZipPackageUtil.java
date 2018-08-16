/*
 * Copyright 2018 European Commission
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
package eu.europa.ec.leos.integration.utils.zip;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipPackageUtil {
    private static Logger LOG = LoggerFactory.getLogger(ZipPackageUtil.class);

    public static File zipFiles(String zipFileName, Map<String, Object> contentToZip) throws IOException{
        String fileExtension = "." + FilenameUtils.getExtension(zipFileName);
        String fileName = FilenameUtils.getBaseName(zipFileName);
        File zipFile = null;
        ZipOutputStream zipOutputStream = null;
        try {
            zipFile = File.createTempFile(fileName, fileExtension);
            zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));
    
            for (Map.Entry<String, Object> entry : contentToZip.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof File) {
                    File fileValue = (File) value;
                    ZipEntry ze = new ZipEntry(key);
                    zipOutputStream.putNextEntry(ze);
                    FileInputStream fileInputStream = new FileInputStream(fileValue);
                    IOUtils.copy(fileInputStream,zipOutputStream);
                    fileInputStream.close();
                    zipOutputStream.closeEntry();
                }
                else if (value instanceof ByteArrayOutputStream) {
                    ByteArrayOutputStream byteArrayOutputStreamValue = (ByteArrayOutputStream) value;
                    ZipEntry ze = new ZipEntry(key);
                    zipOutputStream.putNextEntry(ze);
                    zipOutputStream.write(byteArrayOutputStreamValue.toByteArray());
                    zipOutputStream.closeEntry();
                    byteArrayOutputStreamValue.close();
                }
                else if (value instanceof String) {
                    String stringValue = (String) value;
                    ZipEntry ze = new ZipEntry(key);
                    zipOutputStream.putNextEntry(ze);
                    zipOutputStream.write(stringValue.getBytes());
                    zipOutputStream.closeEntry();
                }
                else if (value instanceof byte[]) {
                    byte[] byteArrayValue = (byte[]) value;
                    ZipEntry ze = new ZipEntry(key);
                    zipOutputStream.putNextEntry(ze);
                    zipOutputStream.write(byteArrayValue);
                    zipOutputStream.closeEntry();
                }
            }
            return zipFile;
        }
        catch (IOException e) {
            LOG.error("Error creating zip package: {}", e.getMessage());
            if (zipFile !=null && zipFile.exists()) {
                zipFile.delete();
            }
            throw new IOException(e.getMessage());
        }
        finally {
            if (zipOutputStream !=null) {
                zipOutputStream.close();
            }
        }
    }

    public static Map<String, Object> unzipFiles(File file) {
        Map<String, Object> unzippedFiles = new HashMap<>();
        byte[] buffer = new byte[1024];

        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String outputFolder = tempDir + "/unzip/" + file.getName() +"_"+ System.currentTimeMillis();
            // create output directory is not exists
            File folder = new File(outputFolder);
            if (!folder.exists()) {
                folder.mkdir();
            }
            // get the zip file content
            ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
            // get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {

                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);
                // create all non exists folders
                // else you will hit FileNotFoundException for compressed folder
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                unzippedFiles.put(newFile.getName(), newFile);
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

        } catch (IOException ex) {
            LOG.error("Error unzipping the file {} : {}", file.getName(), ex.getMessage());
        }

        return unzippedFiles;
    }
}
