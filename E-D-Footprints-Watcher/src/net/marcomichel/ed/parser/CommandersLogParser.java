package net.marcomichel.ed.parser;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public abstract class CommandersLogParser {
	private static final Logger log = Logger.getLogger(CommandersLogParser.class.getName());

	private static final String COMMANDERS_LOG_EVENT_TAG	= "CommandersLogEvent";
	private static final String EVENT_TYPE_TAG 				= "EventType";
	private static final String JUMPED_TO_EVENT 			= "Jumped To";
	private static final String SYSTEM_TAG 					= "System";
	private static final String EVENT_ID_TAG 				= "EventID";
	
	protected void parseLogFile(String file) throws Exception {
		log.info("Parsing " + file);
		// First, create a new XMLInputFactory
	    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
	    // Setup a new eventReader
	    InputStream in = new FileInputStream(file);
	    XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
		String system = null, eventid = null, eventtype = null;
		
	    while (eventReader.hasNext()) {
	    	XMLEvent event = eventReader.nextEvent();

	        if (event.isStartElement()) {
	        	StartElement startElement = event.asStartElement();
	        	
	            if (startElement.getName().getLocalPart() == (COMMANDERS_LOG_EVENT_TAG)) {
		        	  system    = null;
		        	  eventid   = null;
		        	  eventtype = null;
	            }
	                
            	if (startElement.getName().getLocalPart() == (EVENT_TYPE_TAG)) {
            		event = eventReader.nextEvent();
            		if (event.getEventType() == XMLEvent.CHARACTERS && event.asCharacters().getData().equalsIgnoreCase(JUMPED_TO_EVENT)) {
	                    eventtype = JUMPED_TO_EVENT;
	                    continue;
            		}
                }
            	
            	if (startElement.getName().getLocalPart() == (SYSTEM_TAG)) {
            		event = eventReader.nextEvent();
            		if (event.getEventType() == XMLEvent.CHARACTERS) {	            			
	                    system = event.asCharacters().getData();
	                    continue;
            		}
            	}

            	if (startElement.getName().getLocalPart() == (EVENT_ID_TAG)) {
            		event = eventReader.nextEvent();
            		if (event.getEventType() == XMLEvent.CHARACTERS) {	            			
	                    eventid = event.asCharacters().getData();
	                    continue;
            		}
            	}
	        }
	        
	        // If we reach the end of an item element, we call the onEvent-Methode
	        if (event.isEndElement()) {
	          EndElement endElement = event.asEndElement();
	          if (endElement.getName().getLocalPart() == (COMMANDERS_LOG_EVENT_TAG) && eventtype == JUMPED_TO_EVENT) {
	        	  onJumpedToEvent(system, eventid);
	          }
	        }

	    }	            	
	}

	protected abstract void onJumpedToEvent(String system, String eventid);

}
