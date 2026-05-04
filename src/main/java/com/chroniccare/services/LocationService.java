package com.chroniccare.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class LocationService {
    private static final String SEARCH_URL = "https://nominatim.openstreetmap.org/search";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();

    public static Optional<LocationSuggestion> findBestMatchInTunisia(String query) {
        if (query == null || query.trim().length() < 2) {
            return Optional.empty();
        }

        try {
            String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String url = String.format(
                    "%s?q=%s&countrycodes=tn&format=jsonv2&limit=1&addressdetails=1&accept-language=fr",
                    SEARCH_URL,
                    encodedQuery
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "chroniccare-javafx/1.0")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            JsonArray array = GSON.fromJson(response.body(), JsonArray.class);
            if (array == null || array.isEmpty()) {
                return Optional.empty();
            }

            JsonObject first = array.get(0).getAsJsonObject();
            String name = first.has("name") && !first.get("name").isJsonNull()
                    ? first.get("name").getAsString()
                    : "";
            String displayName = first.has("display_name") && !first.get("display_name").isJsonNull()
                    ? first.get("display_name").getAsString()
                    : name;
            double latitude = first.has("lat") && !first.get("lat").isJsonNull()
                    ? first.get("lat").getAsDouble()
                    : 0d;
            double longitude = first.has("lon") && !first.get("lon").isJsonNull()
                    ? first.get("lon").getAsDouble()
                    : 0d;
            return Optional.of(new LocationSuggestion(name, displayName, latitude, longitude));
        } catch (Exception e) {
            System.err.println("[LocationService] Erreur localisation: " + e.getMessage());
            return Optional.empty();
        }
    }

    public record LocationSuggestion(String name, String displayName, double latitude, double longitude) {
        public boolean hasCoordinates() {
            return latitude != 0d || longitude != 0d;
        }
    }
}

