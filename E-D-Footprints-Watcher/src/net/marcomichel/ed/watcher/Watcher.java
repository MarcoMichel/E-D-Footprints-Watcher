package net.marcomichel.ed.watcher;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.json.simple.JSONObject;

import net.marcomichel.ed.parser.GameConfigParser;
import net.marcomichel.ed.parser.GameLogFileParser;
import net.marcomichel.ed.parser.IJumpToCallBack;
import net.marcomichel.ed.parser.IParser;
import net.marcomichel.ed.watcher.util.HttpUtil;

/**
 * Überwachnungsprogramm für Elite Dangerous Footprints.<p>
 *
 * Das Programm muss auf dem Rechner laufen, auf dem Eite Dangerous ausgeführt wird.<br>
 * Es überwacht ob ein Sprung in ein anderes System durchgeführt wurde und sendet ein
 * entsprechendes Event an den E:D-Footprints Server.
 *
 * @author Marco Michel
 */
public class Watcher extends DirectoryWatcher implements IJumpToCallBack {

	private static final Logger log = Logger.getLogger(Watcher.class.getName());

    // Parser mit dem das Log-File geparst wird
	private IParser parser = new GameLogFileParser(this);
	// Parser mit dem das game config file geparst wird
	private GameConfigParser gameConfig;
	// Observer über den die GUI informiert wird
	private IModelObserver modelObserver;
	// Executor running the directory watching
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	// Flag, ob beim stop ein offline event gesendet werden soll
	private boolean sendOfflineEvent = false;

	/**
	 * Konstruktor, bekommt den Pfad auf das Config-File des Users und eine Observer-Instanz übergeben.
	 *
	 * @param propertyFile Pfad auf das Config-File des Users
	 * @param observer Observer über den die GUI informiert wird
	 * @throws IOException wenn das Config-File nicht eingelesen werden konnte
	 */
	public Watcher(String propertyFile, IModelObserver observer) throws IOException {
		this.modelObserver = observer;
		WatcherConfig.getInstance().initConfig(propertyFile);
		gameConfig = new GameConfigParser(WatcherConfig.getInstance().getProperty(WatcherConfig.GAME_CONFIG));
		modelObserver.onSystemChange(parser.getCurrentSystem());
	}

	/**
	 * @see net.marcomichel.ed.watcher.DirectoryWatcher#onModFile(java.lang.String)
	 */
	@Override
	protected void onModFile(String file) {
		// Das geänderte File parsen und prüfen, ob ein Systemsprung durchgeführt wurde
		parser.parseFile(WatcherConfig.getInstance().getProperty(WatcherConfig.DIRECTORY_TO_WATCH) + "/" + file);
	}

	/**
	 * @see net.marcomichel.ed.parser.IJumpToCallBack#jumpedTo(java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void jumpedTo(String from, String to) {
		// JSON_Objekt mit dem Event erzeugen und verschicken
		JSONObject obj = new JSONObject();
		obj.put("cmdr", WatcherConfig.getInstance().getProperty(WatcherConfig.CMDR_NAME));
		obj.put("event", "Jumped To");
		obj.put("to", to);
		obj.put("from", from);
		obj.put("id", WatcherConfig.getInstance().getProperty(WatcherConfig.CMDR_ID));
		log.info(obj.toJSONString());
		modelObserver.onSystemChange(to);
		modelObserver.addMessage("Send jump event from " + from + " to " + to);
		try {
			HttpUtil.publishEvent(obj);
		} catch (IOException e) {
			modelObserver.addMessage("Server problems. Could not send event.");
		}
	}

	/**
	 * Startet den Registrierungs-Prozess.
	 */
	@SuppressWarnings("unchecked")
	public void startRegistration() {
		log.warning("Commander not registert. Starting registration....");
		modelObserver.addMessage("Sending registration request....");
		JSONObject obj = new JSONObject();
		obj.put("cmdr", WatcherConfig.getInstance().getProperty(WatcherConfig.CMDR_NAME));
		obj.put("mail", WatcherConfig.getInstance().getProperty(WatcherConfig.CMDR_MAIL));
		log.info(obj.toJSONString());
		String id = null;

		try {
			id = HttpUtil.sendPost(obj, WatcherConfig.getInstance().getProperty(WatcherConfig.SERVER_URL) + "/register");
			log.info("Got new ID: " + id);
			WatcherConfig.getInstance().setProperty(WatcherConfig.CMDR_ID, id);
			log.info("You will receive an e-mail.");
			log.info("Please restart Watcher after using activation link in e-mail.");
			modelObserver.addMessage("Registration request successful. You will receive an e-mail. Please use activation link in e-mail to complete registration.");
		} catch (IOException e) {
			log.log(Level.SEVERE, "Could not send registration request to server.", e);
			modelObserver.addMessage("Could not send registration request to server. Please restart Watcher and try again.");
			return;
		}

		try {
			WatcherConfig.getInstance().storeConfig();
		} catch (IOException e) {
			log.log(Level.SEVERE, "Could not store config with id.", e);
			modelObserver.addMessage("Could not store config. Registration not complete. Please restart Watcher.");
		}
	}

	/**
	 * Setzt das Verbose Logging in Elite Dangerous
	 * @throws IOException wenn die Konfiguration vom Spiel nicht geändert werden kann
	 */
	public void setVerboseLogging() throws IOException {
		log.fine("Setting verbose logging.");
		String bak = gameConfig.setVerboseLogging();
		log.info("Verbose Logging set. Made backup of original GameConfigFile: " + bak);
		modelObserver.addMessage("Verbose Logging set. Made backup of original GameConfigFile: " + bak);
	}

	@SuppressWarnings("unchecked")
	private void publishStatusChange(String status) {
		// JSON_Objekt mit dem Event erzeugen und verschicken
		JSONObject obj = new JSONObject();
		obj.put("cmdr", WatcherConfig.getInstance().getProperty(WatcherConfig.CMDR_NAME));
		obj.put("event", "status.change");
		obj.put("status", status);
		obj.put("id", WatcherConfig.getInstance().getProperty(WatcherConfig.CMDR_ID));
		log.info(obj.toJSONString());
		modelObserver.addMessage("Sending Status " + status);
		try {
			HttpUtil.publishEvent(obj);
		} catch (IOException e) {
			modelObserver.addMessage("Server problems. Could not send event.");
		}
	}

	/**
	 * Prüft, ob de E:D-Footprints Server erreichbar ist.
	 * @throws ServerNotOnlineException wenn der Server nicht erreichbar ist
	 */
	private void checkServerStatus() throws ServerNotOnlineException {
		log.fine("Checking server status.");
		try {
			int status = HttpUtil.sendGet(WatcherConfig.getInstance().getProperty(WatcherConfig.SERVER_URL) + "/ping");
			if (status != 200) {
				throw new ServerNotOnlineException("Server not ready. Status code " + status);
			}
		} catch (IOException e) {
			throw new ServerNotOnlineException("Cannot connect to server.", e);
		}
		log.info("Server online.");
		modelObserver.addMessage("Server online.");
	}

	/**
	 * Prüft, ob das Verbose Logging in Elite Dangerous aktiv ist
	 * @throws IOException wenn auf die Config von Elite Dangerous nicht zugegriffen werden kann
	 * @throws NoVerboseLoggingException wenn das Verbose Logging nicht aktiv ist
	 */
	private void checkVerboseLogging() throws IOException, NoVerboseLoggingException {
		log.fine("Checking verbose logging.");
		boolean verbose = gameConfig.isVerboseLogging();
		if (!verbose) {
			throw new NoVerboseLoggingException("Logging of Elite Dangerous is not verbose. Cannot detect any jumps. Please set logging to verbose.");
		}
		log.info("Verbose Logging active.");
		modelObserver.addMessage("Verbose Logging active.");
	}

	/**
	 * Prüft, ob der User registriert ist.
	 * @throws CmdrNotRegistertException wenn der User nicht registriert ist
	 */
	private void checkRegistration() throws CmdrNotRegistertException {
		if (WatcherConfig.getInstance().getProperty(WatcherConfig.CMDR_ID, null) == null) {
			throw new CmdrNotRegistertException("Commander is not registert.");
		}
		log.info("Commander is registert.");
		modelObserver.addMessage("Commander is registert.");
	}

	/**
	 * Startet die Überwachung
	 * @throws ServerNotOnlineException
	 * @throws IOException
	 * @throws NoVerboseLoggingException
	 * @throws CmdrNotRegistertException
	 */
	public void startWatching() throws ServerNotOnlineException, IOException, NoVerboseLoggingException, CmdrNotRegistertException  {
		log.info("Checking Prerequisites...");
		modelObserver.addMessage("Checking Prerequisites...");
		checkServerStatus();
		checkRegistration();
		checkVerboseLogging();
		publishStatusChange("online");
		sendOfflineEvent = true;
		log.info("Starting collecting footprints....");
		modelObserver.addMessage("Starting collecting footprints....");
		final String baseName = gameConfig.getLogFileBaseName();
		executor.submit(() -> {
			startWatchDirectory(WatcherConfig.getInstance().getProperty(WatcherConfig.DIRECTORY_TO_WATCH),
					baseName,
					Integer.parseInt(WatcherConfig.getInstance().getProperty(WatcherConfig.FILE_SCAN_INTERVAL)));
		});
	}

	/**
	 * Stoppt die Überwachung
	 */
	public void stopWatching() {
		log.info("Stopping watch executor");
		if (sendOfflineEvent) {
			publishStatusChange("offline");
		}
		super.stopWatchDirectory();
		executor.shutdownNow();
	}

	/**
	 * main programm if you want to run without gui
	 */
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.err.println("Config-File must be provided as first parameter");
		} else {
			try {
				LogManager.getLogManager().readConfiguration(new FileInputStream("config/watcher-logging.properties"));
			} catch (IOException exception) {
				log.log(Level.SEVERE, "Error in loading logging configuration", exception);
			}

			String file = args[0];
			Watcher watcher = new Watcher(file, new DummyObserver());
			boolean retry = true;
			while (retry) {
				try {
					watcher.startWatching();
				} catch (CmdrNotRegistertException e) {
					log.info("Commander is not registert.");
					watcher.startRegistration();
				}
			}
		}
	}

}
