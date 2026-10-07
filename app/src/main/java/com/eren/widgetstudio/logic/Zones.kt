package com.eren.widgetstudio.logic

data class ZoneOption(val id: String, val label: String)

object Zones {
    val ALL = listOf(
        ZoneOption("Europe/Istanbul", "İstanbul"),
        ZoneOption("Europe/London", "Londra"),
        ZoneOption("Europe/Berlin", "Berlin"),
        ZoneOption("Europe/Paris", "Paris"),
        ZoneOption("Europe/Moscow", "Moskova"),
        ZoneOption("Africa/Cairo", "Kahire"),
        ZoneOption("Asia/Dubai", "Dubai"),
        ZoneOption("Asia/Kolkata", "Mumbai"),
        ZoneOption("Asia/Singapore", "Singapur"),
        ZoneOption("Asia/Shanghai", "Pekin"),
        ZoneOption("Asia/Tokyo", "Tokyo"),
        ZoneOption("Australia/Sydney", "Sidney"),
        ZoneOption("Pacific/Auckland", "Auckland"),
        ZoneOption("America/New_York", "New York"),
        ZoneOption("America/Chicago", "Chicago"),
        ZoneOption("America/Los_Angeles", "Los Angeles"),
        ZoneOption("America/Mexico_City", "Mexico City"),
        ZoneOption("America/Sao_Paulo", "São Paulo"),
    )

    val DEFAULT = listOf("Europe/Istanbul", "Europe/London", "America/New_York")

    fun label(id: String): String = ALL.firstOrNull { it.id == id }?.label ?: id.substringAfterLast('/').replace('_', ' ')
}
