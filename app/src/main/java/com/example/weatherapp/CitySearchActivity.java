package com.example.weatherapp;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.weatherapp.api.GeocodingApiService;
import com.example.weatherapp.api.RetrofitClient;
import com.example.weatherapp.model.GeocodingResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CitySearchActivity extends AppCompatActivity {

    private static final long LIST_ITEM_ANIMATION_DURATION_MS = 320L;
    private static final long LIST_ITEM_STAGGER_MS = 70L;
    private static final long SEARCH_TINT_ANIMATION_DURATION_MS = 260L;
    private static final long SKY_ANIMATION_DURATION_MS = 15000L;
    private static final long BUTTON_PRESS_DURATION_MS = 120L;
    private static final long BUTTON_RELEASE_DURATION_MS = 260L;

    private static final GradientDrawable.Orientation[] SKY_ORIENTATIONS = {
            GradientDrawable.Orientation.TOP_BOTTOM,
            GradientDrawable.Orientation.LEFT_RIGHT,
            GradientDrawable.Orientation.BL_TR,
            GradientDrawable.Orientation.BOTTOM_TOP,
            GradientDrawable.Orientation.RIGHT_LEFT,
            GradientDrawable.Orientation.TR_BL
    };

    private EditText etCitySearch;
    private Button btnSearch;
    private Button btnBack;
    private ListView lvResults;
    private View animatedBackground;

    private final List<GeocodingResponse.CityResult> cityResults = new ArrayList<>();
    private final List<String> cityDisplayNames = new ArrayList<>();

    private ArrayAdapter<String> adapter;
    private ValueAnimator skyAnimator;
    private ValueAnimator searchTintAnimator;
    private int lastAnimatedPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_city);

        etCitySearch = findViewById(R.id.etCitySearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnBack = findViewById(R.id.btnBack);
        lvResults = findViewById(R.id.lvResults);
        animatedBackground = findViewById(R.id.viewAnimatedBackground);

        setupImmersiveUi();
        startAnimatedSky(animatedBackground);
        setupSearchTintAnimation();
        setupButtonPressEffect(btnBack);
        setupButtonPressEffect(btnSearch);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, cityDisplayNames) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTextColor(ContextCompat.getColor(CitySearchActivity.this, R.color.white));
                int padding = dp(16);
                textView.setPadding(padding, padding, padding, padding);
                textView.setTextSize(16f);
                view.setBackgroundColor(0x22FFFFFF);

                if (position > lastAnimatedPosition) {
                    view.setAlpha(0f);
                    view.setTranslationY(dp(28));
                    view.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(LIST_ITEM_ANIMATION_DURATION_MS)
                            .setStartDelay(position * LIST_ITEM_STAGGER_MS)
                            .setInterpolator(new OvershootInterpolator(0.9f))
                            .start();
                    lastAnimatedPosition = position;
                }
                return view;
            }
        };
        lvResults.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnSearch.setOnClickListener(v -> searchCity());
        etCitySearch.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            boolean isSearchAction = actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE;
            boolean isEnterKey = keyEvent != null
                    && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && keyEvent.getAction() == KeyEvent.ACTION_DOWN;
            if (isSearchAction || isEnterKey) {
                searchCity();
                return true;
            }
            return false;
        });

        lvResults.setOnItemClickListener((parent, view, position, id) ->
                selectCity(cityResults.get(position)));
    }

    @Override
    protected void onDestroy() {
        if (skyAnimator != null) {
            skyAnimator.cancel();
        }
        if (searchTintAnimator != null) {
            searchTintAnimator.cancel();
        }
        super.onDestroy();
    }

    private void setupImmersiveUi() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View content = findViewById(R.id.layoutSearchContent);
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    dp(16),
                    systemBars.top + dp(16),
                    dp(16),
                    systemBars.bottom + dp(16)
            );
            return insets;
        });
    }

    private void setupSearchTintAnimation() {
        etCitySearch.setOnFocusChangeListener((view, hasFocus) ->
                animateSearchTint(hasFocus
                        ? ContextCompat.getColor(this, R.color.glow_yellow)
                        : ContextCompat.getColor(this, R.color.light_blue)));

        etCitySearch.setOnClickListener(v ->
                animateSearchTint(ContextCompat.getColor(this, R.color.glow_yellow)));
    }

    private void animateSearchTint(int targetColor) {
        int currentColor = etCitySearch.getBackgroundTintList() != null
                ? etCitySearch.getBackgroundTintList().getDefaultColor()
                : ContextCompat.getColor(this, R.color.light_blue);

        if (searchTintAnimator != null) {
            searchTintAnimator.cancel();
        }

        searchTintAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), currentColor, targetColor);
        searchTintAnimator.setDuration(SEARCH_TINT_ANIMATION_DURATION_MS);
        searchTintAnimator.addUpdateListener(animation ->
                etCitySearch.setBackgroundTintList(
                        ColorStateList.valueOf((int) animation.getAnimatedValue())));
        searchTintAnimator.start();
    }

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

    private void showResults(List<GeocodingResponse.CityResult> results) {
        cityResults.clear();
        cityDisplayNames.clear();
        lastAnimatedPosition = -1;

        if (results == null || results.isEmpty()) {
            Toast.makeText(this, "Δεν βρέθηκαν αποτελέσματα", Toast.LENGTH_SHORT).show();
            adapter.notifyDataSetChanged();
            return;
        }

        for (GeocodingResponse.CityResult city : results) {
            cityResults.add(city);
            String display = city.name
                    + (city.admin1 != null ? ", " + city.admin1 : "")
                    + ", " + city.country;
            cityDisplayNames.add(display);
        }

        adapter.notifyDataSetChanged();
    }

    private void selectCity(GeocodingResponse.CityResult city) {
        SharedPreferences.Editor editor =
                getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(MainActivity.PREF_CITY_NAME, city.name + ", " + city.country);
        editor.putFloat(MainActivity.PREF_CITY_LAT, (float) city.latitude);
        editor.putFloat(MainActivity.PREF_CITY_LON, (float) city.longitude);
        editor.apply();

        Toast.makeText(this, "Επιλέχθηκε: " + city.name, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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
        target.setBackground(drawable);

        final int[][] phases = new int[][]{
                {
                        ContextCompat.getColor(this, R.color.background_start),
                        ContextCompat.getColor(this, R.color.background_mid),
                        ContextCompat.getColor(this, R.color.background_end)
                },
                {0xFF2D3A8C, 0xFF2979FF, 0xFF80D8FF},
                {0xFF7C4DFF, 0xFFFF8A65, 0xFF1A237E}
        };

        ArgbEvaluator evaluator = new ArgbEvaluator();
        skyAnimator = ValueAnimator.ofFloat(0f, (float) phases.length);
        skyAnimator.setDuration(SKY_ANIMATION_DURATION_MS);
        skyAnimator.setRepeatCount(ValueAnimator.INFINITE);
        skyAnimator.setInterpolator(new LinearInterpolator());
        skyAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            int currentIndex = ((int) Math.floor(value)) % phases.length;
            int nextIndex = (currentIndex + 1) % phases.length;
            float blend = value - (float) Math.floor(value);

            drawable.setOrientation(SKY_ORIENTATIONS[(int) ((animation.getAnimatedFraction()
                    * SKY_ORIENTATIONS.length) % SKY_ORIENTATIONS.length)]);

            drawable.setColors(new int[]{
                    (int) evaluator.evaluate(blend, phases[currentIndex][0], phases[nextIndex][0]),
                    (int) evaluator.evaluate(blend, phases[currentIndex][1], phases[nextIndex][1]),
                    (int) evaluator.evaluate(blend, phases[currentIndex][2], phases[nextIndex][2])
            });
        });
        skyAnimator.start();
    }

    private void setupButtonPressEffect(View button) {
        button.setTranslationZ(dp(10));
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
                            .translationZ(dp(10))
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
}
