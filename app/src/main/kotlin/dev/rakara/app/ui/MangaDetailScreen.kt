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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import dev.rakara.source.SManga

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailScreen(nav: NavController, sourceId: String, mangaUrl: String) {
    val app = RakaraApp.from(LocalContext.current)
    val vm: MangaDetailViewModel = viewModel(
        key = "manga-$sourceId-$mangaUrl",
        factory = viewModelFactory {
            initializer { MangaDetailViewModel(app.engineRepo, app.library, sourceId, mangaUrl) }
        }
    )
    val s by vm.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Manga Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Bagikan Link") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = { showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Tandai Selesai") },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                onClick = { showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Buka di Browser") },
                                leadingIcon = { Icon(Icons.Default.OpenInBrowser, contentDescription = null) },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            
            if (s.manga != null && s.chapters.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = {}, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmark")
                        }
                        Spacer(Modifier.width(16.dp))
                        Button(
                            onClick = { nav.navigate("reader/$sourceId/${encode(s.manga!!.url)}/${encode(s.chapters.first().url)}") },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Read Chapter ${s.chapters.firstOrNull()?.chapterNumber?.toInt() ?: "1"}", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(16.dp))
                        IconButton(onClick = {}, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))) {
                            Icon(Icons.Default.Download, contentDescription = "Download")
                        }
                    }
                }
            }
        }
    ) { pad ->
        when {
            s.loading -> LoadingBox()
            s.error != null -> ErrorBox(s.error) { vm.load() }
            s.manga == null -> EmptyBox("Data tidak ditemukan.")
            else -> {
                val m = s.manga!!
                LazyColumn(
                    modifier = Modifier.padding(pad).fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    
                    item {
                        Box(Modifier.fillMaxWidth().height(320.dp)) {
                            SubcomposeAsyncImage(
                                model = m.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().blur(30.dp)
                            )
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background))))
                            
                            Row(
                                Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp).padding(bottom = 16.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                SubcomposeAsyncImage(
                                    model = m.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.width(120.dp).height(170.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp)) {
                                        Text("LATEST UPDATE", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text("Chapter ${s.chapters.firstOrNull()?.chapterNumber?.toInt() ?: "1"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(m.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Text(m.genre?.joinToString(" • ") ?: "Komik", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(
                                onClick = vm::toggleLibrary,
                                modifier = Modifier.size(56.dp).background(if (s.inLibrary) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                            ) {
                                Icon(
                                    if (s.inLibrary) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (s.inLibrary) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatBox(icon = Icons.Default.Star, value = "4.8", label = "Rating", modifier = Modifier.weight(1f))
                            StatBox(icon = Icons.Default.MenuBook, value = "${s.chapters.size}", label = "Chapter", modifier = Modifier.weight(1f))
                            StatBox(icon = Icons.Default.Language, value = "ID", label = "Bahasa", modifier = Modifier.weight(1f))
                        }
                    }

                    
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TabButton("Information", selectedTab == 0) { selectedTab = 0 }
                            TabButton("Chapters (${s.chapters.size})", selectedTab == 1) { selectedTab = 1 }
                        }
                    }

                    
                    if (selectedTab == 0) {
                        
                        item {
                            Column(Modifier.padding(20.dp)) {
                                Text("Sinopsis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                var expanded by remember { mutableStateOf(false) }
                                Text(
                                    m.description ?: "Belum ada sinopsis.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (expanded) "Sembunyikan" else "Baca selengkapnya",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(top = 4.dp).clickable { expanded = !expanded }
                                )

                                Spacer(Modifier.height(24.dp))
                                Text("Author", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)) {
                                        Text(m.author?.take(1) ?: "?", modifier = Modifier.align(Alignment.Center), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(m.author ?: "Unknown", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(Modifier.height(24.dp))
                                Text("Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    m.genre?.take(3)?.forEach { g ->
                                        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                                            Text("#$g", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp) 
                                    .padding(horizontal = 20.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    items(s.chapters, key = { it.url }) { c ->
                                        Row(
                                            Modifier.fillMaxWidth()
                                                .clickable { nav.navigate("reader/$sourceId/${encode(m.url)}/${encode(c.url)}") }
                                                .padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(c.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                            }
                                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp)
        )
    }
}

@Composable
private fun StatBox(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.rakaraExtra.gold, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}