package com.chroniccare;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // ── Simuler un utilisateur connecté pour test ──
        com.chroniccare.models.User testUser = new com.chroniccare.models.User();
        testUser.setId(1);
        testUser.setNom("Admin");
        testUser.setPrenom("Super");
        testUser.setRoles("ROLE_ADMIN");
        com.chroniccare.utils.SessionManager.getInstance().setCurrentUser(testUser);
        // ───────────────────────────────────────────────
        Parent root = FXMLLoader.load(
                getClass().getResource("/com/chroniccare/login.fxml")
                //getClass().getResource("/com/chroniccare/forum.fxml")

        );
        primaryStage.setTitle("ChronicCare");
        primaryStage.setScene(new Scene(root, 900, 650));
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}