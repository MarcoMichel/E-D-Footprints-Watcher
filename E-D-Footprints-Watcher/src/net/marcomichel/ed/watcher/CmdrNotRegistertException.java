package net.marcomichel.ed.watcher;

public class CmdrNotRegistertException extends Exception {

	private static final long serialVersionUID = 1L;

	public CmdrNotRegistertException() {
	}

	public CmdrNotRegistertException(String message) {
		super(message);
	}

	public CmdrNotRegistertException(Throwable cause) {
		super(cause);
	}

	public CmdrNotRegistertException(String message, Throwable cause) {
		super(message, cause);
	}

	public CmdrNotRegistertException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
