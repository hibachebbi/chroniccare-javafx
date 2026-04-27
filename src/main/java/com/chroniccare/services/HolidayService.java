package com.chroniccare.services;

import com.chroniccare.models.HolidayInfo;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HolidayService {
    private static final String BASE_URL = "https://date.nager.at/api/v3/PublicHolidays/";
    private static final String DEFAULT_COUNTRY_CODE = "TN";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<Integer, Map<LocalDate, HolidayInfo>> cache = new HashMap<>();

    public Optional<HolidayInfo> findHoliday(LocalDate date) throws IOException, InterruptedException {
        Map<LocalDate, HolidayInfo> holidays = getHolidaysForYear(date.getYear());
        return Optional.ofNullable(holidays.get(date));
    }

    public boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    public String getCountryCode() {
        String override = System.getProperty("chroniccare.holiday.country");
        if (override == null || override.isBlank()) {
            return DEFAULT_COUNTRY_CODE;
        }
        return override.trim().toUpperCase();
    }

    private Map<LocalDate, HolidayInfo> getHolidaysForYear(int year) throws IOException, InterruptedException {
        if (cache.containsKey(year)) {
            return cache.get(year);
        }

        String url = BASE_URL + year + "/" + URLEncoder.encode(getCountryCode(), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "ChronicCare-JavaFX/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Reponse Nager.Date invalide : HTTP " + response.statusCode());
        }

        JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
        Map<LocalDate, HolidayInfo> holidays = new HashMap<>();
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            LocalDate holidayDate = LocalDate.parse(object.get("date").getAsString());
            String localName = getString(object, "localName");
            String name = getString(object, "name");
            holidays.put(holidayDate, new HolidayInfo(holidayDate, localName, name));
        }
        cache.put(year, holidays);
        return holidays;
    }

    private String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }
}
