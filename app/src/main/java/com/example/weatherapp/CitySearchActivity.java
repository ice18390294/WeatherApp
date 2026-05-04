package com.example.weatherapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.weatherapp.api.GeocodingApiService;
import com.example.weatherapp.api.RetrofitClient;
import com.example.weatherapp.model.GeocodingResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CitySearchActivity extends AppCompatActivity {

    private EditText etCitySearch;
    private Button btnSearch;
    private ListView lvResults;

    // Λίστα με τα αποτελέσματα αναζήτησης
    private final List<GeocodingResponse.CityResult> cityResults = new ArrayList<>();
    private final List<String> cityDisplayNames = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_city);

        // Ρύθμιση action bar με back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Αναζήτηση Πόλης");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etCitySearch = findViewById(R.id.etCitySearch);
        btnSearch = findViewById(R.id.btnSearch);
        lvResults = findViewById(R.id.lvResults);

        // Adapter για την εμφάνιση αποτελεσμάτων
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, cityDisplayNames);
        lvResults.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> searchCity());

        // Κλικ σε αποτέλεσμα → αποθήκευση πόλης
        lvResults.setOnItemClickListener((parent, view, position, id) ->
                selectCity(cityResults.get(position)));
    }

    // Αναζήτηση πόλης μέσω του Open-Meteo Geocoding API
    private void searchCity() {
        String query = etCitySearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Γράψτε το όνομα μιας πόλης", Toast.LENGTH_SHORT).show();
            return;
        }

        GeocodingApiService api =
                RetrofitClient.getGeocodingClient().create(GeocodingApiService.class);
        Call<GeocodingResponse> call = api.searchCity(query, 5);

        call.enqueue(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeocodingResponse> call,
                                   @NonNull Response<GeocodingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showResults(response.body().results);
                } else {
                    Toast.makeText(CitySearchActivity.this,
                            "Δεν βρέθηκαν αποτελέσματα", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<GeocodingResponse> call, @NonNull Throwable t) {
                Toast.makeText(CitySearchActivity.this,
                        "Σφάλμα σύνδεσης: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Εμφανίζει τα αποτελέσματα στη λίστα
    private void showResults(List<GeocodingResponse.CityResult> results) {
        cityResults.clear();
        cityDisplayNames.clear();

        if (results == null || results.isEmpty()) {
            Toast.makeText(this, "Δεν βρέθηκαν αποτελέσματα", Toast.LENGTH_SHORT).show();
            adapter.notifyDataSetChanged();
            return;
        }

        for (GeocodingResponse.CityResult city : results) {
            cityResults.add(city);
            // Μορφή: "Αθήνα, Attica, Greece"
            String display = city.name
                    + (city.admin1 != null ? ", " + city.admin1 : "")
                    + ", " + city.country;
            cityDisplayNames.add(display);
        }

        adapter.notifyDataSetChanged();
    }

    // Αποθηκεύει την επιλεγμένη πόλη και επιστρέφει στην κύρια οθόνη
    private void selectCity(GeocodingResponse.CityResult city) {
        SharedPreferences.Editor editor =
                getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(MainActivity.PREF_CITY_NAME, city.name + ", " + city.country);
        editor.putFloat(MainActivity.PREF_CITY_LAT, (float) city.latitude);
        editor.putFloat(MainActivity.PREF_CITY_LON, (float) city.longitude);
        editor.apply();

        Toast.makeText(this, "Επιλέχθηκε: " + city.name, Toast.LENGTH_SHORT).show();
        finish(); // Επιστροφή στο MainActivity
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
