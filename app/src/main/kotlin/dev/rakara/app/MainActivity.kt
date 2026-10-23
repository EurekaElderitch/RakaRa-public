package dev.rakara.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.rakara.app.ui.BrowseScreen
import dev.rakara.app.ui.EnginesScreen
import dev.rakara.app.ui.HomeScreen
import dev.rakara.app.ui.LibraryScreen
import dev.rakara.app.ui.MangaDetailScreen
import dev.rakara.app.ui.ReaderScreen
import dev.rakara.app.ui.RakaraTheme
import dev.rakara.app.ui.SettingsScreen
import dev.rakara.app.ui.SplashScreen
import dev.rakara.app.update.ApkUpdateInfo
import dev.rakara.app.update.ApkUpdater
import kotlinx.coroutines.Job
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

    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        title = { Text("Update Tersedia (v${info.version})") },
        text = {
            Column {
                if (downloadedFile != null) {
                    Text("APK sudah diunduh. Tap \"Install\" untuk melanjutkan.")
                } else if (downloading) {
                    Text("Mengunduh... ${(progress * 100).toInt()}%")
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    info.changelog?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        "Ukuran: ${"%.1f".format(info.size / 1024f / 1024f)} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            if (downloadedFile == null) {
                Button(
                    onClick = {
                        scope.launch {
                            downloading = true
                            error = null
                            val file = updater.downloadApk(info) { dl, total ->
                                if (total > 0) progress = dl.toFloat() / total
                            }
                            downloading = false
                            if (file != null) downloadedFile = file
                            else error = "Gagal mengunduh. Coba lagi."
                        }
                    },
                    enabled = !downloading
                ) { Text(if (downloading) "Mengunduh..." else "Unduh") }
            } else {
                Button(onClick = {
                    updater.installApk(downloadedFile!!)
                    onDismiss()
                }) { Text("Install") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !downloading) { Text("Nanti") }
        }
    )
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