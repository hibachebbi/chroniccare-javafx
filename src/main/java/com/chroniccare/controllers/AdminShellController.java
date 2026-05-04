package com.chroniccare.controllers;

import com.chroniccare.models.User;
import com.chroniccare.services.UserService;
import com.chroniccare.utils.SessionManager;
import com.chroniccare.utils.SidebarRoleBadgeHelper;
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

public class AdminShellController {

    @FXML
    private VBox sidebarRoot;
    @FXML
    private Label sidebarAvatar;
    @FXML
    private Label sidebarUserName;
    @FXML
    private Label sidebarRoleBadge;
    @FXML
    private Label pageTitle;
    @FXML
    private Label topbarDate;
    @FXML
    private Label topbarAvatar;
    @FXML
    private Label topbarUserName;
    @FXML
    private StackPane contentPane;

    @FXML
    private Button btnHome;
    @FXML
    private Button btnProfile;
    @FXML
    private Button btnUsers;
    @FXML
    private Button btnBlocked;
    @FXML
    private Button btnAudit;
    @FXML
    private Button btnSegment;
    @FXML
    private Button btnStats;
    @FXML
    private Button btnConsult;
    @FXML
    private Button btnRdv;
    @FXML
    private Button btnEtat;
    @FXML
    private Button btnActivite;
    @FXML
    private Button btnProducts;
    @FXML
    private Button btnAddProduct;
    @FXML
    private Button btnOrders;
    @FXML
    private Button btnDeliveries;

    private final UserService userService = new UserService();
    private com.chroniccare.modules.suivi.view.SuiviDashboardView suiviDashboardView;
    private javafx.scene.layout.VBox suiviContainer;

    // Référence statique pour que les sous-controllers puissent naviguer dans le
    // shell
    private static AdminShellController instance;

    public static AdminShellController getInstance() {
        return instance;
    }

    /** Vérifie que ce shell est bien la racine de la scène active. */
    public boolean isInActiveScene() {
        try {
            return sidebarRoot != null
                    && sidebarRoot.getScene() != null
                    && sidebarRoot.getScene().getRoot() != null
                    && sidebarRoot.getScene().getRoot().lookup("#sidebarRoot") != null;
        } catch (Exception e) {
            return false;
        }
    }

    @FXML
    public void initialize() {
        instance = this;
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null)
            return;

        String initials = getInitials(user);
        sidebarAvatar.setText(initials);
        topbarAvatar.setText(initials);
        sidebarUserName.setText(user.getPrenom() + " " + user.getNom());
        sidebarRoleBadge.setText("Administrateur");
        SidebarRoleBadgeHelper.applyRoleStyle(sidebarRoleBadge, user);
        topbarUserName.setText(user.getPrenom() + " " + user.getNom());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        topbarDate.setText(LocalDate.now().format(fmt));

        showHome();

        // Préparer le module suivi pour l'admin
        User adminUser = SessionManager.getInstance().getCurrentUser();
        if (adminUser != null) {
            suiviDashboardView = new com.chroniccare.modules.suivi.view.SuiviDashboardView(adminUser);
            suiviContainer = new javafx.scene.layout.VBox();
            suiviContainer.getChildren().setAll(suiviDashboardView.build());
        }

        // Consommer la page en attente (navigation depuis un sous-controller)
        String pending = SessionManager.getInstance().consumePendingAdminPage();
        if (pending != null) {
            switch (pending) {
                case "users" -> showUsers();
                case "blocked" -> showBlocked();
                case "audit" -> showAudit();
                case "segment" -> showSegmentation();
                case "stats" -> showStats();
                case "consult" -> showConsultations();
                case "rdv" -> showRdv();
                case "profile" -> showProfile();
            }
        }
    }

    @FXML
    public void showHome() {
        setActive(btnHome);
        pageTitle.setText("Tableau de bord");
        javafx.scene.layout.VBox dash = new javafx.scene.layout.VBox(20);
        dash.setStyle("-fx-padding: 24;");
        javafx.scene.control.Label title = new javafx.scene.control.Label("Tableau de bord Administrateur");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        javafx.scene.control.Label sub = new javafx.scene.control.Label(
                "Utilisez la barre de navigation pour gérer la plateforme.");
        sub.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        dash.getChildren().addAll(title, sub);
        contentPane.getChildren().setAll(dash);
    }

    @FXML
    public void showProfile() {
        setActive(btnProfile);
        pageTitle.setText("Gestion utilisateurs");
        loadContent("/com/chroniccare/list-users.fxml");
    }

    @FXML
    public void showUsers() {
        setActive(btnUsers);
        pageTitle.setText("Gestion des utilisateurs");
        loadContent("/com/chroniccare/list-users.fxml");
    }

    @FXML
    public void showBlocked() {
        setActive(btnBlocked);
        pageTitle.setText("Comptes bloques");
        loadContent("/com/chroniccare/blocked-accounts.fxml");
    }

    @FXML
    public void showAudit() {
        setActive(btnAudit);
        pageTitle.setText("Audit medical");
        loadContent("/com/chroniccare/medical-audit.fxml");
    }

    @FXML
    public void showSegmentation() {
        setActive(btnSegment);
        pageTitle.setText("Segmentation patients");
        loadContent("/com/chroniccare/patient-segmentation.fxml");
    }

    @FXML
    public void showStats() {
        setActive(btnStats);
        pageTitle.setText("Statistiques");
        loadContent("/com/chroniccare/stats.fxml");
    }

    @FXML
    public void showConsultations() {
        setActive(btnConsult);
        pageTitle.setText("Consultations patient");
        loadContent("/com/chroniccare/patient-consultations.fxml");
    }

    @FXML
    public void showProducts() {
        setActive(btnProducts);
        pageTitle.setText("Produits");
        loadContent("/com/chroniccare/Admin/ProduitsDashboard.fxml");
    }

    @FXML
    public void showAddProduct() {
        setActive(btnAddProduct);
        pageTitle.setText("Ajouter produit");
        loadContent("/com/chroniccare/Admin/AjouterProduit.fxml");
    }

    @FXML
    public void showOrders() {
        setActive(btnOrders);
        pageTitle.setText("Commandes");
        loadContent("/com/chroniccare/Admin/CommandesDashboard.fxml");
    }

    @FXML
    public void showDeliveries() {
        setActive(btnDeliveries);
        pageTitle.setText("Livraisons");
        loadContent("/com/chroniccare/Admin/LivraisonsDashboard.fxml");
    }

    @FXML
    public void showRdv() {
        setActive(btnRdv);
        pageTitle.setText("RDV nutrition");
        loadContent("/com/chroniccare/nutrition-rdv.fxml");
    }

    @FXML
    public void showEtat() {
        setActive(btnEtat);
        pageTitle.setText("Etat patients");
        if (suiviContainer != null) {
            contentPane.getChildren().setAll(suiviContainer);
            if (suiviDashboardView != null)
                suiviDashboardView.showEtatTab();
        }
    }

    @FXML
    public void showActivite() {
        setActive(btnActivite);
        pageTitle.setText("Activite patients");
        if (suiviContainer != null) {
            contentPane.getChildren().setAll(suiviContainer);
            if (suiviDashboardView != null)
                suiviDashboardView.showActiviteTab();
        }
    }

    @FXML
    public void handleLogout() {
        try {
            SessionManager.getInstance().logout();
            Parent root = FXMLLoader.load(getClass().getResource("/com/chroniccare/login.fxml"));
            sidebarRoot.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Charge un FXML dans le contentPane. */
    public void loadContent(String fxmlPath) {
        try {
            javafx.scene.Node content = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActive(Button btn) {
        Button[] all = { btnHome, btnProfile, btnUsers, btnBlocked, btnAudit,
                btnSegment, btnStats, btnConsult, btnRdv, btnEtat, btnActivite,
                btnProducts, btnAddProduct, btnOrders, btnDeliveries };
        for (Button b : all)
            if (b != null)
                b.getStyleClass().remove("nav-btn-active");
        if (btn != null)
            btn.getStyleClass().add("nav-btn-active");
    }

    private String getInitials(User user) {
        String p = (user.getPrenom() != null && !user.getPrenom().isEmpty())
                ? String.valueOf(user.getPrenom().charAt(0)).toUpperCase()
                : "";
        String n = (user.getNom() != null && !user.getNom().isEmpty())
                ? String.valueOf(user.getNom().charAt(0)).toUpperCase()
                : "";
        return p + n;
    }
}
