package net.marcomichel.ed.watcher.fx;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.marcomichel.ed.watcher.CmdrNotRegistertException;
import net.marcomichel.ed.watcher.NoVerboseLoggingException;
import net.marcomichel.ed.watcher.fx.model.Settings;
import net.marcomichel.ed.watcher.fx.view.SettingsEditDialogController;
import net.marcomichel.ed.watcher.fx.view.WatcherStatusController;

public class WatcherMainApp extends Application {

	private static final Logger log = Logger.getLogger(WatcherMainApp.class.getName());

    private Stage primaryStage;
    private BorderPane rootLayout;

    private ObservableList<String> messageData = FXCollections.observableArrayList();
    private Settings settings = null;
    private WatcherService watcherService = null;

    public WatcherMainApp() {
		try {
			LogManager.getLogManager().readConfiguration(new FileInputStream("config/watcher-logging.properties"));
		} catch (IOException exception) {
			log.log(Level.SEVERE, "Error in loading logging configuration", exception);
		}
    }

    /**
     * Returns the messages as an observable list of Messages.
     * @return
     */
    public ObservableList<String> getMessageData() {
        return messageData;
    }

    public Settings getSettings() {
    	return settings;
    }

    public String getUserConfigFile() {
    	return "config/watcher-user.properties";
    }

    /**
     * Returns the main stage.
     * @return
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

	@Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("E:D-Footprints Watcher");
        this.primaryStage.getIcons().add(new Image("file:resources/deathstar.png"));

    	try {
			watcherService = new WatcherService(this);
	    	watcherService.setOnFailed((event) -> selectWatcherFailedStrategy());
			settings = new Settings(watcherService.getWatcherConfig());
		} catch (IOException e) {
			onGeneralError(e);
		}

        initRootLayout();
        showWatcherStatusScene();
    	watcherService.start();
    }

	private void selectWatcherFailedStrategy() {
		Throwable t = watcherService.getException();
		if (t instanceof CmdrNotRegistertException) {
			onCmdNotRegistert();
//		} else if (t instanceof NoVerboseLoggingException) {
//			onNoVerboseLogging();
		} else {
			onGeneralError(t);
		}
	}

	private void onCmdNotRegistert() {
		log.info("Commander not registert.");
		getMessageData().add("Commander not registert.");
		boolean ok = showSettingsEditDialog();
		if (ok) {
			log.info("Starting registration");
			settings.adoptSettings(watcherService.getWatcherConfig());
			watcherService.startRegistration();
			getMessageData().add("Step 1 of registration complete. Restart watcher.");
		} else {
			log.info("Registration canceled.");
			getMessageData().add("Registration canceled.");
		}
	}

	private void onNoVerboseLogging() {
		log.info("Verbose Logging not active.");
		getMessageData().add("Verbose Logging not active.");
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Verbose Logging");
		alert.setHeaderText("Verbose logging not set.");
		alert.setContentText("Verbose logging of Elite Dangerous is not set. I can't detect any jumps.\nShould I set it for you?");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK){
		    log.info("Setting verbose Logging.");
			getMessageData().add("Setting verbose Logging.");
		    watcherService.reset();
		    watcherService.start();
		} else {
			log.info("Verbose Logging still inactive.");
		    watcherService.reset();
		}
	}

	private void onGeneralError(Throwable t) {
		log.log(Level.SEVERE, t.toString(), t);
		getMessageData().add(t.getMessage());
		// Show the error message.
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("General error");
		alert.setHeaderText(t.getMessage());
		alert.setContentText("Watcher has a general error. Must terminate application. Please restart application.\n"
				+ t.toString());
		alert.showAndWait();
		Platform.exit();
	}

    @Override
	public void stop() throws Exception {
    	log.info("Stopping Application.");
    	watcherService.cancel();
		super.stop();
	}

	/**
     * Initializes the root layout.
     */
    public void initRootLayout() {
        try {
            // Load root layout from fxml file.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(WatcherMainApp.class.getResource("view/RootLayout.fxml"));
            rootLayout = (BorderPane) loader.load();

            // Show the scene containing the root layout.
            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
        	onGeneralError(e);
        }
    }

    /**
     * Shows the person overview inside the root layout.
     */
    public void showWatcherStatusScene() {
        try {
            // Load person overview.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(WatcherMainApp.class.getResource("view/WatcherStatus.fxml"));
            AnchorPane watcherStatus = (AnchorPane) loader.load();

            // Set person overview into the center of root layout.
            rootLayout.setCenter(watcherStatus);

            // Give the controller access to the main app.
            WatcherStatusController controller = loader.getController();
            controller.setMainApp(this);
        } catch (IOException e) {
        	onGeneralError(e);
        }
    }

    public boolean showSettingsEditDialog() {
	    try {
	        // Load the fxml file and create a new stage for the popup dialog.
	        FXMLLoader loader = new FXMLLoader();
	        loader.setLocation(WatcherMainApp.class.getResource("view/SettingsEditDialog.fxml"));
	        AnchorPane page = (AnchorPane) loader.load();

	        // Create the dialog Stage.
	        Stage dialogStage = new Stage();
	        dialogStage.setTitle("Edit Settings");
	        dialogStage.initModality(Modality.WINDOW_MODAL);
	        dialogStage.initOwner(primaryStage);
	        Scene scene = new Scene(page);
	        dialogStage.setScene(scene);

	        // Set the person into the controller.
	        SettingsEditDialogController controller = loader.getController();
	        controller.setDialogStage(dialogStage);
	        controller.setSettings(settings);

	        // Show the dialog and wait until the user closes it
	        dialogStage.showAndWait();

	        return controller.isOkClicked();
	    } catch (IOException e) {
	    	onGeneralError(e);
	    	return false;
	    }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
