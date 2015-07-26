package net.marcomichel.ed.watcher.fx.view;

import java.io.File;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.marcomichel.ed.watcher.fx.model.Settings;

public class SettingsEditDialogController {

	@FXML
    private TextField cmdrNameField;
    @FXML
    private TextField emailNameField;
    @FXML
    private TextField directoryToWatchField;
    @FXML
    private TextField gameConfigField;

    private Stage dialogStage;
    private Settings settings;
    private boolean okClicked = false;

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {
    }

    /**
     * Sets the stage of this dialog.
     *
     * @param dialogStage
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Sets the person to be edited in the dialog.
     *
     * @param person
     */
    public void setSettings(Settings settings) {
        this.settings = settings;

        cmdrNameField.setText(settings.getCmdr());
        emailNameField.setText(settings.getEMail());
        directoryToWatchField.setText(settings.getDirectoryToWatch());
        gameConfigField.setText(settings.getGameConfigFile());
    }

    /**
     * Returns true if the user clicked OK, false otherwise.
     *
     * @return
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    @FXML
    private void handleDirectoryChooser() {
    	final DirectoryChooser chooser = new DirectoryChooser();
    	chooser.setTitle("Select Directory to watch");
    	File defaultDirectory = new File(directoryToWatchField.getText());
    	if (defaultDirectory.exists()) {
        	chooser.setInitialDirectory(defaultDirectory);
    	}
    	File selectedDirectory = chooser.showDialog(dialogStage);
    	if (selectedDirectory != null) {
    		directoryToWatchField.setText(selectedDirectory.getAbsolutePath().replace("\\", "/"));
    	}
    }

    @FXML
    private void handleGameConfigChooser() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select game config file");
        final String dir = gameConfigField.getText().substring(0, gameConfigField.getText().lastIndexOf("/"));
    	File defaultDirectory = new File(dir);
    	if (defaultDirectory.exists()) {
    		fileChooser.setInitialDirectory(defaultDirectory);
    	}
    	File file = fileChooser.showOpenDialog(dialogStage);
        if (file != null) {
            gameConfigField.setText(file.getAbsolutePath().replace("\\", "/"));
        }
    }

    /**
     * Called when the user clicks ok.
     */
    @FXML
    private void handleOk() {
        if (isInputValid()) {
        	settings.setCmdr(cmdrNameField.getText());
        	settings.setEMail(emailNameField.getText());
        	settings.setDirectoryToWatch(directoryToWatchField.getText());
        	settings.setGameConfigFile(gameConfigField.getText());

            okClicked = true;
            dialogStage.close();
        }
    }

    /**
     * Called when the user clicks cancel.
     */
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    /**
     * Validates the user input in the text fields.
     *
     * @return true if the input is valid
     */
    private boolean isInputValid() {
        String errorMessage = "";

        if (cmdrNameField.getText() == null || cmdrNameField.getText().length() == 0) {
            errorMessage += "You must set a Commander name!\n";
        }
        if (emailNameField.getText() == null || emailNameField.getText().length() == 0) {
            errorMessage += "No valid e-mail-adress!\n";
        }

        if (directoryToWatchField.getText() == null || directoryToWatchField.getText().length() == 0) {
            errorMessage += "You must set a directory to watch!\n";
        } else {
        	File directory = new File(directoryToWatchField.getText());
        	if (!directory.exists()) {
        		errorMessage += "Directory to watch does not exists!\n";
        	}
        }

        if (gameConfigField.getText() == null || gameConfigField.getText().length() == 0) {
            errorMessage += "You must set the game config file!\n";
        } else {
        	File file = new File(gameConfigField.getText());
        	if (!file.exists()) {
        		errorMessage += "Game config file does not exists!\n";
        	}
        }

        if (errorMessage.length() == 0) {
            return true;
        } else {
            // Show the error message.
            Alert alert = new Alert(AlertType.ERROR);
            alert.initOwner(dialogStage);
            alert.setTitle("Invalid Fields");
            alert.setHeaderText("Please correct invalid fields");
            alert.setContentText(errorMessage);

            alert.showAndWait();

            return false;
        }
    }
}
