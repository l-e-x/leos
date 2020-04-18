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
package eu.europa.ec.leos.ui.wizard;

public enum ErrorResolver {
    EXCEPTION("wizard.document.upload.error.unknown"),
    DOCUMENT_NOT_FOUND("wizard.document.upload.error.document"),
    DOCUMENT_SOURCE_NOT_FOUND ("wizard.document.upload.error.document.source"),
    DOCUMENT_XML_SYNTAX_NOT_VALID ("wizard.document.upload.error.document.source.xml.syntax"),
    DOCUMENT_XSD_VALIDATION_FAILED("wizard.document.upload.error.document.invalid.source.xml"),
    DOCUMENT_CATEGORY_NOT_FOUND ("wizard.document.upload.error.document.category"),
    DOCUMENT_PURPOSE_NOT_FOUND ("wizard.document.upload.error.document.purpose"),
    DOCUMENT_TEMPLATE_NOT_FOUND ("wizard.document.upload.error.document.template"),
    DOCUMENT_PROPOSAL_TEMPLATE_NOT_FOUND ("wizard.document.upload.error.document.proposal.template"),
    DOCUMENT_ANNEX_INDEX_NOT_FOUND ("wizard.document.upload.error.document.annex.index"),
    DOCUMENT_ANNEX_TITLE_NOT_FOUND ("wizard.document.upload.error.document.annex.title"),
    DOCUMENT_ANNEX_NUMBER_NOT_FOUND ("wizard.document.upload.error.document.annex.number");
    
    private String errorMessageKey;

    ErrorResolver(String errorMessageKey)
    {
        this.errorMessageKey = errorMessageKey;
    }

    public String getErrorMessageKey()
    {
        return errorMessageKey;
    }
}
    


