package com.example.weatherapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Αντιστοιχεί στο JSON response του Open-Meteo Geocoding API.
 */
public class GeocodingResponse {

    @SerializedName("results")
    public List<CityResult> results;

    // Αποτέλεσμα αναζήτησης για μία πόλη
    public static class CityResult {

        @SerializedName("id")
        public int id;

        @SerializedName("name")
        public String name;       // Όνομα πόλης

        @SerializedName("country")
        public String country;    // Χώρα

        @SerializedName("latitude")
        public double latitude;

        @SerializedName("longitude")
        public double longitude;

        @SerializedName("admin1")
        public String admin1;     // Περιφέρεια / Νομός
    }
}
