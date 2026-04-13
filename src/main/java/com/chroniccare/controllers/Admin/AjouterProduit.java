package com.chroniccare.controllers.Admin;

import com.chroniccare.entities.Produit;
import com.chroniccare.services.ProduitsService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AjouterProduit {

	@FXML private TextField nomField;
	@FXML private ComboBox<String> categorieCombo;
	@FXML private TextField prixField;
	@FXML private Spinner<Integer> stockSpinner;
	@FXML private TextArea descriptionArea;
	@FXML private Label errorLabel;

	private final ProduitsService service = new ProduitsService();

	@FXML
	public void initialize() {
		// Catégories en liste déroulante : on récupère celles existantes + quelques valeurs par défaut
		try {
			Set<String> cats = new LinkedHashSet<>();
			List<Produit> produits = service.findAll();
			for (Produit p : produits) {
				if (p.getCategorie() != null && !p.getCategorie().isBlank()) {
					cats.add(p.getCategorie().trim());
				}
			}
			// valeurs par défaut (si la DB est vide)
			cats.add("Cosmétiques");
			cats.add("Compléments");
			cats.add("Vitamines");
			cats.add("Autre");

			categorieCombo.setItems(FXCollections.observableArrayList(cats));
			if (!categorieCombo.getItems().isEmpty()) {
				categorieCombo.setValue(categorieCombo.getItems().get(0));
			}
		} catch (Exception e) {
			// fallback : au moins les catégories par défaut
			categorieCombo.setItems(FXCollections.observableArrayList("Cosmétiques", "Compléments", "Vitamines", "Autre"));
			categorieCombo.setValue("Cosmétiques");
		}
	}

	@FXML
	public void handleAdd() {
		clearError();
		try {
			String nom = nomField == null ? "" : nomField.getText();
			String categorie = categorieCombo == null ? null : categorieCombo.getValue();
			String prixTxt = prixField == null ? "" : prixField.getText();
			Integer stock = stockSpinner == null ? 0 : stockSpinner.getValue();
			String description = descriptionArea == null ? "" : descriptionArea.getText();

			if (nom == null || nom.trim().isEmpty()) {
				setError("Nom obligatoire");
				return;
			}
			if (categorie == null || categorie.trim().isEmpty()) {
				setError("Catégorie obligatoire");
				return;
			}

			double prix;
			try {
				prix = Double.parseDouble(prixTxt.trim());
			} catch (Exception ex) {
				setError("Prix invalide");
				return;
			}
			if (prix < 0) {
				setError("Prix invalide");
				return;
			}
			if (stock == null || stock < 0) {
				setError("Stock invalide");
				return;
			}

			Produit p = new Produit();
			p.setNom(nom.trim());
			p.setCategorie(categorie.trim());
			p.setPrix(prix);
			p.setStock(stock);
			p.setDescription(description == null ? "" : description.trim());
			p.setActive(true);
			p.setCreatedAt(LocalDateTime.now());
			p.setPopularite(0);

			service.save(p);

			Alert ok = new Alert(Alert.AlertType.INFORMATION);
			ok.setTitle("Produit");
			ok.setHeaderText(null);
			ok.setContentText("Produit ajouté avec succès.");
			ok.showAndWait();

			goToProduits();
		} catch (Exception e) {
			setError(e.getMessage());
		}
	}

	@FXML
	public void goToProduits() {
		navigate("/com/chroniccare/Admin/ProduitsDashboard.fxml");
	}

	private void navigate(String fxml) {
		try {
			Parent root = FXMLLoader.load(getClass().getResource(fxml));
			nomField.getScene().setRoot(root);
		} catch (Exception e) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("ChronicCare");
			alert.setHeaderText("Navigation échouée");
			alert.setContentText(e.getMessage());
			alert.showAndWait();
		}
	}

	private void clearError() {
		if (errorLabel != null) {
			errorLabel.setText("");
		}
	}

	private void setError(String msg) {
		if (errorLabel != null) {
			errorLabel.setText(msg == null ? "Erreur" : msg);
		} else {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Produit");
			alert.setHeaderText("Erreur");
			alert.setContentText(msg);
			alert.showAndWait();
		}
	}
}
