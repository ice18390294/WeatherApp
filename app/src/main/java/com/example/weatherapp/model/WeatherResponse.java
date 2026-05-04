package com.example.weatherapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Αντιστοιχεί στο JSON response του Open-Meteo Forecast API.
 */
public class WeatherResponse {

    @SerializedName("current_weather")
    public CurrentWeather currentWeather;

    @SerializedName("daily")
    public DailyWeather daily;

    // Τρέχοντες καιρικές συνθήκες
    public static class CurrentWeather {

        @SerializedName("temperature")
        public double temperature;   // Θερμοκρασία σε °C

        @SerializedName("windspeed")
        public double windspeed;     // Ταχύτητα ανέμου σε km/h

        @SerializedName("weathercode")
        public int weathercode;      // WMO κωδικός καιρού
    }

    // Ημερήσια πρόγνωση
    public static class DailyWeather {

        @SerializedName("time")
        public List<String> time;              // Ημερομηνίες π.χ. ["2024-01-01", ...]

        @SerializedName("weathercode")
        public List<Integer> weathercode;      // WMO κωδικοί για κάθε μέρα

        @SerializedName("temperature_2m_max")
        public List<Double> temperatureMax;    // Μέγιστη θερμοκρασία

        @SerializedName("temperature_2m_min")
        public List<Double> temperatureMin;    // Ελάχιστη θερμοκρασία
    }
}
