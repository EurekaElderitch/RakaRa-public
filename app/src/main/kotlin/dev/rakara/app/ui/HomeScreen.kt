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
                        contentDescription = "Logo",
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("RakaRa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = {  }) {
                    Icon(Icons.Default.Tune, contentDescription = "Filter", tint = MaterialTheme.colorScheme.onBackground)
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
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        BasicTextField(
                            value = s.searchQuery,
                            onValueChange = { vm.updateSearch(it) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            decorationBox = { inner ->
                                if (s.searchQuery.isEmpty()) Text("Cari manga, author, atau genre...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                inner()
                            }
                        )
                    }
                }
            }

            
            item {
                Spacer(Modifier.height(8.dp))
                val filters = listOf("Semua", "Manhwa", "Manhua", "Manga")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters) { filter ->
                        FilterChip(text = filter, isSelected = s.selectedFilter == filter, onClick = { vm.updateFilter(filter) })
                    }
                }
            }

            
            if (s.continueReading.isNotEmpty() && s.searchQuery.isEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Text("Pick up where you left off", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(12.dp))
                    val mark = s.continueReading.first()
                    ContinueReadingCardSumi(mark) {
                        nav.navigate("reader/${mark.sourceId}/${encode(mark.mangaUrl)}/${encode(mark.chapterUrl)}")
                    }
                }
            }

            
            if (s.updates.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fresh from the pages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Lihat semua", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(s.updates.take(5)) { m ->
                            TrendingCardSumi(m) { nav.navigate("manga/eng01/${encode(m.url)}/${encode(m.title)}") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueReadingCardSumi(mark: ReadMark, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary, 
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable { onClick() }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SubcomposeAsyncImage(
                model = mark.mangaThumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(80.dp).height(110.dp).clip(RoundedCornerShape(8.dp)),
                loading = { ThumbnailPlaceholder(Modifier.fillMaxSize()) }
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(mark.mangaTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text("Ch. ${mark.chapterName} • Hal 8 dari 24", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Continue reading", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TrendingCardSumi(manga: SManga, onClick: (SManga) -> Unit) {
    Column(Modifier.width(120.dp).clickable { onClick(manga) }) {
        SubcomposeAsyncImage(
            model = manga.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.height(170.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            loading = { ThumbnailPlaceholder(Modifier.fillMaxSize()) }
        )
        Spacer(Modifier.height(8.dp))
        Text(manga.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text("Chapter terbaru", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BottomNav(nav: NavController, current: String) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
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
            icon = { Icon(Icons.Filled.Explore, contentDescription = "Discover") },
            label = { Text("Discover") },
            colors = colors,
        )
        NavigationBarItem(
            selected = current == "library",
            onClick = { if (current != "library") nav.navigate("library") { popUpTo("home") } },
            icon = { Icon(Icons.Filled.Bookmark, contentDescription = "Bookmarks") },
            label = { Text("Bookmarks") },
            colors = colors,
        )
        NavigationBarItem(
            selected = current == "history",
            onClick = { if (current != "history") nav.navigate("history") { popUpTo("home") } },
            icon = { Icon(Icons.Filled.History, contentDescription = "History") },
            label = { Text("History") },
            colors = colors,
        )
    }
}