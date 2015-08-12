package net.marcomichel.ed.watcher;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Config of E:D-Footprints-Watcher
 * @author Marco Michel
 *
 */
public class WatcherConfig {

	private static final Logger log = Logger.getLogger(WatcherConfig.class.getName());

	// Keys für das Property File mit der Config
	public static final String GAME_CONFIG         	= "gameconfig";
	public static final String DIRECTORY_TO_WATCH	= "directory";
	public static final String FILE_SCAN_INTERVAL   = "scan-interval";
	public static final String SERVER_URL			= "server-url";
	public static final String CMDR_NAME			= "cmdr";
	public static final String CMDR_ID 				= "id";
	public static final String CMDR_MAIL 			= "mail";

	private static final WatcherConfig instance = new WatcherConfig();

	// Config
    private Properties config = null;
    private String userConfigFile;

    /**
     * @return Instance of WatcherConfig
     */
	public static WatcherConfig getInstance() {
		return instance;
	}

	/**
	 * Init of WatcherConfig. Must be called on Startup
	 * @param propertyFile name of the Property-File with User specific config
	 * @throws IOException if config cannot be loaded
	 */
	public void initConfig(String propertyFile) throws IOException {
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
	 * Returns a property of the config
	 * @param key of the property
	 * @return value of the property
	 */
	public String getProperty(String key) {
		if (config == null) {
			throw new IllegalStateException("Config not initialized.");
		}

		return config.getProperty(key);
	}

	/**
	 * Returns a property of the config.
	 * @param key of the property
	 * @param defaultValue if no value is in config
	 * @return value of the property or defaultValue
	 */
	public String getProperty(String key, String defaultValue) {
		if (config == null) {
			throw new IllegalStateException("Config not initialized.");
		}

		return config.getProperty(key, defaultValue);
	}

	/**
	 * Sets a property in the config
	 * @param key of the property
	 * @param value of the property
	 */
	public void setProperty(String key, String value) {
		if (config == null) {
			throw new IllegalStateException("Config not initialized.");
		}

		config.put(key, value);
	}

	/**
	 * Stores the config
	 * @throws IOException if config could not be stored
	 */
	public void storeConfig() throws IOException {
		if (config == null) {
			throw new IllegalStateException("Config not initialized.");
		}

		FileWriter writer = new FileWriter(userConfigFile);
		Properties userConfig = new Properties();
		userConfig.putAll(config);
		userConfig.remove(SERVER_URL);
		userConfig.remove(FILE_SCAN_INTERVAL);
		userConfig.store(writer, "User-Config of Watcher.");
	}
}
