package com.chroniccare.services;

import com.chroniccare.models.HolidayInfo;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HolidayService {
    private static final String BASE_URL = "https://date.nager.at/api/v3/PublicHolidays/%d/%s";
    private static final String COUNTRY_CODE = "TN";

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    private static final Map<Integer, Map<LocalDate, String>> HOLIDAY_CACHE = new HashMap<>();

    public static Optional<String> getHolidayName(LocalDate date) {
        if (date == null) {
            return Optional.empty();
        }

        Map<LocalDate, String> holidays = getHolidaysByYear(date.getYear());
        if (holidays.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(holidays.get(date));
    }

    private static Map<LocalDate, String> getHolidaysByYear(int year) {
        if (HOLIDAY_CACHE.containsKey(year)) {
            return HOLIDAY_CACHE.get(year);
        }

        Map<LocalDate, String> holidays = fetchHolidaysByYear(year);
        HOLIDAY_CACHE.put(year, holidays);
        return holidays;
    }

    private static Map<LocalDate, String> fetchHolidaysByYear(int year) {
        Map<LocalDate, String> holidays = new HashMap<>();
        String url = String.format(BASE_URL, year, COUNTRY_CODE);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("[HolidayService] Erreur API jours fériés: " + response.statusCode());
                return holidays;
            }

            JsonArray array = GSON.fromJson(response.body(), JsonArray.class);
            if (array == null) {
                return holidays;
            }

            for (JsonElement element : array) {
                JsonObject holiday = element.getAsJsonObject();
                LocalDate holidayDate = LocalDate.parse(holiday.get("date").getAsString());
                String localName = getTextOrEmpty(holiday, "localName");
                String name = getTextOrEmpty(holiday, "name");
                String label = !localName.isBlank() ? localName : name;
                holidays.put(holidayDate, label);
            }
        } catch (Exception e) {
            System.err.println("[HolidayService] Erreur récupération jours fériés: " + e.getMessage());
        }

        return holidays;
    }

    private static String getTextOrEmpty(JsonObject json, String key) {
        if (json == null || key == null || !json.has(key) || json.get(key).isJsonNull()) {
            return "";
        }
        return json.get(key).getAsString();
    }

    /** Instance : week-end (samedi / dimanche). Utilisé par les écrans RDV. */
    public boolean isWeekend(LocalDate date) {
        if (date == null) {
            return false;
        }
        DayOfWeek d = date.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    /**
     * Instance : jour férié pour la date (même source que {@link #getHolidayName(LocalDate)}).
     */
    public Optional<HolidayInfo> findHoliday(LocalDate date) {
        Optional<String> label = getHolidayName(date);
        return label.map(s -> new HolidayInfo(date, s, s));
    }
}

