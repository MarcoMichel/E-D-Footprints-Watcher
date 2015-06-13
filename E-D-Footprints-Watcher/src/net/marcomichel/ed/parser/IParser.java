package net.marcomichel.ed.parser;

/**
 * Interface f�r den Parser, der Files parst und erkennt, ob in ein anderes System gesprungen wurde
 * 
 * @author Marco Michel
 *
 */
public interface IParser {

	/**
	 * Parst ein �bergebenes File.
	 * 
	 * @param file File das geparst werden soll
	 */
	public void parseFile(String file);
}
