package dev.rakara.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import dev.rakara.app.RakaraApp
import dev.rakara.app.data.NotificationSub
import dev.rakara.app.vm.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(nav: NavController) {
    val app = RakaraApp.from(LocalContext.current)
    val vm: NotificationViewModel = viewModel(
        factory = viewModelFactory {
            initializer { NotificationViewModel(app.engineRepo, app.notifications) }
        }
    )
    val s by vm.state.collectAsStateWithLifecycle()

    
    LaunchedEffect(Unit) { vm.checkForUpdates() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Notifikasi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    if (s.checking) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp).padding(end = 8.dp)
                        )
                    } else {
                        IconButton(onClick = { vm.checkForUpdates() }) {
                            Icon(Icons.Default.Refresh, "Cek update")
                        }
                    }
                    if (s.subs.any { it.unreadCount > 0 }) {
                        TextButton(onClick = { vm.markAllRead() }) {
                            Text("Tandai Dibaca")
                        }
                    }
                }
            )
        }
    ) { pad ->
        if (s.subs.isEmpty()) {
            EmptyNotif(Modifier.padding(pad))
        } else {
            LazyColumn(
                modifier = Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(s.subs, key = { "${it.sourceId}|${it.mangaUrl}" }) { sub ->
                    NotificationItem(
                        sub = sub,
                        onClick = {
                            vm.markRead(sub.sourceId, sub.mangaUrl)
                            nav.navigate("manga/${sub.sourceId}/${encode(sub.mangaUrl)}")
                        },
                        onDelete = { vm.unsubscribe(sub.sourceId, sub.mangaUrl) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    sub: NotificationSub,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val hasUnread = sub.unreadCount > 0
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (hasUnread) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (hasUnread) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SubcomposeAsyncImage(
                model = sub.mangaThumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    sub.mangaTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (hasUnread)
                        "Chapter baru: ${sub.lastKnownChapterName ?: "Update"}"
                    else "Chapter terbaru: ${sub.lastKnownChapterName ?: "-"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasUnread) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatTimeAgo(sub.lastCheckedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (hasUnread) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        sub.unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus langganan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNotif(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.NotificationsNone, null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text("Belum ada notifikasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Berlangganan komik dari halaman detail untuk dapat kabar chapter baru.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatTimeAgo(ts: Long): String {
    if (ts <= 0L) return ""
    val diff = System.currentTimeMillis() - ts
    if (diff < 0) return "Baru saja"
    val m = diff / (1000 * 60)
    val h = diff / (1000 * 60 * 60)
    val d = diff / (1000 * 60 * 60 * 24)
    return when {
        m < 1 -> "Baru saja"
        m < 60 -> "$m menit lalu"
        h < 24 -> "$h jam lalu"
        d < 30 -> "$d hari lalu"
        else -> "${d / 30} bulan lalu"
    }
}