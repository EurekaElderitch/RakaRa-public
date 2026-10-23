package dev.rakara.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.rakara.app.ui.*
import dev.rakara.app.update.ApkUpdateInfo
import dev.rakara.app.update.ApkUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RakaraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showSplash by remember { mutableStateOf(true) }
                    var updateInfo by remember { mutableStateOf<ApkUpdateInfo?>(null) }
                    var splashStatus by remember { mutableStateOf("Menyiapkan...") }
                    var engineError by remember { mutableStateOf<String?>(null) }
                    var showSkipButton by remember { mutableStateOf(false) }

                    if (showSplash) {
                        SplashScreen()
                        LaunchedEffect(Unit) {
                            val start = System.currentTimeMillis()
                            val app = RakaraApp.from(this@MainActivity)

                            
                            val skipTimer = launch {
                                delay(20_000)
                                showSkipButton = true
                            }

                            
                            splashStatus = "Menyiapkan engine..."
                            try {
                                withTimeoutOrNull(60_000) {
                                    app.engineUpdater.checkAndUpdate()
                                }
                            } catch (e: Exception) {
                                println("RAKARA: Engine update failed: ${e.message}")
                                engineError = e.message
                            }

                            
                            val installedCount = runCatching {
                                app.engines.installed().size
                            }.getOrDefault(0)
                            println("RAKARA: Engine installed count = $installedCount")

                            if (installedCount == 0) {
                                engineError = "Engine belum terinstall. Buka Kelola Sumber untuk coba download manual."
                            }

                            
                            splashStatus = "Memeriksa update aplikasi..."
                            try {
                                withTimeoutOrNull(15_000) {
                                    updateInfo = ApkUpdater(this@MainActivity).checkForUpdate()
                                }
                            } catch (_: Exception) {}

                            skipTimer.cancel()

                            
                            val elapsed = System.currentTimeMillis() - start
                            if (elapsed < 1400) delay(1400 - elapsed)

                            showSplash = false
                        }

                        
                        if (showSkipButton) {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Column(
                                    Modifier.padding(bottom = 100.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        splashStatus,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    OutlinedButton(
                                        onClick = { showSplash = false },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Lewati (engine mungkin kosong)")
                                    }
                                }
                            }
                        }
                    } else {
                        App()

                        
                        updateInfo?.let { info ->
                            ApkUpdateDialog(
                                info = info,
                                onDismiss = { updateInfo = null },
                            )
                        }

                        
                        engineError?.let { err ->
                            AlertDialog(
                                onDismissRequest = { engineError = null },
                                title = { Text("Peringatan") },
                                text = { Text(err) },
                                confirmButton = {
                                    Button(onClick = { engineError = null }) { Text("OK") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApkUpdateDialog(info: ApkUpdateInfo, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val updater = remember { ApkUpdater(ctx) }

    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    Dialog(onDismissRequest = { if (!downloading) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                    
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 4.dp,
                        modifier = Modifier.align(Alignment.BottomCenter).offset(y = 12.dp)
                    ) {
                        Text("✨ NEW", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("Update Tersedia", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Pengalaman membaca yang lebih baik telah siap untukmu.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

                Spacer(Modifier.height(16.dp))
                
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp)) {
                    Text("v${BuildConfig.VERSION_NAME} → v${info.version}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }

                Spacer(Modifier.height(24.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("APA YANG BARU", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("Build ${info.version.replace(".", "")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(12.dp))
                        
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(info.changelog ?: "Perbaikan bug dan peningkatan performa.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                
                if (downloading) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                    Spacer(Modifier.height(8.dp))
                    Text("Mengunduh... ${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                } else if (downloadedFile != null) {
                    Button(
                        onClick = { updater.installApk(downloadedFile!!); onDismiss() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Install Sekarang", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                downloading = true
                                error = null
                                val file = updater.downloadApk(info) { dl, total -> if (total > 0) progress = dl.toFloat() / total }
                                downloading = false
                                if (file != null) downloadedFile = file else error = "Gagal mengunduh."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Download & Install (${"%.1f".format(info.size / 1024f / 1024f)} MB)", fontWeight = FontWeight.Bold)
                    }
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, enabled = !downloading) {
                    Text("Ingatkan Nanti", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun App() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "home") {
        composable("home") { HomeScreen(nav) }
        composable("library") { LibraryScreen(nav) }
        composable("settings") { SettingsScreen(nav) }
        composable("engines") { EnginesScreen(nav) }

        composable(
            route = "source/{sourceId}",
            arguments = listOf(navArgument("sourceId") { type = NavType.StringType })
        ) { e ->
            BrowseScreen(nav, e.arguments?.getString("sourceId").orEmpty())
        }

        composable(
            route = "manga/{sourceId}/{url}/{title}",
            arguments = listOf(
                navArgument("sourceId") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType; defaultValue = "" }
            )
        ) { e ->
            val a = e.arguments
            MangaDetailScreen(
                nav,
                a?.getString("sourceId").orEmpty(),
                a?.getString("url").orEmpty(),
            )
        }

        composable(
            route = "reader/{sourceId}/{manga}/{chapter}",
            arguments = listOf(
                navArgument("sourceId") { type = NavType.StringType },
                navArgument("manga") { type = NavType.StringType },
                navArgument("chapter") { type = NavType.StringType }
            )
        ) { e ->
            val a = e.arguments
            ReaderScreen(
                nav,
                a?.getString("sourceId").orEmpty(),
                a?.getString("manga").orEmpty(),
                a?.getString("chapter").orEmpty(),
            )
        }
    }
}