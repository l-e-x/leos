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
package eu.europa.ec.leos.annotate.services.impl;

import eu.europa.ec.leos.annotate.services.UUIDGeneratorService;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

@Service
public class UUIDGeneratorServiceImpl implements UUIDGeneratorService {

    // based on implementation found at https://stackoverflow.com/questions/772802/storing-uuid-as-base64-string/18057117#18057117
    private static Base64 base64 = new Base64(true);

    /**
     * generation of a new URL-safe UUID
     */
    @Override
    public String generateUrlSafeUUID() {

        final UUID uuid = UUID.randomUUID();
        final byte[] uuidArray = toByteArray(uuid);
        final byte[] encodedArray = base64.encode(uuidArray); // UUID becomes URL safe

        String returnValue = new String(encodedArray, Charset.forName("UTF-8"));
        if (returnValue.endsWith("\r\n")) {
            returnValue = returnValue.substring(0, returnValue.length() - 2);
        }

        return returnValue;
    }

    private static byte[] toByteArray(final UUID uuid) {

        final byte[] byteArray = new byte[(Long.SIZE / Byte.SIZE) * 2];
        final ByteBuffer buffer = ByteBuffer.wrap(byteArray);

        final LongBuffer longBuffer = buffer.asLongBuffer();
        longBuffer.put(new long[]{uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()});

        return byteArray;
    }
}
