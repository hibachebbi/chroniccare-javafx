package com.chroniccare.controllers;

import com.chroniccare.models.Notification;
import com.chroniccare.models.Publication;
import com.chroniccare.services.CommentaireService;
import com.chroniccare.services.NotificationService;
import com.chroniccare.services.PublicationService;
import com.chroniccare.utils.SessionManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

public class ForumFrontController implements Initializable {

    @FXML private VBox         feedPublications;
    @FXML private Label        sidebarAvatar;
    @FXML private Label        sidebarUserName;
    @FXML private Label        sidebarRoleBadge;
    @FXML private Label        statPublications;
    @FXML private Label        statCommentaires;
    @FXML private Label        statLikes;
    @FXML private FlowPane     flowCategories;
    @FXML private TextField    fieldRecherche;
    @FXML private Button       btnAjouter;
    @FXML private ComboBox<String> filtreStatut;
    @FXML private ComboBox<String> triPublications;

    // 🔔 Notification bell
    @FXML private StackPane    notifBellContainer;
    @FXML private Button       btnNotification;
    @FXML private Label        badgeNotif;

    private final PublicationService   pubService   = new PublicationService();
    private final CommentaireService   comService   = new CommentaireService();
    private final NotificationService  notifService = new NotificationService();
    private List<Publication> toutesPublications;

    // Popup dropdown notifications
    private Popup notifPopup;
    private boolean popupOuvert = false;
    private Timeline notifPollingTimeline;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerSidebar();
        appliquerPermissions();
        chargerPublications();
        chargerCategories();
        rafraichirBadgeNotif();

        fieldRecherche.textProperty().addListener((obs, old, val) -> filtrerRecherche(val));

        filtreStatut.getItems().addAll("Tous", "publie", "brouillon", "archive");
        filtreStatut.setValue("Tous");

        triPublications.getItems().addAll(
                "Plus récents", "Plus anciens", "Plus likés", "Plus vus"
        );
        triPublications.setValue("Plus récents");
        notifPollingTimeline = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> {
                    chargerPublications();
                    rafraichirBadgeNotif();
                })
        );
        notifPollingTimeline.setCycleCount(Timeline.INDEFINITE);
        notifPollingTimeline.play();
    }

    // ─────────────────────────────────────────
    // 🔔 NOTIFICATIONS
    // ─────────────────────────────────────────

    private void rafraichirBadgeNotif() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        int unread = notifService.countUnread(user.getId());

        if (unread > 0) {
            badgeNotif.setText(unread > 99 ? "99+" : String.valueOf(unread));
            badgeNotif.setVisible(true);
            badgeNotif.setManaged(true);
        } else {
            badgeNotif.setVisible(false);
            badgeNotif.setManaged(false);
        }
    }

    @FXML
    private void toggleNotifications() {
        if (popupOuvert && notifPopup != null) {
            notifPopup.hide();
            popupOuvert = false;
            return;
        }
        ouvrirDropdownNotifs();
    }

    private void ouvrirDropdownNotifs() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        List<Notification> notifs = notifService.getByRecipient(user.getId());

        // Marquer tout comme lu
        notifService.markAllAsRead(user.getId());
        rafraichirBadgeNotif();

        // Construire le popup
        notifPopup = new Popup();
        notifPopup.setAutoHide(true);
        notifPopup.setConsumeAutoHidingEvents(false);

        VBox panel = new VBox(0);
        panel.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:14px;" +
                        "-fx-border-color:#e5e7eb;" +
                        "-fx-border-width:1;" +
                        "-fx-border-radius:14px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),20,0,0,6);" +
                        "-fx-min-width:340px;" +
                        "-fx-max-width:340px;"
        );

        // Header du dropdown
        HBox header = new HBox();
        header.setStyle("-fx-padding:14 16 12 16; -fx-alignment:CENTER_LEFT; -fx-spacing:8;");

        Label titre = new Label("🔔  Notifications");
        titre.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#111827;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        int total = notifs.size();
        Label count = new Label(total + " au total");
        count.setStyle("-fx-font-size:11px; -fx-text-fill:#9ca3af;");

        header.getChildren().addAll(titre, spacer, count);
        panel.getChildren().add(header);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#f3f4f6;");
        panel.getChildren().add(sep);

        // Liste des notifs
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;" +
                "-fx-border-width:0; -fx-padding:0;");
        scroll.setPrefHeight(Math.min(notifs.size() * 72.0 + 10, 360));

        VBox liste = new VBox(0);
        liste.setStyle("-fx-padding:6 0;");

        if (notifs.isEmpty()) {
            Label vide = new Label("Aucune notification pour le moment.");
            vide.setStyle("-fx-text-fill:#9ca3af; -fx-font-size:13px;" +
                    "-fx-padding:24 16; -fx-alignment:CENTER;");
            vide.setMaxWidth(Double.MAX_VALUE);
            liste.getChildren().add(vide);
        } else {
            for (Notification n : notifs) {
                liste.getChildren().add(creerItemNotif(n));
            }
        }

        scroll.setContent(liste);
        panel.getChildren().add(scroll);

        // Footer — bouton "Tout marquer comme lu"
        if (!notifs.isEmpty()) {
            Separator sep2 = new Separator();
            sep2.setStyle("-fx-background-color:#f3f4f6;");

            Button btnToutLu = new Button("✓  Tout marquer comme lu");
            btnToutLu.setMaxWidth(Double.MAX_VALUE);
            btnToutLu.setStyle(
                    "-fx-background-color:transparent;" +
                            "-fx-text-fill:#2563eb;" +
                            "-fx-font-size:12px;" +
                            "-fx-font-weight:bold;" +
                            "-fx-padding:10 16;" +
                            "-fx-border-width:0;" +
                            "-fx-cursor:hand;"
            );
            btnToutLu.setOnAction(e -> {
                notifService.markAllAsRead(user.getId());
                rafraichirBadgeNotif();
                notifPopup.hide();
                popupOuvert = false;
            });

            panel.getChildren().addAll(sep2, btnToutLu);
        }

        notifPopup.getContent().add(panel);

        // Positionner sous le bouton cloche
        Bounds bounds = btnNotification.localToScreen(btnNotification.getBoundsInLocal());
        double x = bounds.getMaxX() - 340;
        double y = bounds.getMaxY() + 6;

        notifPopup.show(btnNotification.getScene().getWindow(), x, y);
        popupOuvert = true;

        notifPopup.setOnHidden(e -> popupOuvert = false);
    }

    private VBox creerItemNotif(Notification n) {
        VBox item = new VBox(3);
        item.setStyle(
                "-fx-padding:10 16;" +
                        "-fx-cursor:hand;" +
                        (n.isRead() ? "-fx-background-color:white;" : "-fx-background-color:#eff6ff;")
        );

        // Icône selon type
        String emoji = n.getType() != null && n.getType().contains("LIKE") ? "❤️" : "💬";

        HBox row = new HBox(10);
        row.setStyle("-fx-alignment:CENTER_LEFT;");

        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size:18px; -fx-min-width:28px;");

        VBox textBlock = new VBox(2);

        Label msg = new Label(n.getMessage());
        msg.setWrapText(true);
        msg.setMaxWidth(260);
        msg.setStyle("-fx-font-size:12px; -fx-text-fill:#374151; -fx-wrap-text:true;");

        String dateStr = n.getCreatedAt() != null
                ? n.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM · HH:mm"))
                : "";
        Label date = new Label(dateStr);
        date.setStyle("-fx-font-size:10px; -fx-text-fill:#9ca3af;");

        textBlock.getChildren().addAll(msg, date);
        row.getChildren().addAll(icon, textBlock);

        // Point bleu si non lu
        if (!n.isRead()) {
            Region dot = new Region();
            dot.setStyle(
                    "-fx-background-color:#2563eb;" +
                            "-fx-background-radius:50%;" +
                            "-fx-min-width:8px; -fx-min-height:8px;" +
                            "-fx-max-width:8px; -fx-max-height:8px;"
            );
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(spacer, dot);
        }

        item.getChildren().add(row);

        // Hover effect
        item.setOnMouseEntered(e -> item.setStyle(
                "-fx-padding:10 16; -fx-cursor:hand; -fx-background-color:#f8fafc;"
        ));
        item.setOnMouseExited(e -> item.setStyle(
                "-fx-padding:10 16; -fx-cursor:hand;" +
                        (n.isRead() ? "-fx-background-color:white;" : "-fx-background-color:#eff6ff;")
        ));

        return item;
    }

    // ─────────────────────────────────────────
    // Helpers icônes
    // ─────────────────────────────────────────
    private FontAwesomeIconView buildIcon(FontAwesomeIcon icon, String color, String sizePx) {
        FontAwesomeIconView view = new FontAwesomeIconView(icon);
        view.setStyle("-fx-fill:" + color + ";");
        view.setSize(sizePx);
        return view;
    }

    private Button buildIconButton(String text, FontAwesomeIcon icon, String iconColor, String style) {
        Button button = new Button(text, buildIcon(icon, iconColor, "14px"));
        button.setStyle(style);
        return button;
    }

    // ─────────────────────────────────────────
    // Couleurs catégories / statuts
    // ─────────────────────────────────────────
    private String[] getCategorieColors(String categorie) {
        if (categorie == null || categorie.isBlank()) {
            return new String[]{"#e5e7eb", "#374151"};
        }
        return switch (categorie.toLowerCase()) {
            case "nutrition"                         -> new String[]{"#dcfce7", "#15803d"};
            case "suivi"                             -> new String[]{"#dbeafe", "#1d4ed8"};
            case "médicaments", "medicaments"        -> new String[]{"#fee2e2", "#dc2626"};
            case "bien-être", "bien etre", "bienetre"-> new String[]{"#f3e8ff", "#9333ea"};
            case "coaching"                          -> new String[]{"#fef3c7", "#b45309"};
            case "santé", "sante"                    -> new String[]{"#cffafe", "#0f766e"};
            case "forum"                             -> new String[]{"#ede9fe", "#7c3aed"};
            default                                  -> new String[]{"#ede9fe", "#7c3aed"};
        };
    }

    private String[] getStatutColors(String statut) {
        if (statut == null || statut.isBlank()) {
            return new String[]{"#e5e7eb", "#374151"};
        }
        return switch (statut.toLowerCase()) {
            case "publie"    -> new String[]{"#dcfce7", "#16a34a"};
            case "brouillon" -> new String[]{"#fef3c7", "#b45309"};
            case "archive"   -> new String[]{"#e5e7eb", "#6b7280"};
            default          -> new String[]{"#e0e7ff", "#4338ca"};
        };
    }

    // ─────────────────────────────────────────
    // Sidebar
    // ─────────────────────────────────────────
    private void configurerSidebar() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        sidebarAvatar.setText(String.valueOf(user.getNom().charAt(0)).toUpperCase());
        sidebarUserName.setText(user.getPrenom() + " " + user.getNom());

        String role = user.getRoles();
        if (role.contains("ROLE_COACH")) {
            sidebarRoleBadge.setText("Coach");
        } else if (role.contains("ROLE_NUTRITIONNISTE")) {
            sidebarRoleBadge.setText("Nutritionniste");
        } else if (role.contains("ROLE_PATIENT")) {
            sidebarRoleBadge.setText("Patient");
        } else {
            sidebarRoleBadge.setText("Utilisateur");
        }
    }

    private void appliquerPermissions() {
        if (SessionManager.getInstance().isPatient()) {
            btnAjouter.setVisible(false);
            btnAjouter.setManaged(false);
        }
    }

    // ─────────────────────────────────────────
    // Chargement publications
    // ─────────────────────────────────────────
    private void chargerPublications() {
        toutesPublications = pubService.getAll();
        afficherPublications(toutesPublications);

        statPublications.setText(String.valueOf(toutesPublications.size()));

        int totalLikes = toutesPublications.stream()
                .mapToInt(Publication::getNbLikes).sum();
        statLikes.setText(String.valueOf(totalLikes));

        int totalComm = toutesPublications.stream()
                .mapToInt(p -> comService.getByPublication(p.getId()).size()).sum();
        statCommentaires.setText(String.valueOf(totalComm));
    }

    private void afficherPublications(List<Publication> liste) {
        feedPublications.getChildren().clear();

        if (liste == null || liste.isEmpty()) {
            Label vide = new Label("Aucune publication pour le moment.");
            vide.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:14px; -fx-padding:40;");
            feedPublications.getChildren().add(vide);
            return;
        }

        for (Publication p : liste) {
            feedPublications.getChildren().add(creerCard(p));
        }
    }

    // ─────────────────────────────────────────
    // Card publication
    // ─────────────────────────────────────────
    private VBox creerCard(Publication p) {
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:16px;" +
                        "-fx-border-color:#e8edf2;" +
                        "-fx-border-width:1;" +
                        "-fx-border-radius:16px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);"
        );

        // Header
        HBox header = new HBox(12);
        header.setStyle("-fx-padding:16 20 12 20; -fx-alignment:CENTER_LEFT;");

        String nomAuteur = p.getAuteurId() != null
                ? pubService.getNomAuteur(p.getAuteurId()) : "Anonyme";
        String initiale = nomAuteur.isEmpty()
                ? "?" : String.valueOf(nomAuteur.charAt(0)).toUpperCase();

        Label avatar = new Label(initiale);
        String[] avatarColors = {
                "#dbeafe:#1d4ed8","#dcfce7:#16a34a",
                "#fce7f3:#be185d","#fef3c7:#b45309","#ede9fe:#7c3aed"
        };
        String[] colors = avatarColors[Math.abs(nomAuteur.hashCode()) % avatarColors.length].split(":");
        avatar.setStyle(
                "-fx-background-color:" + colors[0] + ";" +
                        "-fx-text-fill:" + colors[1] + ";" +
                        "-fx-font-weight:bold; -fx-font-size:14px;" +
                        "-fx-min-width:44px; -fx-min-height:44px;" +
                        "-fx-max-width:44px; -fx-max-height:44px;" +
                        "-fx-background-radius:50%; -fx-alignment:CENTER;"
        );

        VBox authorInfo = new VBox(3);
        Label authorName = new Label(nomAuteur);
        authorName.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#111827;");

        String dateStr = p.getCreatedAt() != null
                ? p.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm")) : "";
        Label meta = new Label(dateStr);
        meta.setStyle("-fx-font-size:11px; -fx-text-fill:#9ca3af;");
        authorInfo.getChildren().addAll(authorName, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox badges = new VBox(4);
        badges.setStyle("-fx-alignment:TOP_RIGHT;");

        if (p.getCategorie() != null && !p.getCategorie().isEmpty()) {
            String[] catColors = getCategorieColors(p.getCategorie());
            Label badgeCat = new Label(p.getCategorie());
            badgeCat.setStyle(
                    "-fx-background-color:" + catColors[0] + ";" +
                            "-fx-text-fill:" + catColors[1] + ";" +
                            "-fx-font-size:10px; -fx-font-weight:bold;" +
                            "-fx-background-radius:10px; -fx-padding:3 10;"
            );
            badges.getChildren().add(badgeCat);
        }

        if (p.getStatut() != null && !p.getStatut().isEmpty()) {
            String[] statutColors = getStatutColors(p.getStatut());
            Label badgeStatut = new Label(p.getStatut());
            badgeStatut.setStyle(
                    "-fx-background-color:" + statutColors[0] + ";" +
                            "-fx-text-fill:" + statutColors[1] + ";" +
                            "-fx-font-size:10px; -fx-font-weight:bold;" +
                            "-fx-background-radius:10px; -fx-padding:3 10;"
            );
            badges.getChildren().add(badgeStatut);
        }

        header.getChildren().addAll(avatar, authorInfo, spacer, badges);
        card.getChildren().add(header);

        // Image
        if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
            try {
                Image img = new Image("file:" + p.getImagePath(), true);
                ImageView imgView = new ImageView(img);
                imgView.setFitWidth(800);
                imgView.setFitHeight(220);
                imgView.setPreserveRatio(false);
                card.getChildren().add(imgView);
            } catch (Exception ignored) {}
        }

        // Body
        VBox body = new VBox(8);
        body.setStyle("-fx-padding:14 20 10 20;");

        Label titre = new Label(p.getTitre());
        titre.setStyle(
                "-fx-font-size:17px; -fx-font-weight:bold;" +
                        "-fx-text-fill:#111827; -fx-wrap-text:true;"
        );
        titre.setWrapText(true);

        String contenuTexte = p.getContenu() == null ? "" : p.getContenu();
        Label contenu = new Label(
                contenuTexte.length() > 180
                        ? contenuTexte.substring(0, 180) + "..." : contenuTexte
        );
        contenu.setStyle("-fx-font-size:13px; -fx-text-fill:#6b7280; -fx-line-spacing:4;");
        contenu.setWrapText(true);
        body.getChildren().addAll(titre, contenu);
        card.getChildren().add(body);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#f3f4f6;");
        card.getChildren().add(sep);

        // Footer
        HBox footer = new HBox(4);
        footer.setStyle("-fx-padding:10 16; -fx-alignment:CENTER_LEFT;");

        String btnBase = "-fx-background-color:transparent; -fx-border-width:0;" +
                "-fx-cursor:hand; -fx-font-size:12px; -fx-padding:6 12;" +
                "-fx-background-radius:8px;";

        Button btnCom = buildIconButton(
                " Commenter", FontAwesomeIcon.COMMENT, "#9ca3af",
                btnBase + "-fx-text-fill:#9ca3af;"
        );
        Button btnVoir = buildIconButton(
                " Voir plus", FontAwesomeIcon.EYE, "#2563eb",
                btnBase + "-fx-text-fill:#2563eb; -fx-font-weight:bold;"
        );

        var currentUser3 = SessionManager.getInstance().getCurrentUser();
        int userId = currentUser3 != null ? currentUser3.getId() : -1;
        boolean dejaLike = userId > 0 && pubService.hasLiked(p.getId(), userId);

        String likeStyle = dejaLike
                ? btnBase + "-fx-text-fill:#e11d48; -fx-background-color:#fce7f3;"
                : btnBase + "-fx-text-fill:#9ca3af;";

        Button btnLike = buildIconButton(
                " " + p.getNbLikes(),
                FontAwesomeIcon.HEART,
                dejaLike ? "#e11d48" : "#9ca3af",
                likeStyle
        );

        btnCom.setOnMouseEntered(e -> {
            btnCom.setStyle(btnBase + "-fx-background-color:#f0f9ff; -fx-text-fill:#0284c7;");
            if (btnCom.getGraphic() instanceof FontAwesomeIconView icon) icon.setStyle("-fx-fill:#0284c7;");
        });
        btnCom.setOnMouseExited(e -> {
            btnCom.setStyle(btnBase + "-fx-text-fill:#9ca3af;");
            if (btnCom.getGraphic() instanceof FontAwesomeIconView icon) icon.setStyle("-fx-fill:#9ca3af;");
        });

        btnLike.setOnAction(e -> {
            int uid = SessionManager.getInstance().getCurrentUser() != null
                    ? SessionManager.getInstance().getCurrentUser().getId() : -1;
            if (uid < 0) return;
            if (pubService.hasLiked(p.getId(), uid)) {
                pubService.unliker(p.getId(), uid);
            } else {
                pubService.liker(p.getId(), uid);
            }
            chargerPublications();
            chargerCategories();
            rafraichirBadgeNotif(); // 🔔 rafraîchir badge après like
        });

        btnVoir.setOnAction(e -> ouvrirDetail(p));
        btnCom.setOnAction(e -> ouvrirDetail(p));

        var session     = SessionManager.getInstance();
        var currentUser = session.getCurrentUser();

        boolean peutModifier = session.isAdmin()
                || (currentUser != null && p.getAuteurId() != null
                && p.getAuteurId().equals(currentUser.getId()));

        footer.getChildren().addAll(btnLike, btnCom, btnVoir);

        if (peutModifier) {
            Region spacer2 = new Region();
            HBox.setHgrow(spacer2, Priority.ALWAYS);

            Button btnEdit = buildIconButton(
                    " Modifier", FontAwesomeIcon.PENCIL, "#b45309",
                    btnBase + "-fx-text-fill:#b45309; -fx-background-color:#fef9c3;"
            );
            Button btnSupp = buildIconButton(
                    " Supprimer", FontAwesomeIcon.TRASH, "#dc2626",
                    btnBase + "-fx-text-fill:#dc2626; -fx-background-color:#fee2e2;"
            );

            btnEdit.setOnAction(e -> ouvrirFormulaire(p));
            btnSupp.setOnAction(e -> {
                Alert alert = new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "Supprimer \"" + p.getTitre() + "\" ?",
                        ButtonType.YES, ButtonType.NO
                );
                alert.setHeaderText(null);
                alert.showAndWait().ifPresent(r -> {
                    if (r == ButtonType.YES) {
                        pubService.supprimer(p.getId());
                        chargerPublications();
                        chargerCategories();
                    }
                });
            });

            footer.getChildren().addAll(spacer2, btnEdit, btnSupp);
        }

        card.getChildren().add(footer);
        return card;
    }

    // ─────────────────────────────────────────
    // Catégories sidebar droite
    // ─────────────────────────────────────────
    private void chargerCategories() {
        flowCategories.getChildren().clear();

        Button btnToutes = creerBtnCategorie("Toutes", true);
        btnToutes.setOnAction(e -> { afficherPublications(toutesPublications); chargerCategories(); });
        flowCategories.getChildren().add(btnToutes);

        toutesPublications.stream()
                .map(Publication::getCategorie)
                .filter(c -> c != null && !c.isEmpty())
                .distinct().sorted()
                .forEach(cat -> {
                    Button btn = creerBtnCategorie(cat, false);
                    btn.setOnAction(e -> afficherCategorieSelectionnee(cat));
                    flowCategories.getChildren().add(btn);
                });
    }

    private void afficherCategorieSelectionnee(String categorieSelectionnee) {
        List<Publication> filtre = toutesPublications.stream()
                .filter(p -> p.getCategorie() != null
                        && p.getCategorie().equalsIgnoreCase(categorieSelectionnee))
                .toList();
        afficherPublications(filtre);
        flowCategories.getChildren().clear();

        Button btnToutes = creerBtnCategorie("Toutes", false);
        btnToutes.setOnAction(e -> { afficherPublications(toutesPublications); chargerCategories(); });
        flowCategories.getChildren().add(btnToutes);

        toutesPublications.stream()
                .map(Publication::getCategorie)
                .filter(c -> c != null && !c.isEmpty())
                .distinct().sorted()
                .forEach(cat -> {
                    boolean active = cat.equalsIgnoreCase(categorieSelectionnee);
                    Button btn = creerBtnCategorie(cat, active);
                    btn.setOnAction(e -> afficherCategorieSelectionnee(cat));
                    flowCategories.getChildren().add(btn);
                });
    }

    private Button creerBtnCategorie(String text, boolean active) {
        Button btn = new Button(text);
        String bg, fg;
        if ("Toutes".equalsIgnoreCase(text)) {
            bg = active ? "#dbeafe" : "#f1f5f9";
            fg = active ? "#1d4ed8" : "#374151";
        } else {
            String[] colors = getCategorieColors(text);
            bg = active ? darkenColor(colors[0]) : colors[0];
            fg = colors[1];
        }
        btn.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-text-fill:" + fg + ";" +
                        "-fx-font-size:11px; -fx-font-weight:bold;" +
                        "-fx-background-radius:12px; -fx-padding:4 12;" +
                        "-fx-cursor:hand; -fx-border-width:0;"
        );
        return btn;
    }

    private String darkenColor(String hex) {
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            return String.format("#%02x%02x%02x",
                    Math.max(0, r - 18), Math.max(0, g - 18), Math.max(0, b - 18));
        } catch (Exception e) { return hex; }
    }

    // ─────────────────────────────────────────
    // Recherche / filtres / tri
    // ─────────────────────────────────────────
    private void filtrerRecherche(String query) {
        String statut = filtreStatut.getValue();
        List<Publication> filtre = toutesPublications.stream()
                .filter(p -> {
                    boolean matchTexte = query == null || query.trim().isEmpty()
                            || (p.getTitre()    != null && p.getTitre().toLowerCase().contains(query.toLowerCase()))
                            || (p.getContenu()  != null && p.getContenu().toLowerCase().contains(query.toLowerCase()))
                            || (p.getCategorie()!= null && p.getCategorie().toLowerCase().contains(query.toLowerCase()));
                    boolean matchStatut = statut == null || statut.equals("Tous")
                            || (p.getStatut() != null && statut.equalsIgnoreCase(p.getStatut()));
                    return matchTexte && matchStatut;
                }).toList();
        afficherPublications(filtre);
    }

    @FXML
    private void filtrerParStatut() {
        String statut = filtreStatut.getValue();
        if (statut == null || statut.equals("Tous")) {
            afficherPublications(toutesPublications);
            return;
        }
        List<Publication> filtre = toutesPublications.stream()
                .filter(p -> p.getStatut() != null && statut.equalsIgnoreCase(p.getStatut()))
                .toList();
        afficherPublications(filtre);
    }

    @FXML
    private void trierPublications() {
        String tri = triPublications.getValue();
        if (tri == null) return;
        List<Publication> liste = new java.util.ArrayList<>(toutesPublications);
        switch (tri) {
            case "Plus récents" -> liste.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            case "Plus anciens" -> liste.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
            case "Plus likés"   -> liste.sort((a, b) -> b.getNbLikes() - a.getNbLikes());
            case "Plus vus"     -> liste.sort((a, b) -> b.getNbVues()  - a.getNbVues());
        }
        afficherPublications(liste);
    }

    // ─────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────
    @FXML
    private void ouvrirAjout() { ouvrirFormulaire(null); }

    private void ouvrirFormulaire(Publication publication) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/chroniccare/ajouter-publication.fxml")
            );
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(publication == null ? "Nouvelle publication" : "Modifier");
            stage.setScene(new Scene(loader.load()));

            AjouterPublicationController ctrl = loader.getController();
            if (publication != null) ctrl.setPublication(publication);

            stage.showAndWait();
            chargerPublications();
            chargerCategories();
            rafraichirBadgeNotif();
        } catch (Exception e) {
            System.err.println("Erreur formulaire : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ouvrirDetail(Publication p) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/chroniccare/detail-publication.fxml")
            );
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(p.getTitre());
            stage.setScene(new Scene(loader.load()));

            DetailPublicationController ctrl = loader.getController();
            ctrl.setPublication(p);

            stage.showAndWait();
            chargerPublications();
            chargerCategories();
            rafraichirBadgeNotif(); // 🔔 rafraîchir badge après commentaire
        } catch (Exception e) {
            System.err.println("Erreur détail : " + e.getMessage());
            e.printStackTrace();
        }
    }
}