package com.example.weatherapp;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.weatherapp.api.RetrofitClient;
import com.example.weatherapp.api.WeatherApiService;
import com.example.weatherapp.model.WeatherResponse;

import retrofit2.Response;

/**
 * Background Worker που τρέχει κάθε ώρα και ελέγχει αν υπάρχουν
 * επικίνδυνες καιρικές συνθήκες για την αποθηκευμένη πόλη.
 */
public class WeatherCheckWorker extends Worker {

    public WeatherCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs =
                context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);

        // Παίρνουμε την αποθηκευμένη πόλη
        String cityName = prefs.getString(MainActivity.PREF_CITY_NAME, null);
        if (cityName == null) {
            return Result.success(); // Δεν έχει επιλεγεί πόλη, τίποτα να κάνουμε
        }

        float lat = prefs.getFloat(MainActivity.PREF_CITY_LAT, 0);
        float lon = prefs.getFloat(MainActivity.PREF_CITY_LON, 0);

        try {
            // Σύγχρονη HTTP κλήση (είμαστε ήδη σε background thread)
            WeatherApiService api =
                    RetrofitClient.getWeatherClient().create(WeatherApiService.class);
            Response<WeatherResponse> response = api.getForecast(
                    lat, lon, true,
                    "weathercode,temperature_2m_max,temperature_2m_min",
                    "auto"
            ).execute();

            if (response.isSuccessful() && response.body() != null) {
                int weatherCode = response.body().currentWeather.weathercode;

                // Αν ο κωδικός είναι επικίνδυνος, στέλνουμε ειδοποίηση
                if (WeatherUtils.isDangerous(weatherCode)) {
                    String description = WeatherUtils.getWeatherDescription(weatherCode);
                    sendAlertNotification(context, cityName, description);
                }
            }

        } catch (Exception e) {
            // Αν αποτύχει η σύνδεση, δοκιμάζουμε ξανά αργότερα
            return Result.retry();
        }

        return Result.success();
    }

    // Στέλνει push notification για επικίνδυνες καιρικές συνθήκες
    private void sendAlertNotification(Context context, String cityName, String condition) {
        // Έλεγχος άδειας notification για Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return; // Χωρίς άδεια, δεν μπορούμε να στείλουμε
            }
        }

        // Intent για άνοιγμα της εφαρμογής όταν πατηθεί η ειδοποίηση
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("⚠️ Επικίνδυνες Καιρικές Συνθήκες!")
                .setContentText(cityName + ": " + condition)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Προσοχή! Στην περιοχή " + cityName
                                + " αναμένεται: " + condition
                                + ". Λάβετε τα απαραίτητα μέτρα προστασίας."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(1001, builder.build());
    }
}
