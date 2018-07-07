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
import java.util.Map;
import java.util.zip.ZipEntry;
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
}
