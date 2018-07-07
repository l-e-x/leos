/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.support;

public class ByteArrayBuilder {

    private byte[] content;

    public ByteArrayBuilder(byte... content) {
        this.content = content;
    }

    public void append(byte... otherContent) {
        if (otherContent.length > 0) {
            byte[] joinedArray = new byte[content.length + otherContent.length];
            System.arraycopy(content, 0, joinedArray, 0, content.length);
            System.arraycopy(otherContent, 0, joinedArray, content.length, otherContent.length);

            content = joinedArray;
        }
    }

    public byte[] getContent() {
        return content;
    }
}
