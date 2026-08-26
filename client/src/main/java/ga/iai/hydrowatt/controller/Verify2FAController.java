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
import javafx.scene.control.TextField;

import java.util.Map;

public class Verify2FAController implements SceneManager.ScreenParams {

    @FXML private TextField codeField;
    @FXML private Label errorLabel;
    @FXML private Button verifyButton;

    private String preAuthToken;
    private String username;

    @Override
    public void setParams(Map<String, Object> params) {
        this.preAuthToken = (String) params.get("preAuthToken");
        this.username = (String) params.get("username");
    }

    @FXML
    private void handleVerify() {
        String code = codeField.getText().trim();
        if (code.length() != 6) {
            showError("Le code doit contenir 6 chiffres.");
            return;
        }
        setLoading(true);

        Task<JsonNode> task = new Task<>() {
            @Override
            protected JsonNode call() throws Exception {
                return ApiClient.verify2fa(preAuthToken, code);
            }
        };
        task.setOnSucceeded(e -> {
            setLoading(false);
            JsonNode result = task.getValue();
            Session.set(result.get("access").asText(), result.get("refresh").asText(), username);
            fetchAdminStatusThenShowDashboard();
        });
        task.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = task.getException();
            showError(ex instanceof ApiException apiEx ? apiEx.getMessage() : "Erreur réseau. Réessayez.");
        });
        new Thread(task, "verify-2fa-task").start();
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
    private void handleCancel() {
        SceneManager.showLogin();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void setLoading(boolean loading) {
        verifyButton.setDisable(loading);
        verifyButton.setText(loading ? "Vérification..." : "Valider");
    }
}
