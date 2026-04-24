package com.chroniccare.models;

import java.util.Locale;

public class Weather {
    private String city;
    private double temperature;
    private double feelsLike;
    private String description;
    private String main;
    private int humidity;
    private double windSpeed;
    private String icon;

    public Weather() {
    }

    public Weather(String city, double temperature, double feelsLike, String description, 
                   String main, int humidity, double windSpeed, String icon) {
        this.city = city;
        this.temperature = temperature;
        this.feelsLike = feelsLike;
        this.description = description;
        this.main = main;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.icon = icon;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getFeelsLike() {
        return feelsLike;
    }

    public void setFeelsLike(double feelsLike) {
        this.feelsLike = feelsLike;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMain() {
        return main;
    }

    public void setMain(String main) {
        this.main = main;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getWeatherDisplay() {
        return String.format(
                "%s  %s\nTemperature %.1f°C  •  Ressenti %.1f°C\nHumidite %d%%  •  Vent %.1f km/h",
                getConditionEmoji(),
                description != null ? description : "-",
                temperature,
                feelsLike,
                humidity,
                windSpeed
        );
    }

    public String getTemperatureDisplay() {
        return String.format("%.1f°C", temperature);
    }

    public String getPreviewDisplay() {
        return String.format(
                "%s  %s  •  %s",
                getConditionEmoji(),
                getTemperatureDisplay(),
                main != null ? main : "Météo"
        );
    }

    private String getConditionEmoji() {
        if (main == null) return "🌤️";
        String value = main.toLowerCase(Locale.ROOT);
        if (value.contains("soleil") || value.contains("clair")) return "☀️";
        if (value.contains("nuage")) return "⛅";
        if (value.contains("pluie") || value.contains("averse")) return "🌧️";
        if (value.contains("orage")) return "⛈️";
        if (value.contains("neige")) return "❄️";
        if (value.contains("brouillard")) return "🌫️";
        return "🌤️";
    }

    @Override
    public String toString() {
        return city + ": " + main + " - " + temperature + "°C";
    }
}
