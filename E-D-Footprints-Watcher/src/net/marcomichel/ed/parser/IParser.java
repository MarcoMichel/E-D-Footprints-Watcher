package net.marcomichel.ed.parser;

/**
 * Interface für den Parser, der Files parst und erkennt, ob in ein anderes System gesprungen wurde
 *
 * @author Marco Michel
 *
 */
public interface IParser {

	/**
	 * Parst ein übergebenes File.
	 *
	 * @param file File das geparst werden soll
	 */
	public void parseFile(String file);

	/**
	 * @return das aktuelle System
	 */
	public String getCurrentSystem();
}
