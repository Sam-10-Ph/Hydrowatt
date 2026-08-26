package ga.iai.hydrowatt.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;

/** Centralise le changement d'écran (login, 2FA, dashboard...) dans la fenêtre principale. */
public class SceneManager {
    private static Stage stage;

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void showLogin() {
        show("/ga/iai/hydrowatt/fxml/login.fxml", "HydroWatt — Connexion", null);
    }

    public static void show2FA(String preAuthToken, String username) {
        show("/ga/iai/hydrowatt/fxml/verify2fa.fxml", "HydroWatt — Double authentification",
                Map.of("preAuthToken", preAuthToken, "username", username));
    }

    public static void showForgotPassword() {
        show("/ga/iai/hydrowatt/fxml/forgot_password.fxml", "HydroWatt — Mot de passe oublié", null);
    }

    public static void showResetPassword() {
        show("/ga/iai/hydrowatt/fxml/reset_password.fxml", "HydroWatt — Réinitialisation", null);
    }

    public static void showDashboard() {
        show("/ga/iai/hydrowatt/fxml/dashboard.fxml", "HydroWatt — Tableau de bord", null);
    }

    private static void show(String fxml, String title, Map<String, Object> params) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxml));
            Parent root = loader.load();
            if (params != null) {
                Object controller = loader.getController();
                if (controller instanceof ScreenParams sp) {
                    sp.setParams(params);
                }
            }
            Scene scene = new Scene(root, 1000, 650);
            scene.getStylesheets().add(SceneManager.class.getResource("/ga/iai/hydrowatt/css/app.css").toExternalForm());
            stage.setTitle(title);
            stage.setScene(scene);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger l'écran " + fxml, e);
        }
    }

    /** Interface implémentée par les contrôleurs qui reçoivent des paramètres de navigation. */
    public interface ScreenParams {
        void setParams(Map<String, Object> params);
    }
}
