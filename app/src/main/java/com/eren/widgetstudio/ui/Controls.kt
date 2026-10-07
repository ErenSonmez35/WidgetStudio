package com.eren.widgetstudio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Düzenleyicide ortak kullanılan küçük kontroller. Yeni bir widget'ın ayarlarında da kullan. */

internal val PALETTE = listOf(
    0xFF000000, 0xFF1E1E2E, 0xFF2D3142, 0xFF1A3A5C, 0xFF6C7086, 0xFFFFFFFF,
    0xFFF5F0E6, 0xFF89B4FA, 0xFF74C7EC, 0xFF94E2D5, 0xFFA6E3A1, 0xFFF9E2AF,
    0xFFFAB387, 0xFFF38BA8, 0xFFCBA6F7, 0xFFE64553,
)

@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
internal fun HintText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
internal fun SliderRow(
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
internal fun ColorPicker(label: String, selected: Long, onSelect: (Long) -> Unit) {
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

/** Gün/ay/yıl seçici. Değer `LocalDate.toEpochDay()` olarak saklanır. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateField(label: String, epochDay: Long, onChange: (Long) -> Unit) {
    var show by remember { mutableStateOf(false) }
    val text = LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr-TR")))
    OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: $text") }
    if (show) {
        val state = rememberDatePickerState(initialSelectedDateMillis = epochDay * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    // Seçici, seçilen günün UTC gece yarısını verir.
                    state.selectedDateMillis?.let { onChange(Math.floorDiv(it, 86_400_000L)) }
                    show = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Vazgeç") } },
        ) { DatePicker(state = state) }
    }
}

internal fun toHex(color: Long): String = "#%06X".format(color and 0xFFFFFF)

internal fun parseHex(input: String): Long? {
    val clean = input.trim().removePrefix("#")
    if (clean.length != 6) return null
    return clean.toLongOrNull(16)?.let { 0xFF000000 or it }
}
