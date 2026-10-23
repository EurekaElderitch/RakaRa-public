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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import dev.rakara.app.R
import dev.rakara.app.RakaraApp
import dev.rakara.app.vm.HomeViewModel
import dev.rakara.source.SManga

@Composable
fun HomeScreen(nav: NavController) {
    val app = RakaraApp.from(LocalContext.current)
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory { initializer { HomeViewModel(app.engineRepo, app.readMarks) } }
    )
    val s by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.mascot_splash),
                        contentDescription = "Profile",
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Selamat Datang,", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Pembaca RakaRa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = {  }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            }
        },
        bottomBar = { BottomNav(nav, current = "home") }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { FilterChip("Semua", isSelected = true) {} }
                    item { FilterChip("Manhwa", isSelected = false) {} }
                    item { FilterChip("Manga", isSelected = false) {} }
                    item { FilterChip("Manhua", isSelected = false) {} }
                }
            }

            
            if (s.top10.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(20.dp))
                    HeroBanner(manga = s.top10.first()) {
                        nav.navigate("manga/eng01/${encode(it.url)}/${encode(it.title)}")
                    }
                }
            }

            
            if (s.continueReading.isNotEmpty()) {
                item {
                    SectionTitle("Lanjut Baca")
                }
                items(s.continueReading.take(2)) { mark ->
                    ContinueReadingCard(mark) {
                        nav.navigate("reader/${mark.sourceId}/${encode(mark.mangaUrl)}/${encode(mark.chapterUrl)}")
                    }
                }
            }

            
            if (s.top10.isNotEmpty()) {
                item {
                    SectionTitle("Trending Minggu Ini")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(s.top10) { m ->
                            TrendingCard(m) {
                                nav.navigate("manga/eng01/${encode(m.url)}/${encode(m.title)}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Lihat Semua", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun HeroBanner(manga: SManga, onClick: (SManga) -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick(manga) }
    ) {
        SubcomposeAsyncImage(
            model = manga.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { ThumbnailPlaceholder(Modifier.fillMaxSize()) }
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                    startY = 200f
                )
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "Pilihan Editor",
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.White, style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                manga.title, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TrendingCard(manga: SManga, onClick: (SManga) -> Unit) {
    Column(Modifier.width(130.dp).clickable { onClick(manga) }) {
        SubcomposeAsyncImage(
            model = manga.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.height(180.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)),
            loading = { ThumbnailPlaceholder(Modifier.fillMaxSize()) }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            manga.title, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        if (!manga.genre.isNullOrEmpty()) {
            Text(
                manga.genre.firstOrNull() ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}




@Composable
fun BottomNav(nav: NavController, current: String) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NavigationBarItem(
            selected = current == "home",
            onClick = { if (current != "home") nav.navigate("home") { popUpTo("home") { inclusive = true } } },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Beranda") },
            label = { Text("Beranda") },
            colors = colors,
        )
        NavigationBarItem(
            selected = current == "library",
            onClick = { if (current != "library") nav.navigate("library") { popUpTo("home") } },
            icon = { Icon(Icons.Filled.MenuBook, contentDescription = "Pustaka") },
            label = { Text("Pustaka") },
            colors = colors,
        )
        NavigationBarItem(
            selected = current == "settings",
            onClick = { if (current != "settings") nav.navigate("settings") { popUpTo("home") } },
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Setelan") },
            label = { Text("Setelan") },
            colors = colors,
        )
    }
}