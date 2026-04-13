package com.chroniccare.controllers;

import com.chroniccare.models.Commentaire;
import com.chroniccare.models.Publication;
import com.chroniccare.services.CommentaireService;
import com.chroniccare.services.PublicationService;
import com.chroniccare.utils.SessionManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DetailPublicationController implements Initializable {

    @FXML private Label labelTitrePublication;
    @FXML private Label badgeCategorie;
    @FXML private Label badgeStatut;
    @FXML private Label labelDate;
    @FXML private Label labelContenu;
    @FXML private Label labelLikes;
    @FXML private Label nbCommentaires;
    @FXML private VBox listeCommentaires;
    @FXML private TextArea fieldCommentaire;
    @FXML private CheckBox checkAnonymous;
    @FXML private Label erreurCommentaire;
    @FXML private Button btnLikerPublication;

    private final PublicationService pubService = new PublicationService();
    private final CommentaireService comService = new CommentaireService();
    private Publication publication;

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

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

    public void setPublication(Publication p) {
        this.publication = p;
        labelTitrePublication.setText(p.getTitre());
        labelContenu.setText(p.getContenu());
        badgeCategorie.setText(p.getCategorie() != null ? p.getCategorie() : "");
        badgeStatut.setText(p.getStatut());
        labelLikes.setText(p.getNbLikes() + " likes");

        if (p.getCreatedAt() != null) {
            labelDate.setText(
                    p.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );
        }

        var user = SessionManager.getInstance().getCurrentUser();
        boolean dejaLike = user != null && pubService.hasLiked(p.getId(), user.getId());

        btnLikerPublication.setText(dejaLike ? " Liké" : " Liker");
        btnLikerPublication.setGraphic(
                buildIcon(FontAwesomeIcon.HEART, dejaLike ? "#e11d48" : "#94a3b8", "14px")
        );
        btnLikerPublication.setStyle(dejaLike
                ? "-fx-background-color:#fce7f3; -fx-text-fill:#e11d48; -fx-background-radius:8px;"
                : "-fx-background-color:#f1f5f9; -fx-text-fill:#64748b; -fx-background-radius:8px;");

        chargerCommentaires();
    }

    private void chargerCommentaires() {
        listeCommentaires.getChildren().clear();
        List<Commentaire> liste = comService.getByPublication(publication.getId());
        nbCommentaires.setText(String.valueOf(liste.size()));

        for (Commentaire c : liste) {
            VBox card = new VBox(6);
            card.setStyle("-fx-background-color:white; -fx-background-radius:8px;" +
                    "-fx-padding:12; -fx-border-color:#e2e8f0;" +
                    "-fx-border-width:1; -fx-border-radius:8px;");

            String auteur = c.isAnonymous() ? "Anonyme"
                    : (c.getAuteurId() != null ? pubService.getNomAuteur(c.getAuteurId()) : "Inconnu");

            Label lblAuteur = new Label(" " + auteur, buildIcon(FontAwesomeIcon.USER, "#374151", "12px"));
            lblAuteur.setStyle("-fx-font-weight:bold; -fx-font-size:12px; -fx-text-fill:#374151;");

            Label lblContenu = new Label(c.getContenu());
            lblContenu.setWrapText(true);
            lblContenu.setStyle("-fx-font-size:13px; -fx-text-fill:#374151;");

            var session = SessionManager.getInstance();
            var currentUser = session.getCurrentUser();
            int userId = currentUser != null ? currentUser.getId() : -1;
            boolean dejaLike = userId > 0 && comService.hasLiked(c.getId(), userId);

            Button btnLike = buildIconButton(
                    " " + c.getLikeCount(),
                    FontAwesomeIcon.HEART,
                    dejaLike ? "#e11d48" : "#94a3b8",
                    dejaLike
                            ? "-fx-background-color:#fce7f3; -fx-text-fill:#e11d48; -fx-background-radius:6px; -fx-cursor:hand;"
                            : "-fx-background-color:#f1f5f9; -fx-text-fill:#94a3b8; -fx-background-radius:6px; -fx-cursor:hand;"
            );

            btnLike.setOnAction(e -> {
                int uid = SessionManager.getInstance().getCurrentUser() != null
                        ? SessionManager.getInstance().getCurrentUser().getId() : -1;
                if (uid < 0) return;

                if (comService.hasLiked(c.getId(), uid)) {
                    comService.unliker(c.getId(), uid);
                } else {
                    comService.liker(c.getId(), uid);
                }
                chargerCommentaires();
            });

            HBox actions = new HBox(6, btnLike);

            boolean peutModifier = session.isAdmin() ||
                    (currentUser != null && c.getAuteurId() != null
                            && c.getAuteurId().equals(currentUser.getId()));

            if (peutModifier) {
                actions.getChildren().add(creerBtnEditCommentaire(c));
                actions.getChildren().add(creerBtnSuppCommentaire(c));
            }

            card.getChildren().addAll(lblAuteur, lblContenu, actions);
            listeCommentaires.getChildren().add(card);
        }
    }

    private Button creerBtnEditCommentaire(Commentaire c) {
        Button btn = new Button("", buildIcon(FontAwesomeIcon.PENCIL, "#b45309", "14px"));
        btn.setStyle("-fx-background-color:#fef3c7; -fx-background-radius:6px; -fx-cursor:hand;");
        btn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(c.getContenu());
            dialog.setTitle("Modifier commentaire");
            dialog.setHeaderText(null);
            dialog.setContentText("Contenu :");
            dialog.showAndWait().ifPresent(nouveau -> {
                if (!nouveau.trim().isEmpty()) {
                    c.setContenu(nouveau.trim());
                    comService.modifier(c);
                    chargerCommentaires();
                }
            });
        });
        return btn;
    }

    private Button creerBtnSuppCommentaire(Commentaire c) {
        Button btn = new Button("", buildIcon(FontAwesomeIcon.TRASH, "#dc2626", "14px"));
        btn.setStyle("-fx-background-color:#fee2e2; -fx-background-radius:6px; -fx-cursor:hand;");
        btn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Supprimer ce commentaire ?", ButtonType.YES, ButtonType.NO);
            alert.setHeaderText(null);
            alert.showAndWait().ifPresent(r -> {
                if (r == ButtonType.YES) {
                    comService.supprimer(c.getId());
                    chargerCommentaires();
                }
            });
        });
        return btn;
    }

    @FXML
    private void likerPublication() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        if (pubService.hasLiked(publication.getId(), user.getId())) {
            pubService.unliker(publication.getId(), user.getId());
            publication.setNbLikes(publication.getNbLikes() - 1);

            labelLikes.setText(publication.getNbLikes() + " likes");
            btnLikerPublication.setText(" Liker");
            btnLikerPublication.setGraphic(buildIcon(FontAwesomeIcon.HEART, "#94a3b8", "14px"));
            btnLikerPublication.setStyle("-fx-background-color:#f1f5f9; -fx-text-fill:#64748b; -fx-background-radius:8px;");
        } else {
            pubService.liker(publication.getId(), user.getId());
            publication.setNbLikes(publication.getNbLikes() + 1);

            labelLikes.setText(publication.getNbLikes() + " likes");
            btnLikerPublication.setText(" Liké");
            btnLikerPublication.setGraphic(buildIcon(FontAwesomeIcon.HEART, "#e11d48", "14px"));
            btnLikerPublication.setStyle("-fx-background-color:#fce7f3; -fx-text-fill:#e11d48; -fx-background-radius:8px;");
        }
    }

    @FXML
    private void ajouterCommentaire() {
        erreurCommentaire.setVisible(false);
        erreurCommentaire.setManaged(false);

        String texte = fieldCommentaire.getText().trim();
        if (texte.length() < 3) {
            erreurCommentaire.setText("Commentaire trop court (min. 3 caractères).");
            erreurCommentaire.setVisible(true);
            erreurCommentaire.setManaged(true);
            return;
        }

        Commentaire c = new Commentaire();
        c.setContenu(texte);
        c.setAnonymous(checkAnonymous.isSelected());
        c.setPublicationId(publication.getId());

        var user = SessionManager.getInstance().getCurrentUser();
        if (user != null) c.setAuteurId(user.getId());

        if (comService.ajouter(c)) {
            fieldCommentaire.clear();
            checkAnonymous.setSelected(false);
            chargerCommentaires();
        }
    }

    @FXML
    private void fermer() {
        ((Stage) labelTitrePublication.getScene().getWindow()).close();
    }
}