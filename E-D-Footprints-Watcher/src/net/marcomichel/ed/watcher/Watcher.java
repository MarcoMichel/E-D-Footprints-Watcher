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
    private static final String DIRECTORY_TO_WATCH 	= "directory";
    private static final String SERVER_URL			= "url";
    private static final String CMDR_NAME			="cmdr";    
    private static final String CMDR_ID 			="id";    
    private static final String CMDR_MAIL 			="mail";    

    // Config
    private Properties config;
    private String configFile;
    // Parser mit dem das Log-File geparst wird 
	private IParser parser = new GameLogFileParser(this);

	/**
	 * Konstruktor, bekommt den Pfad auf das Config-File übergeben.
	 * 
	 * @param propertyFile Pfad auf das Config-File
	 * @throws IOException wenn das Config-File nicht eingelesen werden konnte
	 */
	public Watcher(String propertyFile) throws IOException {
    	log.info("Reading Property-File " + propertyFile);
    	configFile = propertyFile;
    	Reader reader = new FileReader(propertyFile);
    	config = new Properties();
    	config.load(reader);
	}

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
	 * Sendet ein Event an den E:D-Footprints Server.
	 * 
	 * @param json JSON Objekt mit dem Event, das gesendet werden soll
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
		
		boolean success = false;
		int retry = 0;
		while (!success && retry<3) {
			try {
				sendPost(obj, config.getProperty(SERVER_URL) + "/publish");
				success = true;
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
				}
			}			
		}
	}

	@SuppressWarnings("unchecked")
	private void startRegistration() {
		log.warning("Commander not registert. Starting registration....");
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
		} catch (IOException e) {
			log.log(Level.SEVERE, "Could not send registration request to server.", e);
			return;
		}
		
		try {
			storeConfig();
		} catch (IOException e) {
			log.log(Level.SEVERE, "Could not store config with id.", e);
		}
	}
	
	private void storeConfig() throws IOException {
		FileWriter writer = new FileWriter(configFile);
		config.store(writer, "Config of Watcher.");			
	}
	
	private void checkServerStatus() throws IOException {
		int status = sendGet(config.getProperty(SERVER_URL) + "/ping");
		if (status != 200) {
			log.log(Level.SEVERE, "Server is not ready. Status Code " + status);
			throw new IOException("Server not ready. Status code " + status);
		}
	}
	
	/**
	 * Startet die Überwachung 
	 * @throws IOException 
	 */
	public void watch() throws IOException {
		if (config.getProperty(CMDR_ID, null) == null) {
			startRegistration();
		} else {
			checkServerStatus();
			watchDirectoryPath(config.getProperty(DIRECTORY_TO_WATCH));
		}
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.err.println("Config-File must be provided as first parameter");
		} else {
			try {
				LogManager.getLogManager().readConfiguration(new FileInputStream("watcher-logging.properties"));
			} catch (IOException exception) {
				log.log(Level.SEVERE, "Error in loading logging configuration", exception);
			}

			String file = args[0];
			Watcher watcher = new Watcher(file);
			watcher.watch();
		}
	}

}
