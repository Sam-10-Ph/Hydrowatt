package ga.iai.hydrowatt;

import ga.iai.hydrowatt.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class HydroWattApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("HydroWatt — Énergie hydroélectrique gabonaise");
        SceneManager.init(primaryStage);
        SceneManager.showLogin();
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
