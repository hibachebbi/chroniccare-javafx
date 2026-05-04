package com.chroniccare.modules.suivi.service;

import com.chroniccare.modules.suivi.model.Activite;
import com.chroniccare.modules.suivi.model.Etat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

public class OpenAiHealthReportService {
    private static final Path OPENAI_PROPERTIES_PATH = Path.of("openai.properties");
    private static final Path SMTP_PROPERTIES_PATH = Path.of("smtp.properties");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String DEFAULT_MODEL = "gpt-5.4-mini";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateHealthRemark(String patientLabel, List<Etat> etats, List<Activite> activites) {
        if ((etats == null || etats.isEmpty()) && (activites == null || activites.isEmpty())) {
            throw new IllegalStateException("Aucune donnee recente disponible pour generer une synthese IA.");
        }

        OpenAiConfig config = OpenAiConfig.load();
        String userPrompt = buildUserPrompt(patientLabel, etats, activites);

        try {
            ObjectNode payloadNode = objectMapper.createObjectNode();
            payloadNode.put("model", config.model());
            payloadNode.set("input", objectMapper.createArrayNode()
                    .add(objectMapper.createObjectNode()
                            .put("role", "system")
                            .set("content", objectMapper.createArrayNode()
                                    .add(objectMapper.createObjectNode()
                                            .put("type", "input_text")
                                            .put("text",
                                                    "Tu es un assistant de suivi sante pour Chronic Care. " +
                                                    "Tu rediges une synthese en francais a partir des donnees fournies. " +
                                                    "Reste prudent: ne pose pas de diagnostic, ne remplace pas un medecin, " +
                                                    "et limite-toi a une synthese breve de 3 a 5 phrases avec conseils generaux."))))
                    .add(objectMapper.createObjectNode()
                            .put("role", "user")
                            .set("content", objectMapper.createArrayNode()
                                    .add(objectMapper.createObjectNode()
                                            .put("type", "input_text")
                                            .put("text", userPrompt)))));
            payloadNode.set("text", objectMapper.createObjectNode()
                    .set("format", objectMapper.createObjectNode()
                            .put("type", "text")));
            String payload = objectMapper.writeValueAsString(payloadNode);

            HttpRequest request = HttpRequest.newBuilder(config.apiUrl())
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(buildFriendlyErrorMessage(response.statusCode(), response.body()));
            }

            return extractOutputText(response.body());
        } catch (IOException ex) {
            throw new IllegalStateException("Generation OpenAI impossible: connexion reseau echouee.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Generation OpenAI interrompue.", ex);
        }
    }

    private String buildUserPrompt(String patientLabel, List<Etat> etats, List<Activite> activites) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Patient: ").append(blankOr(patientLabel, "Patient inconnu")).append("\n\n");
        prompt.append("Etats de sante recents:\n");
        if (etats == null || etats.isEmpty()) {
            prompt.append("- Aucun etat disponible.\n");
        } else {
            for (Etat etat : etats) {
                prompt.append("- Date: ").append(etat.getDateReleve() == null ? "-" : etat.getDateReleve().toLocalDate().format(DATE_FORMAT))
                        .append(" | Traitement: ").append(blankOr(etat.getTraitementEnCours(), "-"))
                        .append(" | Temperature: ").append(blankOr(etat.getTemperatureCorporelle(), "-"))
                        .append(" | Hydratation: ").append(blankOr(etat.getNiveauHydratation(), "-"))
                        .append(" | Remarques: ").append(blankOr(etat.getRemarquesCliniques(), "-"))
                        .append("\n");
            }
        }

        prompt.append("\nActivites recentes:\n");
        if (activites == null || activites.isEmpty()) {
            prompt.append("- Aucune activite disponible.\n");
        } else {
            for (Activite activite : activites) {
                prompt.append("- Date: ").append(activite.getDateActivite() == null ? "-" : activite.getDateActivite().format(DATE_FORMAT))
                        .append(" | Type: ").append(blankOr(activite.getType(), "-"))
                        .append(" | Duree: ").append(activite.getDuree() == null ? "-" : activite.getDuree() + " min")
                        .append(" | Calories: ").append(activite.getCalories() == null ? "-" : activite.getCalories() + " kcal")
                        .append(" | Distance: ").append(activite.getDistanceKm() == null ? "-" : activite.getDistanceKm() + " km")
                        .append(" | Repos: ").append(activite.getHeuresRepos() == null ? "-" : activite.getHeuresRepos() + " h")
                        .append(" | Note: ").append(blankOr(activite.getNotes(), "-"))
                        .append("\n");
            }
        }

        prompt.append("\nConsigne de sortie:\n");
        prompt.append("- Ecris uniquement la synthese finale.\n");
        prompt.append("- 3 a 5 phrases.\n");
        prompt.append("- Francais simple et professionnel.\n");
        prompt.append("- Mentionne les points de vigilance si necessaire.\n");
        prompt.append("- Termine par une phrase rappelant que cela ne remplace pas un avis medical si la situation semble fragile.\n");
        return prompt.toString();
    }

    private String extractOutputText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode outputText = root.path("output_text");
        if (outputText.isTextual() && !outputText.asText().isBlank()) {
            return outputText.asText().trim();
        }

        for (JsonNode outputItem : root.path("output")) {
            if (!"message".equals(outputItem.path("type").asText())) {
                continue;
            }
            for (JsonNode contentItem : outputItem.path("content")) {
                if ("output_text".equals(contentItem.path("type").asText())) {
                    String text = contentItem.path("text").asText("");
                    if (!text.isBlank()) {
                        return text.trim();
                    }
                }
            }
        }
        throw new IllegalStateException("OpenAI n'a retourne aucun texte exploitable pour la synthese.");
    }

    private String buildFriendlyErrorMessage(int statusCode, String responseBody) {
        String body = responseBody == null ? "" : responseBody.toLowerCase();
        if (statusCode == 401 || statusCode == 403) {
            return "Cle API OpenAI invalide ou refusee. Configurez OPENAI_API_KEY ou CC_OPENAI_API_KEY.";
        }
        if (statusCode == 429) {
            return "Limite OpenAI atteinte. Reessayez dans un instant.";
        }
        if (statusCode == 400 && body.contains("model")) {
            return "Modele OpenAI invalide. Verifiez CC_OPENAI_MODEL.";
        }
        return "Generation OpenAI impossible (code " + statusCode + ").";
    }

    private String blankOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record OpenAiConfig(URI apiUrl, String apiKey, String model) {
        private static OpenAiConfig load() {
            Properties openAiProperties = loadPropertiesFile(OPENAI_PROPERTIES_PATH);
            Properties smtpProperties = loadPropertiesFile(SMTP_PROPERTIES_PATH);

            String apiUrl = firstValue("OPENAI_API_URL", openAiProperties, smtpProperties, "https://api.openai.com/v1/responses");
            String apiKey = requiredValue(openAiProperties, smtpProperties);
            String model = firstValue("CC_OPENAI_MODEL", openAiProperties, smtpProperties, DEFAULT_MODEL);
            return new OpenAiConfig(URI.create(apiUrl), apiKey, model);
        }

        private static String requiredValue(Properties openAiProperties, Properties smtpProperties) {
            String value = firstNonBlank(
                    System.getenv("OPENAI_API_KEY"),
                    System.getenv("CC_OPENAI_API_KEY"),
                    openAiProperties.getProperty("OPENAI_API_KEY"),
                    openAiProperties.getProperty("CC_OPENAI_API_KEY"),
                    smtpProperties.getProperty("OPENAI_API_KEY"),
                    smtpProperties.getProperty("CC_OPENAI_API_KEY"));
            if (value == null || value.contains("YOUR_API")) {
                throw new IllegalStateException("Configuration OpenAI manquante: ajoutez OPENAI_API_KEY ou CC_OPENAI_API_KEY.");
            }
            return value.trim();
        }

        private static String firstValue(String name, Properties openAiProperties, Properties smtpProperties, String defaultValue) {
            String value = firstNonBlank(
                    System.getenv(name),
                    openAiProperties.getProperty(name),
                    smtpProperties.getProperty(name));
            return value == null ? defaultValue : value.trim();
        }

        private static String firstNonBlank(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return null;
        }

        private static Properties loadPropertiesFile(Path path) {
            Properties properties = new Properties();
            if (!Files.exists(path)) {
                return properties;
            }

            try (InputStream inputStream = Files.newInputStream(path)) {
                properties.load(inputStream);
                return properties;
            } catch (IOException ex) {
                throw new IllegalStateException("Impossible de lire " + path.getFileName() + ": " + ex.getMessage(), ex);
            }
        }
    }
}
