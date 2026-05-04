package com.chroniccare.controllers;

import com.chroniccare.models.User;
import com.chroniccare.modules.suivi.view.SuiviDashboardView;
import com.chroniccare.utils.SessionManager;
import com.chroniccare.utils.SidebarRoleBadgeHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class NutriShellController {

    @FXML private VBox sidebarRoot;
    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarRoleBadge;
    @FXML private Label pageTitle;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;
    @FXML private StackPane contentPane;

    @FXML private Button btnHome;
    @FXML private Button btnProfile;
    @FXML private Button btnSuivi;
    @FXML private Button btnEtat;
    @FXML private Button btnActivite;
    @FXML private Button btnRdv;

    private SuiviDashboardView suiviDashboardView;
    private VBox suiviContainer;

    private static NutriShellController instance;
    public static NutriShellController getInstance() { return instance; }

    public boolean isInActiveScene() {
        try {
            return sidebarRoot != null
                    && sidebarRoot.getScene() != null
                    && sidebarRoot.getScene().getRoot() != null
                    && sidebarRoot.getScene().getRoot().lookup("#sidebarRoot") != null;
        } catch (Exception e) { return false; }
    }

    @FXML
    public void initialize() {
        instance = this;
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        String initials = getInitials(user);
        sidebarAvatar.setText(initials);
        topbarAvatar.setText(initials);
        sidebarUserName.setText(user.getPrenom() + " " + user.getNom());
        sidebarRoleBadge.setText("Nutritionniste");
        SidebarRoleBadgeHelper.applyRoleStyle(sidebarRoleBadge, user);
        topbarUserName.setText(user.getPrenom() + " " + user.getNom());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        topbarDate.setText(LocalDate.now().format(fmt));

        // Préparer le module suivi
        suiviDashboardView = new SuiviDashboardView(user);
        suiviContainer = new VBox();
        suiviContainer.getChildren().setAll(suiviDashboardView.build());
        Platform.runLater(() -> {
            try { suiviDashboardView.showLoginMessageNotification(); }
            catch (Exception e) { e.printStackTrace(); }
        });

        showHome();

        // Consommer la page en attente
        String pending = SessionManager.getInstance().consumePendingNutriPage();
        if (pending != null) {
            switch (pending) {
                case "profile"  -> showProfile();
                case "suivi"    -> showSuivi();
                case "etat"     -> showEtat();
                case "activite" -> showActivite();
                case "rdv"      -> showRdv();
            }
        }
    }

    @FXML public void showHome() {
        setActive(btnHome);
        pageTitle.setText("Tableau de bord");
        // Afficher un dashboard simple inline
        javafx.scene.layout.VBox dash = new javafx.scene.layout.VBox(20);
        dash.setStyle("-fx-padding: 24;");
        javafx.scene.control.Label title = new javafx.scene.control.Label("Bienvenue sur votre espace Nutritionniste");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        javafx.scene.control.Label sub = new javafx.scene.control.Label("Utilisez la barre de navigation pour accéder à vos outils.");
        sub.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        dash.getChildren().addAll(title, sub);
        contentPane.getChildren().setAll(dash);
    }

    @FXML public void showProfile() {
        setActive(btnProfile);
        pageTitle.setText("Mon Profil");
        loadContent("/com/chroniccare/profile-nutritionniste.fxml");
    }

    @FXML public void showSuivi() {
        setActive(btnSuivi);
        pageTitle.setText("Mon Suivi");
        contentPane.getChildren().setAll(suiviContainer);
    }

    @FXML public void showEtat() {
        setActive(btnEtat);
        pageTitle.setText("Etat patient");
        contentPane.getChildren().setAll(suiviContainer);
        if (suiviDashboardView != null) suiviDashboardView.showEtatTab();
    }

    @FXML public void showActivite() {
        setActive(btnActivite);
        pageTitle.setText("Activite patient");
        contentPane.getChildren().setAll(suiviContainer);
        if (suiviDashboardView != null) suiviDashboardView.showActiviteTab();
    }

    @FXML public void showRdv() {
        setActive(btnRdv);
        pageTitle.setText("RDV");
        loadContent("/com/chroniccare/nutrition-rdv.fxml");
    }

    @FXML public void handleLogout() {
        try {
            SessionManager.getInstance().logout();
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/login.fxml"));
            sidebarRoot.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void loadContent(String fxmlPath) {
        try {
            javafx.scene.Node content = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActive(Button btn) {
        Button[] all = {btnHome, btnProfile, btnSuivi, btnEtat, btnActivite, btnRdv};
        for (Button b : all) if (b != null) b.getStyleClass().remove("nav-btn-active");
        if (btn != null) btn.getStyleClass().add("nav-btn-active");
    }

    private String getInitials(User user) {
        String p = (user.getPrenom() != null && !user.getPrenom().isEmpty())
                ? String.valueOf(user.getPrenom().charAt(0)).toUpperCase() : "";
        String n = (user.getNom() != null && !user.getNom().isEmpty())
                ? String.valueOf(user.getNom().charAt(0)).toUpperCase() : "";
        return p + n;
    }
}
