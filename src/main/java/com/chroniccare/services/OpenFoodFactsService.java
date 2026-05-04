package com.chroniccare.services;

import com.chroniccare.models.OpenFoodFactsProduct;
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
import java.util.Optional;

public class OpenFoodFactsService {
    private static final String SEARCH_URL = "https://world.openfoodfacts.org/cgi/search.pl?search_simple=1&json=1&page_size=1&search_terms=";
    private static final int MAX_ATTEMPTS = 2;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public Optional<OpenFoodFactsProduct> searchFirstProduct(String searchTerm) throws IOException, InterruptedException {
        if (searchTerm == null || searchTerm.isBlank()) {
            return Optional.empty();
        }

        String url = SEARCH_URL + URLEncoder.encode(searchTerm.trim(), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "ChronicCare-JavaFX/1.0 (student project)")
                .GET()
                .build();

        HttpResponse<String> response = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                break;
            }
            if (!isRetryableStatus(response.statusCode()) || attempt == MAX_ATTEMPTS) {
                throw new IOException(buildErrorMessage(response.statusCode()));
            }
            Thread.sleep(500L);
        }
        if (response == null || response.statusCode() != 200) {
            throw new IOException("Open Food Facts est temporairement indisponible. Veuillez reessayer plus tard.");
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray products = root.getAsJsonArray("products");
        if (products == null || products.isEmpty()) {
            return Optional.empty();
        }

        JsonObject productJson = products.get(0).getAsJsonObject();
        return Optional.of(mapProduct(productJson));
    }

    private OpenFoodFactsProduct mapProduct(JsonObject productJson) {
        OpenFoodFactsProduct product = new OpenFoodFactsProduct();
        product.setProductName(getString(productJson, "product_name"));
        product.setBrands(getString(productJson, "brands"));
        product.setNutriScoreGrade(getString(productJson, "nutriscore_grade"));
        product.setIngredientsText(getString(productJson, "ingredients_text"));
        product.setImageUrl(getString(productJson, "image_url"));

        JsonObject nutriments = productJson.getAsJsonObject("nutriments");
        if (nutriments != null) {
            product.setEnergyKcal100g(getDouble(nutriments, "energy-kcal_100g"));
            product.setSugars100g(getDouble(nutriments, "sugars_100g"));
            product.setFat100g(getDouble(nutriments, "fat_100g"));
            product.setProteins100g(getDouble(nutriments, "proteins_100g"));
            product.setFiber100g(getDouble(nutriments, "fiber_100g"));
        }
        return product;
    }

    private String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    private Double getDouble(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsDouble();
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private String buildErrorMessage(int statusCode) {
        if (statusCode == 503) {
            return "Open Food Facts est temporairement indisponible. Veuillez reessayer plus tard.";
        }
        if (statusCode == 429) {
            return "Open Food Facts limite temporairement les requetes. Reessayez dans quelques instants.";
        }
        return "Reponse Open Food Facts invalide : HTTP " + statusCode;
    }
}
