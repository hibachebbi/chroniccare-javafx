package com.chroniccare.controllers;

import com.chroniccare.models.User;
import com.chroniccare.services.UserService;
import com.chroniccare.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class HomeController {

    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarRoleBadge;
    @FXML private Button btnHome;
    @FXML private Button btnProfile;
    @FXML private Button btnUsers;
    @FXML private Button btnEvents;
    @FXML private Button btnExercises;
    @FXML private Button btnNutritionRdv;
    @FXML private Button btnPatientEvents;
    @FXML private Button btnPatientRegistrations;
    @FXML private Label patientCareSectionLabel;
    @FXML private Button btnPatientFollowup;
    @FXML private Button btnPatientBookRdv;
    @FXML private Button btnPatientConsultations;
    @FXML private Button btnPatientNutrition;
    @FXML private Button btnPatientActivity;
    @FXML private Button btnPatientMedications;
    @FXML private Button btnPatientStats;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private Label welcomeTitle;
    @FXML private Label welcomeSubtitle;
    @FXML private Label statUsers;
    @FXML private VBox adminStatCard;
    @FXML private Label infoMedical;
    @FXML private Label infoGenre;
    @FXML private Label infoTel;
    @FXML private VBox adminPanel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        String initials = getInitials(user);
        sidebarAvatar.setText(initials);
        sidebarUserName.setText(user.getPrenom() + " " + user.getNom());
        sidebarRoleBadge.setText(getRoleLabel(user));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        topbarDate.setText(LocalDate.now().format(fmt));
        topbarAvatar.setText(initials);
        topbarUserName.setText(user.getPrenom() + " " + user.getNom());

        welcomeTitle.setText("Bonjour, " + user.getPrenom() + " !");
        welcomeSubtitle.setText(getWelcomeSubtitle());

        infoMedical.setText(user.getMedicalCondition() != null && !user.getMedicalCondition().isEmpty()
                ? capitalize(user.getMedicalCondition()) : "Non renseigne");
        infoGenre.setText(user.getGenre() != null ? user.getGenre() : "-");
        infoTel.setText(user.getTelephone() != null && !user.getTelephone().isEmpty()
                ? user.getTelephone() : "-");

        boolean isAdmin = SessionManager.getInstance().isAdmin();
        boolean isCoach = SessionManager.getInstance().isCoach();
        boolean isPatient = SessionManager.getInstance().isPatient();
        boolean isNutritionniste = SessionManager.getInstance().isNutritionniste();
        adminPanel.setVisible(isAdmin);
        adminPanel.setManaged(isAdmin);
        adminStatCard.setVisible(isAdmin);
        adminStatCard.setManaged(isAdmin);
        btnUsers.setVisible(isAdmin);
        btnUsers.setManaged(isAdmin);
        btnEvents.setVisible(isCoach);
        btnEvents.setManaged(isCoach);
        btnExercises.setVisible(isCoach);
        btnExercises.setManaged(isCoach);
        btnNutritionRdv.setVisible(isNutritionniste);
        btnNutritionRdv.setManaged(isNutritionniste);
        btnPatientEvents.setVisible(isPatient);
        btnPatientEvents.setManaged(isPatient);
        btnPatientRegistrations.setVisible(isPatient);
        btnPatientRegistrations.setManaged(isPatient);
        patientCareSectionLabel.setVisible(isPatient);
        patientCareSectionLabel.setManaged(isPatient);
        btnPatientFollowup.setVisible(isPatient);
        btnPatientFollowup.setManaged(isPatient);
        btnPatientBookRdv.setVisible(isPatient);
        btnPatientBookRdv.setManaged(isPatient);
        btnPatientConsultations.setVisible(isPatient);
        btnPatientConsultations.setManaged(isPatient);
        btnPatientNutrition.setVisible(isPatient);
        btnPatientNutrition.setManaged(isPatient);
        btnPatientActivity.setVisible(isPatient);
        btnPatientActivity.setManaged(isPatient);
        btnPatientMedications.setVisible(isPatient);
        btnPatientMedications.setManaged(isPatient);
        btnPatientStats.setVisible(isPatient);
        btnPatientStats.setManaged(isPatient);

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
    public void goToEvents() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/coach-events.fxml"));
            btnHome.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToExercises() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/coach-exercises.fxml"));
            btnHome.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToPatientEvents() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/patient-events.fxml"));
            btnHome.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToPatientRegistrations() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/patient-registrations.fxml"));
            btnHome.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToNutritionRdv() {}

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
}
