package com.eren.widgetstudio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.eren.widgetstudio.data.TodoItem
import com.eren.widgetstudio.data.WeatherApi
import com.eren.widgetstudio.data.WidgetDesign
import com.eren.widgetstudio.data.WidgetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val PALETTE = listOf(
    0xFF000000, 0xFF1E1E2E, 0xFF2D3142, 0xFF1A3A5C, 0xFF6C7086, 0xFFFFFFFF,
    0xFFF5F0E6, 0xFF89B4FA, 0xFF74C7EC, 0xFF94E2D5, 0xFFA6E3A1, 0xFFF9E2AF,
    0xFFFAB387, 0xFFF38BA8, 0xFFCBA6F7, 0xFFE64553,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initial: WidgetDesign,
    isNew: Boolean,
    onSave: (WidgetDesign) -> Unit,
    onCancel: () -> Unit,
    onDelete: (WidgetDesign) -> Unit,
) {
    var d by remember(initial.id) { mutableStateOf(initial) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Yeni widget" else "Düzenle") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { onDelete(d) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Sil")
                        }
                    }
                    TextButton(onClick = { onSave(d) }) { Text("Kaydet") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Sahte duvar kağıdı üzerinde canlı önizleme
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFF5B6CF9), Color(0xFFE86ED0), Color(0xFFFFB86B))))
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                val wide = d.type != WidgetType.CLOCK
                WidgetPreview(
                    d,
                    Modifier
                        .fillMaxHeight()
                        .then(if (wide) Modifier.widthIn(max = 320.dp).fillMaxWidth() else Modifier.width(182.dp)),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = d.name,
                    onValueChange = { d = d.copy(name = it) },
                    label = { Text("Tasarım adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                SectionTitle("${d.type.emoji} ${d.type.label}")
                when (d.type) {
                    WidgetType.CLOCK -> ClockOptions(d) { d = it }
                    WidgetType.TODO -> TodoOptions(d) { d = it }
                    WidgetType.WEATHER -> WeatherOptions(d, getCurrent = { d }) { d = it }
                    WidgetType.SYSTEM -> SystemOptions(d) { d = it }
                }

                HorizontalDivider()
                SectionTitle("🎨 Görünüm")
                ColorPicker("Arka plan", d.bgColor) { d = d.copy(bgColor = it) }
                SliderRow("Arka plan opaklığı", d.bgAlpha, 0f..1f, "%${(d.bgAlpha * 100).roundToInt()}") {
                    d = d.copy(bgAlpha = it)
                }
                ColorPicker("Yazı rengi", d.textColor) { d = d.copy(textColor = it) }
                ColorPicker("Vurgu rengi", d.accentColor) { d = d.copy(accentColor = it) }
                SliderRow("Yazı boyutu", d.fontScale, 0.6f..2f, "x${"%.1f".format(d.fontScale)}") {
                    d = d.copy(fontScale = it)
                }
                SliderRow("Köşe yuvarlaklığı", d.cornerRadius.toFloat(), 0f..40f, "${d.cornerRadius} dp") {
                    d = d.copy(cornerRadius = it.roundToInt())
                }
                SliderRow("İç boşluk", d.padding.toFloat(), 0f..32f, "${d.padding} dp") {
                    d = d.copy(padding = it.roundToInt())
                }
                if (d.type != WidgetType.TODO) {
                    SwitchRow("İçeriği ortala", d.centered) { d = d.copy(centered = it) }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ---------- Türe özel ayarlar ----------

@Composable
private fun ClockOptions(d: WidgetDesign, onChange: (WidgetDesign) -> Unit) {
    SwitchRow("24 saat biçimi", d.clock24h) { onChange(d.copy(clock24h = it)) }
    SwitchRow("Saniyeleri göster", d.clockSeconds) { onChange(d.copy(clockSeconds = it)) }
    SwitchRow("Tarihi göster", d.clockShowDate) { onChange(d.copy(clockShowDate = it)) }
}

@Composable
private fun TodoOptions(d: WidgetDesign, onChange: (WidgetDesign) -> Unit) {
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
            placeholder = { Text("Yeni görev") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add() }),
            modifier = Modifier.weight(1f),
        )
        Button(onClick = { add() }) { Text("Ekle") }
    }
    if (d.todos.any { it.done }) {
        OutlinedButton(onClick = { onChange(d.copy(todos = d.todos.filterNot { it.done })) }) {
            Text("Tamamlananları temizle")
        }
    }
}

@Composable
private fun WeatherOptions(
    d: WidgetDesign,
    getCurrent: () -> WidgetDesign,
    onChange: (WidgetDesign) -> Unit,
) {
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
    Text(
        "Veriler Open-Meteo'dan gelir, 30 dakikada bir yenilenir. Widget'a dokununca hemen yenilenir.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SystemOptions(d: WidgetDesign, onChange: (WidgetDesign) -> Unit) {
    SwitchRow("Pil", d.showBattery) { onChange(d.copy(showBattery = it)) }
    SwitchRow("Depolama", d.showStorage) { onChange(d.copy(showStorage = it)) }
    SwitchRow("RAM kullanımı", d.showRam) { onChange(d.copy(showRam = it)) }
    Text(
        "Değerler 30 dakikada bir ve widget'a dokunduğunda güncellenir.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ---------- Ortak kontroller ----------

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onChange: (Float) -> Unit,
) {
    Column {
        Row {
            Text(label, modifier = Modifier.weight(1f))
            Text(valueText, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun ColorPicker(label: String, selected: Long, onSelect: (Long) -> Unit) {
    var hex by remember(selected) { mutableStateOf(toHex(selected)) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = hex,
                onValueChange = { input ->
                    hex = input
                    parseHex(input)?.let(onSelect)
                },
                singleLine = true,
                modifier = Modifier.width(130.dp),
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PALETTE) { color ->
                val isSelected = (color and 0xFFFFFF) == (selected and 0xFFFFFF)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(
                            BorderStroke(
                                if (isSelected) 3.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            ),
                            CircleShape,
                        )
                        .padding(3.dp)
                        .background(Color(color), CircleShape)
                        .clickable { onSelect(color) },
                )
            }
        }
    }
}

private fun toHex(color: Long): String = "#%06X".format(color and 0xFFFFFF)

private fun parseHex(input: String): Long? {
    val clean = input.trim().removePrefix("#")
    if (clean.length != 6) return null
    return clean.toLongOrNull(16)?.let { 0xFF000000 or it }
}
