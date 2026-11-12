package dev.rakara.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import dev.rakara.app.RakaraApp
import dev.rakara.app.data.ReadMark
import dev.rakara.app.vm.MangaDetailViewModel
import dev.rakara.source.SChapter
import dev.rakara.source.SManga

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailScreen(nav: NavController, sourceId: String, mangaUrl: String) {
    val app = RakaraApp.from(LocalContext.current)
    val vm: MangaDetailViewModel = viewModel(
        key = "manga-$sourceId-$mangaUrl",
        factory = viewModelFactory {
            initializer {
                MangaDetailViewModel(
                    app.engineRepo,
                    app.library,
                    app.notifications,
                    sourceId,
                    mangaUrl,
                )
            }
        }
    )
    val s by vm.state.collectAsStateWithLifecycle()

    val readMarks by app.readMarks.state.collectAsStateWithLifecycle()

    var expandedSynopsis by remember { mutableStateOf(false) }
    var showChaptersSheet by remember { mutableStateOf(false) }

    val firstChapter = s.chapters.minByOrNull { it.chapterNumber }
    val lastReadMark = readMarks
        .filter { it.sourceId == sourceId && it.mangaUrl == mangaUrl }
        .maxByOrNull { it.updatedAt }
    val resumeChapter = lastReadMark?.let { mark ->
        s.chapters.firstOrNull { it.url == mark.chapterUrl }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = vm::toggleSubscription) {
                        Icon(
                            if (s.subscribed) Icons.Default.NotificationsActive
                            else Icons.Default.NotificationsNone,
                            contentDescription = if (s.subscribed) "Berhenti berlangganan" else "Berlangganan",
                            tint = if (s.subscribed) MaterialTheme.colorScheme.primary
                                   else LocalContentColor.current
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (s.manga != null && s.chapters.isNotEmpty()) {
                val targetChapter = resumeChapter ?: firstChapter
                if (targetChapter != null) {
                    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                        Box(Modifier.padding(16.dp).fillMaxWidth()) {
                            Button(
                                onClick = {
                                    nav.navigate("reader/$sourceId/${encode(s.manga!!.url)}/${encode(targetChapter.url)}")
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    if (resumeChapter != null) Icons.Default.PlayArrow else Icons.Default.MenuBook,
                                    null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (resumeChapter != null) "Lanjut Baca ${resumeChapter.name}"
                                    else "Mulai Baca ${targetChapter.name}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { pad ->
        if (s.manga != null) {
            val m = s.manga!!
            val previewChapters = s.chapters.take(5)

            LazyColumn(
                modifier = Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                
                item {
                    Row(Modifier.fillMaxWidth().padding(20.dp)) {
                        SubcomposeAsyncImage(
                            model = m.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.width(120.dp).height(170.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Text(m.author ?: "Unknown", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            m.genre.firstOrNull()?.takeIf { it.isNotBlank() }?.let { type ->
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        type,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                m.description ?: "Belum ada sinopsis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (expandedSynopsis) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (expandedSynopsis) "Sembunyikan" else "Read More",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp).clickable { expandedSynopsis = !expandedSynopsis }
                            )
                        }
                    }
                }

                
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                firstChapter?.let {
                                    nav.navigate("reader/$sourceId/${encode(m.url)}/${encode(it.url)}")
                                }
                            },
                            enabled = firstChapter != null,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.MenuBook, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Mulai Baca", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = vm::toggleLibrary,
                            modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                if (s.inLibrary) Icons.Default.Check else Icons.Default.Add,
                                null,
                                tint = if (s.inLibrary) Color.Green else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Chapters (${s.chapters.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            StatusChip(m.status)
                        }
                        Text(
                            "Terbaru",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                
                items(previewChapters, key = { it.url }) { c ->
                    val isLastRead = c.url == lastReadMark?.chapterUrl
                    ChapterRow(
                        chapter = c,
                        isLastRead = isLastRead,
                        onClick = { nav.navigate("reader/$sourceId/${encode(m.url)}/${encode(c.url)}") }
                    )
                }

                
                if (s.chapters.size > 5) {
                    item {
                        TextButton(
                            onClick = { showChaptersSheet = true },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.List, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Lihat Semua Chapter (${s.chapters.size})",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    
    if (showChaptersSheet && s.manga != null) {
        val m = s.manga!!
        ModalBottomSheet(
            onDismissRequest = { showChaptersSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            ChapterListSheet(
                chapters = s.chapters,
                lastReadMark = lastReadMark,
                onNavigate = { c ->
                    showChaptersSheet = false
                    nav.navigate("reader/$sourceId/${encode(m.url)}/${encode(c.url)}")
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterListSheet(
    chapters: List<SChapter>,
    lastReadMark: ReadMark?,
    onNavigate: (SChapter) -> Unit,
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(chapters, query) {
        if (query.isBlank()) chapters
        else chapters.filter { c ->
            c.name.contains(query, ignoreCase = true) ||
            c.chapterNumber.toString().contains(query, ignoreCase = true)
        }
    }

    Column(Modifier.fillMaxWidth()) {
        
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Semua Chapter",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${filtered.size} / ${chapters.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Cari chapter (misal: 120)") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, "Hapus")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Search,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )

        
        if (filtered.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Chapter tidak ditemukan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(filtered, key = { it.url }) { c ->
                    val isLastRead = c.url == lastReadMark?.chapterUrl
                    ChapterRow(
                        chapter = c,
                        isLastRead = isLastRead,
                        onClick = { onNavigate(c) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: SManga.Status) {
    val (label, color) = when (status) {
        SManga.Status.ONGOING -> "Ongoing" to MaterialTheme.colorScheme.secondary
        SManga.Status.COMPLETED -> "Completed" to MaterialTheme.colorScheme.primary
        SManga.Status.ON_HIATUS -> "Hiatus" to MaterialTheme.colorScheme.tertiary
        else -> "Unknown" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ChapterRow(chapter: SChapter, isLastRead: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isLastRead) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isLastRead) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(chapter.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    if (isLastRead) {
                        Spacer(Modifier.width(8.dp))
                        Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                "Terakhir",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                val dateText = formatChapterDate(chapter.dateUpload)
                if (dateText.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(dateText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                if (isLastRead) Icons.Default.PlayArrow else Icons.Default.KeyboardArrowRight,
                null,
                tint = if (isLastRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun formatChapterDate(ts: Long): String {
    if (ts <= 0L) return ""
    val diff = System.currentTimeMillis() - ts
    if (diff < 0) return "Baru saja"
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)
    return when {
        minutes < 1 -> "Baru saja"
        minutes < 60 -> "$minutes menit lalu"
        hours < 24 -> "$hours jam lalu"
        days < 7 -> "$days hari lalu"
        days < 30 -> "${days / 7} minggu lalu"
        days < 365 -> "${days / 30} bulan lalu"
        else -> "${days / 365} tahun lalu"
    }
}