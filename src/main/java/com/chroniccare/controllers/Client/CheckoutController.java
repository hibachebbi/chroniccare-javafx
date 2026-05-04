package com.chroniccare.controllers.Client;

import com.chroniccare.models.Commande;
import com.chroniccare.models.Livraison;
import com.chroniccare.models.User;
import com.chroniccare.services.CartService;
import com.chroniccare.services.CommandeService;
import com.chroniccare.services.LivraisonService;
import com.chroniccare.services.StripePaymentService;
import com.chroniccare.utils.FxNavigator;
import com.chroniccare.utils.SessionManager;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CheckoutController {

    @FXML
    private Node root;
    @FXML
    private TextArea adresseArea;
    @FXML
    private ComboBox<String> paiementCombo;
    @FXML
    private Label totalLabel;
    @FXML
    private Label infoLabel;
    @FXML
    private VBox deliverySection;
    @FXML
    private ComboBox<String> livraisonModeCombo;
    @FXML
    private TextField villeField;
    @FXML
    private TextField codePostalField;
    @FXML
    private Button confirmButton;

    private Commande currentCommande;
    private boolean livraisonStep = false;

    private final CommandeService commandeService = new CommandeService();
    private final LivraisonService livraisonService = new LivraisonService();
    private final StripePaymentService stripePaymentService = new StripePaymentService();

    @FXML
    public void initialize() {
        paiementCombo.setItems(FXCollections.observableArrayList("stripe"));
        paiementCombo.setValue("stripe");
        paiementCombo.setDisable(true);

        livraisonModeCombo.setItems(FXCollections.observableArrayList("standard", "express", "point_relais"));
        livraisonModeCombo.setValue("standard");

        updateView();
    }

    public void setCommande(Commande commande) {
        this.currentCommande = commande;
        updateView();
    }

    private void updateView() {
        if (totalLabel == null || paiementCombo == null)
            return;
        if (currentCommande == null) {
            double total = CartService.getInstance().getTotalPrice();
            totalLabel.setText("Total: " + total);
            if (infoLabel != null) {
                infoLabel.setText(total > 0 ? "Commande panier" : "Panier vide");
            }
            return;
        }
        totalLabel.setText("Total: " + currentCommande.getTotal());
        paiementCombo.setValue("stripe");
        if (infoLabel != null && currentCommande.getNumeroCommande() != null) {
            infoLabel.setText("Commande: " + currentCommande.getNumeroCommande());
        }
    }

    @FXML
    public void annuler() {
        FxNavigator.go(anchor(), "/com/chroniccare/Client/Panier.fxml");
    }

    @FXML
    public void confirmerCommande() {
        if (!livraisonStep) {
            if (adresseArea.getText() == null || adresseArea.getText().trim().isEmpty()) {
                showError("Adresse obligatoire");
                return;
            }
            livraisonStep = true;
            if (deliverySection != null) {
                deliverySection.setManaged(true);
                deliverySection.setVisible(true);
            }
            if (infoLabel != null) {
                infoLabel.setText("Choisissez le mode de livraison et completez les informations.");
            }
            if (confirmButton != null) {
                confirmButton.setText("Valider livraison");
            }
            return;
        }

        // Etape 2: valider et enregistrer la livraison + (si besoin) la commande panier
        try {
            String adresse = adresseArea.getText() == null ? "" : adresseArea.getText().trim();
            String ville = villeField.getText() == null ? "" : villeField.getText().trim();
            String codePostal = codePostalField.getText() == null ? "" : codePostalField.getText().trim();
            String modeLivraison = livraisonModeCombo.getValue();
            String paiement = "stripe";
            String stripeSessionId = null;
            String stripePaymentIntentId = null;

            if (adresse.isEmpty() || ville.isEmpty() || codePostal.isEmpty()) {
                showError("Veuillez completer adresse, ville et code postal");
                return;
            }
            if (modeLivraison == null || modeLivraison.isBlank()) {
                showError("Mode de livraison obligatoire");
                return;
            }
            paiementCombo.setValue("stripe");

            // 1) Créer la commande si on vient du panier
            if (currentCommande == null) {
                User user = SessionManager.getInstance().getCurrentUser();
                if (user == null || user.getId() <= 0) {
                    showError("Utilisateur non connecté");
                    return;
                }
                if (CartService.getInstance().getItems().isEmpty()) {
                    showError("Panier vide");
                    return;
                }

                for (CartService.CartItem item : CartService.getInstance().getItems().values()) {
                    if (item.getProduit() == null) {
                        showError("Produit manquant dans le panier");
                        return;
                    }
                    if (item.getQuantity() > item.getProduit().getStock()) {
                        showError("Stock insuffisant pour " + item.getProduit().getNom() + " (stock disponible: "
                                + item.getProduit().getStock() + ")");
                        return;
                    }
                }

                double total = CartService.getInstance().getTotalPrice();

                StripeCheckoutFlowResult stripeCheckout = processStripeCheckout(
                        total,
                        "Commande ChronicCare utilisateur " + user.getId());
                if (!stripeCheckout.isSuccess()) {
                    showError("Paiement Stripe echoue: " + stripeCheckout.getMessage());
                    return;
                }
                stripeSessionId = stripeCheckout.getSessionId();
                stripePaymentIntentId = stripeCheckout.getPaymentIntentId();

                currentCommande = commandeService.createFromCart(user.getId(), total, paiement,
                        CartService.getInstance().getItems());

                if ((stripeSessionId != null && !stripeSessionId.isBlank())
                        || (stripePaymentIntentId != null && !stripePaymentIntentId.isBlank())) {
                    currentCommande.setStripeSessionId(stripeSessionId);
                    currentCommande.setStripePaymentIntentId(stripePaymentIntentId);
                    commandeService.updateStripeReferences(currentCommande.getId(), stripeSessionId,
                            stripePaymentIntentId);
                }
            } else if (currentCommande.getStripeSessionId() == null
                    || currentCommande.getStripeSessionId().isBlank()
                    || currentCommande.getStripePaymentIntentId() == null
                    || currentCommande.getStripePaymentIntentId().isBlank()) {
                StripeCheckoutFlowResult stripeCheckout = processStripeCheckout(
                        currentCommande.getTotal(),
                        "Commande ChronicCare " + (currentCommande.getNumeroCommande() == null ? ""
                                : currentCommande.getNumeroCommande()));
                if (!stripeCheckout.isSuccess()) {
                    showError("Paiement Stripe echoue: " + stripeCheckout.getMessage());
                    return;
                }
                stripeSessionId = stripeCheckout.getSessionId();
                stripePaymentIntentId = stripeCheckout.getPaymentIntentId();
                currentCommande.setStripeSessionId(stripeSessionId);
                currentCommande.setStripePaymentIntentId(stripePaymentIntentId);
                commandeService.updateStripeReferences(currentCommande.getId(), stripeSessionId, stripePaymentIntentId);
            }

            currentCommande.setMethodePaiement("stripe");

            // 2) Enregistrer la livraison
            Livraison livraison = new Livraison();
            livraison.setCommandeId(currentCommande.getId());
            livraison.setStatut("en_preparation");
            livraison.setAdresse(adresse);
            livraison.setVille(ville);
            livraison.setCodePostal(codePostal);
            livraison.setTrackingCode(null);
            livraison.setDatePrevue(LocalDateTime.now().plusDays(modeLivraison.equals("express") ? 1 : 3));
            livraison.setCreatedAt(LocalDateTime.now());
            livraison.setUpdatedAt(LocalDateTime.now());

            livraisonService.save(livraison);

            // 2bis) Mettre à jour la commande en 'validee' après création livraison
            commandeService.updateAdminStatusAndPayment(currentCommande.getId(), "validee", "stripe");

            // 3) Nettoyer panier si commande issue du panier
            CartService.getInstance().clear();

            showError("Livraison enregistrée avec succes");
            FxNavigator.go(anchor(), "/com/chroniccare/Client/CommandesDashboard.fxml");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private StripeCheckoutFlowResult processStripeCheckout(double amount, String description) {
        StripePaymentService.CheckoutSessionStartResult startResult = stripePaymentService.startCheckoutSession(amount,
                description);
        if (!startResult.isSuccess()) {
            return StripeCheckoutFlowResult.failed(startResult.getMessage());
        }

        String callbackUrl = openStripeCheckoutWindow(
                startResult.getCheckoutUrl(),
                startResult.getSuccessUrlPrefix(),
                startResult.getCancelUrlPrefix());
        if (callbackUrl == null || callbackUrl.isBlank()) {
            return StripeCheckoutFlowResult.failed("Paiement annule ou fenetre fermee avant validation.");
        }

        String sessionId = extractQueryParam(callbackUrl, "session_id");
        if (sessionId == null || sessionId.isBlank()) {
            return StripeCheckoutFlowResult.failed("Retour Stripe invalide: session_id introuvable.");
        }

        StripePaymentService.CheckoutSessionVerificationResult verification = stripePaymentService
                .verifyCheckoutSession(sessionId);
        if (!verification.isSuccess()) {
            return StripeCheckoutFlowResult.failed(verification.getMessage());
        }

        return StripeCheckoutFlowResult.success(verification.getSessionId(), verification.getPaymentIntentId());
    }

    private String openStripeCheckoutWindow(String checkoutUrl, String successPrefix, String cancelPrefix) {
        WebView webView = new WebView();
        AtomicReference<String> callbackRef = new AtomicReference<>(null);

        Stage stage = new Stage();
        stage.setTitle("ChronicCare - Stripe Checkout");
        stage.initModality(Modality.APPLICATION_MODAL);
        if (anchor() != null && anchor().getScene() != null && anchor().getScene().getWindow() != null) {
            stage.initOwner(anchor().getScene().getWindow());
        }

        Label title = new Label("Paiement securise Stripe (mode test)");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Button closeBtn = new Button("Annuler paiement");
        closeBtn.setStyle(
                "-fx-background-color: #dc2626; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6;");
        closeBtn.setOnAction(event -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(10, title, spacer, closeBtn);
        topBar.setStyle("-fx-padding: 10; -fx-alignment: center-left; -fx-background-color: #f8fafc;");

        BorderPane pane = new BorderPane();
        pane.setTop(topBar);
        pane.setCenter(webView);

        webView.getEngine().locationProperty().addListener((obs, oldUrl, newUrl) -> {
            if (newUrl == null || newUrl.isBlank()) {
                return;
            }
            if (successPrefix != null && newUrl.startsWith(successPrefix)) {
                callbackRef.set(newUrl);
                stage.close();
                return;
            }
            if (cancelPrefix != null && newUrl.startsWith(cancelPrefix)) {
                callbackRef.set(null);
                stage.close();
            }
        });

        webView.getEngine().load(checkoutUrl);
        stage.setScene(new Scene(pane, 1120, 760));
        stage.showAndWait();

        return callbackRef.get();
    }

    private String extractQueryParam(String url, String key) {
        try {
            URI uri = URI.create(url);
            String query = uri.getRawQuery();
            if (query == null || query.isBlank()) {
                return null;
            }
            Map<String, String> params = new HashMap<>();
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                if (pair == null || pair.isBlank()) {
                    continue;
                }
                String[] kv = pair.split("=", 2);
                String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                params.put(k, v);
            }
            return params.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    private static final class StripeCheckoutFlowResult {
        private final boolean success;
        private final String sessionId;
        private final String paymentIntentId;
        private final String message;

        private StripeCheckoutFlowResult(boolean success, String sessionId, String paymentIntentId, String message) {
            this.success = success;
            this.sessionId = sessionId;
            this.paymentIntentId = paymentIntentId;
            this.message = message;
        }

        private static StripeCheckoutFlowResult success(String sessionId, String paymentIntentId) {
            return new StripeCheckoutFlowResult(true, sessionId, paymentIntentId, "OK");
        }

        private static StripeCheckoutFlowResult failed(String message) {
            return new StripeCheckoutFlowResult(false, null, null, message);
        }

        private boolean isSuccess() {
            return success;
        }

        private String getSessionId() {
            return sessionId;
        }

        private String getPaymentIntentId() {
            return paymentIntentId;
        }

        private String getMessage() {
            return message;
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ChronicCare");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void goToHome() {
        FxNavigator.go(anchor(), "/com/chroniccare/home.fxml");
    }

    @FXML
    public void goToCommandes() {
        FxNavigator.go(anchor(), "/com/chroniccare/Client/CommandesDashboard.fxml");
    }

    @FXML
    public void goToPanier() {
        FxNavigator.go(anchor(), "/com/chroniccare/Client/Panier.fxml");
    }

    @FXML
    public void goToCatalogue() {
        FxNavigator.go(anchor(), "/com/chroniccare/Client/ProduitsDashboard.fxml");
    }

    @FXML
    public void goToLogin() {
        SessionManager.getInstance().logout();
        FxNavigator.go(anchor(), "/com/chroniccare/login.fxml");
    }

    private Node anchor() {
        if (root != null)
            return root;
        throw new IllegalStateException(
                "Ancre de navigation manquante: ajoutez fx:id=\"root\" sur le noeud racine du FXML");
    }
}
