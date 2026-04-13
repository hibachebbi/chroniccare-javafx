package controllers.Client;

import com.chroniccare.entities.Commande;
import com.chroniccare.entities.User;
import com.chroniccare.services.CartService;
import com.chroniccare.services.CommandeService;
import com.chroniccare.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.time.LocalDateTime;

public class CheckoutController {

    @FXML private TextArea adresseArea;
    @FXML private ComboBox<String> paiementCombo;
    @FXML private Label totalLabel;
    @FXML private Label infoLabel;

    private final CartService cart = CartService.getInstance();
    private final CommandeService commandeService = new CommandeService();

    @FXML
    public void initialize() {
        if (paiementCombo != null) {
            paiementCombo.getItems().setAll("card", "cash", "stripe");
            paiementCombo.setValue("card");
        }
        refreshTotal();
    }

    private void refreshTotal() {
        if (totalLabel != null) {
            totalLabel.setText(String.format("Total: %.2f", cart.getTotalPrice()));
        }
        if (infoLabel != null) {
            infoLabel.setText("Articles: " + cart.getTotalItems() + "  •  Commande simple (sans Stripe pour le moment)");
        }
    }

    @FXML
    public void annuler() {
        navigate("/com/chroniccare/Client/Panier.fxml");
    }

    @FXML
    public void confirmerCommande() {
        refreshTotal();
        if (cart.getItems().isEmpty()) {
            Alert warn = new Alert(Alert.AlertType.WARNING);
            warn.setTitle("Commande");
            warn.setHeaderText(null);
            warn.setContentText("Votre panier est vide.");
            warn.showAndWait();
            return;
        }

        String adresse = adresseArea == null ? "" : adresseArea.getText();
        if (adresse == null || adresse.trim().isEmpty()) {
            Alert warn = new Alert(Alert.AlertType.WARNING);
            warn.setTitle("Commande");
            warn.setHeaderText(null);
            warn.setContentText("Veuillez saisir une adresse de livraison.");
            warn.showAndWait();
            return;
        }

        String paiement = paiementCombo == null ? null : paiementCombo.getValue();
        if (paiement == null || paiement.isBlank()) {
            Alert warn = new Alert(Alert.AlertType.WARNING);
            warn.setTitle("Commande");
            warn.setHeaderText(null);
            warn.setContentText("Veuillez choisir une méthode de paiement.");
            warn.showAndWait();
            return;
        }

        try {
            User user = SessionManager.getInstance().getCurrentUser();
            int userId = (user == null) ? 0 : user.getId();
            if (userId <= 0) {
                Alert warn = new Alert(Alert.AlertType.WARNING);
                warn.setTitle("Commande");
                warn.setHeaderText(null);
                warn.setContentText("Utilisateur non connecté.");
                warn.showAndWait();
                return;
            }

            Commande commande = new Commande();
            // Enregistrement DB + décrément stock (transaction)
            commandeService.createFromCart(userId, cart.getTotalPrice(), paiement, cart.getItems());

            cart.clear();

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Commande");
            ok.setHeaderText(null);
            ok.setContentText("Commande confirmée.");
            ok.showAndWait();

            navigate("/com/chroniccare/Client/CommandesDashboard.fxml");
        } catch (Exception e) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Commande");
            err.setHeaderText("Confirmation échouée");
            err.setContentText(e.getMessage());
            err.showAndWait();
        }
    }

    private void navigate(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            if (totalLabel != null && totalLabel.getScene() != null) {
                totalLabel.getScene().setRoot(root);
            } else if (adresseArea != null && adresseArea.getScene() != null) {
                adresseArea.getScene().setRoot(root);
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("ChronicCare");
            alert.setHeaderText("Navigation échouée");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void goToHome() {
        navigate("/com/chroniccare/home.fxml");
    }

    @FXML
    public void goToCommandes() {
        navigate("/com/chroniccare/Client/CommandesDashboard.fxml");
    }

    @FXML
    public void goToPanier() {
        navigate("/com/chroniccare/Client/Panier.fxml");
    }

    @FXML
    public void goToCatalogue() {
        navigate("/com/chroniccare/Client/ProduitsDashboard.fxml");
    }

    @FXML
    public void goToLogin() {
        SessionManager.getInstance().logout();
        navigate("/com/chroniccare/login.fxml");
    }
}

