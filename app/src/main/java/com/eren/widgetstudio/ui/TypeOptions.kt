package com.eren.widgetstudio.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.eren.widgetstudio.catalog.QuickSettingsSpec
import com.eren.widgetstudio.data.TodoItem
import com.eren.widgetstudio.data.WeatherApi
import com.eren.widgetstudio.data.WidgetDesign
import com.eren.widgetstudio.data.WidgetType
import com.eren.widgetstudio.data.opt
import com.eren.widgetstudio.data.withOpt
import com.eren.widgetstudio.logic.Zones
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Her widget türünün ayar alanları. Yeni tür eklerken buraya bir `when` kolu ekle;
 * ortak kontroller için Controls.kt'ye bak. [getCurrent] asenkron işlerde güncel tasarımı verir.
 */
@Composable
fun TypeOptions(d: WidgetDesign, getCurrent: () -> WidgetDesign, onChange: (WidgetDesign) -> Unit) {
    when (d.type) {
        WidgetType.CLOCK -> {
            SwitchRow("24 saat biçimi", d.clock24h) { onChange(d.copy(clock24h = it)) }
            SwitchRow("Saniyeleri göster", d.clockSeconds) { onChange(d.copy(clockSeconds = it)) }
            SwitchRow("Tarihi göster", d.clockShowDate) { onChange(d.copy(clockShowDate = it)) }
        }

        WidgetType.WORLD_CLOCK -> WorldClockOptions(d, onChange)

        WidgetType.DATE -> SwitchRow("Hafta ve yılın günü", d.opt("week", true)) { onChange(d.withOpt("week", it)) }

        WidgetType.CALENDAR -> SwitchRow("Hafta Pazartesi başlasın", d.opt("mondayFirst", true)) {
            onChange(d.withOpt("mondayFirst", it))
        }

        WidgetType.TIME_PROGRESS -> {
            SwitchRow("Gün", d.opt("day", true)) { onChange(d.withOpt("day", it)) }
            SwitchRow("Hafta", d.opt("week", true)) { onChange(d.withOpt("week", it)) }
            SwitchRow("Ay", d.opt("month", true)) { onChange(d.withOpt("month", it)) }
            SwitchRow("Yıl", d.opt("year", true)) { onChange(d.withOpt("year", it)) }
        }

        WidgetType.STOPWATCH -> HintText("Başlat/Duraklat ve Sıfırla butonları ana ekrandaki widget'ın üzerindedir.")

        WidgetType.TIMER -> {
            SliderRow("Süre", d.timerMinutes.toFloat(), 1f..120f, "${d.timerMinutes} dk") {
                onChange(d.copy(timerMinutes = it.roundToInt(), elapsedMs = 0, running = false, runStartMs = 0))
            }
            ChipRow(listOf(5, 10, 15, 25, 45, 60), d.timerMinutes, { "$it dk" }) {
                onChange(d.copy(timerMinutes = it, elapsedMs = 0, running = false, runStartMs = 0))
            }
            HintText("Süre dolunca sayaç eksiye geçer (ör. -0:05). Bildirim/alarm gönderilmez.")
        }

        WidgetType.TODO -> ChecklistOptions(d, onChange, allowClear = true)
        WidgetType.HABIT -> {
            ChecklistOptions(d, onChange, allowClear = false)
            HintText("İşaretler her gün otomatik sıfırlanır.")
        }

        WidgetType.COUNTDOWN, WidgetType.DAYS_SINCE, WidgetType.AGE -> {
            OutlinedTextField(
                value = d.label,
                onValueChange = { onChange(d.copy(label = it)) },
                label = { Text("Başlık") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            DateField(
                when (d.type) {
                    WidgetType.COUNTDOWN -> "Hedef tarih"
                    WidgetType.DAYS_SINCE -> "Başlangıç tarihi"
                    else -> "Doğum tarihi"
                },
                d.targetEpochDay,
            ) { onChange(d.copy(targetEpochDay = it)) }
        }

        WidgetType.NOTE -> {
            OutlinedTextField(
                value = d.label,
                onValueChange = { onChange(d.copy(label = it)) },
                label = { Text("Başlık (boş bırakılabilir)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = d.text,
                onValueChange = { onChange(d.copy(text = it)) },
                label = { Text("Not") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        WidgetType.QUOTE -> {
            OutlinedTextField(
                value = d.text,
                onValueChange = { onChange(d.copy(text = it)) },
                label = { Text("Kendi sözlerim (her satıra bir söz)") },
                supportingText = { Text("Boş bırakırsan hazır atasözleri gösterilir. Her gün yeni bir söz çıkar.") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        WidgetType.COUNTER, WidgetType.WATER -> CounterOptions(d, onChange)

        WidgetType.DICE -> {
            Text("Yüz sayısı")
            ChipRow(listOf(2, 4, 6, 8, 10, 12, 20, 100), d.step, { if (it == 2) "Yazı-Tura" else "d$it" }) {
                onChange(d.copy(step = it, counter = 0, text = ""))
            }
        }

        WidgetType.QUICK_SETTINGS -> {
            QuickSettingsSpec.SHORTCUTS.forEach { sc ->
                SwitchRow(sc.label, d.opt("sc_${sc.key}", sc.default)) { onChange(d.withOpt("sc_${sc.key}", it)) }
            }
            HintText("Butonlar ilgili Android ayar sayfasını açar. Hiçbir izin gerekmez.")
        }

        WidgetType.SYSTEM -> {
            SwitchRow("Pil", d.showBattery) { onChange(d.copy(showBattery = it)) }
            SwitchRow("Depolama", d.showStorage) { onChange(d.copy(showStorage = it)) }
            SwitchRow("RAM kullanımı", d.showRam) { onChange(d.copy(showRam = it)) }
            HintText("Değerler 15 dakikada bir ve widget'a dokunduğunda güncellenir.")
        }

        WidgetType.BATTERY, WidgetType.NETWORK, WidgetType.DEVICE ->
            HintText("Bu widget ayar gerektirmez. Değerler 15 dakikada bir ve dokunduğunda güncellenir.")

        WidgetType.WEATHER -> {
            CityPicker(d, getCurrent, onChange)
            SwitchRow("Ayrıntıları göster (hissedilen, nem, rüzgâr)", d.opt("details")) { onChange(d.withOpt("details", it)) }
        }

        WidgetType.FORECAST, WidgetType.SUN -> CityPicker(d, getCurrent, onChange)

        WidgetType.MOON -> HintText("Ay evresi cihazda hesaplanır, internet gerekmez.")
    }
}

// ---------- Ortak parçalar ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(values: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { v ->
            FilterChip(selected = v == selected, onClick = { onSelect(v) }, label = { Text(label(v)) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorldClockOptions(d: WidgetDesign, onChange: (WidgetDesign) -> Unit) {
    val zones = d.zones.ifEmpty { Zones.DEFAULT }
    SwitchRow("24 saat biçimi", d.clock24h) { onChange(d.copy(clock24h = it)) }
    Text("Şehirler (en fazla 4)")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Zones.ALL.forEach { zone ->
            val selected = zone.id in zones
            FilterChip(
                selected = selected,
                onClick = {
                    val next = if (selected) zones - zone.id else if (zones.size < 4) zones + zone.id else zones
                    if (next.isNotEmpty()) onChange(d.copy(zones = next))
                },
                label = { Text(zone.label) },
            )
        }
    }
}

@Composable
private fun ChecklistOptions(d: WidgetDesign, onChange: (WidgetDesign) -> Unit, allowClear: Boolean) {
    var newItem by remember { mutableStateOf("") }
    fun add() {
        if (newItem.isNotBlank()) {
            onChange(d.copy(todos = d.todos + TodoItem(newItem.trim())))
            newItem = ""
        }
    }

    OutlinedTextField(
        value = d.todoTitle,
        onValueChange = { onChange(d.copy(todoTitle = it)) },
        label = { Text("Başlık") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    d.todos.forEachIndexed { index, item ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = item.done,
                onCheckedChange = { checked ->
                    onChange(d.copy(todos = d.todos.mapIndexed { i, t -> if (i == index) t.copy(done = checked) else t }))
                },
            )
            OutlinedTextField(
                value = item.text,
                onValueChange = { text ->
                    onChange(d.copy(todos = d.todos.mapIndexed { i, t -> if (i == index) t.copy(text = text) else t }))
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onChange(d.copy(todos = d.todos.filterIndexed { i, _ -> i != index })) }) {
                Icon(Icons.Filled.Close, contentDescription = "Kaldır")
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = newItem,
            onValueChange = { newItem = it },
            placeholder = { Text("Yeni madde") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add() }),
            modifier = Modifier.weight(1f),
        )
        Button(onClick = { add() }) { Text("Ekle") }
    }
    if (allowClear && d.todos.any { it.done }) {
        OutlinedButton(onClick = { onChange(d.copy(todos = d.todos.filterNot { it.done })) }) {
            Text("Tamamlananları temizle")
        }
    }
}

@Composable
private fun CounterOptions(d: WidgetDesign, onChange: (WidgetDesign) -> Unit) {
    OutlinedTextField(
        value = d.label,
        onValueChange = { onChange(d.copy(label = it)) },
        label = { Text("Başlık") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = d.unit,
        onValueChange = { onChange(d.copy(unit = it)) },
        label = { Text("Birim (ör. bardak, ₺, sayfa)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    SliderRow("Her dokunuşta değişim", d.step.toFloat(), 1f..100f, "${d.step}") { onChange(d.copy(step = it.roundToInt())) }
    SliderRow("Hedef (0 = yok)", d.goal.toFloat(), 0f..200f, if (d.goal == 0) "yok" else "${d.goal}") {
        onChange(d.copy(goal = it.roundToInt()))
    }
    SwitchRow("Her gün sıfırla", d.opt("dailyReset")) { onChange(d.withOpt("dailyReset", it).copy(stateDay = 0)) }
    OutlinedButton(onClick = { onChange(d.copy(counter = 0)) }) { Text("Sayacı sıfırla") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityPicker(d: WidgetDesign, getCurrent: () -> WidgetDesign, onChange: (WidgetDesign) -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WeatherApi.City>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun search() {
        if (query.isBlank()) return
        scope.launch {
            loading = true
            error = null
            val found = withContext(Dispatchers.IO) { runCatching { WeatherApi.searchCity(query) } }
            loading = false
            found.onSuccess {
                results = it
                if (it.isEmpty()) error = "Sonuç bulunamadı"
            }.onFailure { error = "Bağlantı hatası, internetini kontrol et" }
        }
    }

    if (d.city.isNotBlank()) {
        Text("Seçili şehir: ${d.city}", style = MaterialTheme.typography.bodyLarge)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Şehir ara") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { search() }),
            modifier = Modifier.weight(1f),
        )
        if (loading) {
            CircularProgressIndicator(Modifier.size(28.dp))
        } else {
            IconButton(onClick = { search() }) { Icon(Icons.Filled.Search, contentDescription = "Ara") }
        }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    results.forEach { city ->
        ListItem(
            headlineContent = { Text(city.name) },
            supportingContent = { Text(city.detail) },
            modifier = Modifier.clickable {
                results = emptyList()
                query = ""
                onChange(d.copy(city = city.name, lat = city.lat, lon = city.lon, weather = null))
                // Önizleme için havayı hemen çek
                scope.launch {
                    val w = withContext(Dispatchers.IO) {
                        runCatching { WeatherApi.current(city.lat, city.lon) }.getOrNull()
                    }
                    val latest = getCurrent()
                    if (w != null && latest.lat == city.lat && latest.lon == city.lon) {
                        onChange(latest.copy(weather = w))
                    }
                }
            },
        )
    }
    HintText("Veriler Open-Meteo'dan gelir (anahtar gerekmez), 15 dakikada bir yenilenir. Widget'a dokununca hemen yenilenir. Yalnızca şehir koordinatı gönderilir.")
}
