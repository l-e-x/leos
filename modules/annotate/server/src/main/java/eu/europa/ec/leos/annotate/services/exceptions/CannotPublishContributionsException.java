package eu.europa.ec.leos.annotate.services.exceptions;

/**
 * Exception that is thrown when it's not possible to publish the annotations of a contributor (ISC)
 * The message gives more details about the reason
 */
public class CannotPublishContributionsException extends Exception {

    private static final long serialVersionUID = -4121224399081412546L;

    public CannotPublishContributionsException(final String msg) {
        super(msg);
    }
}
