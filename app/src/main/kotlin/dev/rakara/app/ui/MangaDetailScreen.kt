package dev.rakara.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import dev.rakara.app.RakaraApp
import dev.rakara.app.vm.MangaDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailScreen(nav: NavController, sourceId: String, mangaUrl: String) {
    val app = RakaraApp.from(LocalContext.current)
    val vm: MangaDetailViewModel = viewModel(
        key = "manga-$sourceId-$mangaUrl",
        factory = viewModelFactory { initializer { MangaDetailViewModel(app.engineRepo, app.library, sourceId, mangaUrl) } }
    )
    val s by vm.state.collectAsStateWithLifecycle()
    
    var expandedSynopsis by remember { mutableStateOf(false) }
    var showAllChapters by remember { mutableStateOf(false) }

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
                    IconButton(onClick = {  }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notif")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (s.manga != null && s.chapters.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Box(Modifier.padding(16.dp).fillMaxWidth()) {
                        Button(
                            onClick = { nav.navigate("reader/$sourceId/${encode(s.manga!!.url)}/${encode(s.chapters.first().url)}") },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Lanjut Baca Chapter ${s.chapters.first().chapterNumber.toInt()}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { pad ->
        if (s.manga != null) {
            val m = s.manga!!
            LazyColumn(modifier = Modifier.padding(pad).fillMaxSize()) {
                
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
                            Text(m.author ?: "Unknown", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("4.8", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                    Text("Ongoing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }

                
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { nav.navigate("reader/$sourceId/${encode(m.url)}/${encode(s.chapters.firstOrNull()?.url ?: "")}") },
                            modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.MenuBook, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Mulai Baca")
                        }
                        OutlinedButton(
                            onClick = vm::toggleLibrary,
                            modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(if (s.inLibrary) Icons.Default.Check else Icons.Default.Add, null, tint = if (s.inLibrary) Color.Green else MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                
                item {
                    Column(Modifier.padding(20.dp)) {
                        Text("Synopsis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            m.description ?: "Belum ada sinopsis.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (expandedSynopsis) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (expandedSynopsis) "Sembunyikan" else "Read More",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp).clickable { expandedSynopsis = !expandedSynopsis }
                        )
                    }
                }

                
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Chapters (${s.chapters.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Terbaru", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                
                item {
                    val displayChapters = if (showAllChapters) s.chapters else s.chapters.take(5)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    ) {
                        Column {
                            displayChapters.forEach { c ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { nav.navigate("reader/$sourceId/${encode(m.url)}/${encode(c.url)}") }.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(c.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("Baru saja", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
                                }
                                if (c != displayChapters.last()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                            
                            if (s.chapters.size > 5) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                TextButton(
                                    onClick = { showAllChapters = !showAllChapters },
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                ) {
                                    Text(if (showAllChapters) "Tampilkan Lebih Sedikit" else "Lihat Semua Chapter (${s.chapters.size})", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}