package com.chroniccare.services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class TranslationService {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Traduit automatiquement FR->EN ou EN->FR.
     * Essaie FR->EN d'abord. Si le resultat est identique au texte original,
     * c'est que le texte est en anglais -> essaie EN->FR.
     */
    public String traduireAuto(String texte) {
        if (texte == null || texte.isBlank()) return texte;

        String r1 = appelerAPI(texte, "fr", "en");
        if (r1 != null && !r1.equalsIgnoreCase(texte.trim())) {
            return r1;
        }

        String r2 = appelerAPI(texte, "en", "fr");
        if (r2 != null && !r2.equalsIgnoreCase(texte.trim())) {
            return r2;
        }

        return texte;
    }

    private String appelerAPI(String texte, String from, String to) {
        try {
            // Tronquer a 450 chars (limite MyMemory)
            String cleaned = texte.trim();
            if (cleaned.length() > 450) cleaned = cleaned.substring(0, 450);

            // Encoder proprement le texte AVANT de construire l'URI
            String encodedQ      = URLEncoder.encode(cleaned, StandardCharsets.UTF_8);
            String encodedLangpair = URLEncoder.encode(from + "|" + to, StandardCharsets.UTF_8);

            // Construire l'URL avec des caracteres deja encodes -> URI.create() ne plante pas
            String url = "https://api.mymemory.translated.net/get"
                    + "?q=" + encodedQ
                    + "&langpair=" + encodedLangpair;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            // Parser translatedText sans librairie externe
            String key = "\"translatedText\":\"";
            int start = body.indexOf(key);
            if (start == -1) return null;
            start += key.length();
            int end = body.indexOf("\"", start);
            if (end == -1) return null;

            String result = body.substring(start, end)
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\\\", "\\");

            return result.isBlank() ? null : result;

        } catch (Exception e) {
            System.err.println("Erreur traduction [" + from + "->" + to + "] : " + e.getMessage());
            return null;
        }
    }
}