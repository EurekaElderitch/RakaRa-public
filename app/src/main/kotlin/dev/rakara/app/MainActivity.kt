package dev.rakara.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
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
            
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    window.navigationBarColor = android.graphics.Color.parseColor("#F7F5F0")
                    WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
                }
            }

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
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    Dialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss, enabled = !downloading) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Tutup") 
                    }
                    Icon(Icons.Default.Info, "Info", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.weight(1f))

                
                Image(
                    painter = painterResource(id = R.drawable.mascot_splash),
                    contentDescription = "App Icon",
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp))
                )
                
                Spacer(Modifier.height(24.dp))
                Text("A little better, page by\npage.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("RakaRa ${info.version} • ${"%.1f".format(info.size / 1024f / 1024f)} MB", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(Modifier.height(32.dp))

                
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Made for the next chapter", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("More page. Less chrome.", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("A gentler Auto Scroll.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                
                if (downloading) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Downloading update", style = MaterialTheme.typography.labelSmall)
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = {  }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Pause") }
                        OutlinedButton(onClick = { downloading = false }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                    }
                } else if (downloadedFile != null) {
                    Button(onClick = { updater.installApk(downloadedFile!!); onDismiss() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                        Text("Install Update", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                downloading = true
                                val file = updater.downloadApk(info) { dl, total -> if (total > 0) progress = dl.toFloat() / total }
                                downloading = false
                                if (file != null) downloadedFile = file
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Download update • ${"%.1f".format(info.size / 1024f / 1024f)} MB", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) { Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Text("Your bookmarks and reading history stay safe.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
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