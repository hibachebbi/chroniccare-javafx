package com.chroniccare.controllers;
import javafx.application.Platform;
import javafx.scene.control.Button;
import com.chroniccare.models.User;
import com.chroniccare.services.UserService;
import com.chroniccare.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsController {

    @FXML private Label sidebarAvatar;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarRoleBadge;
    @FXML private Label topbarDate;
    @FXML private Label topbarAvatar;
    @FXML private Label topbarUserName;

    @FXML private Button btnUsers;
    @FXML private Label statTotal;
    @FXML private Label statHommes;
    @FXML private Label statFemmes;

    @FXML private Canvas canvasRoles;
    @FXML private Canvas canvasGenre;
    @FXML private VBox legendeRoles;
    @FXML private VBox legendeGenre;

    private final UserService userService = new UserService();

    // Palette de couleurs pour les camemberts
    private static final Color[] PALETTE = {
            Color.web("#3b82f6"), // bleu
            Color.web("#10b981"), // vert
            Color.web("#f59e0b"), // orange
            Color.web("#8b5cf6"), // violet
            Color.web("#ef4444"), // rouge
            Color.web("#06b6d4")  // cyan
    };

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        if (!SessionManager.getInstance().isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Seul l'administrateur peut accéder à cette page.");
            alert.show();
            Platform.runLater(this::goToHome);
            return;
        }
        // Topbar et sidebar
        String initials = getInitials(user);
        sidebarAvatar.setText(initials);
        sidebarUserName.setText(user.getPrenom() + " " + user.getNom());
        sidebarRoleBadge.setText(getRoleLabel(user));
        topbarAvatar.setText(initials);
        topbarUserName.setText(user.getPrenom() + " " + user.getNom());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        topbarDate.setText(LocalDate.now().format(fmt));

        // Bouton utilisateurs visible uniquement pour admin
        // (btnUsers est un Label dans le FXML, on le gère via le contrôleur parent)

        try {
            List<User> users = userService.getAll();

            // --- Cartes résumé ---
            statTotal.setText(String.valueOf(users.size()));
            long nbHommes = users.stream()
                    .filter(u -> "Homme".equalsIgnoreCase(u.getGenre()))
                    .count();
            long nbFemmes = users.stream()
                    .filter(u -> "Femme".equalsIgnoreCase(u.getGenre()))
                    .count();
            statHommes.setText(String.valueOf(nbHommes));
            statFemmes.setText(String.valueOf(nbFemmes));

            // --- Camembert rôles ---
            Map<String, Integer> rolesMap = userService.countByRole();
            dessinerBarres(canvasRoles, legendeRoles, rolesMap);

            // --- Camembert genre ---
            Map<String, Integer> genreMap = new LinkedHashMap<>();
            genreMap.put("Homme", (int) nbHommes);
            genreMap.put("Femme", (int) nbFemmes);
            long autres = users.size() - nbHommes - nbFemmes;
            if (autres > 0) genreMap.put("Autre", (int) autres);
            dessinerBarres(canvasGenre, legendeGenre, genreMap);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Dessine un camembert sur un Canvas JavaFX.
     * @param canvas  le Canvas cible
     * @param legende le VBox où ajouter les labels de légende
     * @param data    les données (label → valeur)
     */
    private void dessinerBarres(Canvas canvas, VBox legende, Map<String, Integer> data) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        int total = data.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            gc.setFill(Color.web("#94a3b8"));
            gc.fillText("Aucune donnée", w / 2 - 40, h / 2);
            return;
        }

        int nbBarres = data.size();
        double marginLeft = 50;
        double marginBottom = 40;
        double marginTop = 20;
        double marginRight = 20;
        double graphW = w - marginLeft - marginRight;
        double graphH = h - marginBottom - marginTop;

        // Valeur max pour l'échelle
        int maxVal = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        // Axe Y — lignes horizontales de référence
        gc.setStroke(Color.web("#e2e8f0"));
        gc.setLineWidth(1);
        int nbLignes = 4;
        for (int i = 0; i <= nbLignes; i++) {
            double y = marginTop + graphH - (i * graphH / nbLignes);
            gc.strokeLine(marginLeft, y, marginLeft + graphW, y);

            // Valeur sur l'axe Y
            int valLabel = (int) Math.round((double) i * maxVal / nbLignes);
            gc.setFill(Color.web("#94a3b8"));
            gc.setFont(javafx.scene.text.Font.font("Segoe UI", 11));
            gc.fillText(String.valueOf(valLabel), 5, y + 4);
        }

        // Axe X et Y (bordures)
        gc.setStroke(Color.web("#cbd5e1"));
        gc.setLineWidth(1.5);
        gc.strokeLine(marginLeft, marginTop, marginLeft, marginTop + graphH);
        gc.strokeLine(marginLeft, marginTop + graphH, marginLeft + graphW, marginTop + graphH);

        // Barres
        double largeurBarre = (graphW / nbBarres) * 0.55;
        double espaceBarre = graphW / nbBarres;
        int colorIndex = 0;

        legende.getChildren().clear();

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            double hauteurBarre = ((double) entry.getValue() / maxVal) * graphH;
            double x = marginLeft + colorIndex * espaceBarre + (espaceBarre - largeurBarre) / 2;
            double y = marginTop + graphH - hauteurBarre;

            Color couleur = PALETTE[colorIndex % PALETTE.length];

            // Ombre légère
            gc.setFill(Color.web("#00000015"));
            gc.fillRoundRect(x + 3, y + 3, largeurBarre, hauteurBarre, 8, 8);

            // Barre principale
            gc.setFill(couleur);
            gc.fillRoundRect(x, y, largeurBarre, hauteurBarre, 8, 8);

            // Valeur au dessus de la barre
            gc.setFill(Color.web("#1e293b"));
            gc.setFont(javafx.scene.text.Font.font("Segoe UI",
                    javafx.scene.text.FontWeight.BOLD, 13));
            gc.fillText(String.valueOf(entry.getValue()),
                    x + largeurBarre / 2 - 6, y - 6);

            // Label en dessous de l'axe X
            gc.setFill(Color.web("#475569"));
            gc.setFont(javafx.scene.text.Font.font("Segoe UI", 11));
            gc.fillText(entry.getKey(),
                    x + largeurBarre / 2 - (entry.getKey().length() * 3),
                    marginTop + graphH + 18);

            // Légende
            HBox ligne = new HBox(10);
            ligne.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            javafx.scene.shape.Rectangle carre = new javafx.scene.shape.Rectangle(14, 14);
            carre.setFill(couleur);
            carre.setArcWidth(4);
            carre.setArcHeight(4);
            Label labelLegende = new Label(entry.getKey() + " — " + entry.getValue() + " utilisateur(s)");
            labelLegende.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
            ligne.getChildren().addAll(carre, labelLegende);
            legende.getChildren().add(ligne);

            colorIndex++;
        }
    }

    // --- Navigation ---

    @FXML
    public void goToHome() {
        naviguer("/com/chroniccare/home.fxml");
    }

    @FXML
    public void goToProfile() {
        String fxml;
        if (SessionManager.getInstance().isPatient())
            fxml = "/com/chroniccare/profile-patient.fxml";
        else if (SessionManager.getInstance().isCoach())
            fxml = "/com/chroniccare/profile-coach.fxml";
        else if (SessionManager.getInstance().isNutritionniste())
            fxml = "/com/chroniccare/profile-nutritionniste.fxml";
        else
            fxml = "/com/chroniccare/list-users.fxml";
        naviguer(fxml);
    }

    @FXML
    public void goToUsers() {
        naviguer("/com/chroniccare/list-users.fxml");
    }


    @FXML
    public void goToBlockedAccounts() {
        if (!SessionManager.getInstance().isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Seul l'administrateur peut accéder aux comptes bloqués.");
            alert.show();
            Platform.runLater(this::goToHome);
            return;
        }
        naviguer("/com/chroniccare/blocked-accounts.fxml");
    }

    @FXML
    public void goToMedicalAudit() {
        if (!SessionManager.getInstance().isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Seul l'administrateur peut accéder à l'audit médical.");
            alert.show();
            Platform.runLater(this::goToHome);
            return;
        }
        naviguer("/com/chroniccare/medical-audit.fxml");
    }


    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        naviguer("/com/chroniccare/login.fxml");
    }

    private void naviguer(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            canvasRoles.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Helpers ---

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
}
