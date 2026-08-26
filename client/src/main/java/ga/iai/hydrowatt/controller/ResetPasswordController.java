package ga.iai.hydrowatt.controller;

import ga.iai.hydrowatt.service.ApiClient;
import ga.iai.hydrowatt.service.ApiException;
import ga.iai.hydrowatt.util.SceneManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ResetPasswordController {

    @FXML private TextField tokenField;
    @FXML private PasswordField newPasswordField;
    @FXML private Label infoLabel;
    @FXML private Button resetButton;

    @FXML
    private void handleReset() {
        String token = tokenField.getText().trim();
        String newPassword = newPasswordField.getText();
        if (token.isEmpty() || newPassword.isEmpty()) {
            showInfo("Veuillez renseigner le token et le nouveau mot de passe.");
            return;
        }
        resetButton.setDisable(true);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ApiClient.resetPassword(token, newPassword);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            resetButton.setDisable(false);
            showInfo("Mot de passe réinitialisé. Vous pouvez vous connecter.");
        });
        task.setOnFailed(e -> {
            resetButton.setDisable(false);
            Throwable ex = task.getException();
            showInfo(ex instanceof ApiException apiEx ? apiEx.getMessage() : "Erreur réseau. Réessayez.");
        });
        new Thread(task, "reset-password-task").start();
    }

    @FXML
    private void handleBack() {
        SceneManager.showLogin();
    }

    private void showInfo(String message) {
        infoLabel.setText(message);
        infoLabel.setVisible(true);
        infoLabel.setManaged(true);
    }
}
