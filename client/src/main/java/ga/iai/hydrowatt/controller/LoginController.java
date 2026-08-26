package ga.iai.hydrowatt.controller;

import com.fasterxml.jackson.databind.JsonNode;
import ga.iai.hydrowatt.service.ApiClient;
import ga.iai.hydrowatt.service.ApiException;
import ga.iai.hydrowatt.service.Session;
import ga.iai.hydrowatt.util.SceneManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Veuillez renseigner l'identifiant et le mot de passe.");
            return;
        }
        setLoading(true);

        Task<JsonNode> task = new Task<>() {
            @Override
            protected JsonNode call() throws Exception {
                return ApiClient.login(username, password);
            }
        };
        task.setOnSucceeded(e -> {
            setLoading(false);
            JsonNode result = task.getValue();
            if (result.get("requires_2fa").asBoolean()) {
                SceneManager.show2FA(result.get("pre_auth_token").asText(), username);
            } else {
                Session.set(result.get("access").asText(), result.get("refresh").asText(), username);
                fetchAdminStatusThenShowDashboard();
            }
        });
        task.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = task.getException();
            if (ex instanceof ApiException apiEx) {
                showError(apiEx.getMessage());
            } else {
                showError("Impossible de contacter le serveur HydroWatt. Vérifiez votre connexion.");
            }
        });
        new Thread(task, "login-task").start();
    }

    private void fetchAdminStatusThenShowDashboard() {
        Task<JsonNode> meTask = new Task<>() {
            @Override
            protected JsonNode call() throws Exception {
                return ApiClient.get("/auth/me/");
            }
        };
        meTask.setOnSucceeded(e -> {
            Session.setAdmin(meTask.getValue().path("is_staff").asBoolean(false));
            SceneManager.showDashboard();
        });
        meTask.setOnFailed(e -> SceneManager.showDashboard());
        new Thread(meTask, "fetch-me-task").start();
    }

    @FXML
    private void handleForgotPassword() {
        SceneManager.showForgotPassword();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void setLoading(boolean loading) {
        loginButton.setDisable(loading);
        loginButton.setText(loading ? "Connexion..." : "Se connecter");
    }
}
