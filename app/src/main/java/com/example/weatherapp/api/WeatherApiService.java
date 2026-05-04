package com.example.weatherapp.api;

import com.example.weatherapp.model.WeatherResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface για το Open-Meteo Forecast API.
 * Δωρεάν, χωρίς API key: https://open-meteo.com/
 *
 * Παράδειγμα URL:
 * https://api.open-meteo.com/v1/forecast?latitude=37.97&longitude=23.72
 *   &current_weather=true&daily=weathercode,temperature_2m_max,temperature_2m_min
 *   &timezone=auto
 */
public interface WeatherApiService {

    @GET("forecast")
    Call<WeatherResponse> getForecast(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("current_weather") boolean currentWeather,
            @Query("daily") String daily,
            @Query("timezone") String timezone
    );
}
