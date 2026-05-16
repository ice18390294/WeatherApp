package com.example.weatherapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.weatherapp.api.RetrofitClient;
import com.example.weatherapp.api.WeatherApiService;
import com.example.weatherapp.model.WeatherResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    // Σταθερές για SharedPreferences
    public static final String CHANNEL_ID = "weather_alerts";
    public static final String PREFS_NAME = "WeatherPrefs";
    public static final String PREF_CITY_NAME = "city_name";
    public static final String PREF_CITY_LAT = "city_lat";
    public static final String PREF_CITY_LON = "city_lon";

    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 101;

    // Views
    private TextView tvCityName, tvTemperature, tvCondition, tvWind;
    private LinearLayout layoutNoCity, layoutForecast;
    private View cardWeather, cardForecast;
    private Button btnSearchCity, btnUseLocation, btnRefresh;

    // Location client για GPS
    private FusedLocationProviderClient locationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Αρχικοποίηση views
        tvCityName = findViewById(R.id.tvCityName);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvCondition = findViewById(R.id.tvCondition);
        tvWind = findViewById(R.id.tvWind);
        layoutNoCity = findViewById(R.id.layoutNoCity);
        cardWeather = findViewById(R.id.cardWeather);
        cardForecast = findViewById(R.id.cardForecast);
        layoutForecast = findViewById(R.id.layoutForecast);
        btnSearchCity = findViewById(R.id.btnSearchCity);
        btnUseLocation = findViewById(R.id.btnUseLocation);
        btnRefresh = findViewById(R.id.btnRefresh);

        // Αρχικοποίηση Location client
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        // Δημιουργία notification channel για τις ειδοποιήσεις
        createNotificationChannel();

        // Ζητάμε άδεια notifications (Android 13+)
        requestNotificationPermission();

        // Προγραμματισμός background ελέγχου καιρού κάθε ώρα
        scheduleWeatherCheck();

        // Listeners για τα κουμπιά
        btnSearchCity.setOnClickListener(v ->
                startActivity(new Intent(this, CitySearchActivity.class)));

        btnUseLocation.setOnClickListener(v -> useCurrentLocation());

        btnRefresh.setOnClickListener(v -> loadSavedCityWeather());

        // Φόρτωση καιρού για αποθηκευμένη πόλη
        loadSavedCityWeather();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ανανέωση όταν επιστρέφουμε από την αναζήτηση πόλης
        loadSavedCityWeather();
    }

    // Φορτώνει τον καιρό για την αποθηκευμένη πόλη
    private void loadSavedCityWeather() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String cityName = prefs.getString(PREF_CITY_NAME, null);
        float lat = prefs.getFloat(PREF_CITY_LAT, 0);
        float lon = prefs.getFloat(PREF_CITY_LON, 0);

        if (cityName != null) {
            fetchWeather(lat, lon, cityName);
        } else {
            // Δεν έχει επιλεγεί πόλη, δείχνουμε το μήνυμα
            showNoCityLayout();
        }
    }

    // Εμφανίζει το layout "δεν έχει επιλεγεί πόλη"
    private void showNoCityLayout() {
        layoutNoCity.setVisibility(View.VISIBLE);
        cardWeather.setVisibility(View.GONE);
        cardForecast.setVisibility(View.GONE);
        btnRefresh.setVisibility(View.GONE);
    }

    // Παίρνει την τρέχουσα τοποθεσία GPS
    private void useCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Ζητάμε άδεια αν δεν έχουμε
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        Toast.makeText(this, "Λήψη τοποθεσίας...", Toast.LENGTH_SHORT).show();

        CancellationTokenSource cts = new CancellationTokenSource();
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();

                        // Reverse geocoding: μετατρέπουμε συντεταγμένες σε όνομα πόλης
                        String cityName = getCityNameFromCoordinates(lat, lon);

                        // Αποθηκεύουμε την τοποθεσία με το πραγματικό όνομα πόλης
                        SharedPreferences.Editor editor =
                                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                        editor.putString(PREF_CITY_NAME, cityName);
                        editor.putFloat(PREF_CITY_LAT, (float) lat);
                        editor.putFloat(PREF_CITY_LON, (float) lon);
                        editor.apply();

                        fetchWeather(lat, lon, cityName);
                    } else {
                        Toast.makeText(this, "Δεν βρέθηκε τοποθεσία. Δοκιμάστε ξανά.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Σφάλμα τοποθεσίας: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // Reverse geocoding: επιστρέφει το όνομα της πόλης από GPS συντεταγμένες
    private String getCityNameFromCoordinates(double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // Προτεραιότητα: locality (πόλη) → subAdminArea (περιοχή) → adminArea (νομός) → countryName
                if (address.getLocality() != null) {
                    return address.getLocality();
                } else if (address.getSubAdminArea() != null) {
                    return address.getSubAdminArea();
                } else if (address.getAdminArea() != null) {
                    return address.getAdminArea();
                } else if (address.getCountryName() != null) {
                    return address.getCountryName();
                }
            }
        } catch (IOException e) {
            // Αν αποτύχει το geocoding, επιστρέφουμε fallback
            e.printStackTrace();
        }
        return "Τρέχουσα Τοποθεσία";
    }

    // Καλεί το API του Open-Meteo για να πάρει δεδομένα καιρού
    private void fetchWeather(double lat, double lon, String cityName) {
        WeatherApiService api = RetrofitClient.getWeatherClient().create(WeatherApiService.class);

        Call<WeatherResponse> call = api.getForecast(
                lat, lon, true,
                "weathercode,temperature_2m_max,temperature_2m_min",
                "auto"
        );

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call,
                                   @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayWeather(response.body(), cityName);
                } else {
                    Toast.makeText(MainActivity.this,
                            "Σφάλμα λήψης δεδομένων καιρού", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this,
                        "Σφάλμα σύνδεσης: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Εμφανίζει τα δεδομένα καιρού στην οθόνη
    private void displayWeather(WeatherResponse weather, String cityName) {
        layoutNoCity.setVisibility(View.GONE);
        cardWeather.setVisibility(View.VISIBLE);
        cardForecast.setVisibility(View.VISIBLE);
        btnRefresh.setVisibility(View.VISIBLE);

        WeatherResponse.CurrentWeather current = weather.currentWeather;

        tvCityName.setText(cityName);
        tvTemperature.setText(String.format("%.0f°C", current.temperature));
        tvCondition.setText(WeatherUtils.getWeatherEmoji(current.weathercode)
                + "  " + WeatherUtils.getWeatherDescription(current.weathercode));
        tvWind.setText(String.format("💨  Άνεμος: %.0f km/h", current.windspeed));

        // Εμφάνιση πρόγνωσης 5 ημερών
        if (weather.daily != null && weather.daily.time != null) {
            displayDailyForecast(weather.daily);
        }
    }

    // Δημιουργεί δυναμικά γραμμές για κάθε μέρα της πρόγνωσης
    private void displayDailyForecast(WeatherResponse.DailyWeather daily) {
        layoutForecast.removeAllViews();

        int days = Math.min(daily.time.size(), 5); // Μέγιστο 5 μέρες
        for (int i = 0; i < days; i++) {
            int code = daily.weathercode.get(i);
            double maxT = daily.temperatureMax.get(i);
            double minT = daily.temperatureMin.get(i);

            String text = String.format("%s   %s %s   ↑%.0f° ↓%.0f°",
                    daily.time.get(i),
                    WeatherUtils.getWeatherEmoji(code),
                    WeatherUtils.getWeatherDescription(code),
                    maxT, minT);

            TextView tv = new TextView(this);
            tv.setText(text);
            tv.setTextColor(getResources().getColor(R.color.white, null));
            tv.setTextSize(14);
            tv.setPadding(0, 10, 0, 10);

            layoutForecast.addView(tv);

            // Διαχωριστική γραμμή (εκτός τελευταίας)
            if (i < days - 1) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(getResources().getColor(R.color.divider, null));
                layoutForecast.addView(divider);
            }
        }
    }

    // Επεξεργασία αποτελέσματος αίτησης άδειας
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                useCurrentLocation();
            } else {
                Toast.makeText(this, "Η άδεια τοποθεσίας δεν δόθηκε.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Δημιουργεί το notification channel για τις ειδοποιήσεις καιρού
    private void createNotificationChannel() {
        NotificationChannelCompat channel = new NotificationChannelCompat.Builder(
                CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Ειδοποιήσεις Καιρού")
                .setDescription("Ειδοποιήσεις για επικίνδυνα καιρικά φαινόμενα")
                .build();
        NotificationManagerCompat.from(this).createNotificationChannel(channel);
    }

    // Ζητάει άδεια notifications στο Android 13+
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST);
            }
        }
    }

    // Προγραμματίζει τον έλεγχο καιρού κάθε ώρα στο background
    private void scheduleWeatherCheck() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                WeatherCheckWorker.class, 1, TimeUnit.HOURS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "weather_check",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest);
    }
}
