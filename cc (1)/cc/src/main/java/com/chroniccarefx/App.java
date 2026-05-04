package com.chroniccarefx;

import com.chroniccarefx.controller.ActiviteController;
import com.chroniccarefx.controller.EtatController;
import com.chroniccarefx.repository.ActiviteRepository;
import com.chroniccarefx.repository.EtatRepository;
import com.chroniccarefx.repository.JdbcActiviteRepository;
import com.chroniccarefx.repository.JdbcEtatRepository;
import com.chroniccarefx.service.ActiviteService;
import com.chroniccarefx.service.EtatService;
import com.chroniccarefx.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        ActiviteRepository activiteRepository = new JdbcActiviteRepository();
        EtatRepository etatRepository = new JdbcEtatRepository();

        EtatService etatService = new EtatService(etatRepository, activiteRepository);
        ActiviteService activiteService = new ActiviteService(activiteRepository, etatRepository);

        EtatController etatController = new EtatController(etatService);
        ActiviteController activiteController = new ActiviteController(activiteService, etatService);

        try {
            MainView view = new MainView(etatController, activiteController);
            Scene scene = new Scene(view.build(), 980, 620);
            stage.setTitle("CHRONNICCARE - Gestion Suivi");
            stage.setScene(scene);
            stage.show();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Connexion DB impossible");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
            throw ex;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
