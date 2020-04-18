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
package eu.europa.ec.leos.annotate.services.exceptions;

/**
 * Exception that is thrown when it's not possible to update an annotation since its response status is SENT
 * The message gives more details about the reason
 */
public class CannotUpdateSentAnnotationException extends Exception {

    private static final long serialVersionUID = 2535014877111379018L;

    public CannotUpdateSentAnnotationException(final String msg) {
        super(msg);
    }
}
