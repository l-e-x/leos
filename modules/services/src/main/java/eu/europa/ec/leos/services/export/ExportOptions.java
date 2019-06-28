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

public enum ExportOptions {
    TO_PDF_LW("PDF_", "", true, "", ComparisonType.NONE),
    TO_WORD_LW("LW_", "", true, "", ComparisonType.NONE),

    TO_PDF_DW("PDF_", "", true, "", ComparisonType.NONE),
    TO_WORD_DW("DW_", "", true, "", ComparisonType.NONE),

    TO_PDF_DW_LT("PDF_", "LEGALTEXT", false, "", ComparisonType.DOUBLE),
    TO_WORD_DW_LT("DW_", "LEGALTEXT", false, "", ComparisonType.DOUBLE),

    TO_WORD_MILESTONE_LW("LW_", "", false, "", ComparisonType.NONE),
    TO_WORD_MILESTONE_DW("DW_", "", false, "", ComparisonType.NONE);

    private String filePrefix;
    private String fileType;
    private boolean convertAnnotations;
    private String technicalKey;
    private ComparisonType comparisonType;
    
    public enum ComparisonType {
        NONE("none"),
        SIMPLE("simple"),
        DOUBLE("double");
        
        private String type;
        
        ComparisonType(String value) {
            this.type = value;
        }

        public String getType() {
            return type;
        }
    }
    
    ExportOptions(String value, String fileType, boolean convertAnnotations, String technicalKey, ComparisonType comparisonType) {
        this.filePrefix=value;
        this.fileType = fileType;
        this.convertAnnotations = convertAnnotations;
        this.technicalKey = technicalKey;
        this.comparisonType = comparisonType;
    }
    
    public String getFilePrefix(){
        return filePrefix;
    }
    
    public String getFileType() {
        return fileType;
    }

    public boolean isConvertAnnotations() {
        return convertAnnotations;
    }

    public String getTechnicalKey() {
        return technicalKey;
    }
    
    public ComparisonType getComparisonType() {
        return this.comparisonType;
    }
}
