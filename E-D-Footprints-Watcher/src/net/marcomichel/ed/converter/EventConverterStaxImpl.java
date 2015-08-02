package net.marcomichel.ed.converter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import net.marcomichel.ed.parser.CommandersLogParser;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class EventConverterStaxImpl extends CommandersLogParser {

	private static final Logger log = Logger.getLogger(EventConverterStaxImpl.class.getName());

	private HashSet<String> systems = new HashSet<String>();
	private JSONArray systemList = new JSONArray();
	private HashSet<Edge> edges = new HashSet<Edge>();
	private JSONArray edgeList = new JSONArray();
	private String quelle = null, ziel = null;

	@SuppressWarnings("unchecked")
	@Override
	protected void onJumpedToEvent(String system, String eventid) {

		if (!systems.contains(system)) {
			systems.add(system);
			JSONObject obj = new JSONObject();
			obj.put("id", system);
			obj.put("label", system);
			obj.put("discovered", "ZERO SENSE");
			systemList.add(obj);
		}

		quelle = ziel;
		ziel = system;

		Edge edge = new Edge(quelle, ziel);
		if (!edges.contains(edge)) {
			edges.add(edge);
			// Rückweg auch aufnehmen, dass diese Verbindung nur einmal aufgenommen wird
			edges.add(new Edge(ziel, quelle));
			JSONObject obj = new JSONObject();
			obj.put("from", quelle);
			obj.put("to", ziel);
			obj.put("style", "line");
			edgeList.add(obj);
		}
	}

	private void writeJsonObject(JSONObject json) throws Exception {
		String jsonAsString = json.toJSONString();
		Files.write(Paths.get("data.json"), jsonAsString.getBytes());
	}

	@SuppressWarnings("unchecked")
	public void convert() {
		log.info("Start parsing...");
		long ts = System.currentTimeMillis();
		try {
			parseLogFile("log1.xml");
			JSONObject data = new JSONObject();
			data.put("name", "ZERO SENSE");
			data.put("email", "marco-michel@gmx.de");
			data.put("id", "e7884875-e908-32c6-ba43-cb0a10b5eb20");
			data.put("teamid", "bb17fe71-5cef-4681-a7c8-c8d006e7b940");
			JSONObject footprint = new JSONObject();
			footprint.put("nodes", systemList);
			footprint.put("edges", edgeList);
			data.put("footprint", footprint);
			writeJsonObject(data);
		} catch (Exception e) {
			log.severe(e.toString());
		}
		long duration = System.currentTimeMillis() - ts;
		log.info("End nach " + duration + " ms");

	}

	public static void main(String[] args) {
		try {
			LogManager.getLogManager().readConfiguration(new FileInputStream("config/watcher-logging.properties"));
		} catch (IOException exception) {
			log.log(Level.SEVERE, "Error in loading logging configuration", exception);
		}
		EventConverterStaxImpl ec = new EventConverterStaxImpl();
		ec.convert();
	}

}
