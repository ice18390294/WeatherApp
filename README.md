# WeatherApp 🌤️

Απλή Android εφαρμογή πρόγνωσης καιρού — φοιτητικό project τμήματος Πληροφορικής.

## Λειτουργίες

- 🌍 **Αναζήτηση πόλης** — οποιαδήποτε περιοχή του πλανήτη
- 📍 **Τρέχουσα τοποθεσία** — αυτόματος εντοπισμός μέσω GPS
- 📅 **Πρόγνωση 5 ημερών** — max/min θερμοκρασία + καιρός
- 🔔 **Έκτακτες ειδοποιήσεις** — background έλεγχος κάθε ώρα για επικίνδυνα φαινόμενα (καταιγίδα, χαλάζι, έντονη βροχή κ.ά.)

## APIs (Δωρεάν — Χωρίς API Key)

- **[Open-Meteo](https://open-meteo.com/)** — Δεδομένα καιρού (WMO κωδικοί)
- **[Open-Meteo Geocoding](https://open-meteo.com/en/docs/geocoding-api)** — Αναζήτηση πόλης με γεωγραφικές συντεταγμένες

## Τεχνολογίες

| Βιβλιοθήκη | Χρήση |
|---|---|
| Retrofit 2 | HTTP κλήσεις στα APIs |
| Gson | Αποκωδικοποίηση JSON |
| WorkManager | Background εργασίες (έλεγχος καιρού) |
| FusedLocationProvider | Εντοπισμός GPS |
| NotificationCompat | Push notifications |

## Δομή Project

```
app/src/main/java/com/example/weatherapp/
│
├── MainActivity.java          ← Κύρια οθόνη
├── CitySearchActivity.java    ← Αναζήτηση πόλης
├── WeatherCheckWorker.java    ← Background worker (WorkManager)
├── WeatherUtils.java          ← Βοηθητικές μέθοδοι (WMO κωδικοί)
│
├── api/
│   ├── RetrofitClient.java    ← Singleton HTTP client
│   ├── WeatherApiService.java ← Forecast API interface
│   └── GeocodingApiService.java ← Geocoding API interface
│
└── model/
    ├── WeatherResponse.java   ← JSON model καιρού
    └── GeocodingResponse.java ← JSON model αναζήτησης πόλης
```

## Εγκατάσταση

1. Κατεβάστε το project και ανοίξτε το στο **Android Studio**
2. Αφήστε το Android Studio να κατεβάσει τα dependencies (Gradle sync)
3. Τρέξτε σε emulator ή φυσική συσκευή (Android 8.0+, API 26+)

> **Σημείωση:** Δεν χρειάζεται κανένα API key — τα Open-Meteo APIs είναι εντελώς δωρεάν!

## Επικίνδυνα Φαινόμενα (WMO κωδικοί)

Η εφαρμογή στέλνει ειδοποίηση για τους εξής WMO κωδικούς:

| Κωδικός | Φαινόμενο |
|---|---|
| 65 | Έντονη βροχή |
| 66, 67 | Παγωτή βροχή |
| 75 | Έντονη χιονόπτωση |
| 82 | Ισχυρές μπόρες |
| 85, 86 | Χιονομπόρες |
| 95 | Καταιγίδα |
| 96, 99 | Καταιγίδα με χαλάζι |
