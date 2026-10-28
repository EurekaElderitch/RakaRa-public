package dev.rakara.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import dev.rakara.app.RakaraApp
import dev.rakara.app.data.LibraryEntry
import dev.rakara.app.data.NotificationSub

private enum class SortMode(val label: String) {
    NEWEST("Baru Ditambahkan"),
    TITLE_AZ("Judul (A-Z)"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(nav: NavController) {
    val app = RakaraApp.from(LocalContext.current)
    val entries by app.library.state.collectAsStateWithLifecycle()
    val subs by app.notifications.state.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSortModal by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(SortMode.NEWEST) }

    
    val sortedEntries = remember(entries, sortMode) {
        when (sortMode) {
            SortMode.NEWEST -> entries.sortedByDescending { it.addedAt }
            SortMode.TITLE_AZ -> entries.sortedBy { it.title.lowercase() }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Library", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                if (selectedTab == 0) {
                    IconButton(onClick = { showSortModal = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Sort", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        bottomBar = { BottomNav(nav, current = "library") }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Koleksi") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Subscribed") })
            }

            if (selectedTab == 0) {
                KoleksiTab(
                    entries = sortedEntries,
                    onClick = { e -> nav.navigate("manga/${e.sourceId}/${encode(e.mangaUrl)}") }
                )
            } else {
                SubscribedTab(
                    subs = subs,
                    onClick = { sub -> nav.navigate("manga/${sub.sourceId}/${encode(sub.mangaUrl)}") }
                )
            }
        }

        if (showSortModal) {
            ModalBottomSheet(onDismissRequest = { showSortModal = false }) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Urutkan Berdasarkan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    SortMode.entries.forEachIndexed { i, mode ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { sortMode = mode; showSortModal = false }
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                            RadioButton(
                                selected = sortMode == mode,
                                onClick = { sortMode = mode; showSortModal = false }
                            )
                        }
                        if (i != SortMode.entries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun KoleksiTab(entries: List<LibraryEntry>, onClick: (LibraryEntry) -> Unit) {
    if (entries.isEmpty()) {
        EmptyBox("Belum ada koleksi.\nTambahkan komik dari halaman detail.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        gridItems(entries, key = { "${it.sourceId}|${it.mangaUrl}" }) { e ->
            Column(Modifier.clickable { onClick(e) }) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(0.75f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    SubcomposeAsyncImage(
                        model = e.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(e.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Lanjut Baca", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SubscribedTab(subs: List<NotificationSub>, onClick: (NotificationSub) -> Unit) {
    if (subs.isEmpty()) {
        EmptyBox("Belum ada langganan.\nKetuk ikon lonceng di halaman detail komik\nuntuk berlangganan chapter baru.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(subs, key = { "${it.sourceId}|${it.mangaUrl}" }) { sub ->
            val hasUnread = sub.unreadCount > 0
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (hasUnread) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { onClick(sub) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubcomposeAsyncImage(
                    model = sub.mangaThumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.width(16.dp))
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
                        if (hasUnread) "Chapter baru: ${sub.lastKnownChapterName ?: "-"}"
                        else "Chapter terbaru: ${sub.lastKnownChapterName ?: "-"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasUnread) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (hasUnread) {
                    Box(
                        Modifier.size(22.dp).clip(RoundedCornerShape(11.dp))
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
                }
            }
        }
    }
}