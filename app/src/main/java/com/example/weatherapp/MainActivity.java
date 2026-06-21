package com.example.weatherapp;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.weatherapp.api.RetrofitClient;
import com.example.weatherapp.api.WeatherApiService;
import com.example.weatherapp.model.WeatherResponse;
import com.example.weatherapp.ui.WeatherParticleView;
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

    public static final String CHANNEL_ID = "weather_alerts";
    public static final String PREFS_NAME = "WeatherPrefs";
    public static final String PREF_CITY_NAME = "city_name";
    public static final String PREF_CITY_LAT = "city_lat";
    public static final String PREF_CITY_LON = "city_lon";

    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 101;
    private static final float MAX_CARD_TILT = 15f;
    private static final long CARD_ENTRANCE_DURATION_MS = 500L;
    private static final long CARD_FORECAST_DELAY_MS = 120L;
    private static final long CITY_SLIDE_DURATION_MS = 450L;
    private static final long CONDITION_FADE_DELAY_MS = 200L;
    private static final long CONDITION_FADE_DURATION_MS = 320L;
    private static final long TEMPERATURE_COUNT_DURATION_MS = 650L;
    private static final long TEMPERATURE_GLOW_DURATION_MS = 2000L;
    private static final long SKY_ANIMATION_DURATION_MS = 18000L;
    private static final long FORECAST_STAGGER_STEP_MS = 80L;
    private static final long FORECAST_ROW_DURATION_MS = 420L;
    private static final long FORECAST_DIVIDER_DURATION_MS = 250L;
    private static final long BUTTON_PRESS_DURATION_MS = 120L;
    private static final long BUTTON_RELEASE_DURATION_MS = 260L;

    private static final GradientDrawable.Orientation[] SKY_ORIENTATIONS = {
            GradientDrawable.Orientation.TOP_BOTTOM,
            GradientDrawable.Orientation.TL_BR,
            GradientDrawable.Orientation.LEFT_RIGHT,
            GradientDrawable.Orientation.BL_TR,
            GradientDrawable.Orientation.BOTTOM_TOP,
            GradientDrawable.Orientation.BR_TL,
            GradientDrawable.Orientation.RIGHT_LEFT,
            GradientDrawable.Orientation.TR_BL
    };

    private TextView tvCityName;
    private TextView tvTemperature;
    private TextView tvCondition;
    private TextView tvWind;
    private LinearLayout layoutNoCity;
    private LinearLayout layoutForecast;
    private View cardWeather;
    private View cardForecast;
    private View animatedBackground;
    private WeatherParticleView weatherParticleView;
    private Button btnSearchCity;
    private Button btnUseLocation;
    private Button btnRefresh;

    private FusedLocationProviderClient locationClient;
    private ValueAnimator skyAnimator;
    private ValueAnimator temperatureGlowAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        animatedBackground = findViewById(R.id.viewAnimatedBackground);
        weatherParticleView = findViewById(R.id.weatherParticleView);

        locationClient = LocationServices.getFusedLocationProviderClient(this);

        setupImmersiveUi();
        startAnimatedSky(animatedBackground);
        startTemperatureGlow();
        setupWeatherCardTilt();
        setupButtonPressEffect(btnSearchCity);
        setupButtonPressEffect(btnUseLocation);
        setupButtonPressEffect(btnRefresh);

        createNotificationChannel();
        requestNotificationPermission();
        scheduleWeatherCheck();

        btnSearchCity.setOnClickListener(v ->
                startActivity(new Intent(this, CitySearchActivity.class)));

        btnUseLocation.setOnClickListener(v -> useCurrentLocation());

        btnRefresh.setOnClickListener(v -> loadSavedCityWeather());

        loadSavedCityWeather();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedCityWeather();
    }

    @Override
    protected void onDestroy() {
        if (skyAnimator != null) {
            skyAnimator.cancel();
        }
        if (temperatureGlowAnimator != null) {
            temperatureGlowAnimator.cancel();
        }
        super.onDestroy();
    }

    private void setupImmersiveUi() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ScrollView scrollView = findViewById(R.id.scrollMainContent);
        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    view.getPaddingLeft(),
                    systemBars.top + dp(8),
                    view.getPaddingRight(),
                    systemBars.bottom + dp(24)
            );
            return insets;
        });
    }

    private void loadSavedCityWeather() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String cityName = prefs.getString(PREF_CITY_NAME, null);
        float lat = prefs.getFloat(PREF_CITY_LAT, 0);
        float lon = prefs.getFloat(PREF_CITY_LON, 0);

        if (cityName != null) {
            fetchWeather(lat, lon, cityName);
        } else {
            showNoCityLayout();
        }
    }

    private void showNoCityLayout() {
        layoutNoCity.setVisibility(View.VISIBLE);
        cardWeather.setVisibility(View.GONE);
        cardForecast.setVisibility(View.GONE);
        btnRefresh.setVisibility(View.GONE);
        weatherParticleView.setWeatherCondition("clouds");
    }

    private void useCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
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

                        String cityName = getCityNameFromCoordinates(lat, lon);

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

    private String getCityNameFromCoordinates(double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
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
            e.printStackTrace();
        }
        return "Τρέχουσα Τοποθεσία";
    }

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

    private void displayWeather(WeatherResponse weather, String cityName) {
        layoutNoCity.setVisibility(View.GONE);
        cardWeather.setVisibility(View.VISIBLE);
        cardForecast.setVisibility(View.VISIBLE);
        btnRefresh.setVisibility(View.VISIBLE);

        WeatherResponse.CurrentWeather current = weather.currentWeather;
        String conditionDescription = WeatherUtils.getWeatherDescription(current.weathercode);
        String conditionText = WeatherUtils.getWeatherEmoji(current.weathercode)
                + "  " + conditionDescription;

        tvWind.setText(String.format(Locale.getDefault(), "💨  Άνεμος: %.0f km/h", current.windspeed));
        weatherParticleView.setWeatherCondition(conditionDescription);

        if (weather.daily != null && weather.daily.time != null) {
            displayDailyForecast(weather.daily);
        }

        animateCardEntrance(cardWeather, 0L);
        animateCardEntrance(cardForecast, CARD_FORECAST_DELAY_MS);
        animateWeatherDetails(cityName, conditionText, current.temperature);
    }

    private void displayDailyForecast(WeatherResponse.DailyWeather daily) {
        layoutForecast.removeAllViews();

        int days = Math.min(daily.time.size(), 5);
        for (int i = 0; i < days; i++) {
            int code = daily.weathercode.get(i);
            double maxT = daily.temperatureMax.get(i);
            double minT = daily.temperatureMin.get(i);

            String text = String.format(Locale.getDefault(), "%s   %s %s   ↑%.0f° ↓%.0f°",
                    daily.time.get(i),
                    WeatherUtils.getWeatherEmoji(code),
                    WeatherUtils.getWeatherDescription(code),
                    maxT, minT);

            TextView tv = new TextView(this);
            tv.setText(text);
            tv.setTextColor(ContextCompat.getColor(this, R.color.white));
            tv.setTextSize(14);
            int verticalPadding = dp(10);
            tv.setPadding(0, verticalPadding, 0, verticalPadding);
            tv.setAlpha(0f);
            tv.setTranslationX(dp(48));

            layoutForecast.addView(tv);
            tv.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(FORECAST_ROW_DURATION_MS)
                    .setStartDelay(i * FORECAST_STAGGER_STEP_MS)
                    .setInterpolator(new OvershootInterpolator(0.9f))
                    .start();

            if (i < days - 1) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
                divider.setAlpha(0f);
                layoutForecast.addView(divider);
                divider.animate()
                        .alpha(1f)
                        .setDuration(FORECAST_DIVIDER_DURATION_MS)
                        .setStartDelay(i * FORECAST_STAGGER_STEP_MS + CARD_FORECAST_DELAY_MS)
                        .start();
            }
        }
    }

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

    private void createNotificationChannel() {
        NotificationChannelCompat channel = new NotificationChannelCompat.Builder(
                CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Ειδοποιήσεις Καιρού")
                .setDescription("Ειδοποιήσεις για επικίνδυνα καιρικά φαινόμενα")
                .build();
        NotificationManagerCompat.from(this).createNotificationChannel(channel);
    }

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

    private void scheduleWeatherCheck() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                WeatherCheckWorker.class, 1, TimeUnit.HOURS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "weather_check",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest);
    }

    private void animateCardEntrance(View card, long delay) {
        card.setAlpha(0f);
        card.setScaleX(0.6f);
        card.setScaleY(0.6f);
        card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(CARD_ENTRANCE_DURATION_MS)
                .setStartDelay(delay)
                .setInterpolator(new OvershootInterpolator(1.1f))
                .start();
    }

    private void animateWeatherDetails(String cityName, String conditionText, double temperature) {
        tvCityName.setText(cityName);
        tvCityName.setAlpha(0f);
        tvCityName.setTranslationX(-dp(40));
        tvCityName.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(CITY_SLIDE_DURATION_MS)
                .setInterpolator(new OvershootInterpolator(1.05f))
                .start();

        tvCondition.setText(conditionText);
        tvCondition.setAlpha(0f);
        tvCondition.animate()
                .alpha(1f)
                .setDuration(CONDITION_FADE_DURATION_MS)
                .setStartDelay(CONDITION_FADE_DELAY_MS)
                .start();

        int targetTemperature = (int) Math.round(temperature);
        int animationTarget = Math.abs(targetTemperature);
        ValueAnimator counterAnimator = ValueAnimator.ofInt(0, animationTarget);
        counterAnimator.setDuration(TEMPERATURE_COUNT_DURATION_MS);
        counterAnimator.setInterpolator(new OvershootInterpolator(0.7f));
        counterAnimator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            if (targetTemperature < 0) {
                animatedValue = -animatedValue;
            }
            tvTemperature.setText(String.format(Locale.getDefault(), "%d°C", animatedValue));
        });
        counterAnimator.start();
    }

    private void startTemperatureGlow() {
        temperatureGlowAnimator = ValueAnimator.ofObject(
                new ArgbEvaluator(),
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.glow_yellow)
        );
        temperatureGlowAnimator.setDuration(TEMPERATURE_GLOW_DURATION_MS);
        temperatureGlowAnimator.setRepeatMode(ValueAnimator.REVERSE);
        temperatureGlowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        temperatureGlowAnimator.addUpdateListener(animation ->
                tvTemperature.setTextColor((int) animation.getAnimatedValue()));
        temperatureGlowAnimator.start();
    }

    private void startAnimatedSky(View target) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        ContextCompat.getColor(this, R.color.background_start),
                        ContextCompat.getColor(this, R.color.background_mid),
                        ContextCompat.getColor(this, R.color.background_end)
                }
        );
        drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        target.setBackground(drawable);

        final int[][] phases = new int[][]{
                {
                        ContextCompat.getColor(this, R.color.background_start),
                        ContextCompat.getColor(this, R.color.background_mid),
                        ContextCompat.getColor(this, R.color.background_end)
                },
                {
                        0xFF3B2A78,
                        0xFF2962FF,
                        0xFF6EC6FF
                },
                {
                        0xFFFF7043,
                        0xFF5E35B1,
                        0xFF1A237E
                },
                {
                        0xFF090B1A,
                        0xFF1A237E,
                        0xFF0D47A1
                }
        };

        skyAnimator = ValueAnimator.ofFloat(0f, (float) phases.length);
        skyAnimator.setDuration(SKY_ANIMATION_DURATION_MS);
        skyAnimator.setRepeatCount(ValueAnimator.INFINITE);
        skyAnimator.setInterpolator(new LinearInterpolator());
        ArgbEvaluator evaluator = new ArgbEvaluator();
        skyAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            int currentIndex = ((int) Math.floor(value)) % phases.length;
            int nextIndex = (currentIndex + 1) % phases.length;
            float blend = value - (float) Math.floor(value);

            int[] colors = new int[3];
            for (int i = 0; i < colors.length; i++) {
                colors[i] = (int) evaluator.evaluate(blend,
                        phases[currentIndex][i],
                        phases[nextIndex][i]);
            }

            int orientationIndex = (int) ((animation.getAnimatedFraction()
                    * SKY_ORIENTATIONS.length) % SKY_ORIENTATIONS.length);
            drawable.setOrientation(SKY_ORIENTATIONS[orientationIndex]);
            drawable.setColors(colors);
        });
        skyAnimator.start();
    }

    private void setupWeatherCardTilt() {
        cardWeather.setCameraDistance(24000f);
        SpringAnimation rotationXSpring = buildCardSpring(cardWeather, DynamicAnimation.ROTATION_X);
        SpringAnimation rotationYSpring = buildCardSpring(cardWeather, DynamicAnimation.ROTATION_Y);

        cardWeather.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                    float centerX = view.getWidth() / 2f;
                    float centerY = view.getHeight() / 2f;
                    float offsetX = (event.getX() - centerX) / Math.max(centerX, 1f);
                    float offsetY = (event.getY() - centerY) / Math.max(centerY, 1f);
                    view.setRotationY(clamp(offsetX * MAX_CARD_TILT, -MAX_CARD_TILT, MAX_CARD_TILT));
                    view.setRotationX(clamp(-offsetY * MAX_CARD_TILT, -MAX_CARD_TILT, MAX_CARD_TILT));
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    rotationXSpring.animateToFinalPosition(0f);
                    rotationYSpring.animateToFinalPosition(0f);
                    view.performClick();
                    return true;
                default:
                    return false;
            }
        });
    }

    private SpringAnimation buildCardSpring(View card, DynamicAnimation.ViewProperty property) {
        SpringAnimation springAnimation = new SpringAnimation(card, property);
        SpringForce springForce = new SpringForce(0f);
        springForce.setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        springForce.setStiffness(SpringForce.STIFFNESS_LOW);
        springAnimation.setSpring(springForce);
        return springAnimation;
    }

    private void setupButtonPressEffect(View button) {
        button.setTranslationZ(dp(12));
        button.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate()
                            .scaleX(0.93f)
                            .scaleY(0.93f)
                            .translationZ(dp(2))
                            .setDuration(BUTTON_PRESS_DURATION_MS)
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .translationZ(dp(12))
                            .setDuration(BUTTON_RELEASE_DURATION_MS)
                            .setInterpolator(new OvershootInterpolator())
                            .start();
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
