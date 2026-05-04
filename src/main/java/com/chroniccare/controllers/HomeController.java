package com.chroniccare.controllers;

import com.chroniccare.models.User;
import com.chroniccare.modules.suivi.view.SuiviDashboardView;
import com.chroniccare.services.AppointmentService;
import com.chroniccare.services.UserService;
import com.chroniccare.utils.SessionManager;
import com.chroniccare.utils.SidebarNavHighlight;
import com.chroniccare.utils.SidebarRoleBadgeHelper;
import javafx.application.Platform;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.File;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class HomeController {

    @FXML private VBox sidebarRoot;
    @FXML private Label sidebarAvatar;
    @FXML private ImageView sidebarAvatarImage;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarRoleBadge;

    @FXML private VBox adminNavBox;
    @FXML private VBox nutriNavBox;
    @FXML private VBox defaultNavBox;

    @FXML private Button btnHome;
    @FXML private Button btnProfile;
    @FXML private Button btnEtat;
    @FXML private Button btnActivite;
    @FXML private Button btnStats;
    @FXML private Button btnCoachEvents;
    @FXML private Button btnCoachExercises;
    @FXML private Button btnCoachEtat;
    @FXML private Button btnCoachActivite;
    @FXML private VBox coachNav;
    @FXML private VBox patientNav;
    @FXML private VBox patientEventsNav;
    @FXML private Button btnPatientBookRdv;
    @FXML private Button btnPatientConsultations;

    @FXML private Label pageTitle;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private ImageView topbarAvatarImage;
    @FXML private Label topbarUserName;

    @FXML private Label welcomeTitle;
    @FXML private Label welcomeSubtitle;
    @FXML private Label statUsers;
    @FXML private Label statRdv;
    @FXML private Label statActivity;

    @FXML private VBox adminStatCard;
    @FXML private VBox adminPanel;

    @FXML private Label infoMedical;
    @FXML private Label infoGenre;
    @FXML private Label infoTel;

    @FXML private VBox dashboardOverview;
    @FXML private VBox suiviModuleContainer;

    private final UserService userService = new UserService();
    private final AppointmentService appointmentService = new AppointmentService();
    private SuiviDashboardView suiviDashboardView;

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
        SidebarRoleBadgeHelper.applyRoleStyle(sidebarRoleBadge, user);

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

        applySidebarByRole();

        boolean isAdmin = SessionManager.getInstance().isAdmin();
        adminPanel.setVisible(isAdmin);
        adminPanel.setManaged(isAdmin);
        adminStatCard.setVisible(isAdmin);
        adminStatCard.setManaged(isAdmin);

        if (isAdmin) {
            try { statUsers.setText(String.valueOf(userService.countAll())); }
            catch (Exception e) { statUsers.setText("-"); }
        }

        if (statRdv != null) {
            try {
                if (SessionManager.getInstance().isPatient())
                    statRdv.setText(String.valueOf(appointmentService.countUpcomingForPatient(user.getId())));
                else if (SessionManager.getInstance().isNutritionniste())
                    statRdv.setText(String.valueOf(appointmentService.countPendingForNutritionist(user.getId())));
                else statRdv.setText("-");
            } catch (Exception e) { statRdv.setText("-"); }
        }
        if (statActivity != null && SessionManager.getInstance().isPatient())
            statActivity.setText(String.valueOf(user.getActivityScore()));

        if (suiviModuleContainer != null) {
            suiviDashboardView = new SuiviDashboardView(user);
            suiviModuleContainer.getChildren().setAll(suiviDashboardView.build());
            Platform.runLater(() -> {
                try { if (suiviDashboardView != null) suiviDashboardView.showLoginMessageNotification(); }
                catch (Exception e) { e.printStackTrace(); }
            });
        }

        showHome();

        String pendingSuivi = SessionManager.getInstance().consumePendingHomeSuiviTab();
        if ("activite".equalsIgnoreCase(pendingSuivi)) showActiviteModule();
        else if ("etat".equalsIgnoreCase(pendingSuivi)) showEtatModule();
    }

    private void applySidebarByRole() {
        boolean isAdmin = SessionManager.getInstance().isAdmin();
        boolean isNutri = SessionManager.getInstance().isNutritionniste();
        boolean isCoach = SessionManager.getInstance().isCoach();
        boolean isPatient = SessionManager.getInstance().isPatient();

        setVBoxVisible(adminNavBox, isAdmin);
        setVBoxVisible(nutriNavBox, isNutri);
        setVBoxVisible(defaultNavBox, !isAdmin && !isNutri);

        if (!isAdmin && !isNutri) {
            if (coachNav != null) { coachNav.setVisible(isCoach); coachNav.setManaged(isCoach); }
            if (patientNav != null) { patientNav.setVisible(isPatient); patientNav.setManaged(isPatient); }
            applySuiviLabels();
        }

        if (isAdmin) {
            if (sidebarRoot != null) SidebarNavHighlight.activate(sidebarRoot, "adm:home");
        } else if (isNutri) {
            if (sidebarRoot != null) SidebarNavHighlight.activate(sidebarRoot, "nut:home");
        }
    }

    private void setVBoxVisible(VBox vbox, boolean visible) {
        if (vbox != null) { vbox.setVisible(visible); vbox.setManaged(visible); }
    }

    // Sidebar admin
    @FXML public void goToUsers() { navigateTo("/com/chroniccare/list-users.fxml"); }
    @FXML public void goToBlockedAccounts() { navigateTo("/com/chroniccare/blocked-accounts.fxml"); }
    @FXML public void goToMedicalAudit() { navigateTo("/com/chroniccare/medical-audit.fxml"); }
    @FXML public void goToPatientSegmentation() { navigateTo("/com/chroniccare/patient-segmentation.fxml"); }
    @FXML public void goToStats() { navigateTo("/com/chroniccare/stats.fxml"); }
    @FXML public void goToAdminConsultLedger() { navigateTo("/com/chroniccare/patient-consultations.fxml"); }
    @FXML public void goToAdminNutritionRdv() { navigateTo("/com/chroniccare/nutrition-rdv.fxml"); }

    // Sidebar nutritionniste
    @FXML public void goToNutritionHomeSuivi() {
        if (pageTitle != null) pageTitle.setText("Mon Suivi");
        setDashboardVisible(false);
        setSuiviVisible(true);
        if (sidebarRoot != null) SidebarNavHighlight.activate(sidebarRoot, "nut:suivi");
    }

    @FXML public void goToRdv() { navigateTo("/com/chroniccare/nutrition-rdv.fxml"); }

    // Navigation commune
    @FXML public void showHome() {
        if (pageTitle != null) pageTitle.setText("Tableau de bord");
        setDashboardVisible(true);
        setSuiviVisible(false);
        if (SessionManager.getInstance().isAdmin())
            { if (sidebarRoot != null) SidebarNavHighlight.activate(sidebarRoot, "adm:home"); }
        else if (SessionManager.getInstance().isNutritionniste())
            { if (sidebarRoot != null) SidebarNavHighlight.activate(sidebarRoot, "nut:home"); }
        else setActiveNav(btnHome);
    }

    @FXML public void showEtatModule() {
        boolean simple = SessionManager.getInstance().isPatient() || SessionManager.getInstance().isCoach();
        if (pageTitle != null) pageTitle.setText(simple ? "Mon Etat" : "Etat patient");
        setDashboardVisible(false);
        setSuiviVisible(true);
        if (suiviDashboardView != null) suiviDashboardView.showEtatTab();
        if (SessionManager.getInstance().isNutritionniste())
            { if (sidebarRoot != null) SidebarNavHighlight.activate(sidebarRoot, "nut:etat"); }
        else setActiveNav(SessionManager.getInstance().isCoach() ? btnCoachEtat : btnEtat);
    }

    @FXML public void showActiviteModule() {
        boolean simple = SessionManager.getInstance().isPatient() || SessionManager.getInstance().isCoach();
        if (pageTitle != null) pageTitle.setText(simple ? "Mon Activite" : "Activite patient");
        setDashboardVisible(false);
        setSuiviVisible(true);
        if (suiviDashboardView != null) suiviDashboardView.showActiviteTab();
        if (SessionManager.getInstance().isNutritionniste())
            { if (sidebarRoot != null) SidebarNavHighlight.activate(sidebarRoot, "nut:activite"); }
        else setActiveNav(SessionManager.getInstance().isCoach() ? btnCoachActivite : btnActivite);
    }

    @FXML public void goToProfile() {
        try {
            String fxml;
            if (SessionManager.getInstance().isPatient()) fxml = "/com/chroniccare/profile-patient.fxml";
            else if (SessionManager.getInstance().isCoach()) fxml = "/com/chroniccare/profile-coach.fxml";
            else if (SessionManager.getInstance().isNutritionniste()) fxml = "/com/chroniccare/profile-nutritionniste.fxml";
            else fxml = "/com/chroniccare/list-users.fxml";
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            sidebarRoot.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void goToPatientEvents() { navigateTo("/com/chroniccare/patient-events.fxml"); }
    @FXML public void goToPatientRegistrations() { navigateTo("/com/chroniccare/patient-registrations.fxml"); }
    @FXML public void goToPatientBookRdv() { navigateTo("/com/chroniccare/patient-rdv.fxml"); }
    @FXML public void goToPatientConsultations() { navigateTo("/com/chroniccare/patient-consultations.fxml"); }
    @FXML public void goToCoachEvents() { navigateTo("/com/chroniccare/coach-events.fxml"); }
    @FXML public void goToCoachExercises() { navigateTo("/com/chroniccare/coach-exercises.fxml"); }
    @FXML public void goToAddUser() { navigateTo("/com/chroniccare/add-user.fxml"); }

    @FXML public void handleLogout() {
        try {
            SessionManager.getInstance().logout();
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/login.fxml"));
            sidebarRoot.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            sidebarRoot.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void applySuiviLabels() {
        boolean coach = SessionManager.getInstance().isCoach();
        boolean patient = SessionManager.getInstance().isPatient();
        if (btnEtat != null) btnEtat.setText((patient || coach) ? "Mon Etat" : "Etat patient");
        if (btnActivite != null) btnActivite.setText((patient || coach) ? "Activite" : "Activite patient");
        if (btnCoachEtat != null && btnEtat != null) btnCoachEtat.setText(btnEtat.getText());
        if (btnCoachActivite != null && btnActivite != null) btnCoachActivite.setText(btnActivite.getText());
    }

    private void setDashboardVisible(boolean v) {
        if (dashboardOverview != null) { dashboardOverview.setVisible(v); dashboardOverview.setManaged(v); }
    }

    private void setSuiviVisible(boolean v) {
        if (suiviModuleContainer != null) { suiviModuleContainer.setVisible(v); suiviModuleContainer.setManaged(v); }
    }

    private void setActiveNav(Button active) {
        Button[] all = {btnHome, btnEtat, btnActivite, btnCoachEvents, btnCoachExercises, btnCoachEtat, btnCoachActivite};
        for (Button b : all) if (b != null) b.getStyleClass().remove("nav-btn-active");
        if (active != null && !active.getStyleClass().contains("nav-btn-active"))
            active.getStyleClass().add("nav-btn-active");
    }

    private void loadUserPhotoIfExists(User user) {
        try {
            if (user.getPhotoProfil() == null || user.getPhotoProfil().isBlank()) { showInitialAvatars(); return; }
            File file = new File(user.getPhotoProfil());
            if (!file.exists()) { showInitialAvatars(); return; }
            Image image = new Image(file.toURI().toString());
            sidebarAvatarImage.setImage(image); topbarAvatarImage.setImage(image);
            sidebarAvatarImage.setVisible(true); sidebarAvatarImage.setManaged(true);
            topbarAvatarImage.setVisible(true);  topbarAvatarImage.setManaged(true);
            sidebarAvatar.setVisible(false);     sidebarAvatar.setManaged(false);
            topbarAvatar.setVisible(false);      topbarAvatar.setManaged(false);
        } catch (Exception e) { showInitialAvatars(); }
    }

    private void showInitialAvatars() {
        sidebarAvatar.setVisible(true);       sidebarAvatar.setManaged(true);
        topbarAvatar.setVisible(true);        topbarAvatar.setManaged(true);
        sidebarAvatarImage.setVisible(false); sidebarAvatarImage.setManaged(false);
        topbarAvatarImage.setVisible(false);  topbarAvatarImage.setManaged(false);
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
        if (user.getRoles().contains("ROLE_ADMIN"))          return "Administrateur";
        if (user.getRoles().contains("ROLE_NUTRITIONNISTE")) return "Nutritionniste";
        if (user.getRoles().contains("ROLE_COACH"))          return "Coach";
        if (user.getRoles().contains("ROLE_PATIENT"))        return "Patient";
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
    @FXML
    private void goToForum() {
        try {
            String fxml = SessionManager.getInstance().isAdmin()
                    ? "/com/chroniccare/forum.fxml"
                    : "/com/chroniccare/forum-front.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            BorderPane forumPane = loader.load();

            // Prendre seulement le centre du forum (sans sidebar ni topbar)
            javafx.scene.Node centerContent = forumPane.getCenter();

            if (pageTitle != null) pageTitle.setText("Forum");
            setDashboardVisible(false);
            setSuiviVisible(false);

            VBox mainContent = (VBox) sidebarRoot.getScene().lookup("#mainContent");
            if (mainContent != null && centerContent != null) {
                // Garder la topbar de home (premier enfant) et remplacer le reste
                if (mainContent.getChildren().size() > 1) {
                    mainContent.getChildren().remove(1, mainContent.getChildren().size());
                }
                mainContent.getChildren().add(centerContent);
            }
        } catch (Exception e) {
            System.err.println("Erreur navigation forum : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showRewardPopup(User user) {
        if (user == null || !user.isMostActive()) return;
        if (user.getActivityBadge() == null || user.getActivityReward() == null) return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Recompense obtenue");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.OK);

        Label iconLabel = new Label("ðŸ†");
        iconLabel.setStyle("-fx-font-size: 42px;");
        Label titleLabel = new Label("Felicitations " + user.getPrenom() + " !");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1d4ed8;");
        Label subtitleLabel = new Label("Vous etes l'un des utilisateurs recompenses sur ChronicCare.");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        Label badgeLabel = new Label("ðŸ… Badge : " + user.getActivityBadge());
        badgeLabel.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 10 14 10 14; -fx-background-radius: 10; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label rewardLabel = new Label("ðŸŽ Recompense : " + user.getActivityReward());
        rewardLabel.setWrapText(true);
        rewardLabel.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1e3a8a; -fx-padding: 10 14 10 14; -fx-background-radius: 10; -fx-font-size: 14px;");
        Label footerLabel = new Label("Merci pour votre engagement sur la plateforme.");
        footerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        VBox content = new VBox(12, iconLabel, titleLabel, subtitleLabel, badgeLabel, rewardLabel, footerLabel);
        content.setStyle("-fx-padding: 20; -fx-background-color: white;");
        content.setPrefWidth(420);
        dialogPane.setContent(content);
        dialogPane.setStyle("-fx-background-color: white;");

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setText("Super !");
        okButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 18 8 18;");
        dialog.showAndWait();
    }
}