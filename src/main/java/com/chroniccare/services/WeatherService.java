package com.chroniccare.services;

import com.chroniccare.models.Weather;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class WeatherService {
    private static final String GEO_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String COUNTRY_CODE = "TN";

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    // Mapping: petites villes -> grandes villes
    private static final Map<String, String> TUNISIA_CITY_FALLBACK = new HashMap<>();

    static {
        // Banlieues de Tunis
        TUNISIA_CITY_FALLBACK.put("bardo", "Tunis");
        TUNISIA_CITY_FALLBACK.put("ariana", "Tunis");
        TUNISIA_CITY_FALLBACK.put("la marsa", "Tunis");
        TUNISIA_CITY_FALLBACK.put("la goulette", "Tunis");
        TUNISIA_CITY_FALLBACK.put("goulette", "Tunis");
        TUNISIA_CITY_FALLBACK.put("le kram", "Tunis");
        TUNISIA_CITY_FALLBACK.put("kram", "Tunis");
        TUNISIA_CITY_FALLBACK.put("rades", "Tunis");
        TUNISIA_CITY_FALLBACK.put("sidi bou said", "Tunis");
        TUNISIA_CITY_FALLBACK.put("carthage", "Tunis");
        TUNISIA_CITY_FALLBACK.put("manouba", "Tunis");
        TUNISIA_CITY_FALLBACK.put("ben arous", "Tunis");
        TUNISIA_CITY_FALLBACK.put("megrine", "Tunis");
        TUNISIA_CITY_FALLBACK.put("mornaguia", "Tunis");
        TUNISIA_CITY_FALLBACK.put("el menzah", "Tunis");
        TUNISIA_CITY_FALLBACK.put("menzah", "Tunis");
        TUNISIA_CITY_FALLBACK.put("sidi daoudi", "Tunis");
        TUNISIA_CITY_FALLBACK.put("youssofia", "Tunis");

        // Banlieues de Sfax
        TUNISIA_CITY_FALLBACK.put("sakiet ezzit", "Sfax");
        TUNISIA_CITY_FALLBACK.put("thyna", "Sfax");

        // Banlieues de Sousse
        TUNISIA_CITY_FALLBACK.put("msaken", "Sousse");
        TUNISIA_CITY_FALLBACK.put("skhira", "Sousse");
        TUNISIA_CITY_FALLBACK.put("hammam sousse", "Sousse");

        // Autres communes
        TUNISIA_CITY_FALLBACK.put("bizerte", "Tunis");
        TUNISIA_CITY_FALLBACK.put("menzel bourguiba", "Tunis");
        TUNISIA_CITY_FALLBACK.put("nabeul", "Sousse");
        TUNISIA_CITY_FALLBACK.put("hammamet", "Sousse");
        TUNISIA_CITY_FALLBACK.put("monastir", "Sousse");
        TUNISIA_CITY_FALLBACK.put("kairouan", "Kairouan");
        TUNISIA_CITY_FALLBACK.put("gafsa", "Gafsa");
        TUNISIA_CITY_FALLBACK.put("tataouine", "Tataouine");
        TUNISIA_CITY_FALLBACK.put("djerba", "Djerba");
        TUNISIA_CITY_FALLBACK.put("tozeur", "Tozeur");
        TUNISIA_CITY_FALLBACK.put("kasserine", "Kasserine");
        TUNISIA_CITY_FALLBACK.put("sidi bouzid", "Sidi Bouzid");
    }

    public static Optional<Weather> getWeatherByCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            System.err.println("[WeatherService] Erreur: ville null ou vide");
            return Optional.empty();
        }

        String originalCity = city.trim();
        String simplifiedCity = extractPrimaryLocation(originalCity);
        String normalizedCity = normalizeCityKey(simplifiedCity);
        String targetCity = TUNISIA_CITY_FALLBACK.getOrDefault(normalizedCity, simplifiedCity);

        if (!targetCity.equalsIgnoreCase(simplifiedCity)) {
            System.out.println("[WeatherService] '" + originalCity + "' -> fallback vers '" + targetCity + "'");
        }

        return getWeatherFromApi(targetCity, originalCity);
    }

    private static Optional<Weather> getWeatherFromApi(String city, String originalCity) {
        try {
            Optional<LocationResult> location = geocodeCity(city);
            if (location.isEmpty()) {
                System.err.println("[WeatherService] Ville introuvable via Open-Meteo: " + city + ", " + COUNTRY_CODE);
                Optional<LocationResult> nominatimLocation = geocodeWithNominatim(originalCity);
                if (nominatimLocation.isEmpty()) {
                    return Optional.empty();
                }

                System.out.println("[WeatherService] Fallback coordonnees Nominatim pour: " + originalCity);
                return fetchCurrentWeather(nominatimLocation.get(), originalCity);
            }

            System.out.println("[WeatherService] Appel API pour: " + originalCity);
            return fetchCurrentWeather(location.get(), originalCity);
        } catch (Exception e) {
            System.err.println("[WeatherService] Exception: " + e.getClass().getSimpleName());
            System.err.println("[WeatherService] Message: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static Optional<LocationResult> geocodeCity(String city) throws Exception {
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = String.format("%s?name=%s&count=1&language=fr&countryCode=%s",
                GEO_URL, encodedCity, COUNTRY_CODE);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.err.println("[WeatherService] Erreur geocoding: " + response.statusCode());
            return Optional.empty();
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        if (json == null || !json.has("results")) {
            return Optional.empty();
        }

        JsonArray results = json.getAsJsonArray("results");
        if (results == null || results.isEmpty()) {
            return Optional.empty();
        }

        JsonObject first = results.get(0).getAsJsonObject();
        String name = first.get("name").getAsString();
        double latitude = first.get("latitude").getAsDouble();
        double longitude = first.get("longitude").getAsDouble();
        return Optional.of(new LocationResult(name, latitude, longitude));
    }

    private static Optional<LocationResult> geocodeWithNominatim(String city) {
        Optional<LocationService.LocationSuggestion> suggestion = LocationService.findBestMatchInTunisia(city);
        if (suggestion.isEmpty() || !suggestion.get().hasCoordinates()) {
            System.err.println("[WeatherService] Lieu introuvable via Nominatim: " + city);
            return Optional.empty();
        }

        LocationService.LocationSuggestion match = suggestion.get();
        String resolvedName = (match.name() != null && !match.name().isBlank()) ? match.name() : city;
        return Optional.of(new LocationResult(resolvedName, match.latitude(), match.longitude()));
    }

    private static Optional<Weather> fetchCurrentWeather(LocationResult location, String originalCity) throws Exception {
        String url = String.format(
                "%s?latitude=%s&longitude=%s&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code&timezone=auto",
                FORECAST_URL,
                location.latitude(),
                location.longitude()
        );

        System.out.println("[WeatherService] URL: " + url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[WeatherService] Reponse status: " + response.statusCode());

        if (response.statusCode() != 200) {
            System.err.println("[WeatherService] Erreur API meteo: " + response.statusCode());
            return Optional.empty();
        }

        JsonObject root = gson.fromJson(response.body(), JsonObject.class);
        if (root == null || !root.has("current")) {
            return Optional.empty();
        }

        JsonObject current = root.getAsJsonObject("current");
        double temperature = current.get("temperature_2m").getAsDouble();
        double feelsLike = current.get("apparent_temperature").getAsDouble();
        int humidity = current.get("relative_humidity_2m").getAsInt();
        double windSpeed = current.get("wind_speed_10m").getAsDouble();
        int weatherCode = current.get("weather_code").getAsInt();
        String main = mapWeatherMain(weatherCode);
        String description = mapWeatherDescription(weatherCode);

        Weather weather = new Weather(location.name(), temperature, feelsLike, description, main, humidity, windSpeed, "");
        System.out.println("[WeatherService] Meteo obtenue pour " + originalCity + ": " + weather);
        return Optional.of(weather);
    }

    private static String mapWeatherMain(int code) {
        if (code == 0) return "Soleil";
        if (code <= 3) return "Nuages";
        if (code <= 48) return "Brouillard";
        if (code <= 67) return "Pluie";
        if (code <= 77) return "Neige";
        if (code <= 99) return "Orage";
        return "Meteo";
    }

    private static String mapWeatherDescription(int code) {
        return switch (code) {
            case 0 -> "Ciel degage";
            case 1 -> "Principalement degage";
            case 2 -> "Partiellement nuageux";
            case 3 -> "Couvert";
            case 45, 48 -> "Brouillard";
            case 51, 53, 55 -> "Bruine";
            case 56, 57 -> "Bruine verglacante";
            case 61, 63, 65 -> "Pluie";
            case 66, 67 -> "Pluie verglacante";
            case 71, 73, 75, 77 -> "Neige";
            case 80, 81, 82 -> "Averses";
            case 85, 86 -> "Averses de neige";
            case 95 -> "Orage";
            case 96, 99 -> "Orage avec grele";
            default -> "Condition meteo";
        };
    }

    private static String extractPrimaryLocation(String city) {
        String[] separators = {",", "-", "|", ";"};
        String result = city;
        for (String separator : separators) {
            int index = result.indexOf(separator);
            if (index > 0) {
                result = result.substring(0, index);
                break;
            }
        }
        return result.trim();
    }

    private static String normalizeCityKey(String city) {
        String normalized = Normalizer.normalize(city == null ? "" : city, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
        if (normalized.startsWith("el ")) {
            normalized = normalized.substring(3);
        } else if (normalized.startsWith("le ")) {
            normalized = normalized.substring(3);
        }
        return normalized;
    }

    private record LocationResult(String name, double latitude, double longitude) {
    }

    public static boolean isWeatherAvailable() {
        return true;
    }
}
