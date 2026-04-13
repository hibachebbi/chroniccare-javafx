package com.chroniccare.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Utilitaire simple pour naviguer entre les écrans (FXML) de l'application.
 *
 * Règle: tous les chemins FXML doivent être absolus et commencer par "/com/chroniccare/...".
 */
public final class FxNavigator {

    private FxNavigator() {
    }

    public static void go(Node anyNodeInScene, String fxmlPath) {
        Objects.requireNonNull(anyNodeInScene, "anyNodeInScene is null");
        Objects.requireNonNull(fxmlPath, "fxmlPath is null");

        if (anyNodeInScene.getScene() == null) {
            throw new IllegalStateException("Impossible de naviguer: le Node n'est pas attaché à une Scene");
        }

        URL url = FxNavigator.class.getResource(fxmlPath);
        if (url == null) {
            throw new IllegalStateException("FXML introuvable ou invalide: " + fxmlPath);
        }

        Parent root;
        try {
            root = FXMLLoader.load(url);
        } catch (IOException e) {
            throw new IllegalStateException("FXML introuvable ou invalide: " + fxmlPath, e);
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

