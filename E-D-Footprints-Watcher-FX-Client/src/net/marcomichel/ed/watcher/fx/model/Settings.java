package net.marcomichel.ed.watcher.fx.model;

import java.util.Properties;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import net.marcomichel.ed.watcher.Watcher;

public class Settings {

	final static private String USER_HOME 	= System.getProperty("user.home").replace("\\", "/");
	final static private String GAME_FOLDER = "/AppData/Local/Frontier_Developments/Products/FORC-FDEV-D-1010/";
	final static private String LOG_FOLDER 	= USER_HOME + GAME_FOLDER + "/Logs";
	final static private String GAME_CONFIG = USER_HOME + GAME_FOLDER + "/AppConfig.xml";

	private final StringProperty cmdr;
	private final StringProperty eMail;
	private final StringProperty directoryToWatch;
	private final StringProperty gameConfigFile;
	private final StringProperty currentSystem;

	public Settings() {
		this(new Properties());
	}

	public Settings(Properties config) {
		this.cmdr = new SimpleStringProperty(config.getProperty(Watcher.CMDR_NAME, ""));
		this.eMail = new SimpleStringProperty(config.getProperty(Watcher.CMDR_MAIL, ""));
		this.directoryToWatch = new SimpleStringProperty(config.getProperty(Watcher.DIRECTORY_TO_WATCH, LOG_FOLDER));
		this.gameConfigFile = new SimpleStringProperty(config.getProperty(Watcher.GAME_CONFIG, GAME_CONFIG));
		this.currentSystem = new SimpleStringProperty("unknown");
	}

	public void adoptSettings(Properties config) {
		config.put(Watcher.CMDR_NAME, getCmdr());
		config.put(Watcher.CMDR_MAIL, getEMail());
		config.put(Watcher.DIRECTORY_TO_WATCH, getDirectoryToWatch());
		config.put(Watcher.GAME_CONFIG, getGameConfigFile());
	}

	public String getGameConfigFile() {
		return gameConfigFile.get();
	}

	public void setGameConfigFile(String gameConfigFile) {
	    this.gameConfigFile.set(gameConfigFile);
	}

    public StringProperty gameConfigFileProperty() {
        return gameConfigFile;
    }

	public String getDirectoryToWatch() {
		return directoryToWatch.get();
	}

	public void setDirectoryToWatch(String directoryToWatch) {
	    this.directoryToWatch.set(directoryToWatch);
	}

    public StringProperty directoryToWatchProperty() {
        return directoryToWatch;
    }

	public String getEMail() {
		return eMail.get();
	}

	public void setEMail(String eMail) {
	    this.eMail.set(eMail);
	}

    public StringProperty eMailProperty() {
        return eMail;
    }

	public String getCmdr() {
		return cmdr.get();
	}

	public void setCmdr(String cmdr) {
	    this.cmdr.set(cmdr);
	}

    public StringProperty cmdrProperty() {
        return cmdr;
    }

	public String getCurrentSystem() {
		return currentSystem.get();
	}

	public void setCurrentSystem(String currentSystem) {
	    this.currentSystem.set(currentSystem);
	}

    public StringProperty currentSystemProperty() {
        return currentSystem;
    }

}
