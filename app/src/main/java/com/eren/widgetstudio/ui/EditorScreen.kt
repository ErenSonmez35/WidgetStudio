package com.eren.widgetstudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eren.widgetstudio.catalog.WidgetSpecs
import com.eren.widgetstudio.data.WidgetDesign
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initial: WidgetDesign,
    isNew: Boolean,
    onSave: (WidgetDesign) -> Unit,
    onCancel: () -> Unit,
    onDelete: (WidgetDesign) -> Unit,
    onDuplicate: (WidgetDesign) -> Unit = {},
) {
    val spec = WidgetSpecs.of(initial.type)
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
                        TextButton(onClick = { onDuplicate(d) }) { Text("Çoğalt") }
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
                val wide = spec.wide
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
                TypeOptions(d, getCurrent = { d }) { d = it }

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
                if (!spec.alignTop) {
                    SwitchRow("İçeriği ortala", d.centered) { d = d.copy(centered = it) }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
