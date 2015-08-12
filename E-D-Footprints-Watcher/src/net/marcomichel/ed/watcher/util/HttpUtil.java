package net.marcomichel.ed.watcher.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import net.marcomichel.ed.watcher.WatcherConfig;

public abstract class HttpUtil {

	private static final Logger log = Logger.getLogger(HttpUtil.class.getName());

	/**
	 * Sendet ein HTTP-GET an den E:D-Footprints Server
	 *
	 * @param url URL die aufgerufen werden soll
	 * @return HTTP Status-Code der Response
	 * @throws IOException
	 */
	public static int sendGet(String url) throws IOException {
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
    public static final String sendPost(JSONObject json, String url) throws IOException {
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
     * Publiziert ein Event an den Server.
     *
     * @param event the event to be published
     * @throws IOException if event could not be published
     */
	public static void publishEvent(JSONObject event) throws IOException {
		boolean success = false;
		int retry = 0;
		while (!success && retry<3) {
			try {
				HttpUtil.sendPost(event, WatcherConfig.getInstance().getProperty(WatcherConfig.SERVER_URL) + "/publish");
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
					throw new IOException("Server problems. Could not send event.");
				}
			}
		}
	}

}
