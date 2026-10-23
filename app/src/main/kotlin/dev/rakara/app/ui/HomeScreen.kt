package dev.rakara.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField 
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import dev.rakara.app.R
import dev.rakara.app.RakaraApp
import dev.rakara.app.data.ReadMark
import dev.rakara.app.vm.HomeViewModel
import dev.rakara.source.SManga

@Composable
fun HomeScreen(nav: NavController) {
    val app = RakaraApp.from(LocalContext.current)
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory { initializer { HomeViewModel(app.engineRepo, app.readMarks) } }
    )
    val s by vm.state.collectAsStateWithLifecycle()
    var showProfileSheet by remember { mutableStateOf(false) }

    if (showProfileSheet) {
        ProfileBottomSheet(onDismiss = { showProfileSheet = false })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { BottomNav(nav, current = "home") }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showProfileSheet = true }) {
                        Image(
                            painter = painterResource(id = R.drawable.mascot_splash),
                            contentDescription = "Profile",
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE6E2F2)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Selamat Datang,", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Pembaca RakaRa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = { showProfileSheet = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = s.searchQuery,
                                onValueChange = { vm.updateSearch(it) },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                decorationBox = { innerTextField ->
                                    if (s.searchQuery.isEmpty()) {
                                        Text("Cari judul komik...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    innerTextField()
                                }
                            )
                            if (s.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { vm.updateSearch("") }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            
            item {
                Spacer(Modifier.height(16.dp))
                val filters = listOf("Semua", "Manhwa", "Manhua", "Manga")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters) { filter ->
                        FilterChip(
                            text = filter,
                            isSelected = s.selectedFilter == filter,
                            onClick = { vm.updateFilter(filter) }
                        )
                    }
                }
            }

            
            if (s.continueReading.isNotEmpty() && s.searchQuery.isEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    val mark = s.continueReading.first()
                    ContinueReadingBanner(mark) {
                        nav.navigate("reader/${mark.sourceId}/${encode(mark.mangaUrl)}/${encode(mark.chapterUrl)}")
                    }
                }
            }

            
            if (s.top10.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    SectionTitle("Trending Comic")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(s.top10) { m ->
                            TrendingComicCard(m) { nav.navigate("manga/eng01/${encode(m.url)}/${encode(m.title)}") }
                        }
                    }
                }
            }

            
            if (s.updates.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    SectionTitle("Latest Updates", showRefresh = true)
                }
                items(s.updates.take(5)) { m ->
                    LatestUpdateCard(m) { nav.navigate("manga/eng01/${encode(m.url)}/${encode(m.title)}") }
                }
            }
        }
    }
}

@Composable
fun ProfileBottomSheet(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profil & Pengaturan") },
        text = {
            Column {
                Text("Fitur yang direncanakan:")
                Text("• Statistik Baca (Total Chapter)")
                Text("• Sinkronisasi Bookmark (Backup)")
                Text("• Mode Gelap / Terang")
                Text("• Tentang Aplikasi (HynchLabs)")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
private fun SectionTitle(title: String, showRefresh: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (showRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        } else {
            Text("Lihat Semua", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ContinueReadingBanner(mark: ReadMark, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                )
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubcomposeAsyncImage(
                model = mark.mangaThumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(80.dp).height(110.dp).clip(RoundedCornerShape(12.dp)),
                loading = { ThumbnailPlaceholder(Modifier.fillMaxSize()) }
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("LANJUT BACA", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text(mark.mangaTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text("Chapter ${mark.chapterName}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { 0.78f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(4.dp))
                Text("78% Selesai", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            }
            Spacer(Modifier.width(8.dp))
            Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Lanjut", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
private fun TrendingComicCard(manga: SManga, onClick: (SManga) -> Unit) {
    Box(
        Modifier.width(150.dp).height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick(manga) }
    ) {
        SubcomposeAsyncImage(
            model = manga.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { ThumbnailPlaceholder(Modifier.fillMaxSize()) }
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 300f)))
        
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.rakaraExtra.gold, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("4.8", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }

        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp)) {
                Text("MANHWA", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(manga.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LatestUpdateCard(manga: SManga, onClick: (SManga) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick(manga) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubcomposeAsyncImage(
            model = manga.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(60.dp).height(80.dp).clip(RoundedCornerShape(8.dp)),
            loading = { ThumbnailPlaceholder(Modifier.fillMaxSize()) }
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("MANGA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text("Baru saja", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(manga.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("Chapter terbaru", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.MenuBook, contentDescription = "Baca", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    }
}

@Composable
fun BottomNav(nav: NavController, current: String) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavIcon(Icons.Default.Home, "Home", current == "home") { nav.navigate("home") { popUpTo("home") { inclusive = true } } }
                NavIcon(Icons.Default.MenuBook, "Pustaka", current == "library") { nav.navigate("library") { popUpTo("home") } }
                NavIcon(Icons.Default.Update, "Update", false) {  }
                NavIcon(Icons.Default.Download, "Download", false) {  }
                NavIcon(Icons.Default.MoreHoriz, "Lainnya", current == "settings") { nav.navigate("settings") { popUpTo("home") } }
            }
        }
    }
}

@Composable
private fun NavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(8.dp)) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 9.sp)
    }
}