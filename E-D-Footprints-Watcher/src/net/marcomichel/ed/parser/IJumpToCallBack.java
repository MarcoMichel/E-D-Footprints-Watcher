package net.marcomichel.ed.parser;

/**
 * Interface für den Callback, der aufgeufen werden soll, wenn ein Systemsprung erkannt wurde.
 * 
 * @author Marco Michel
 */
public interface IJumpToCallBack {

	/**
	 * Wird aufgefufen, wenn in ein anderes System gesprungen wurde.
	 * 
	 * @param from Das System aus dem gesprungen wurde
	 * @param to System in das gesprungen wurde
	 */
	public void jumpedTo(String from, String to);
}
