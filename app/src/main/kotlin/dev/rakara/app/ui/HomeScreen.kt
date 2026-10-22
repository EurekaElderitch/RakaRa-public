package dev.rakara.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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
import dev.rakara.app.RakaraApp
import dev.rakara.app.data.ReadMark
import dev.rakara.app.vm.HomeViewModel
import dev.rakara.source.SManga

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController) {
    val app = RakaraApp.from(LocalContext.current)
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HomeViewModel(app.engineRepo, app.readMarks) }
        }
    )
    val s by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "RakaRa",
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 6.sp,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = { BottomNav(nav, current = "home") },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        when {
            s.loading -> Box(Modifier.padding(pad).fillMaxSize()) {
                LoadingState("Menata rak")
            }
            s.continueReading.isEmpty() && s.top10.isEmpty() && s.updates.isEmpty() -> Box(
                Modifier.padding(pad).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.error ?: "Belum ada konten")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { vm.load() }) { Text("Coba lagi") }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(pad).fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    if (s.continueReading.isNotEmpty()) {
                        item {
                            SectionHeader("LANJUT BACA")
                            ContinueReadingRow(
                                items = s.continueReading,
                                onClick = { mark ->
                                    nav.navigate(
                                        "reader/${mark.sourceId}/${encode(mark.mangaUrl)}/${encode(mark.chapterUrl)}"
                                    )
                                },
                            )
                        }
                    }

                    if (s.top10.isNotEmpty()) {
                        item {
                            SectionHeader("TOP 10")
                            Top10Row(
                                items = s.top10,
                                onClick = { m ->
                                    nav.navigate("manga/eng01/${encode(m.url)}/${encode(m.title)}")
                                },
                            )
                        }
                    }

                    if (s.updates.isNotEmpty()) {
                        item {
                            SectionHeader("UPDATE KOMIK")
                        }
                        items(s.updates) { m ->
                            UpdateRow(
                                manga = m,
                                onClick = {
                                    nav.navigate("manga/eng01/${encode(m.url)}/${encode(m.title)}")
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Row(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.tertiary),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ContinueReadingRow(
    items: List<ReadMark>,
    onClick: (ReadMark) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { "${it.sourceId}|${it.mangaUrl}|${it.chapterUrl}" }) { mark ->
            Column(
                modifier = Modifier
                    .width(130.dp)
                    .clickable { onClick(mark) },
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    SubcomposeAsyncImage(
                        model = mark.mangaThumbnail,
                        contentDescription = mark.mangaTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = { ThumbnailPlaceholder(Modifier.fillMaxSize()) },
                        error = { ThumbnailPlaceholder(Modifier.fillMaxSize()) },
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.tertiary)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    ) {
                        Text(
                            mark.chapterName.ifBlank { "Lanjut" },
                            color = MaterialTheme.colorScheme.onTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    mark.mangaTitle.ifBlank { "Manga" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Top10Row(
    items: List<SManga>,
    onClick: (SManga) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(items.size) { idx ->
            val m = items[idx]
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .clickable { onClick(m) },
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    SubcomposeAsyncImage(
                        model = m.thumbnailUrl,
                        contentDescription = m.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = { ThumbnailPlaceholder(Modifier.fillMaxSize()) },
                        error = { ThumbnailPlaceholder(Modifier.fillMaxSize()) },
                    )
                    
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f),
                                    )
                                )
                            )
                    )
                    Text(
                        text = (idx + 1).toString().padStart(2, '0'),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 36.sp,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.rakaraExtra.gold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    m.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun UpdateRow(
    manga: SManga,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(60.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            SubcomposeAsyncImage(
                model = manga.thumbnailUrl,
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { ThumbnailPlaceholder(Modifier.fillMaxSize()) },
                error = { ThumbnailPlaceholder(Modifier.fillMaxSize()) },
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                manga.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!manga.genre.isNullOrEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    manga.genre.joinToString(" • "), 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 94.dp, end = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp,
    )
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
            onClick = {
                if (current != "home") nav.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Beranda") },
            label = { Text("Beranda") },
            colors = colors,
        )
        NavigationBarItem(
            selected = current == "library",
            onClick = {
                if (current != "library") nav.navigate("library") { popUpTo("home") }
            },
            icon = { Icon(Icons.Filled.MenuBook, contentDescription = "Pustaka") },
            label = { Text("Pustaka") },
            colors = colors,
        )
        NavigationBarItem(
            selected = current == "settings",
            onClick = {
                if (current != "settings") nav.navigate("settings") { popUpTo("home") }
            },
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Setelan") },
            label = { Text("Setelan") },
            colors = colors,
        )
    }
}