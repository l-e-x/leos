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
 * Exception to be thrown when a {@link Metadata} object cannot be registered in the database
 * (e.g. due to incomplete data)
 */
public class CannotCreateMetadataException extends Exception {

    private static final long serialVersionUID = -7921142223308636417L;

    public CannotCreateMetadataException(final Throwable exc) {
        super("The metadata cannot be registered in the database", exc);
    }

    public CannotCreateMetadataException(final String msg) {
        super(msg);
    }
}
