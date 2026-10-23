package dev.rakara.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.ui.draw.rotate
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
                    if (s.manga != null) {
                        IconButton(onClick = vm::toggleLibrary) {
                            Icon(
                                if (s.inLibrary) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Pustaka",
                                tint = if (s.inLibrary) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
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
                        Box(Modifier.fillMaxWidth().height(300.dp)) {
                            SubcomposeAsyncImage(
                                model = m.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().blur(20.dp)
                            )
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background))
                                )
                            )
                            
                            SubcomposeAsyncImage(
                                model = m.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                                    .width(140.dp).height(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }

                    
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(m.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                m.genre?.joinToString(" • ") ?: "Komik",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBox("Rating", "4.8")
                            StatBox("Chapter", "${s.chapters.size}")
                            StatBox("Status", if (m.status == SManga.Status.ONGOING) "Ongoing" else "Tamat")
                        }
                    }

                    
                    item {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            Text("Sinopsis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                m.description ?: "Belum ada sinopsis.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    
                    item {
                        Spacer(Modifier.height(24.dp))
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Chapter") })
                            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Info") })
                        }
                    }

                    
                    if (selectedTab == 0) {
                        items(s.chapters, key = { it.url }) { c ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable {
                                        nav.navigate("reader/$sourceId/${encode(m.url)}/${encode(c.url)}")
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(c.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp).rotate(180f))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    } else {
                        item {
                            Column(Modifier.padding(20.dp)) {
                                Text("Penulis: ${m.author ?: "-"}", style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.height(8.dp))
                                Text("Tipe: ${m.genre?.firstOrNull() ?: "-"}", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}