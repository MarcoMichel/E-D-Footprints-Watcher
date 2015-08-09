package net.marcomichel.ed.watcher;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import net.marcomichel.ed.parser.GameConfigParser;
import net.marcomichel.ed.parser.GameLogFileParser;
import net.marcomichel.ed.parser.IJumpToCallBack;
import net.marcomichel.ed.parser.IParser;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONObject;

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

	// Keys für das Property File mit der Config
	public static final String GAME_CONFIG         	= "gameconfig";
	public static final String DIRECTORY_TO_WATCH	= "directory";
	public static final String SERVER_URL			= "server-url";
	public static final String CMDR_NAME			= "cmdr";
	public static final String CMDR_ID 				= "id";
	public static final String CMDR_MAIL 			= "mail";

    // Config
    private Properties config;
    private String userConfigFile;
    // Parser mit dem das Log-File geparst wird
	private IParser parser = new GameLogFileParser(this);
	// Observer über den die GUI informiert wird
	private IModelObserver modelObserver;

	/**
	 * Konstruktor, bekommt den Pfad auf das Config-File des Users und eine Observer-Instanz übergeben.
	 *
	 * @param propertyFile Pfad auf das Config-File des Users
	 * @param observer Observer über den die GUI informiert wird
	 * @throws IOException wenn das Config-File nicht eingelesen werden konnte
	 */
	public Watcher(String propertyFile, IModelObserver observer) throws IOException {
		this.modelObserver = observer;
    	log.info("Reading Property-File " + propertyFile);
    	userConfigFile = propertyFile;
    	// Config der Application einlesen
    	InputStream is = new FileInputStream("config/watcher-application.properties");
    	config = new Properties();
    	config.load(is);
    	// Config des Users einlesen
    	try {
        	Reader reader = new FileReader(propertyFile);
        	config.load(reader);
		} catch (IOException e) {
			log.warning("No user configuration. File not found: " + propertyFile);
		}
	}

	/**
	 * Sendet ein HTTP-GET an den E:D-Footprints Server
	 *
	 * @param url URL die aufgerufen werden soll
	 * @return HTTP Status-Code der Response
	 * @throws IOException
	 */
	private int sendGet(String url) throws IOException {
		log.fine("Sending Query to " + url);
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url);
		CloseableHttpResponse response = httpClient.execute(httpGet);
		int statusCode = -1;
		try {
	        statusCode = response.getStatusLine().getStatusCode();
	        log.finer("Response is " + statusCode);
		} finally {
			response.close();
		}
		return statusCode;
	}

	/**
	 * Sendet ein HTTP-POST an den E:D-Footprints Server.
	 *
	 * @param json JSON Objekt mit dem Event, das gesendet werden soll
	 * @param url URL die aufgerufen werden soll
	 * @throws IOException
	 */
    private String sendPost(JSONObject json, String url) throws IOException {
    	final String body = json.toJSONString();
    	log.fine("Sending " + body);
    	HttpClient httpClient = new DefaultHttpClient();
    	HttpPost httpPost = new HttpPost(url);

		StringEntity stringEntity = new StringEntity(body);
        httpPost.setEntity(stringEntity);
        httpPost.setHeader("Content-type", "application/json");
        HttpResponse response = httpClient.execute(httpPost);
        int statusCode = response.getStatusLine().getStatusCode();
        log.finer("Response is " + statusCode);
        InputStream is = response.getEntity().getContent();
        int ch;
        StringBuilder sb = new StringBuilder();
        while ((ch = is.read()) != -1)
        	sb.append((char)ch);
        return sb.toString();
    }

	/**
	 * @see net.marcomichel.ed.watcher.DirectoryWatcher#onModFile(java.lang.String)
	 */
	@Override
	protected void onModFile(String file) {
		// Das geänderte File parsen und prüfen, ob ein Systemsprung durchgeführt wurde
		parser.parseFile(config.getProperty(DIRECTORY_TO_WATCH) + "/" + file);
	}

	/**
	 * @see net.marcomichel.ed.parser.IJumpToCallBack#jumpedTo(java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void jumpedTo(String from, String to) {
		// JSON_Objekt mit dem Event erzeugen und verschicken
		JSONObject obj = new JSONObject();
		obj.put("cmdr", config.getProperty(CMDR_NAME));
		obj.put("event", "Jumped To");
		obj.put("to", to);
		obj.put("from", from);
		obj.put("id", config.getProperty(CMDR_ID));
		log.info(obj.toJSONString());
		modelObserver.onSystemChange(to);
		modelObserver.addMessage("Send jump event from " + from + " to " + to);
		publishEvent(obj);
	}

	private void publishEvent(JSONObject event) {
		boolean success = false;
		int retry = 0;
		while (!success && retry<3) {
			try {
				sendPost(event, config.getProperty(SERVER_URL) + "/publish");
				success = true;
				log.info("sending event");
			} catch (IOException e) {
				log.log(Level.SEVERE, "Could not send event to server. " + e.toString(), e);
				retry++;
				if (retry<3) {
					log.info("Retrying after 2 seconds....");
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
						log.warning(e1.toString());
					}
				} else {
					log.info("Could not send event. No more retrys for this event.");
					modelObserver.addMessage("Server problems. Could not send event.");
				}
			}
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
		obj.put("cmdr", config.getProperty(CMDR_NAME));
		obj.put("mail", config.getProperty(CMDR_MAIL));
		log.info(obj.toJSONString());
		String id = null;

		try {
			id = sendPost(obj, config.getProperty(SERVER_URL) + "/register");
			log.info("Got new ID: " + id);
			config.put(CMDR_ID, id);
			log.info("You will receive an e-mail.");
			log.info("Please restart Watcher after using activation link in e-mail.");
			modelObserver.addMessage("Registration request successful. You will receive an e-mail. Please use activation link in e-mail to complete registration.");
		} catch (IOException e) {
			log.log(Level.SEVERE, "Could not send registration request to server.", e);
			modelObserver.addMessage("Could not send registration request to server. Please restart Watcher and try again.");
			return;
		}

		try {
			storeConfig();
		} catch (IOException e) {
			log.log(Level.SEVERE, "Could not store config with id.", e);
			modelObserver.addMessage("Could not store config. Registration not complete. Please restart Watcher.");
		}
	}

	/**
	 * Liefert die Konfiguration
	 */
	public Properties getConfig() {
		return config;
	}

	/**
	 * Speichert die Konfiguration
	 * @throws IOException wenn die Config nicht gespeichert werden konnte
	 */
	public void storeConfig() throws IOException {
		FileWriter writer = new FileWriter(userConfigFile);
		Properties userConfig = new Properties();
		userConfig.putAll(config);
		userConfig.remove(SERVER_URL);
		userConfig.store(writer, "User-Config of Watcher.");
	}

	/**
	 * Setzt das Verbose Logging in Elite Dangerous
	 * @throws IOException wenn die Konfiguration vom Spiel nicht geändert werden kann
	 */
	public void setVerboseLogging() throws IOException {
		log.fine("Setting verbose logging.");
		GameConfigParser parser = new GameConfigParser(config.getProperty(GAME_CONFIG));
		String bak = parser.setVerboseLogging();
		log.info("Verbose Logging set. Made backup of original GameConfigFile: " + bak);
		modelObserver.addMessage("Verbose Logging set. Made backup of original GameConfigFile: " + bak);
	}

	@SuppressWarnings("unchecked")
	private void publishStatusChange(String status) {
		// JSON_Objekt mit dem Event erzeugen und verschicken
		JSONObject obj = new JSONObject();
		obj.put("cmdr", config.getProperty(CMDR_NAME));
		obj.put("event", "status.change");
		obj.put("status", status);
		obj.put("id", config.getProperty(CMDR_ID));
		log.info(obj.toJSONString());
		modelObserver.addMessage("Sending Status " + status);
		publishEvent(obj);
	}

	public void publishOnline() {
		publishStatusChange("online");
	}

	public void publishOffline() {
		publishStatusChange("offline");
	}

	/**
	 * Prüft, ob de E:D-Footprints Server erreichbar ist.
	 * @throws ServerNotOnlineException wenn der Server nicht erreichbar ist
	 */
	private void checkServerStatus() throws ServerNotOnlineException {
		log.fine("Checking server status.");
		try {
			int status = sendGet(config.getProperty(SERVER_URL) + "/ping");
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
		GameConfigParser parser = new GameConfigParser(config.getProperty(GAME_CONFIG));
		boolean verbose = parser.isVerboseLogging();
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
		if (config.getProperty(CMDR_ID, null) == null) {
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
	public void watch() throws ServerNotOnlineException, IOException, NoVerboseLoggingException, CmdrNotRegistertException  {
		log.info("Checking Prerequisites...");
		modelObserver.addMessage("Checking Prerequisites...");
		checkServerStatus();
		checkRegistration();
		checkVerboseLogging();
		log.info("Starting collecting footprints....");
		modelObserver.addMessage("Starting collecting footprints....");
		watchDirectoryPath(config.getProperty(DIRECTORY_TO_WATCH));
	}

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
					watcher.watch();
				} catch (CmdrNotRegistertException e) {
					log.info("Commander is not registert.");
					watcher.startRegistration();
				}
			}
		}
	}

}
