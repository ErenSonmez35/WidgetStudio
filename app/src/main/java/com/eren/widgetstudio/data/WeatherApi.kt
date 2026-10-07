package com.eren.widgetstudio.data

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Open-Meteo: ücretsiz, API anahtarı gerektirmez. Ağ çağrıları arka planda yapılmalı. */
object WeatherApi {

    data class City(val name: String, val detail: String, val lat: Double, val lon: Double)

    fun searchCity(query: String): List<City> {
        val url = "https://geocoding-api.open-meteo.com/v1/search?count=6&language=tr&format=json&name=" +
            URLEncoder.encode(query.trim(), "UTF-8")
        val results = JSONObject(httpGet(url)).optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val o = results.getJSONObject(i)
            City(
                name = o.getString("name"),
                detail = listOf(o.optString("admin1"), o.optString("country"))
                    .filter { it.isNotBlank() }
                    .joinToString(", "),
                lat = o.getDouble("latitude"),
                lon = o.getDouble("longitude"),
            )
        }
    }

    fun current(lat: Double, lon: Double): WeatherData {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code" +
            "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1"
        val root = JSONObject(httpGet(url))
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        return WeatherData(
            temp = current.getDouble("temperature_2m"),
            code = current.getInt("weather_code"),
            max = daily.getJSONArray("temperature_2m_max").getDouble(0),
            min = daily.getJSONArray("temperature_2m_min").getDouble(0),
            updatedAt = System.currentTimeMillis(),
        )
    }

    /** WMO hava kodu -> (emoji, Türkçe açıklama) */
    fun describe(code: Int): Pair<String, String> = when (code) {
        0 -> "☀️" to "Açık"
        1, 2 -> "🌤️" to "Az bulutlu"
        3 -> "☁️" to "Kapalı"
        45, 48 -> "🌫️" to "Sisli"
        in 51..57 -> "🌦️" to "Çiseleme"
        in 61..67 -> "🌧️" to "Yağmurlu"
        in 71..77 -> "❄️" to "Karlı"
        in 80..82 -> "🌧️" to "Sağanak"
        85, 86 -> "🌨️" to "Kar sağanağı"
        in 95..99 -> "⛈️" to "Fırtınalı"
        else -> "🌡️" to "—"
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        try {
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
