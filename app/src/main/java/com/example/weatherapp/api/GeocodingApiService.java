package com.example.weatherapp.api;

import com.example.weatherapp.model.GeocodingResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface για το Open-Meteo Geocoding API.
 * Αναζήτηση πόλης με βάση το όνομα.
 *
 * Παράδειγμα URL:
 * https://geocoding-api.open-meteo.com/v1/search?name=Athens&count=5
 */
public interface GeocodingApiService {

    @GET("search")
    Call<GeocodingResponse> searchCity(
            @Query("name") String name,
            @Query("count") int count
    );
}
