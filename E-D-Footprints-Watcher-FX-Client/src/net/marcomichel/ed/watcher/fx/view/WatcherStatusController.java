package net.marcomichel.ed.watcher.fx.view;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import net.marcomichel.ed.watcher.fx.WatcherMainApp;
import net.marcomichel.ed.watcher.fx.model.Settings;

public class WatcherStatusController {

	   @FXML
	    private ListView<String> msgList;
	    @FXML
	    private Label cmdrLabel;
	    @FXML
	    private Label currentSystemLabel;

	    private WatcherMainApp mainApp;

	    /**
	     * Initializes the controller class. This method is automatically called
	     * after the fxml file has been loaded.
	     */
	    @FXML
	    private void initialize() {
	    }

	    /**
	     * Is called by the main application to give a reference back to itself.
	     *
	     * @param mainApp
	     */
	    public void setMainApp(WatcherMainApp mainApp) {
	        this.mainApp = mainApp;

	        // Add observable list data to the table
	        msgList.setItems(mainApp.getMessageData());
	        Settings settings = mainApp.getSettings();
	        cmdrLabel.textProperty().bind(settings.cmdrProperty());
	        currentSystemLabel.textProperty().bind(settings.currentSystemProperty());
	    }

}
