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
package eu.europa.ec.leos.annotate.services.exceptions;

/**
 * Exception that is thrown when it's not possible to reject a suggestion
 * The message gives more details about the reason
 */
public class CannotRejectSuggestionException extends Exception {

    private static final long serialVersionUID = 6491126400384810982L;

    public CannotRejectSuggestionException(String msg) {
        super(msg);
    }
    
    public CannotRejectSuggestionException(Throwable e) {
        super(e);
    }
}
