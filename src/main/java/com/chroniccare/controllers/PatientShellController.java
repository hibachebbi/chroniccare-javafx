package com.chroniccare.controllers;

import com.chroniccare.models.User;
import com.chroniccare.modules.suivi.view.SuiviDashboardView;
import com.chroniccare.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PatientShellController {

    @FXML
    private VBox sidebarRoot;
    @FXML
    private Label sidebarAvatar;
    @FXML
    private ImageView sidebarAvatarImage;
    @FXML
    private Label sidebarUserName;
    @FXML
    private Label pageTitle;
    @FXML
    private Label topbarDate;
    @FXML
    private Label topbarAvatar;
    @FXML
    private ImageView topbarAvatarImage;
    @FXML
    private Label topbarUserName;
    @FXML
    private StackPane contentPane;

    @FXML
    private Button btnHome;
    @FXML
    private Button btnProfile;
    @FXML
    private Button btnEtat;
    @FXML
    private Button btnActivite;
    @FXML
    private Button btnEvents;
    @FXML
    private Button btnRegistrations;
    @FXML
    private Button btnRdv;
    @FXML
    private Button btnConsultations;

    // Buttons ajoutés dynamiquement pour la navigation boutique
    private Button btnProduits;
    private Button btnPanier;
    private Button btnCommandes;
    private Button btnLivraisons;
    private Button btnWishlist;
    private Button btnCheckout;

    private SuiviDashboardView suiviDashboardView;
    private VBox suiviContainer;

    private static PatientShellController instance;

    public static PatientShellController getInstance() {
        return instance;
    }

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
        if (user == null) {
            return;
        }

        String initials = getInitials(user);
        sidebarAvatar.setText(initials);
        topbarAvatar.setText(initials);
        loadUserPhoto(user);
        sidebarUserName.setText(user.getPrenom() + " " + user.getNom());
        topbarUserName.setText(user.getPrenom() + " " + user.getNom());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        topbarDate.setText(LocalDate.now().format(fmt));

        suiviDashboardView = new SuiviDashboardView(user);
        suiviContainer = new VBox();
        suiviContainer.getChildren().setAll(suiviDashboardView.build());
        Platform.runLater(() -> {
            try {
                suiviDashboardView.showLoginMessageNotification();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        addShopButtonsToSidebar();
        showHome();

        String pending = SessionManager.getInstance().consumePendingPatientPage();
        if (pending != null) {
            switch (pending) {
                case "profile" -> showProfile();
                case "etat" -> showEtat();
                case "activite" -> showActivite();
                case "events" -> showEvents();
                case "registrations" -> showRegistrations();
                case "rdv" -> showRdv();
                case "consultations" -> showConsultations();
                case "produits" -> showProduits();
                case "produit" -> showProduitDetail();
                case "panier" -> showPanier();
                case "commandes" -> showCommandes();
                case "livraisons" -> showLivraisons();
                case "wishlist" -> showWishlist();
                case "checkout" -> showCheckout();
            }
        }
    }

    private void addShopButtonsToSidebar() {
        if (sidebarRoot == null) {
            return;
        }
        if (sidebarRoot.lookup("#btnProduits") != null) {
            return;
        }

        VBox shopSection = new VBox(2);
        shopSection.getStyleClass().add("nav-section");

        Label shopLabel = new Label("BOUTIQUE");
        shopLabel.getStyleClass().add("nav-section-label");

        btnProduits = createShopButton("btnProduits", "Produits", this::showProduits);
        btnPanier = createShopButton("btnPanier", "Panier", this::showPanier);
        btnCommandes = createShopButton("btnCommandes", "Commandes", this::showCommandes);
        btnLivraisons = createShopButton("btnLivraisons", "Livraisons", this::showLivraisons);
        btnWishlist = createShopButton("btnWishlist", "Wishlist", this::showWishlist);
        btnCheckout = createShopButton("btnCheckout", "Checkout", this::showCheckout);

        shopSection.getChildren().addAll(shopLabel, btnProduits, btnPanier, btnCommandes, btnLivraisons, btnWishlist,
                btnCheckout);

        int insertIndex = sidebarRoot.getChildren().size();
        for (int i = 0; i < sidebarRoot.getChildren().size(); i++) {
            if (sidebarRoot.getChildren().get(i).getStyleClass().contains("sidebar-bottom")) {
                insertIndex = i;
                break;
            }
        }

        sidebarRoot.getChildren().add(insertIndex, shopSection);
    }

    private Button createShopButton(String id, String text, Runnable action) {
        Button button = new Button(text);
        button.setId(id);
        button.getStyleClass().add("nav-btn");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> action.run());
        return button;
    }

    // ----- Navigation boutique (client) -----
    public void showProduits() {
        pageTitle.setText("Produits");
        loadContent("/com/chroniccare/Client/ProduitsDashboard.fxml");
    }

    public void showProduitDetail() {
        pageTitle.setText("Détails produit");
        loadContent("/com/chroniccare/Client/ProduitDetail.fxml");
    }

    public void showPanier() {
        pageTitle.setText("Mon panier");
        loadContent("/com/chroniccare/Client/Panier.fxml");
    }

    public void showCommandes() {
        pageTitle.setText("Mes commandes");
        loadContent("/com/chroniccare/Client/CommandesDashboard.fxml");
    }

    public void showLivraisons() {
        pageTitle.setText("Mes livraisons");
        loadContent("/com/chroniccare/Client/LivraisonsDashboard.fxml");
    }

    public void showWishlist() {
        pageTitle.setText("Ma wishlist");
        loadContent("/com/chroniccare/Client/Wishlist.fxml");
    }

    public void showCheckout() {
        pageTitle.setText("Checkout");
        loadContent("/com/chroniccare/Client/Checkout.fxml");
    }

    @FXML
    public void showHome() {
        setActive(btnHome);
        pageTitle.setText("Tableau de bord");

        User user = SessionManager.getInstance().getCurrentUser();
        String prenom = user != null && user.getPrenom() != null ? user.getPrenom() : "";
        String condition = user != null && user.getMedicalCondition() != null && !user.getMedicalCondition().isBlank()
                ? user.getMedicalCondition()
                : "Non renseignee";
        String genre = user != null && user.getGenre() != null && !user.getGenre().isBlank()
                ? user.getGenre()
                : "Non renseigne";
        String telephone = user != null && user.getTelephone() != null && !user.getTelephone().isBlank()
                ? user.getTelephone()
                : "Non renseigne";

        VBox dash = new VBox(22);
        dash.getStyleClass().add("dashboard-body");

        HBox welcomeBanner = new HBox(18);
        welcomeBanner.getStyleClass().add("welcome-banner");

        VBox welcomeText = new VBox(8);
        Label title = new Label("Bonjour, " + prenom + " !");
        title.getStyleClass().add("welcome-title");
        Label subtitle = new Label("Suivez votre sante au quotidien et consultez vos donnees medicales.");
        subtitle.getStyleClass().add("welcome-subtitle");
        subtitle.setWrapText(true);
        welcomeText.getChildren().addAll(title, subtitle);
        HBox.setHgrow(welcomeText, Priority.ALWAYS);

        Label bannerIcon = new Label("\u2695");
        bannerIcon.getStyleClass().add("banner-emoji");
        welcomeBanner.getChildren().addAll(welcomeText, bannerIcon);

        HBox stats = new HBox(16,
                createStatCard("Prochain RDV", "-", "Aucun rendez-vous planifie", "stat-blue"),
                createStatCard("Medicaments du jour", "0", "Aucun rappel actif", "stat-green"),
                createStatCard("Score d'activite", "0", "Points cette semaine", "stat-purple"));

        VBox healthCard = new VBox(12);
        healthCard.getStyleClass().add("card");
        HBox.setHgrow(healthCard, Priority.ALWAYS);
        Label healthTitle = new Label("Mon etat de sante");
        healthTitle.getStyleClass().add("card-title");
        healthCard.getChildren().addAll(
                healthTitle,
                new Separator(),
                createInfoRow("Condition medicale", condition),
                createInfoRow("Genre", genre),
                createInfoRow("Telephone", telephone));

        Button profileButton = new Button("Modifier mon profil");
        profileButton.getStyleClass().add("btn-primary");
        profileButton.setOnAction(event -> showProfile());
        healthCard.getChildren().add(profileButton);

        VBox tipsCard = new VBox(14);
        tipsCard.getStyleClass().addAll("card", "card-tips");
        tipsCard.setPrefWidth(300);
        Label tipsTitle = new Label("Conseils du jour");
        tipsTitle.getStyleClass().add("card-title");
        tipsCard.getChildren().addAll(
                tipsTitle,
                new Separator(),
                createTipBox("Hydratation", "Buvez au moins 1,5L d'eau par jour pour maintenir un bon equilibre."),
                createTipBox("Activite",
                        "30 minutes de marche quotidienne aident a mieux gerer les maladies chroniques."),
                createTipBox("Sommeil", "Un sommeil regulier de 7 a 9 heures soutient votre recuperation."));

        HBox bottom = new HBox(16, healthCard, tipsCard);

        dash.getChildren().addAll(welcomeBanner, stats, bottom);
        contentPane.getChildren().setAll(dash);
    }

    @FXML
    public void showProfile() {
        setActive(btnProfile);
        pageTitle.setText("Mon Profil");
        loadContent("/com/chroniccare/profile-patient.fxml");
    }

    @FXML
    public void showEtat() {
        setActive(btnEtat);
        pageTitle.setText("Mon Etat");
        contentPane.getChildren().setAll(suiviContainer);
        if (suiviDashboardView != null)
            suiviDashboardView.showEtatTab();
    }

    @FXML
    public void showActivite() {
        setActive(btnActivite);
        pageTitle.setText("Mon Activite");
        contentPane.getChildren().setAll(suiviContainer);
        if (suiviDashboardView != null)
            suiviDashboardView.showActiviteTab();
    }

    @FXML
    public void showEvents() {
        setActive(btnEvents);
        pageTitle.setText("Evenements");
        loadContent("/com/chroniccare/patient-events.fxml");
    }

    @FXML
    public void showRegistrations() {
        setActive(btnRegistrations);
        pageTitle.setText("Mes inscriptions");
        loadContent("/com/chroniccare/patient-registrations.fxml");
    }

    @FXML
    public void showRdv() {
        setActive(btnRdv);
        pageTitle.setText("Prendre RDV");
        loadContent("/com/chroniccare/patient-rdv.fxml");
    }

    @FXML
    public void showConsultations() {
        setActive(btnConsultations);
        pageTitle.setText("Mes consultations");
        loadContent("/com/chroniccare/patient-consultations.fxml");
    }
    @FXML
    public void goToForum() {
        pageTitle.setText("Forum");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chroniccare/forum-front.fxml"));
            BorderPane forumPane = loader.load();
            javafx.scene.Node centerContent = forumPane.getCenter();
            if (centerContent != null) {
                contentPane.getChildren().setAll(centerContent);
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    public void loadContent(String fxmlPath) {
        try {
            javafx.scene.Node content = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(content);
        } catch (Exception e) {
            e.printStackTrace();
            VBox errorBox = new VBox(12);
            errorBox.setStyle("-fx-padding: 24;");
            Label title = new Label("Erreur d'affichage");
            title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            Label message = new Label(
                    "Impossible de charger la page demandee : " + fxmlPath + "\n"
                            + e.getClass().getSimpleName() + " : " + e.getMessage());
            message.setWrapText(true);
            message.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
            errorBox.getChildren().addAll(title, message);
            contentPane.getChildren().setAll(errorBox);
        }
    }

    public void setContent(javafx.scene.Node content) {
        if (contentPane != null && content != null) {
            contentPane.getChildren().setAll(content);
        }
    }

    public void setPageTitle(String title) {
        if (pageTitle != null)
            pageTitle.setText(title);
    }

    private void setActive(Button btn) {
        Button[] all = { btnHome, btnProfile, btnEtat, btnActivite, btnEvents, btnRegistrations, btnRdv,
                btnConsultations,
                btnProduits, btnPanier, btnCommandes, btnLivraisons, btnWishlist, btnCheckout };
        for (Button button : all) {
            if (button != null)
                button.getStyleClass().remove("nav-btn-active");
        }
        if (btn != null)
            btn.getStyleClass().add("nav-btn-active");
    }

    private void loadUserPhoto(User user) {
        try {
            if (user.getPhotoProfil() == null || user.getPhotoProfil().isBlank())
                return;
            File file = new File(user.getPhotoProfil());
            if (!file.exists())
                return;
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
        } catch (Exception ignored) {
        }
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

    private VBox createStatCard(String labelText, String valueText, String subText, String accentClass) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("stat-card", accentClass);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label label = new Label(labelText);
        label.getStyleClass().add("stat-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("stat-value");
        Label sub = new Label(subText);
        sub.getStyleClass().add("stat-sub");
        sub.setWrapText(true);

        card.getChildren().addAll(label, value, sub);
        return card;
    }

    private VBox createInfoRow(String labelText, String valueText) {
        VBox row = new VBox(4);
        Label label = new Label(labelText);
        label.getStyleClass().add("info-label");
        Label value = new Label(valueText);
        value.setWrapText(true);
        value.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 18px; -fx-font-weight: bold;");
        row.getChildren().addAll(label, value);
        return row;
    }

    private VBox createTipBox(String titleText, String bodyText) {
        VBox tip = new VBox(6);
        tip.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 14;");

        Label title = new Label(titleText);
        title.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label body = new Label(bodyText);
        body.setWrapText(true);
        body.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        tip.getChildren().addAll(title, body);
        return tip;
    }
}
