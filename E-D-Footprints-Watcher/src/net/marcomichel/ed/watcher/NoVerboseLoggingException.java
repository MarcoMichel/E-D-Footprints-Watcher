package net.marcomichel.ed.watcher;

public class NoVerboseLoggingException extends Exception {

	private static final long serialVersionUID = 1L;

	public NoVerboseLoggingException() {
	}

	public NoVerboseLoggingException(String message) {
		super(message);
	}

	public NoVerboseLoggingException(Throwable cause) {
		super(cause);
	}

	public NoVerboseLoggingException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoVerboseLoggingException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
