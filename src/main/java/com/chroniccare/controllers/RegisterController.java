package com.chroniccare.controllers;

import com.chroniccare.models.User;
import com.chroniccare.services.UserService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import javafx.scene.layout.VBox;
import com.chroniccare.services.PatientMedicalRecordService;
import com.chroniccare.utils.PasswordUtils;


public class RegisterController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField telephoneField;
    @FXML private ComboBox<String> genreCombo;
    @FXML private ComboBox<String> rolesCombo;
    @FXML private Label errorLabel;
    @FXML private ImageView photoPreview;
    @FXML private VBox patientMedicalSection;
    @FXML private TextField medicalConditionField;

    private final UserService userService = new UserService();
    private final PatientMedicalRecordService patientMedicalRecordService = new PatientMedicalRecordService();
    private File selectedPhotoFile;

    @FXML
    public void initialize() {
        genreCombo.setItems(FXCollections.observableArrayList("Homme", "Femme"));
        rolesCombo.setItems(FXCollections.observableArrayList(
                "ROLE_PATIENT", "ROLE_COACH", "ROLE_NUTRITIONNISTE"
        ));

        rolesCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            boolean isPatient = "ROLE_PATIENT".equals(newValue);
            patientMedicalSection.setVisible(isPatient);
            patientMedicalSection.setManaged(isPatient);

            if (!isPatient) {
                medicalConditionField.clear();
            }
        });
    }


    @FXML
    public void handleChoosePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(nomField.getScene().getWindow());

        if (file != null) {
            selectedPhotoFile = file;
            photoPreview.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    public void handleRegister() {
        StringBuilder errors = new StringBuilder();

        if (nomField.getText().trim().isEmpty())
            errors.append("• Nom obligatoire\n");

        if (prenomField.getText().trim().isEmpty())
            errors.append("• Prénom obligatoire\n");

        if (!emailField.getText().matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$"))
            errors.append("• Email invalide\n");

        if (passwordField.getText().length() < 8)
            errors.append("• Mot de passe : minimum 8 caractères\n");

        if (!telephoneField.getText().matches("\\d{8}"))
            errors.append("• Téléphone : exactement 8 chiffres\n");

        if (genreCombo.getValue() == null)
            errors.append("• Genre obligatoire\n");

        if (rolesCombo.getValue() == null)
            errors.append("• Rôle obligatoire\n");

        if ("ROLE_PATIENT".equals(rolesCombo.getValue())
                && (medicalConditionField.getText() == null || medicalConditionField.getText().trim().isEmpty())) {
            errors.append("• Maladie principale obligatoire\n");
        }

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreurs de saisie");
            alert.setHeaderText("Veuillez corriger les erreurs suivantes :");
            alert.setContentText(errors.toString());
            alert.show();
            return;
        }


        try {
            User u = new User(
                    nomField.getText().trim(),
                    prenomField.getText().trim(),
                    emailField.getText().trim(),
                    PasswordUtils.hash(passwordField.getText()),
                    "[\"" + rolesCombo.getValue() + "\"]",
                    telephoneField.getText().trim(),
                    genreCombo.getValue()
            );
            if ("ROLE_PATIENT".equals(rolesCombo.getValue())) {
                u.setMedicalCondition(medicalConditionField.getText().trim());
            }

            if (selectedPhotoFile != null) {
                String photoPath = savePhoto(selectedPhotoFile);
                u.setPhotoProfil(photoPath);
            }

            userService.insert(u);

            if (u.getRoles() != null && u.getRoles().contains("ROLE_PATIENT")) {
                User savedUser = userService.findByEmail(u.getEmail());
                if (savedUser != null) {
                    patientMedicalRecordService.createInitialRecord(savedUser.getId(), u.getMedicalCondition());
                }
            }

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Succès");
            success.setHeaderText(null);
            success.setContentText("Compte créé avec succès !");
            success.showAndWait();

            goToLogin();

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    private String savePhoto(File photoFile) throws IOException {
        File uploadDir = new File("uploads/profile");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String originalName = photoFile.getName();
        String extension = "";

        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }

        String uniqueName = UUID.randomUUID() + extension;
        Path destination = Paths.get(uploadDir.getPath(), uniqueName);

        Files.copy(photoFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

        return destination.toString();
    }

    @FXML
    public void goToLogin() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/chroniccare/login.fxml"));
            nomField.getScene().setRoot(root);
        } catch (Exception e) {
            errorLabel.setText("Erreur navigation : " + e.getMessage());
        }
    }
}
