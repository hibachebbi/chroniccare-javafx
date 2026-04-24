package com.chroniccare;

import com.chroniccare.models.Weather;
import com.chroniccare.services.WeatherService;

import java.util.Optional;

/**
 * Démonstration simple du WeatherService
 * Test avec villes tunisiennes
 */
public class WeatherDemo {
    public static void main(String[] args) {
        System.out.println("=== Weather API Demo - Tunisie ===\n");

        // Villes tunisiennes
        String[] cities = {"Tunis", "Sfax", "Sousse", "Gafsa", "Kairouan"};

        for (String city : cities) {
            System.out.println("📍 " + city);
            Optional<Weather> weather = WeatherService.getWeatherByCity(city);

            if (weather.isPresent()) {
                Weather w = weather.get();
                System.out.println("  ✓ " + w);
                System.out.println("  📊 " + w.getWeatherDisplay());
            } else {
                System.out.println("  ✗ Données indisponibles");
            }
            System.out.println();
        }
    }
}
