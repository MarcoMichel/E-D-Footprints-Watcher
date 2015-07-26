package net.marcomichel.ed.watcher.fx.view;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
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
        }

        if (gameConfigField.getText() == null || gameConfigField.getText().length() == 0) {
            errorMessage += "You must set the game config file!\n";
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
