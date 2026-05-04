package com.chroniccare.controllers;

import com.chroniccare.models.Commentaire;
import com.chroniccare.models.Publication;
import com.chroniccare.services.CommentaireService;
import com.chroniccare.services.PublicationService;
import com.chroniccare.services.SummaryService;
import com.chroniccare.services.TranslationService;
import com.chroniccare.utils.SessionManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DetailPublicationController implements Initializable {

    @FXML private Label    labelTitrePublication;
    @FXML private Label    badgeCategorie;
    @FXML private Label    badgeStatut;
    @FXML private Label    labelDate;
    @FXML private Label    labelContenu;
    @FXML private Label    labelLikes;
    @FXML private Label    nbCommentaires;
    @FXML private VBox     listeCommentaires;
    @FXML private TextArea fieldCommentaire;
    @FXML private CheckBox checkAnonymous;
    @FXML private Label    erreurCommentaire;
    @FXML private Button   btnLikerPublication;
    @FXML private Button   btnTraduirePublication;
    @FXML private VBox     summaryBox;
    @FXML private Label    labelSummary;
    @FXML private Button   btnSummary;
    @FXML private Button   btnFermerSummary;

    private final PublicationService pubService     = new PublicationService();
    private final CommentaireService comService     = new CommentaireService();
    private final TranslationService traducteur     = new TranslationService();
    private final SummaryService     summaryService = new SummaryService();

    private Publication publication;
    private boolean     publicationTraduite = false;
    private String      contenuOriginal     = null;
    private String      titreOriginal       = null;

    // Reponse en cours : si non null, on repond a ce commentaire
    private Commentaire commentaireParent = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────
    private FontAwesomeIconView buildIcon(FontAwesomeIcon icon, String color, String sizePx) {
        FontAwesomeIconView view = new FontAwesomeIconView(icon);
        view.setStyle("-fx-fill:" + color + ";");
        view.setSize(sizePx);
        return view;
    }

    private Button buildIconButton(String text, FontAwesomeIcon icon, String iconColor, String style) {
        Button b = new Button(text, buildIcon(icon, iconColor, "14px"));
        b.setStyle(style);
        return b;
    }

    // ─────────────────────────────────────────
    // Init
    // ─────────────────────────────────────────
    public void setPublication(Publication p) {
        this.publication         = p;
        this.titreOriginal       = p.getTitre();
        this.contenuOriginal     = p.getContenu();
        this.publicationTraduite = false;

        labelTitrePublication.setText(p.getTitre());
        labelContenu.setText(p.getContenu());
        badgeCategorie.setText(p.getCategorie() != null ? p.getCategorie() : "");
        badgeStatut.setText(p.getStatut());
        labelLikes.setText(p.getNbLikes() + " likes");

        if (p.getCreatedAt() != null)
            labelDate.setText(p.getCreatedAt().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        var user = SessionManager.getInstance().getCurrentUser();
        boolean dejaLike = user != null && pubService.hasLiked(p.getId(), user.getId());
        btnLikerPublication.setText(dejaLike ? " Like" : " Liker");
        btnLikerPublication.setGraphic(buildIcon(FontAwesomeIcon.HEART, dejaLike ? "#e11d48" : "#94a3b8", "14px"));
        btnLikerPublication.setStyle(dejaLike
                ? "-fx-background-color:#fce7f3; -fx-text-fill:#e11d48; -fx-background-radius:8px;"
                : "-fx-background-color:#f1f5f9; -fx-text-fill:#64748b; -fx-background-radius:8px;");

        if (btnTraduirePublication != null) btnTraduirePublication.setText("Traduire");
        summaryBox.setVisible(false);
        summaryBox.setManaged(false);
        chargerCommentaires();
    }

    // ─────────────────────────────────────────
    // AI SUMMARY
    // ─────────────────────────────────────────
    @FXML
    private void genererSummary() {
        summaryBox.setVisible(true);
        summaryBox.setManaged(true);
        labelSummary.setText("Generation du resume en cours...");
        btnSummary.setDisable(true);
        btnSummary.setText("...");
        new Thread(() -> {
            String summary = summaryService.resumer(titreOriginal, contenuOriginal);
            Platform.runLater(() -> {
                labelSummary.setText(summary);
                btnSummary.setDisable(false);
                btnSummary.setText("Resumer avec IA");
            });
        }).start();
    }

    @FXML
    private void fermerSummary() {
        summaryBox.setVisible(false);
        summaryBox.setManaged(false);
    }

    // ─────────────────────────────────────────
    // TRADUCTION
    // ─────────────────────────────────────────
    @FXML
    private void traduirePublication() {
        if (publication == null || contenuOriginal == null) return;
        if (publicationTraduite) {
            labelTitrePublication.setText(titreOriginal);
            labelContenu.setText(contenuOriginal);
            btnTraduirePublication.setText("Traduire");
            publicationTraduite = false;
            return;
        }
        btnTraduirePublication.setText("...");
        btnTraduirePublication.setDisable(true);
        labelContenu.setText("Traduction en cours...");
        new Thread(() -> {
            String ct = traducteur.traduireAuto(contenuOriginal);
            String tt = traducteur.traduireAuto(titreOriginal);
            Platform.runLater(() -> {
                labelTitrePublication.setText(tt);
                labelContenu.setText(ct);
                btnTraduirePublication.setText("Original");
                btnTraduirePublication.setDisable(false);
                publicationTraduite = true;
            });
        }).start();
    }

    // ─────────────────────────────────────────
    // COMMENTAIRES — chargement arbre
    // ─────────────────────────────────────────
    private void chargerCommentaires() {
        listeCommentaires.getChildren().clear();
        List<Commentaire> racines = comService.getByPublication(publication.getId());

        // Compter total (racines + reponses)
        int total = racines.stream()
                .mapToInt(c -> 1 + c.getReponses().size())
                .sum();
        nbCommentaires.setText(String.valueOf(total));

        for (Commentaire c : racines) {
            listeCommentaires.getChildren().add(creerCardCommentaire(c, false));

            // Reponses imbriquees
            for (Commentaire reponse : c.getReponses()) {
                listeCommentaires.getChildren().add(creerCardCommentaire(reponse, true));
            }
        }

        // Reinitialiser le champ reponse
        annulerReponse();
    }

    // ─────────────────────────────────────────
    // CARD commentaire (racine ou reponse)
    // ─────────────────────────────────────────
    private VBox creerCardCommentaire(Commentaire c, boolean estReponse) {
        VBox card = new VBox(6);

        if (estReponse) {
            // Indentation + style different pour les reponses
            card.setStyle(
                    "-fx-background-color:#f8fafc;" +
                            "-fx-background-radius:8px;" +
                            "-fx-padding:10 12;" +
                            "-fx-border-color:#c7d2fe;" +
                            "-fx-border-width:0 0 0 3;" +
                            "-fx-border-radius:0 8px 8px 0;"
            );
            card.setTranslateX(28); // indentation visuelle
        } else {
            card.setStyle(
                    "-fx-background-color:white;" +
                            "-fx-background-radius:8px;" +
                            "-fx-padding:12;" +
                            "-fx-border-color:#e2e8f0;" +
                            "-fx-border-width:1;" +
                            "-fx-border-radius:8px;"
            );
        }

        // Auteur
        String auteur = c.isAnonymous() ? "Anonyme"
                : (c.getAuteurId() != null ? pubService.getNomAuteur(c.getAuteurId()) : "Inconnu");

        HBox headerRow = new HBox(6);
        headerRow.setStyle("-fx-alignment:CENTER_LEFT;");

        // Badge "Reponse a" si c'est une reponse
        if (estReponse) {
            Label badgeReponse = new Label("↩ Reponse");
            badgeReponse.setStyle(
                    "-fx-background-color:#e0e7ff; -fx-text-fill:#4338ca;" +
                            "-fx-font-size:10px; -fx-font-weight:bold;" +
                            "-fx-background-radius:8px; -fx-padding:2 7;"
            );
            headerRow.getChildren().add(badgeReponse);
        }

        Label lblAuteur = new Label(" " + auteur, buildIcon(FontAwesomeIcon.USER, "#374151", "12px"));
        lblAuteur.setStyle("-fx-font-weight:bold; -fx-font-size:12px; -fx-text-fill:#374151;");
        headerRow.getChildren().add(lblAuteur);

        // Contenu
        Label lblContenu = new Label(c.getContenu());
        lblContenu.setWrapText(true);
        lblContenu.setStyle("-fx-font-size:13px; -fx-text-fill:#374151;");

        // Actions
        var session     = SessionManager.getInstance();
        var currentUser = session.getCurrentUser();
        int userId      = currentUser != null ? currentUser.getId() : -1;
        boolean dejaLike = userId > 0 && comService.hasLiked(c.getId(), userId);

        Button btnLike = buildIconButton(
                " " + c.getLikeCount(), FontAwesomeIcon.HEART,
                dejaLike ? "#e11d48" : "#94a3b8",
                dejaLike
                        ? "-fx-background-color:#fce7f3; -fx-text-fill:#e11d48; -fx-background-radius:6px; -fx-cursor:hand;"
                        : "-fx-background-color:#f1f5f9; -fx-text-fill:#94a3b8; -fx-background-radius:6px; -fx-cursor:hand;"
        );
        btnLike.setOnAction(e -> {
            int uid = currentUser != null ? currentUser.getId() : -1;
            if (uid < 0) return;
            if (comService.hasLiked(c.getId(), uid)) comService.unliker(c.getId(), uid);
            else                                      comService.liker(c.getId(), uid);
            chargerCommentaires();
        });

        // Bouton Traduire
        final String[] texteOrig = { c.getContenu() };
        final boolean[] traduit  = { false };
        Button btnTrad = new Button("Traduire");
        btnTrad.setStyle(
                "-fx-background-color:#eff6ff; -fx-text-fill:#2563eb;" +
                        "-fx-font-size:11px; -fx-background-radius:6px;" +
                        "-fx-border-width:0; -fx-cursor:hand; -fx-padding:4 10;"
        );
        btnTrad.setOnAction(e -> {
            if (traduit[0]) {
                lblContenu.setText(texteOrig[0]);
                btnTrad.setText("Traduire");
                traduit[0] = false;
            } else {
                btnTrad.setText("...");
                btnTrad.setDisable(true);
                new Thread(() -> {
                    String tr = traducteur.traduireAuto(texteOrig[0]);
                    Platform.runLater(() -> {
                        lblContenu.setText(tr);
                        btnTrad.setText("Original");
                        btnTrad.setDisable(false);
                        traduit[0] = true;
                    });
                }).start();
            }
        });

        HBox actions = new HBox(6, btnLike, btnTrad);

// Bouton Repondre sur TOUS les niveaux
// Si c'est une reponse, on repond au meme parent (pas d'imbrication infinie)
        Button btnRepondre = new Button("↩ Repondre");
        btnRepondre.setStyle(
                "-fx-background-color:#f0fdf4; -fx-text-fill:#16a34a;" +
                        "-fx-font-size:11px; -fx-background-radius:6px;" +
                        "-fx-border-width:0; -fx-cursor:hand; -fx-padding:4 10;"
        );
        btnRepondre.setOnAction(e -> activerReponse(c, auteur));
        actions.getChildren().add(btnRepondre);


        // Modifier / Supprimer
        boolean peutModifier = session.isAdmin() ||
                (currentUser != null && c.getAuteurId() != null
                        && c.getAuteurId().equals(currentUser.getId()));
        if (peutModifier) {
            actions.getChildren().add(creerBtnEdit(c));
            actions.getChildren().add(creerBtnSupp(c));
        }

        card.getChildren().addAll(headerRow, lblContenu, actions);
        return card;
    }

    // ─────────────────────────────────────────
    // REPONDRE : active le mode reponse
    // ─────────────────────────────────────────
    private void activerReponse(Commentaire commentaire, String nomAuteur) {
        // Si on repond a une reponse, on pointe vers le commentaire racine
        if (commentaire.getParentId() != null) {
            // Trouver le parent racine dans la liste
            List<Commentaire> racines = comService.getByPublication(publication.getId());
            for (Commentaire racine : racines) {
                if (racine.getId() == commentaire.getParentId()) {
                    this.commentaireParent = racine;
                    break;
                }
            }
        } else {
            this.commentaireParent = commentaire;
        }

        fieldCommentaire.setPromptText("Repondre a " + nomAuteur + "...");
        fieldCommentaire.setText("@" + nomAuteur + " ");
        fieldCommentaire.requestFocus();
        fieldCommentaire.positionCaret(fieldCommentaire.getText().length());
        fieldCommentaire.setStyle(
                "-fx-background-radius:8px; -fx-border-radius:8px;" +
                        "-fx-border-color:#6366f1; -fx-padding:10;" +
                        "-fx-border-width:2;"
        );
    }

    private void annulerReponse() {
        this.commentaireParent = null;
        fieldCommentaire.setPromptText("Votre commentaire...");
        fieldCommentaire.setStyle(
                "-fx-background-radius:8px; -fx-border-radius:8px;" +
                        "-fx-border-color:#d1d5db; -fx-padding:10;"
        );
    }

    // ─────────────────────────────────────────
    // AJOUTER commentaire ou reponse
    // ─────────────────────────────────────────
    @FXML
    private void ajouterCommentaire() {
        erreurCommentaire.setVisible(false);
        erreurCommentaire.setManaged(false);

        String texte = fieldCommentaire.getText().trim();
        if (texte.length() < 3) {
            erreurCommentaire.setText("Commentaire trop court (min. 3 caracteres).");
            erreurCommentaire.setVisible(true);
            erreurCommentaire.setManaged(true);
            return;
        }

        Commentaire c = new Commentaire();
        c.setContenu(texte);
        c.setAnonymous(checkAnonymous.isSelected());
        c.setPublicationId(publication.getId());

        // Si on repond a un commentaire, set le parent
        if (commentaireParent != null) {
            c.setParentId(commentaireParent.getId());
        }

        var user = SessionManager.getInstance().getCurrentUser();
        if (user != null) c.setAuteurId(user.getId());

        if (comService.ajouter(c)) {
            fieldCommentaire.clear();
            checkAnonymous.setSelected(false);
            chargerCommentaires();
        }
    }

    // ─────────────────────────────────────────
    // Edit / Supprimer
    // ─────────────────────────────────────────
    private Button creerBtnEdit(Commentaire c) {
        Button btn = new Button("", buildIcon(FontAwesomeIcon.PENCIL, "#b45309", "14px"));
        btn.setStyle("-fx-background-color:#fef3c7; -fx-background-radius:6px; -fx-cursor:hand;");
        btn.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog(c.getContenu());
            d.setTitle("Modifier"); d.setHeaderText(null); d.setContentText("Contenu :");
            d.showAndWait().ifPresent(nouveau -> {
                if (!nouveau.trim().isEmpty()) {
                    c.setContenu(nouveau.trim());
                    comService.modifier(c);
                    chargerCommentaires();
                }
            });
        });
        return btn;
    }

    private Button creerBtnSupp(Commentaire c) {
        Button btn = new Button("", buildIcon(FontAwesomeIcon.TRASH, "#dc2626", "14px"));
        btn.setStyle("-fx-background-color:#fee2e2; -fx-background-radius:6px; -fx-cursor:hand;");
        btn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Supprimer ce commentaire ?", ButtonType.YES, ButtonType.NO);
            alert.setHeaderText(null);
            alert.showAndWait().ifPresent(r -> {
                if (r == ButtonType.YES) { comService.supprimer(c.getId()); chargerCommentaires(); }
            });
        });
        return btn;
    }

    // ─────────────────────────────────────────
    // LIKER publication
    // ─────────────────────────────────────────
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
            btnLikerPublication.setText(" Like");
            btnLikerPublication.setGraphic(buildIcon(FontAwesomeIcon.HEART, "#e11d48", "14px"));
            btnLikerPublication.setStyle("-fx-background-color:#fce7f3; -fx-text-fill:#e11d48; -fx-background-radius:8px;");
        }
    }

    @FXML
    private void fermer() {
        ((Stage) labelTitrePublication.getScene().getWindow()).close();
    }
}