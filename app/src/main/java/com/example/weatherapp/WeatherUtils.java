package com.example.weatherapp;

/**
 * Βοηθητική κλάση για μετατροπή WMO κωδικών καιρού
 * σε κατανοητές περιγραφές και emojis.
 */
public class WeatherUtils {

    // Επιστρέφει ελληνική περιγραφή για τον WMO κωδικό καιρού
    public static String getWeatherDescription(int code) {
        if (code == 0)  return "Αίθριος";
        if (code == 1)  return "Κυρίως αίθριος";
        if (code == 2)  return "Μερικώς συννεφιά";
        if (code == 3)  return "Συννεφιά";
        if (code == 45 || code == 48) return "Ομίχλη";
        if (code == 51 || code == 53) return "Ψιλόβροχο";
        if (code == 55) return "Έντονο ψιλόβροχο";
        if (code == 56 || code == 57) return "Παγωτό ψιλόβροχο";
        if (code == 61 || code == 63) return "Βροχή";
        if (code == 65) return "Έντονη βροχή";
        if (code == 66 || code == 67) return "Παγωτή βροχή";
        if (code == 71 || code == 73) return "Χιόνι";
        if (code == 75) return "Έντονη χιονόπτωση";
        if (code == 77) return "Χαλαζόνερο";
        if (code == 80 || code == 81) return "Μπόρες";
        if (code == 82) return "Ισχυρές μπόρες";
        if (code == 85 || code == 86) return "Χιονομπόρες";
        if (code == 95) return "Καταιγίδα";
        if (code == 96 || code == 99) return "Καταιγίδα με χαλάζι";
        return "Άγνωστες συνθήκες";
    }

    // Επιστρέφει emoji για γρήγορη οπτική αναπαράσταση
    public static String getWeatherEmoji(int code) {
        if (code == 0 || code == 1)  return "☀️";
        if (code == 2)               return "⛅";
        if (code == 3)               return "☁️";
        if (code == 45 || code == 48) return "🌫️";
        if (code >= 51 && code <= 57) return "🌦️";
        if (code >= 61 && code <= 67) return "🌧️";
        if (code >= 71 && code <= 77) return "❄️";
        if (code >= 80 && code <= 86) return "🌦️";
        if (code >= 95 && code <= 99) return "⛈️";
        return "🌡️";
    }

    // Ελέγχει αν ο κωδικός αντιστοιχεί σε επικίνδυνο φαινόμενο
    public static boolean isDangerous(int code) {
        return code == 65   // Έντονη βροχή
                || code == 66 || code == 67  // Παγωτή βροχή
                || code == 75   // Έντονη χιονόπτωση
                || code == 82   // Ισχυρές μπόρες
                || code == 85 || code == 86  // Χιονομπόρες
                || code == 95   // Καταιγίδα
                || code == 96 || code == 99; // Καταιγίδα με χαλάζι
    }
}
