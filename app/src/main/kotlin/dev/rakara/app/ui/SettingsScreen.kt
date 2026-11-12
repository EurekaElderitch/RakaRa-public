package dev.rakara.app.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.rakara.app.BuildConfig
import dev.rakara.app.data.HomeDataCache
import dev.rakara.app.data.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController, prefsRepo: UserPreferencesRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val userKey by prefsRepo.userKey.collectAsState(initial = null)
    val keyActivatedAt by prefsRepo.keyActivatedAt.collectAsState(initial = null)
    val keyExpiresAt by prefsRepo.keyExpiresAt.collectAsState(initial = null)

    var showLogoutConfirm by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        cacheSize = withContext(Dispatchers.IO) { calculateCacheSize(context) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            
            SectionHeader("SUMBER")
            SettingRow(
                title = "Kelola Sumber",
                subtitle = "Lihat, aktifkan, atau hapus engine",
                onClick = { nav.navigate("engines") },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            
            if (!userKey.isNullOrBlank()) {
                SectionHeader("AKUN")
                InfoRow("Status", "Aktif")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                InfoRow("Diaktivasi", formatDate(keyActivatedAt))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                InfoRow("Berlaku hingga", formatExpiry(keyExpiresAt))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingRow(
                    title = "Logout",
                    subtitle = "Hapus key dari perangkat ini",
                    onClick = { showLogoutConfirm = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            
            SectionHeader("DATA")
            SettingRow(
                title = "Hapus Cache",
                subtitle = when (val c = cacheSize) {
                    null -> "Menghitung..."
                    else -> "Ukuran saat ini: ${formatSize(c)}"
                },
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                        }
                        HomeDataCache.clear()
                        cacheSize = withContext(Dispatchers.IO) { calculateCacheSize(context) }
                    }
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            
            SectionHeader("TENTANG")
            InfoRow("Versi", BuildConfig.VERSION_NAME)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            InfoRow("Dibuat oleh", "HynchLabs")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            InfoRow("Aplikasi", "RakaRa")

            Spacer(Modifier.height(48.dp))

            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "HynchLabs",
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }

    
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout?") },
            text = {
                Text("Key akan dihapus dari perangkat ini. Kamu harus memasukkan key lagi untuk mengakses aplikasi.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    scope.launch {
                        prefsRepo.clearKey()
                        nav.navigate("gate") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}



private fun formatDate(millis: Long?): String {
    if (millis == null || millis <= 0L) return "Tidak diketahui"
    return SimpleDateFormat("d MMM yyyy", Locale("id", "ID")).format(Date(millis))
}

private fun formatExpiry(seconds: Long?): String {
    if (seconds == null || seconds <= 0L) return "Tidak diketahui"
    return SimpleDateFormat("d MMM yyyy", Locale("id", "ID")).format(Date(seconds * 1000L))
}

private fun calculateCacheSize(context: Context): Long = runCatching {
    context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}.getOrDefault(0L)

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
    else -> "%.1f MB".format(bytes / (1024f * 1024f))
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(key: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(key, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}