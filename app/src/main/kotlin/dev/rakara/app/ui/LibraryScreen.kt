package dev.rakara.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import dev.rakara.app.RakaraApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(nav: NavController) {
    val app = RakaraApp.from(LocalContext.current)
    val entries by app.library.state.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSortModal by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Library", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                IconButton(onClick = { showSortModal = true }) {
                    Icon(Icons.Default.Tune, contentDescription = "Sort", tint = MaterialTheme.colorScheme.primary)
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
                if (entries.isEmpty()) EmptyBox("Belum ada koleksi.")
                else LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(entries, key = { "${it.sourceId}|${it.mangaUrl}" }) { e ->
                        Column(Modifier.clickable { nav.navigate("manga/${e.sourceId}/${encode(e.mangaUrl)}") }) {
                            Box(Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                SubcomposeAsyncImage(model = e.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(e.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Lanjut Baca", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (entries.isEmpty()) item { EmptyBox("Belum ada langganan.") }
                    else items(entries) { e ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).clickable { nav.navigate("manga/${e.sourceId}/${encode(e.mangaUrl)}") }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            SubcomposeAsyncImage(model = e.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)))
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(e.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Chapter baru", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha=0.1f), shape = RoundedCornerShape(4.dp)) {
                                Text("Baru", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        
        if (showSortModal) {
            ModalBottomSheet(onDismissRequest = { showSortModal = false }) {
                Column(Modifier.padding(20.dp)) {
                    Text("Urutkan Berdasarkan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    listOf("Terakhir Dibaca", "Baru Ditambahkan", "Judul (A-Z)", "Rating Tertinggi").forEachIndexed { i, text ->
                        Row(Modifier.fillMaxWidth().clickable { showSortModal = false }.padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text, style = MaterialTheme.typography.bodyLarge)
                            RadioButton(selected = i == 0, onClick = { showSortModal = false })
                        }
                        if (i != 3) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}