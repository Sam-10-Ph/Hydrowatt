package ga.iai.hydrowatt.controller;

import ga.iai.hydrowatt.service.ApiClient;
import ga.iai.hydrowatt.service.ApiException;
import ga.iai.hydrowatt.util.SceneManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Label infoLabel;
    @FXML private Button sendButton;

    @FXML
    private void handleSend() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showInfo("Veuillez saisir votre email.");
            return;
        }
        sendButton.setDisable(true);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ApiClient.forgotPassword(email);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            sendButton.setDisable(false);
            showInfo("Si ce compte existe, un email avec un lien de réinitialisation (valide 30 min) vient d'être envoyé.");
        });
        task.setOnFailed(e -> {
            sendButton.setDisable(false);
            Throwable ex = task.getException();
            showInfo(ex instanceof ApiException apiEx ? apiEx.getMessage() : "Erreur réseau. Réessayez.");
        });
        new Thread(task, "forgot-password-task").start();
    }

    @FXML
    private void handleGoToReset() {
        SceneManager.showResetPassword();
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
