package com.eren.widgetstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eren.widgetstudio.data.DesignRepository
import com.eren.widgetstudio.data.WidgetDesign
import com.eren.widgetstudio.data.WidgetType
import com.eren.widgetstudio.data.newDesign
import com.eren.widgetstudio.ui.AppTheme
import com.eren.widgetstudio.ui.EditorScreen
import com.eren.widgetstudio.ui.WidgetPreview
import com.eren.widgetstudio.widget.WidgetUpdater
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WidgetUpdater.schedule(this)
        setContent { AppTheme { App() } }
    }
}

@Composable
private fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var designs by remember { mutableStateOf(DesignRepository.all(context)) }
    var editing by remember { mutableStateOf<WidgetDesign?>(null) }
    var editingIsNew by remember { mutableStateOf(false) }
    var showTypePicker by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<WidgetDesign?>(null) }

    val current = editing
    if (current != null) {
        BackHandler { editing = null }
        EditorScreen(
            initial = current,
            isNew = editingIsNew,
            onSave = { design ->
                DesignRepository.save(context, design)
                designs = DesignRepository.all(context)
                editing = null
                scope.launch { WidgetUpdater.refreshAll(context) }
                if (design.type == WidgetType.WEATHER) WidgetUpdater.refreshNow(context)
            },
            onCancel = { editing = null },
            onDelete = { pendingDelete = it },
        )
    } else {
        DesignListScreen(
            designs = designs,
            onOpen = {
                editingIsNew = false
                editing = it
            },
            onNew = { showTypePicker = true },
        )
    }

    if (showTypePicker) {
        AlertDialog(
            onDismissRequest = { showTypePicker = false },
            title = { Text("Widget türü seç") },
            text = {
                Column {
                    WidgetType.entries.forEach { type ->
                        ListItem(
                            headlineContent = { Text("${type.emoji}  ${type.label}") },
                            modifier = Modifier.clickable {
                                showTypePicker = false
                                editingIsNew = true
                                editing = newDesign(type)
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showTypePicker = false }) { Text("Vazgeç") } },
        )
    }

    pendingDelete?.let { target ->
        val usage = remember(target.id) { DesignRepository.usageCount(context, target.id) }
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("\"${target.name}\" silinsin mi?") },
            text = {
                Text(
                    if (usage > 0) "Bu tasarım ana ekranda $usage widget'ta kullanılıyor. Silersen o widget'lar boş kalır."
                    else "Bu işlem geri alınamaz.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    DesignRepository.delete(context, target.id)
                    designs = DesignRepository.all(context)
                    pendingDelete = null
                    editing = null
                    scope.launch { WidgetUpdater.refreshAll(context) }
                }) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Vazgeç") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesignListScreen(
    designs: List<WidgetDesign>,
    onOpen: (WidgetDesign) -> Unit,
    onNew: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Widget Stüdyo") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Yeni widget") },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { HowToCard(empty = designs.isEmpty()) }
            items(designs, key = { it.id }) { design ->
                Card(onClick = { onOpen(design) }, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .background(Brush.linearGradient(listOf(Color(0xFF5B6CF9), Color(0xFFE86ED0))))
                            .padding(16.dp),
                    ) {
                        WidgetPreview(design, Modifier.fillMaxWidth().height(158.dp))
                    }
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("${design.type.emoji}  ${design.name}", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun HowToCard(empty: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (empty) "Henüz tasarımın yok" else "Ana ekrana nasıl eklenir?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                (if (empty) "Önce \"Yeni widget\" ile bir tasarım oluştur. " else "") +
                    "Sonra ana ekranda boş bir yere uzun bas → Widget'lar → \"Widget Stüdyo\"yu seç ve " +
                    "açılan listeden tasarımını seç. Tasarımı burada değiştirince ana ekrandaki widget da güncellenir.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
