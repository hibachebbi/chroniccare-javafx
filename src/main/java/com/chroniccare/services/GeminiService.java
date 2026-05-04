package com.chroniccare.services;

import com.chroniccare.models.ConsultationDraft;
import com.chroniccare.models.OpenFoodFactsProduct;
import com.chroniccare.utils.GeminiConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class GeminiService {
    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public ConsultationDraft generateConsultationDraft(String patientName,
                                                       String motif,
                                                       String followUpType,
                                                       String medicalContext,
                                                       String currentTheme,
                                                       String currentSummary,
                                                       String currentRecommendations,
                                                       String currentMealPlan,
                                                       String currentObjectives,
                                                       String currentMedicalNotes,
                                                       OpenFoodFactsProduct selectedProduct)
            throws IOException, InterruptedException {
        String apiKey = GeminiConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("Cle Gemini absente. Configure GEMINI_API_KEY ou config/gemini.properties.");
        }

        String model = GeminiConfig.getModel();
        String url = BASE_URL + URLEncoder.encode(model, StandardCharsets.UTF_8) +
                ":generateContent?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        JsonObject body = new JsonObject();
        body.add("systemInstruction", buildSystemInstruction());
        body.add("contents", buildContents(patientName, motif, followUpType, medicalContext, currentTheme,
                currentSummary, currentRecommendations, currentMealPlan, currentObjectives,
                currentMedicalNotes, selectedProduct));

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.4);
        generationConfig.addProperty("responseMimeType", "application/json");
        body.add("generationConfig", generationConfig);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Reponse Gemini invalide : HTTP " + response.statusCode() + " - " + response.body());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray candidates = root.getAsJsonArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IOException("Aucune reponse retournee par Gemini.");
        }

        JsonObject candidate = candidates.get(0).getAsJsonObject();
        JsonObject content = candidate.getAsJsonObject("content");
        if (content == null) {
            throw new IOException("Contenu Gemini manquant.");
        }

        JsonArray parts = content.getAsJsonArray("parts");
        if (parts == null || parts.isEmpty()) {
            throw new IOException("Texte Gemini manquant.");
        }

        String text = null;
        for (JsonElement partElement : parts) {
            JsonObject part = partElement.getAsJsonObject();
            if (part.has("text") && !part.get("text").isJsonNull()) {
                text = part.get("text").getAsString();
                break;
            }
        }
        if (text == null || text.isBlank()) {
            throw new IOException("Aucun texte exploitable retourne par Gemini.");
        }

        return gson.fromJson(text, ConsultationDraft.class);
    }

    private JsonObject buildSystemInstruction() {
        JsonObject instruction = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text",
                "Tu es un assistant de redaction pour des consultations nutritionnelles. " +
                        "Tu aides un nutritionniste a produire un contenu professionnel, clair, concis et utile au patient. " +
                        "Tu ne donnes pas de diagnostic medical. Tu n'inventes pas de donnees manquantes. " +
                        "Tu dois repondre uniquement en JSON valide avec les cles attendues.");
        parts.add(part);
        instruction.add("parts", parts);
        return instruction;
    }

    private JsonArray buildContents(String patientName,
                                    String motif,
                                    String followUpType,
                                    String medicalContext,
                                    String currentTheme,
                                    String currentSummary,
                                    String currentRecommendations,
                                    String currentMealPlan,
                                    String currentObjectives,
                                    String currentMedicalNotes,
                                    OpenFoodFactsProduct selectedProduct) {
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");

        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", buildPrompt(patientName, motif, followUpType, medicalContext, currentTheme,
                currentSummary, currentRecommendations, currentMealPlan, currentObjectives,
                currentMedicalNotes, selectedProduct));
        parts.add(part);

        content.add("parts", parts);
        contents.add(content);
        return contents;
    }

    private String buildPrompt(String patientName,
                               String motif,
                               String followUpType,
                               String medicalContext,
                               String currentTheme,
                               String currentSummary,
                               String currentRecommendations,
                               String currentMealPlan,
                               String currentObjectives,
                               String currentMedicalNotes,
                               OpenFoodFactsProduct selectedProduct) {
        StringBuilder builder = new StringBuilder();
        builder.append("Genere un brouillon de consultation nutritionnelle en francais.\n");
        builder.append("Reponds uniquement avec un JSON valide au format suivant:\n");
        builder.append("{\"theme\":\"...\",\"summary\":\"...\",\"recommendations\":\"...\",\"mealPlan\":\"...\",\"objectives\":\"...\",\"followUpMessage\":\"...\"}\n\n");
        builder.append("Contexte patient:\n");
        builder.append("- Patient: ").append(valueOrUnknown(patientName)).append('\n');
        builder.append("- Motif du rendez-vous: ").append(valueOrUnknown(motif)).append('\n');
        builder.append("- Type de suivi: ").append(valueOrUnknown(followUpType)).append('\n');
        builder.append("- Contexte medical: ").append(valueOrUnknown(medicalContext)).append('\n');
        builder.append('\n');
        builder.append("Contenu saisi actuellement par le nutritionniste:\n");
        builder.append("- Theme actuel: ").append(valueOrUnknown(currentTheme)).append('\n');
        builder.append("- Resume actuel: ").append(valueOrUnknown(currentSummary)).append('\n');
        builder.append("- Recommandations actuelles: ").append(valueOrUnknown(currentRecommendations)).append('\n');
        builder.append("- Plan alimentaire actuel: ").append(valueOrUnknown(currentMealPlan)).append('\n');
        builder.append("- Objectifs actuels: ").append(valueOrUnknown(currentObjectives)).append('\n');
        builder.append("- Notes medicales actuelles: ").append(valueOrUnknown(currentMedicalNotes)).append('\n');

        if (selectedProduct != null) {
            builder.append('\n');
            builder.append("Produit Open Food Facts a prendre en compte:\n");
            builder.append("- ").append(selectedProduct.toRecommendationSnippet()).append('\n');
            builder.append("- Ingredients: ").append(valueOrUnknown(selectedProduct.getIngredientsText())).append('\n');
        }

        builder.append('\n');
        builder.append("Contraintes:\n");
        builder.append("- Reste concret et professionnel.\n");
        builder.append("- N'invente pas de pathologies.\n");
        builder.append("- Si une information manque, formule des conseils generaux prudents.\n");
        builder.append("- Les objectifs doivent etre simples a suivre.\n");
        builder.append("- Reponds uniquement en JSON brut, sans markdown.\n");
        return builder.toString();
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "Non precise" : value.trim();
    }
}
