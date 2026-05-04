package com.chroniccare.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public final class FxNavigator {

    private FxNavigator() {
    }

    public static void go(Node anyNodeInScene, String fxmlPath) {
        Objects.requireNonNull(anyNodeInScene, "anyNodeInScene is null");
        Objects.requireNonNull(fxmlPath, "fxmlPath is null");

        if (anyNodeInScene.getScene() == null) {
            throw new IllegalStateException("Impossible de naviguer: le Node n'est pas attaché à une Scene");
        }

        // Essayer plusieurs approches pour charger le FXML
        URL url = null;

        // Approche 1: Utiliser ClassLoader standard
        String cleanPath = fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath;
        url = FxNavigator.class.getClassLoader().getResource(cleanPath);

        // Approche 2: Si pas trouvé, essayer avec
        // Thread.currentThread().getContextClassLoader()
        if (url == null) {
            url = Thread.currentThread().getContextClassLoader().getResource(cleanPath);
        }

        // Approche 3: Si pas trouvé, essayer directement via getClass().getResource()
        // en utilisant le chemin orignal avec /
        if (url == null && fxmlPath.startsWith("/")) {
            url = FxNavigator.class.getResource(fxmlPath);
        }

        // Approche 4: Si toujours pas trouvé, essayer sans le /
        if (url == null && !fxmlPath.startsWith("/")) {
            url = FxNavigator.class.getResource("/" + fxmlPath);
        }

        if (url == null) {
            System.err.println("[ERROR] FXML NOT FOUND - Loading attempts failed:");
            System.err.println("   1. ClassLoader.getResource(\"" + cleanPath + "\")");
            System.err.println("   2. ContextClassLoader.getResource(\"" + cleanPath + "\")");
            System.err.println("   3. getClass().getResource(\"" + fxmlPath + "\")");
            if (!fxmlPath.startsWith("/")) {
                System.err.println("   4. getClass().getResource(\"/" + fxmlPath + "\")");
            }
            throw new IllegalStateException("FXML introuvable ou invalide: " + fxmlPath);
        }

        System.out.println("[INFO] FXML found: " + url);

        Parent root;
        try {
            root = FXMLLoader.load(url);
        } catch (IOException e) {
            System.err.println("\n[ERROR] Failed to load FXML:");
            System.err.println("   URL: " + url);
            System.err.println("   Message: " + e.getMessage());
            System.err.println("\nFull stacktrace:");
            e.printStackTrace();
            throw new IllegalStateException("Erreur chargement FXML: " + fxmlPath, e);
        }

        Scene scene = anyNodeInScene.getScene();
        Stage stage = (Stage) scene.getWindow();
        if (stage != null) {
            stage.setScene(new Scene(root, scene.getWidth(), scene.getHeight()));
        } else {
            // fallback
            scene.setRoot(root);
        }
    }
}
