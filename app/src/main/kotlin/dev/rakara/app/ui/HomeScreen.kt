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
import dev.rakara.app.data.ReadMark
import dev.rakara.app.vm.HomeViewModel
import dev.rakara.source.SManga

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController) {
    val app = RakaraApp.from(LocalContext.current)
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory { initializer { HomeViewModel(app.engineRepo, app.readMarks) } }
    )
    val s by vm.state.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }

    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }, containerColor = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(24.dp)) {
                Text("Tipe Bacaan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { showFilterSheet = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.MenuBook, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Comic")
                    }
                    OutlinedButton(onClick = {}, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Book, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Novel")
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("Fitur Novel akan hadir segera!", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(32.dp))
            }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.mascot_splash),
                        contentDescription = "Logo",
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("RakaRa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { showFilterSheet = true }) {
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
                            onValueChange = { vm.performSearch(it) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            decorationBox = { inner ->
                                if (s.searchQuery.isEmpty()) Text("Cari manga di Engine 1...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                inner()
                            }
                        )
                        if (s.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { vm.performSearch("") }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            
            if (s.searchQuery.isNotEmpty()) {
                if (s.isSearching) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (s.searchResults.isEmpty()) {
                    item {
                        EmptyBox("Tidak ada komik yang cocok dengan \"${s.searchQuery}\"")
                    }
                } else {
                    items(s.searchResults) { m ->
                        SearchResultCard(m) { nav.navigate("manga/eng01/${encode(m.url)}/${encode(m.title)}") }
                    }
                }
                return@LazyColumn 
            }

            
            
            

            
            if (s.continueReading.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            s.continueReading.take(2).forEach { mark ->
                                HistoryCard(mark) {
                                    nav.navigate("reader/${mark.sourceId}/${encode(mark.mangaUrl)}/${encode(mark.chapterUrl)}")
                                }
                                if (mark != s.continueReading.take(2).last()) Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            
            if (s.top10.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Text("Trending", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 16.dp)
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(s.top10.take(5)) { m ->
                                TrendingCard(m) { nav.navigate("manga/eng01/${encode(m.url)}/${encode(m.title)}") }
                            }
                        }
                    }
                }
            }

            
            if (s.updates.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Text("Latest Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 16.dp)
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(s.updates.take(5)) { m ->
                                TrendingCard(m) { nav.navigate("manga/eng01/${encode(m.url)}/${encode(m.title)}") }
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun HistoryCard(mark: ReadMark, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubcomposeAsyncImage(
            model = mark.mangaThumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(60.dp).height(80.dp).clip(RoundedCornerShape(8.dp)),
            loading = { ThumbnailPlaceholder(Modifier.fillMaxSize()) }
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(mark.mangaTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("Chapter ${mark.chapterName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun TrendingCard(manga: SManga, onClick: (SManga) -> Unit) {
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
private fun SearchResultCard(manga: SManga, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 8.dp),
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
            Text(manga.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(manga.genre?.joinToString(" • ") ?: "Komik", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
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