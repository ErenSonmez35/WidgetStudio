package com.eren.widgetstudio

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.eren.widgetstudio.data.DesignRepository
import com.eren.widgetstudio.data.WidgetDesign
import com.eren.widgetstudio.ui.AppTheme
import com.eren.widgetstudio.ui.WidgetPreview
import com.eren.widgetstudio.widget.WidgetUpdater
import kotlinx.coroutines.launch

/** Ana ekrana widget eklenirken (veya uzun basıp yeniden yapılandırırken) açılan tasarım seçici. */
class ConfigureActivity : ComponentActivity() {

    private val designs = mutableStateOf<List<WidgetDesign>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Kullanıcı seçim yapmadan çıkarsa widget eklenmesin
        setResult(RESULT_CANCELED, resultIntent(appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            AppTheme {
                ConfigureScreen(
                    designs = designs.value,
                    onPick = { design ->
                        DesignRepository.assign(this, appWidgetId, design.id)
                        WidgetUpdater.schedule(this)
                        lifecycleScope.launch {
                            WidgetUpdater.refreshAll(this@ConfigureActivity)
                            if (design.weather == null && design.city.isNotBlank()) {
                                WidgetUpdater.refreshNow(this@ConfigureActivity)
                            }
                            setResult(RESULT_OK, resultIntent(appWidgetId))
                            finish()
                        }
                    },
                    onCreateNew = {
                        startActivity(Intent(this, MainActivity::class.java))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Yeni tasarım oluşturup geri dönünce liste tazelensin
        designs.value = DesignRepository.all(this)
    }

    private fun resultIntent(appWidgetId: Int) =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigureScreen(
    designs: List<WidgetDesign>,
    onPick: (WidgetDesign) -> Unit,
    onCreateNew: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Bir tasarım seç") }) }) { padding ->
        if (designs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Henüz hiç tasarımın yok.")
                    Button(onClick = onCreateNew) { Text("Tasarım oluştur") }
                }
            }
        } else LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(designs, key = { it.id }) { design ->
                Card(onClick = { onPick(design) }, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .background(Brush.linearGradient(listOf(Color(0xFF5B6CF9), Color(0xFFE86ED0))))
                            .padding(14.dp),
                    ) {
                        WidgetPreview(design, Modifier.fillMaxWidth().height(142.dp))
                    }
                    Text(
                        "${design.type.emoji}  ${design.name}",
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
            item {
                Button(onClick = onCreateNew, modifier = Modifier.fillMaxWidth()) { Text("Yeni tasarım oluştur") }
            }
        }
    }
}
