package it.giuval.cloud.telemetry_services.exceptions;

public class CommandPublishException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CommandPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}