package com.chroniccare.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class OpenAiSuggestionService {
    private static final String API_URL = "https://api.openai.com/v1/responses";
    private static final String MODEL = "gpt-5.4-mini";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public String suggestEventDescription(
            String title,
            String status,
            String location,
            LocalDateTime start,
            LocalDateTime end
    ) throws Exception {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("Titre", valueOrFallback(title));
        context.put("Statut", valueOrFallback(status));
        context.put("Lieu", valueOrFallback(location));
        context.put("Date debut", formatDateTime(start));
        context.put("Date fin", formatDateTime(end));

        String prompt = buildPrompt(
                "Tu rediges des descriptions d'evenements de sante et d'activite physique pour une application patient.",
                "Redige une description en francais, professionnelle, rassurante et concise.",
                "La description doit tenir en 2 a 4 phrases, decrire l'objectif de la session, l'ambiance ou le niveau attendu, et rester claire pour un patient.",
                context
        );
        return generateText(prompt);
    }

    public String suggestExerciseDescription(
            String name,
            String durationMinutes,
            String repetitions,
            String eventTitle
    ) throws Exception {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("Nom", valueOrFallback(name));
        context.put("Duree (minutes)", valueOrFallback(durationMinutes));
        context.put("Repetitions", valueOrFallback(repetitions));
        context.put("Evenement lie", valueOrFallback(eventTitle));

        String prompt = buildPrompt(
                "Tu rediges des descriptions d'exercices pour une application de suivi patient.",
                "Redige une description en francais, simple, motivante et precise.",
                "La description doit tenir en 2 ou 3 phrases, expliquer l'objectif de l'exercice, le rythme attendu ou le niveau, et donner une consigne utile au patient.",
                context
        );
        return generateText(prompt);
    }

    private String generateText(String prompt) throws Exception {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Cle API OpenAI introuvable. Definis OPENAI_API_KEY ou -Dopenai.api.key.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", MODEL);
        payload.put("input", prompt);
        payload.put("max_output_tokens", 220);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject root = GSON.fromJson(response.body(), JsonObject.class);

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(extractErrorMessage(root, response.statusCode()));
        }

        String text = extractResponseText(root);
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Aucune suggestion n'a ete retournee par l'API OpenAI.");
        }
        return text.trim();
    }

    private String buildPrompt(String roleInstruction, String taskInstruction, String qualityInstruction, Map<String, String> context) {
        StringBuilder builder = new StringBuilder();
        builder.append(roleInstruction).append('\n');
        builder.append(taskInstruction).append('\n');
        builder.append(qualityInstruction).append('\n');
        builder.append("Ne renvoie que le texte final, sans titre, sans puces, sans commentaire meta.").append('\n');
        builder.append('\n');
        builder.append("Contexte:").append('\n');
        for (Map.Entry<String, String> entry : context.entrySet()) {
            builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        return builder.toString();
    }

    private String extractResponseText(JsonObject root) {
        if (root == null) {
            return "";
        }
        if (root.has("output_text") && !root.get("output_text").isJsonNull()) {
            return root.get("output_text").getAsString();
        }
        if (!root.has("output") || root.get("output").isJsonNull()) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        JsonArray outputArray = root.getAsJsonArray("output");
        for (JsonElement outputElement : outputArray) {
            if (!outputElement.isJsonObject()) {
                continue;
            }
            JsonObject outputObject = outputElement.getAsJsonObject();
            if (!outputObject.has("content") || outputObject.get("content").isJsonNull()) {
                continue;
            }
            JsonArray contentArray = outputObject.getAsJsonArray("content");
            for (JsonElement contentElement : contentArray) {
                if (!contentElement.isJsonObject()) {
                    continue;
                }
                JsonObject contentObject = contentElement.getAsJsonObject();
                if (!contentObject.has("text") || contentObject.get("text").isJsonNull()) {
                    continue;
                }

                JsonElement textElement = contentObject.get("text");
                if (textElement.isJsonPrimitive()) {
                    text.append(textElement.getAsString());
                } else if (textElement.isJsonObject()) {
                    JsonObject textObject = textElement.getAsJsonObject();
                    if (textObject.has("value") && !textObject.get("value").isJsonNull()) {
                        text.append(textObject.get("value").getAsString());
                    }
                }
            }
        }
        return text.toString();
    }

    private String extractErrorMessage(JsonObject root, int statusCode) {
        if (root != null && root.has("error") && root.get("error").isJsonObject()) {
            JsonObject error = root.getAsJsonObject("error");
            String code = getOptionalString(error, "code");
            String type = getOptionalString(error, "type");
            String message = getOptionalString(error, "message");
            return formatApiError(statusCode, code, type, message);
        }
        return "OpenAI API (" + statusCode + "): echec de la generation.";
    }

    private String resolveApiKey() {
        String propertyKey = System.getProperty("openai.api.key");
        if (propertyKey != null && !propertyKey.isBlank()) {
            return propertyKey.trim();
        }
        String envKey = System.getenv("OPENAI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        return null;
    }

    private String getOptionalString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return object.get(key).getAsString();
    }

    private String formatApiError(int statusCode, String code, String type, String message) {
        if ("insufficient_quota".equals(code)) {
            return "OpenAI API (" + statusCode + ", insufficient_quota): quota/billing indisponible pour cette cle de projet. "
                    + "Verifie le projet API, le budget et la facturation sur platform.openai.com.";
        }
        if ("rate_limit_exceeded".equals(code)) {
            return "OpenAI API (" + statusCode + ", rate_limit_exceeded): limite de requetes atteinte. Reessaie dans quelques instants.";
        }

        StringBuilder builder = new StringBuilder("OpenAI API (").append(statusCode);
        if (code != null && !code.isBlank()) {
            builder.append(", ").append(code);
        } else if (type != null && !type.isBlank()) {
            builder.append(", ").append(type);
        }
        builder.append(")");
        if (message != null && !message.isBlank()) {
            builder.append(": ").append(message);
        } else {
            builder.append(": echec de la generation.");
        }
        return builder.toString();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "Non precise" : value.format(DATE_TIME_FORMATTER);
    }

    private String valueOrFallback(String value) {
        return value == null || value.isBlank() ? "Non precise" : value.trim();
    }
}
