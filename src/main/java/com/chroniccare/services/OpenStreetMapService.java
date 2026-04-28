package com.chroniccare.services;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenStreetMapService {

    private static final Pattern LAT_PATTERN = Pattern.compile("\"lat\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern LON_PATTERN = Pattern.compile("\"lon\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public Optional<double[]> geocode(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }

        try {
            String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=" + encoded;

            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .timeout(Duration.ofSeconds(8))
                    .uri(URI.create(url))
                    .header("User-Agent", "chroniccare-javafx/1.0 (support@chroniccare.local)")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }

            String body = response.body();
            Matcher latMatcher = LAT_PATTERN.matcher(body);
            Matcher lonMatcher = LON_PATTERN.matcher(body);
            if (!latMatcher.find() || !lonMatcher.find()) {
                return Optional.empty();
            }

            double lat = Double.parseDouble(latMatcher.group(1));
            double lon = Double.parseDouble(lonMatcher.group(1));
            return Optional.of(new double[] { lat, lon });
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public String buildSearchQuery(String adresse, String ville, String codePostal) {
        StringBuilder query = new StringBuilder();
        appendPart(query, adresse);
        appendPart(query, ville);
        appendPart(query, codePostal);
        appendPart(query, "Tunisia");
        return query.toString();
    }

    public String buildMapUrl(String adresse, String ville, String codePostal) {
        String query = buildSearchQuery(adresse, ville, codePostal);
        Optional<double[]> geo = geocode(query);
        if (geo.isPresent()) {
            double[] point = geo.get();
            return "https://www.openstreetmap.org/?mlat=" + point[0] + "&mlon=" + point[1] + "#map=15/" + point[0] + "/"
                    + point[1];
        }

        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return "https://www.openstreetmap.org/search?query=" + encoded;
    }

    public void openInBrowser(String url) throws IOException {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL invalide");
        }
        if (!Desktop.isDesktopSupported()) {
            throw new IOException("Desktop non supporte sur ce systeme");
        }
        Desktop.getDesktop().browse(URI.create(url));
    }

    private void appendPart(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(value.trim());
    }
}
