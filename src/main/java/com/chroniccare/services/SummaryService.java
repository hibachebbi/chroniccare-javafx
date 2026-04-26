package com.chroniccare.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SummaryService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile";
    private static final String API_KEY = chargerCle();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    // Charge la cle depuis config.properties (jamais commite sur Git)
    private static String chargerCle() {
        try (var is = SummaryService.class.getResourceAsStream("/com/chroniccare/config.properties")) {
            if (is == null) {
                System.err.println("config.properties introuvable !");
                return "";
            }
            java.util.Properties props = new java.util.Properties();
            props.load(is);
            return props.getProperty("groq.api.key", "");
        } catch (Exception e) {
            System.err.println("Erreur chargement cle : " + e.getMessage());
            return "";
        }
    }

    public String resumer(String titre, String contenu) {
        if (contenu == null || contenu.isBlank()) return "Contenu vide, impossible de resumer.";
        if (API_KEY.isBlank()) return "Cle API manquante. Verifie config.properties.";

        try {
            String prompt = "Voici une publication medicale intitulee : \""
                    + titre + "\"\n\nContenu :\n" + contenu
                    + "\n\nFais un resume clair et concis en 2-3 phrases maximum en francais. "
                    + "Mets en avant les points cles et le message principal. "
                    + "Reponds uniquement avec le resume, sans introduction ni conclusion.";

            String escaped = prompt
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "");

            String body = "{"
                    + "\"model\":\"" + MODEL + "\","
                    + "\"max_tokens\":200,"
                    + "\"temperature\":0.3,"
                    + "\"messages\":["
                    + "{\"role\":\"system\",\"content\":\"Tu es un assistant medical. Tu resumes des publications de maniere claire et concise en francais.\"},"
                    + "{\"role\":\"user\",\"content\":\"" + escaped + "\"}"
                    + "]"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String json = response.body();

            System.out.println("Groq status: " + response.statusCode());

            if (response.statusCode() != 200) {
                System.err.println("Erreur Groq API: " + json);
                return "Erreur API (" + response.statusCode() + "). Verifie ta cle Groq.";
            }

            String key = "\"content\":\"";
            int start = json.indexOf(key);
            if (start == -1) return "Impossible de parser la reponse.";
            start += key.length();

            StringBuilder result = new StringBuilder();
            int i = start;
            while (i < json.length()) {
                char ch = json.charAt(i);
                if (ch == '"') break;
                if (ch == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    switch (next) {
                        case 'n'  -> { result.append('\n'); i += 2; }
                        case 't'  -> { result.append('\t'); i += 2; }
                        case '"'  -> { result.append('"');  i += 2; }
                        case '\\' -> { result.append('\\'); i += 2; }
                        case 'u'  -> {
                            if (i + 5 < json.length()) {
                                try {
                                    int code = Integer.parseInt(json.substring(i + 2, i + 6), 16);
                                    result.append((char) code);
                                    i += 6;
                                } catch (NumberFormatException e) {
                                    result.append(ch); i++;
                                }
                            } else { result.append(ch); i++; }
                        }
                        default -> { result.append(ch); i++; }
                    }
                } else {
                    result.append(ch);
                    i++;
                }
            }

            String summary = result.toString().trim();
            return summary.isBlank() ? "Resume indisponible." : summary;

        } catch (Exception e) {
            System.err.println("Erreur SummaryService: " + e.getMessage());
            return "Erreur lors de la generation du resume.";
        }
    }
}