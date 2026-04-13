package com.chroniccare.controllers;

import com.chroniccare.models.Publication;
import com.chroniccare.services.PublicationService;
import com.chroniccare.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class AjouterPublicationController implements Initializable {

    @FXML private Label            labelTitre;
    @FXML private TextField        fieldTitre;
    @FXML private ComboBox<String> fieldCategorie;
    @FXML private TextArea         fieldContenu;
    @FXML private ComboBox<String> fieldStatut;
    @FXML private TextField        fieldImagePath;
    @FXML private Label            erreurTitre;
    @FXML private Label            erreurCategorie;
    @FXML private Label            erreurContenu;
    @FXML private Label            messageLabel;
    @FXML private Button           btnSauvegarder;

    private final PublicationService service = new PublicationService();
    private Publication publicationAModifier = null;
    private String imagePathSelectionne = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        fieldCategorie.getItems().addAll(
                "Santé générale", "Diabète", "Hypertension",
                "Cardiologie", "Nutrition", "Activité physique",
                "Mental & bien-être", "Autre"
        );
        fieldStatut.getItems().addAll("publie", "brouillon", "archive");
        fieldStatut.setValue("publie");

        if (SessionManager.getInstance().isPatient()) {
            fieldTitre.setDisable(true);
            fieldContenu.setDisable(true);
            fieldCategorie.setDisable(true);
            fieldStatut.setDisable(true);
            btnSauvegarder.setDisable(true);
            afficherErreur(messageLabel, "Vous n'avez pas la permission de publier.");
        }
    }

    public void setPublication(Publication p) {
        this.publicationAModifier = p;
        labelTitre.setText("Modifier la publication");
        btnSauvegarder.setText("Enregistrer");
        fieldTitre.setText(p.getTitre());
        fieldContenu.setText(p.getContenu());
        fieldCategorie.setValue(p.getCategorie());
        fieldStatut.setValue(p.getStatut());
        if (p.getImagePath() != null) {
            imagePathSelectionne = p.getImagePath();
            fieldImagePath.setText(p.getImagePath());
        }
    }

    @FXML
    private void choisirImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images",
                        "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        File file = fileChooser.showOpenDialog(fieldTitre.getScene().getWindow());
        if (file != null) {
            imagePathSelectionne = file.getAbsolutePath();
            fieldImagePath.setText(file.getName());
        }
    }

    @FXML
    private void supprimerImage() {
        imagePathSelectionne = null;
        fieldImagePath.clear();
    }

    @FXML
    private void sauvegarder() {
        cacherErreurs();
        if (!valider()) return;

        if (publicationAModifier == null) {
            Publication p = new Publication();
            p.setTitre(fieldTitre.getText().trim());
            p.setContenu(fieldContenu.getText().trim());
            p.setCategorie(fieldCategorie.getValue());
            p.setStatut(fieldStatut.getValue());
            p.setImagePath(imagePathSelectionne);
            var user = SessionManager.getInstance().getCurrentUser();
            if (user != null) p.setAuteurId(user.getId());

            if (service.ajouter(p)) {
                afficherSucces("Publication ajoutée !");
                viderFormulaire();
            } else {
                afficherErreur(messageLabel, "Erreur lors de l'ajout.");
            }
        } else {
            publicationAModifier.setTitre(fieldTitre.getText().trim());
            publicationAModifier.setContenu(fieldContenu.getText().trim());
            publicationAModifier.setCategorie(fieldCategorie.getValue());
            publicationAModifier.setStatut(fieldStatut.getValue());
            publicationAModifier.setImagePath(imagePathSelectionne);

            if (service.modifier(publicationAModifier)) {
                afficherSucces("Publication modifiée !");
            } else {
                afficherErreur(messageLabel, "Erreur lors de la modification.");
            }
        }
    }

    private boolean valider() {
        boolean ok = true;
        if (fieldTitre.getText().trim().length() < 5) {
            afficherErreur(erreurTitre, "Titre : minimum 5 caractères.");
            ok = false;
        }
        if (fieldCategorie.getValue() == null) {
            afficherErreur(erreurCategorie, "Veuillez choisir une catégorie.");
            ok = false;
        }
        if (fieldContenu.getText().trim().length() < 10) {
            afficherErreur(erreurContenu, "Contenu : minimum 10 caractères.");
            ok = false;
        }
        if (publicationAModifier == null) {
            boolean titreExiste = service.titreExiste(fieldTitre.getText().trim());
            if (titreExiste) {
                afficherErreur(erreurTitre, "Une publication avec ce titre existe déjà.");
                ok = false;
            }
        }
        return ok;
    }

    @FXML
    private void annuler() {
        ((Stage) fieldTitre.getScene().getWindow()).close();
    }

    private void afficherErreur(Label l, String msg) {
        l.setText(msg); l.setVisible(true); l.setManaged(true);
        l.setStyle("-fx-text-fill:#dc2626; -fx-font-size:12px;" +
                "-fx-background-color:#fef2f2; -fx-background-radius:6px;" +
                "-fx-padding:6 10; -fx-border-color:#fecaca;" +
                "-fx-border-width:1; -fx-border-radius:6px;");
    }

    private void afficherSucces(String msg) {
        messageLabel.setText(msg);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        messageLabel.setStyle("-fx-text-fill:#16a34a; -fx-font-size:12px;" +
                "-fx-background-color:#f0fdf4; -fx-background-radius:6px;" +
                "-fx-padding:6 10; -fx-border-color:#bbf7d0;" +
                "-fx-border-width:1; -fx-border-radius:6px;");
    }

    private void cacherErreurs() {
        for (Label l : new Label[]{erreurTitre, erreurCategorie, erreurContenu, messageLabel}) {
            l.setVisible(false);
            l.setManaged(false);
        }
    }

    private void viderFormulaire() {
        fieldTitre.clear();
        fieldContenu.clear();
        fieldCategorie.setValue(null);
        fieldStatut.setValue("publie");
        fieldImagePath.clear();
        imagePathSelectionne = null;
    }
}