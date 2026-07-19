package com.elite.jarvishud

import android.content.Context
import android.content.SharedPreferences

class KeyStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_keys", Context.MODE_PRIVATE)

    var openRouterKey: String
        get() = prefs.getString("openrouter_key", "") ?: ""
        set(value) = prefs.edit().putString("openrouter_key", value).apply()

    var openRouterModel: String
        get() = prefs.getString("openrouter_model", "google/gemini-2.0-flash-exp:free") ?: "google/gemini-2.0-flash-exp:free"
        set(value) = prefs.edit().putString("openrouter_model", value).apply()

    var elevenLabsKey: String
        get() = prefs.getString("elevenlabs_key", "") ?: ""
        set(value) = prefs.edit().putString("elevenlabs_key", value).apply()

    var elevenLabsVoiceId: String
        get() = prefs.getString("elevenlabs_voice_id", "21m00Tcm4TlvDq8ikWAM") ?: "21m00Tcm4TlvDq8ikWAM"
        set(value) = prefs.edit().putString("elevenlabs_voice_id", value).apply()

    var newsApiKey: String
        get() = prefs.getString("news_api_key", "") ?: ""
        set(value) = prefs.edit().putString("news_api_key", value).apply()

    var newsCountry: String
        get() = prefs.getString("news_country", "in") ?: "in"
        set(value) = prefs.edit().putString("news_country", value).apply()

    fun hasRequiredKeys(): Boolean = openRouterKey.isNotBlank()
}
