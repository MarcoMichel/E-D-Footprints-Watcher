package net.marcomichel.ed.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parst das Log-File von Elite Dangerous und prüft dabei, ob in ein anderes System gesprungen wurde.<p>
 *
 * Dem Konstruktor muss eine Klasse vom Typ @see net.marcomichel.ed.parser.IJumpToCallBack
 * mitgegeben werden. Diese Klasse wird aufgerufen, wenn ein Sprung in ein anderes System erkannt wurde.<p>
 *
 * Zum parsen des Log-Files muss die Methode parseFile(String) aufgerufen werden und der Name, incl. Pfad,
 * des Files als Parameter übergeben werden.<p>
 *
 * Achtung: Die KLasse ist stateful. Die Instanz muss also weiter verwendet werden und darf nicht
 * bei jedem Aufruf der Methode neu instanziiert werden.<p>
 *
 * Zum parsen wird das übergebene File zeilenweise eingelesen. Jede Zeile wird überprüft, ob dort ein
 * bestimmter Eintrag steht, der angibt, in welchem System der Spieler sich befindet.<br>
 * Das ermittelte System wird mit dem im State gespeicherten System verglichen. Dies ist das System, in dem
 * der Spieler bisher war. Steht im Log-File ein anderes System, wurde ein Sprung durchgeführt.
 * Das neue System wird im State gespeichert und der Sprung über den Calback gemeldet.<br>
 * Im State wird zudem gespeichert, in welcher Zeile der Treffer war. Wird das File erneut eingelesen,
 * wird erst ab dieser Zeile auf den Systemeintrag geprüft. Damit wird verhindert, dass alte Einträge erneut
 * als Sprung interpretiert und gemeldet werden.<br>
 * Das zuletzt eingelesen File wird ebenfalls im State gespeichert. Damit wird erkannt, ob ein bereits
 * geparstes File erneut eingelesen wird, oder ob ein neues File geparst wird und damit wieder ab der
 * ersten Zeile geparst werden muss.
 *
 * @author Marco Michel
 */
public class GameLogFileParser implements IParser {

	private static final Logger log = Logger.getLogger(GameLogFileParser.class.getName());

	// Das Regex-Pattern des Log-Eintrages, in dem das System steht, in dem der Spieler sich befindet
	// Beispiel: {18:51:11} System:20(Eravate) Body:34 Pos:(-912.113,387.702,-133.182)
	private static final String PATTERN_EXPRESSION   = "\\{\\d+:\\d+:\\d+\\} System:\\d+\\(([^)]+)\\).*";
	private static final Pattern PATTERN             = Pattern.compile(PATTERN_EXPRESSION);

	// File und Keys des State
	private static final String STATE_FILE           = "config/gamelogfileparser-state.properties";
	private static final String STATE_LAST_FILE      = "lastFile";
	private static final String STATE_CURRENT_SYSTEM = "currentSystem";
	private static final String STATE_LAST_MATCH     = "lastMatch";

	// Instanz der Klasse mit dem Callback
	private IJumpToCallBack callback;

	// Zeile in der der letzte Treffer mit einem Systemname war
	private int lastMatch = 0;
	// System in dem der Spieler sich aktuell befindet
	private String currentSystem = "";
	// System in dem der Spieler sich vor dem letzten Sprung befunden hatte
	private String previousSystem = "";
	// Name des Files, das als letztes geparst wurde
	private String lastFile = null;

	// der gespeicherte State
	private Properties state = new Properties();

	/**
	 * Konstruktor.<p>
	 *
	 * Bekommt die Instanz einer KLasse übergeben, die aufgerufen wird, sobald ein Systemsprung erkannt wurde
	 *
	 * @param callback Instanz einer Klasse vom Typ IJumpToCallBack
	 */
	public GameLogFileParser(IJumpToCallBack callback) {
		this.callback = callback;

		// den gespeicherten State einlesen
		try {
			FileReader reader = new FileReader(STATE_FILE);
			state.load(reader);
		} catch (IOException e) {
			log.warning("State file not found, using defaults.");
		}

		currentSystem = state.getProperty(STATE_CURRENT_SYSTEM, "");
		lastFile      = state.getProperty(STATE_LAST_FILE, "");
		lastMatch     = Integer.parseInt(state.getProperty(STATE_LAST_MATCH, "0"));

		if (log.isLoggable(Level.INFO)) {
			log.info("Starting with following state:");
			log.info("Last Match: " + lastMatch);
			log.info("Last file: " + lastFile);
			log.info("Current system: " + currentSystem);
		}
	}

	/**
	 * Parst einen String und prüft, ob in ein anderes System gesprungen wurde.
	 *
	 * @param str Der zu parsende String
	 * @return true, wenn in ein anderes System gesprungen wurde, false wenn nicht
	 */
	private boolean findSystemChange(String str) {
		log.finest(str);
		boolean match = false;
		// den übergebenen String gegen unsere Regex checken
		Matcher m = PATTERN.matcher(str);

		 if (m.matches()) {
			 log.fine("Matches found");

			 // Wenn es einen Treffer gibt und wir ein anderes System haben als bisher
			 // das vorherige System setzen, das aktuelle aus dem String übernehmen
			 // und Treffer zurück melden
			 if (!currentSystem.equals(m.group(1))) {
				 previousSystem = currentSystem;
				 currentSystem = m.group(1);
				 if (previousSystem.isEmpty()) {
					 previousSystem = currentSystem;
				 }
				 match = true;
			 }
		 }

		 return match;
	}

	/**
	 * Speichert den aktuellen State
	 */
	private void storeCurrentState() {
		try {
			FileWriter writer = new FileWriter(STATE_FILE);
			state.put(STATE_LAST_MATCH, String.valueOf(lastMatch));
			state.put(STATE_CURRENT_SYSTEM, currentSystem);
			state.put(STATE_LAST_FILE, lastFile);
			state.store(writer, "State of GameLogFileParser");
		} catch (IOException e) {
			log.log(Level.SEVERE, "Cannot store state.", e);
		}
	}

	/**
	 * @return das aktuelle System
	 */
	public String getCurrentSystem() {
		return currentSystem;
	}

	/**
	 * @see net.marcomichel.ed.parser.IParser
	 */
	public void parseFile(String file) {

		// Wenn ein anderes File kommt als bisher, die zuletzt gelesene Zeile auf 0,
		// um am Anfang des Files zu lesen
		// Wir gehen davon aus, dass ein neues Log-File geschrieben wird.
		// Es könnte aber auch irgendein anderes File sein, das mit dem Logging
		// das wir brauchen, nichts zu tun hat. Für den Fall merken wir uns die
		// alte Zeile und stellen sie wieder her, falls es in dem File keine Treffer gibt
		log.finer("Parsing from line " + lastMatch);
		int oldMatch  = lastMatch;
		boolean fileMatch = false;

		if (!lastFile.equals(file)) {
			log.finer("got a different file than the last one. Setting lastMatch = 0");
			lastMatch = 0;
		}

		// Das File Zeilenweise durchlesen
		BufferedReader in = null;
		int row = 0;

		try {
			in = new BufferedReader(new FileReader(file));
			String zeile = null;

			while ((zeile = in.readLine()) != null) {
				log.finest("row readed: " + zeile);
				row++;
				// Wenn die aktuell gelesene Zeile größer ist, als die letzte, bei der es einen Treffer gab,
				// suchen wir, nach Treffern für Sprung in anderes System in der Zeile
				if (row > lastMatch) {
					boolean match = findSystemChange(zeile);
					// Wenn es einen Treffer gab, Callback aufrufen,
					// diesen Filenamen und die aktuelle Zeile merken und State speichern
					if (match) {
						fileMatch = true;
						callback.jumpedTo(previousSystem, currentSystem);
						lastFile = file;
						lastMatch = row;
						storeCurrentState();
					}
				}
			}

		} catch (IOException e) {
			log.warning(e.toString() + " during read of file " + file);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.warning("Cannot close FileInputStream of file " + file);
				}
			}
		}

		// Im ganzen File gab es keinen Treffer.
		// War also etwas uninteressantes. Auf Zeile des letzten Treffers zurücksetzen
		// um beim alten Stand aufzusetzen, wenn wieder das korrekte File kommt
		if (!fileMatch) {
			lastMatch = oldMatch;
			log.finer("no match in file, restoring old lastMatch");
		}
		log.finer("lastMatch after parsing is " + lastMatch);
	}

}
