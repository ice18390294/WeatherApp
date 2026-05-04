package com.example.weatherapp.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton που παρέχει τα Retrofit clients για τα δύο APIs:
 * - Open-Meteo (καιρός)
 * - Open-Meteo Geocoding (αναζήτηση πόλης)
 */
public class RetrofitClient {

    private static final String WEATHER_BASE_URL = "https://api.open-meteo.com/v1/";
    private static final String GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com/v1/";

    private static Retrofit weatherRetrofit;
    private static Retrofit geocodingRetrofit;

    // Επιστρέφει το Retrofit client για το weather API
    public static Retrofit getWeatherClient() {
        if (weatherRetrofit == null) {
            weatherRetrofit = new Retrofit.Builder()
                    .baseUrl(WEATHER_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return weatherRetrofit;
    }

    // Επιστρέφει το Retrofit client για το geocoding API
    public static Retrofit getGeocodingClient() {
        if (geocodingRetrofit == null) {
            geocodingRetrofit = new Retrofit.Builder()
                    .baseUrl(GEOCODING_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return geocodingRetrofit;
    }
}
