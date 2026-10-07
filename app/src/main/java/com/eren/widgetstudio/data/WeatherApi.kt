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
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset" +
            "&timezone=auto&forecast_days=5"
        return parseForecast(httpGet(url))
    }

    /** Open-Meteo yanıtını [WeatherData]'ya çevirir (ağdan bağımsız, test edilebilir). */
    fun parseForecast(body: String, now: Long = System.currentTimeMillis()): WeatherData {
        val root = JSONObject(body)
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        val times = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val maxes = daily.getJSONArray("temperature_2m_max")
        val mins = daily.getJSONArray("temperature_2m_min")
        val days = (0 until times.length()).map { i ->
            DayForecast(times.getString(i), codes.getInt(i), maxes.getDouble(i), mins.getDouble(i))
        }
        fun hhmm(key: String): String =
            daily.optJSONArray(key)?.optString(0).orEmpty().substringAfter('T', "")
        return WeatherData(
            temp = current.getDouble("temperature_2m"),
            code = current.getInt("weather_code"),
            max = maxes.getDouble(0),
            min = mins.getDouble(0),
            updatedAt = now,
            feelsLike = current.optDouble("apparent_temperature").takeUnless { it.isNaN() },
            humidity = current.optInt("relative_humidity_2m", -1),
            wind = current.optDouble("wind_speed_10m", -1.0),
            sunrise = hhmm("sunrise"),
            sunset = hhmm("sunset"),
            daily = days,
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
