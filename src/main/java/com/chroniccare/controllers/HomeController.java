package com.chroniccare.controllers;

import com.chroniccare.models.User;
import com.chroniccare.services.UserService;
import com.chroniccare.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class HomeController {

    @FXML private Label sidebarAvatar;
    @FXML private ImageView sidebarAvatarImage;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarRoleBadge;

    @FXML private Button btnHome;
    @FXML private Button btnProfile;
    @FXML private Button btnUsers;
    @FXML private Button btnStats;
    @FXML private Button btnBlockedAccounts;
    @FXML private Button btnMedicalAudit;
    @FXML private Button btnPatientSegmentation;

    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private ImageView topbarAvatarImage;
    @FXML private Label topbarUserName;

    @FXML private Label welcomeTitle;
    @FXML private Label welcomeSubtitle;
    @FXML private Label statUsers;

    @FXML private VBox adminStatCard;
    @FXML private VBox adminPanel;

    @FXML private Label infoMedical;
    @FXML private Label infoGenre;
    @FXML private Label infoTel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        String initials = getInitials(user);
        sidebarAvatar.setText(initials);
        topbarAvatar.setText(initials);

        loadUserPhotoIfExists(user);

        sidebarUserName.setText(user.getPrenom() + " " + user.getNom());
        sidebarRoleBadge.setText(getRoleLabel(user));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        topbarDate.setText(LocalDate.now().format(fmt));
        topbarUserName.setText(user.getPrenom() + " " + user.getNom());

        welcomeTitle.setText("Bonjour, " + user.getPrenom() + " !");
        welcomeSubtitle.setText(getWelcomeSubtitle());
        showRewardPopup(user);

        infoMedical.setText(user.getMedicalCondition() != null && !user.getMedicalCondition().isEmpty()
                ? capitalize(user.getMedicalCondition()) : "Non renseigne");
        infoGenre.setText(user.getGenre() != null ? user.getGenre() : "-");
        infoTel.setText(user.getTelephone() != null && !user.getTelephone().isEmpty()
                ? user.getTelephone() : "-");

        boolean isAdmin = SessionManager.getInstance().isAdmin();
        adminPanel.setVisible(isAdmin);
        adminPanel.setManaged(isAdmin);
        adminStatCard.setVisible(isAdmin);
        adminStatCard.setManaged(isAdmin);
        btnUsers.setVisible(isAdmin);
        btnUsers.setManaged(isAdmin);
        btnStats.setVisible(isAdmin);
        btnStats.setManaged(isAdmin);
        if (btnBlockedAccounts != null) {
            btnBlockedAccounts.setVisible(isAdmin);
            btnBlockedAccounts.setManaged(isAdmin);
        }
        if (btnMedicalAudit != null) {
            btnMedicalAudit.setVisible(isAdmin);
            btnMedicalAudit.setManaged(isAdmin);
        }
        if (btnPatientSegmentation != null) {
            btnPatientSegmentation.setVisible(isAdmin);
            btnPatientSegmentation.setManaged(isAdmin);
        }

        if (isAdmin) {
            try {
                statUsers.setText(String.valueOf(userService.countAll()));
            } catch (Exception e) {
                statUsers.setText("-");
            }
        }
    }

    @FXML
    public void showHome() {}

    @FXML
    public void goToProfile() {
        try {
            String fxml;
            if (SessionManager.getInstance().isPatient()) {
                fxml = "/com/chroniccare/profile-patient.fxml";
            } else if (SessionManager.getInstance().isCoach()) {
                fxml = "/com/chroniccare/profile-coach.fxml";
            } else if (SessionManager.getInstance().isNutritionniste()) {
                fxml = "/com/chroniccare/profile-nutritionniste.fxml";
            } else if (SessionManager.getInstance().isAdmin()) {
                fxml = "/com/chroniccare/list-users.fxml";
            } else {
                fxml = "/com/chroniccare/profile-patient.fxml";
            }

            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            btnProfile.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToUsers() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/list-users.fxml"));
            btnUsers.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToStats() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/stats.fxml"));
            btnHome.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToAddUser() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/add-user.fxml"));
            btnUsers.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        try {
            SessionManager.getInstance().logout();
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/login.fxml"));
            sidebarUserName.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void goToBlockedAccounts() {
        if (!SessionManager.getInstance().isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Seul l'administrateur peut accéder aux comptes bloqués.");
            alert.showAndWait();
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/blocked-accounts.fxml"));
            btnHome.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToMedicalAudit() {
        if (!SessionManager.getInstance().isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Seul l'administrateur peut accéder à l'audit médical.");
            alert.showAndWait();
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/medical-audit.fxml"));
            btnHome.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToPatientSegmentation() {
        if (!SessionManager.getInstance().isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Seul l'administrateur peut accéder à la segmentation patients.");
            alert.showAndWait();
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/patient-segmentation.fxml"));
            btnHome.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String getInitials(User user) {
        String p = (user.getPrenom() != null && !user.getPrenom().isEmpty())
                ? String.valueOf(user.getPrenom().charAt(0)).toUpperCase() : "";
        String n = (user.getNom() != null && !user.getNom().isEmpty())
                ? String.valueOf(user.getNom().charAt(0)).toUpperCase() : "";
        return p + n;
    }

    private String getRoleLabel(User user) {
        if (user.getRoles() == null) return "Utilisateur";
        if (user.getRoles().contains("ROLE_ADMIN")) return "Administrateur";
        if (user.getRoles().contains("ROLE_NUTRITIONNISTE")) return "Nutritionniste";
        if (user.getRoles().contains("ROLE_COACH")) return "Coach";
        if (user.getRoles().contains("ROLE_PATIENT")) return "Patient";
        return "Utilisateur";
    }

    private String getWelcomeSubtitle() {
        if (SessionManager.getInstance().isAdmin())
            return "Gerez la plateforme ChronicCare et les utilisateurs inscrits.";
        if (SessionManager.getInstance().isPatient())
            return "Suivez votre sante au quotidien et consultez vos donnees medicales.";
        if (SessionManager.getInstance().isCoach())
            return "Accompagnez vos patients dans leur parcours de sante.";
        if (SessionManager.getInstance().isNutritionniste())
            return "Guidez vos patients vers une alimentation adaptee a leur condition.";
        return "Bienvenue sur ChronicCare.";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private void loadUserPhotoIfExists(User user) {
        try {
            if (user.getPhotoProfil() == null || user.getPhotoProfil().isBlank()) {
                showInitialAvatars();
                return;
            }

            File file = new File(user.getPhotoProfil());

            if (!file.exists()) {
                showInitialAvatars();
                return;
            }

            Image image = new Image(file.toURI().toString());

            sidebarAvatarImage.setImage(image);
            topbarAvatarImage.setImage(image);

            sidebarAvatarImage.setVisible(true);
            sidebarAvatarImage.setManaged(true);
            topbarAvatarImage.setVisible(true);
            topbarAvatarImage.setManaged(true);

            sidebarAvatar.setVisible(false);
            sidebarAvatar.setManaged(false);
            topbarAvatar.setVisible(false);
            topbarAvatar.setManaged(false);

        } catch (Exception e) {
            e.printStackTrace();
            showInitialAvatars();
        }
    }

    private void showInitialAvatars() {
        sidebarAvatar.setVisible(true);
        sidebarAvatar.setManaged(true);
        topbarAvatar.setVisible(true);
        topbarAvatar.setManaged(true);

        sidebarAvatarImage.setVisible(false);
        sidebarAvatarImage.setManaged(false);
        topbarAvatarImage.setVisible(false);
        topbarAvatarImage.setManaged(false);
    }

    private void showRewardPopup(User user) {
        if (user == null) return;
        if (!user.isMostActive()) return;
        if (user.getActivityBadge() == null || user.getActivityReward() == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Récompense obtenue");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.OK);

        Label iconLabel = new Label("🏆");
        iconLabel.setStyle("-fx-font-size: 42px;");

        Label titleLabel = new Label("Félicitations " + user.getPrenom() + " !");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1d4ed8;");

        Label subtitleLabel = new Label("Vous êtes l’un des utilisateurs récompensés sur ChronicCare.");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        Label badgeLabel = new Label("🏅 Badge : " + user.getActivityBadge());
        badgeLabel.setStyle(
                "-fx-background-color: #fef3c7; " +
                        "-fx-text-fill: #92400e; " +
                        "-fx-padding: 10 14 10 14; " +
                        "-fx-background-radius: 10; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold;"
        );

        Label rewardLabel = new Label("🎁 Récompense : " + user.getActivityReward());
        rewardLabel.setWrapText(true);
        rewardLabel.setStyle(
                "-fx-background-color: #dbeafe; " +
                        "-fx-text-fill: #1e3a8a; " +
                        "-fx-padding: 10 14 10 14; " +
                        "-fx-background-radius: 10; " +
                        "-fx-font-size: 14px;"
        );

        Label footerLabel = new Label("Merci pour votre engagement sur la plateforme.");
        footerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        VBox content = new VBox(12, iconLabel, titleLabel, subtitleLabel, badgeLabel, rewardLabel, footerLabel);
        content.setStyle("-fx-padding: 20; -fx-background-color: white;");
        content.setPrefWidth(420);

        dialogPane.setContent(content);
        dialogPane.setStyle("-fx-background-color: white;");

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setText("Super !");
        okButton.setStyle(
                "-fx-background-color: #2563eb; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 8 18 8 18;"
        );

        dialog.showAndWait();
    }
}
