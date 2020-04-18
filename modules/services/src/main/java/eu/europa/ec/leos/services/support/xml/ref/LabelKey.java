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
package eu.europa.ec.leos.services.support.xml.ref;

public class LabelKey {

    private String labelName; // name to be shown
    private String labelNumber; // number can be letters or numbers
    private boolean unNumbered; // in case of unnumbered, the number has to be expressed in letters rather than in number.
    private String documentRef;

    public LabelKey(String labelName, String labelNumber, boolean unNumbered, String documentRef) {
        this.labelName = labelName;
        this.labelNumber = labelNumber;
        this.unNumbered = unNumbered;
        this.documentRef = documentRef;
    }

    public String getLabelName() {
        return labelName;
    }

    public String getLabelNumber() {
        return labelNumber;
    }

    public String getDocumentRef() {
        return documentRef;
    }

    public boolean isUnNumbered() {
        return unNumbered;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + labelName.hashCode() + labelNumber.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        LabelKey other = (LabelKey) obj;
        if (other.labelName.equals(labelName)
                && other.labelNumber.equals(labelNumber)) {
            return true;
        }
        return true;
    }

    @Override
    public String toString() {
        return "LabelKey [labelName=" + labelName + ", labelNumber=" + labelNumber + ", unNumbered=" + unNumbered + "]";
    }

}
