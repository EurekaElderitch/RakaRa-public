package dev.rakara.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import dev.rakara.app.RakaraApp
import dev.rakara.app.data.ReadMark
import dev.rakara.app.data.UserPreferencesRepository
import dev.rakara.app.vm.HomeViewModel
import dev.rakara.source.SManga
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController, prefsRepo: UserPreferencesRepository) {
    val app = RakaraApp.from(LocalContext.current)
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory { initializer { HomeViewModel(app.engineRepo, app.readMarks) } }
    )
    val s by vm.state.collectAsStateWithLifecycle()

    val profileName by prefsRepo.profileName.collectAsState(initial = "User Raka")
    val profilePhotoUri by prefsRepo.profilePhotoUri.collectAsState(initial = null)
    val isDarkMode by prefsRepo.isDarkMode.collectAsState(initial = false)

    var showMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { BottomNav(nav, current = "home") }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("RAKARA", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.navigate("notification") }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notif", tint = MaterialTheme.colorScheme.primary)
                    }
                    Box {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { showMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profilePhotoUri != null) {
                                AsyncImage(model = profilePhotoUri, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Text(profileName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Komik") },
                                leadingIcon = { Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = { showMenu = false }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Novel")
                                        Spacer(Modifier.width(8.dp))
                                        Surface(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                            Text("SOON", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Book, null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showMenu = false
                                    scope.launch { snackbarHostState.showSnackbar("Fitur Novel segera hadir!") }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Profil") },
                                leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showMenu = false
                                    nav.navigate("profile") { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true }
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (isDarkMode) "Mode Terang" else "Mode Gelap") },
                                leadingIcon = { Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    scope.launch { prefsRepo.setDarkMode(!isDarkMode) }
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Pengaturan") },
                                leadingIcon = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = { showMenu = false; nav.navigate("settings") }
                            )
                        }
                    }
                }
            }

            if (s.loading) {
                HomeSkeleton(Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable { nav.navigate("search") }
                        ) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(12.dp))
                                Text("Cari judul, genre, author...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    
                    if (s.continueReading.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(24.dp))
                            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Continue Reading", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "See all", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { 
                                        nav.navigate("library") { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true }
                                    }
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(s.continueReading.take(5)) { mark ->
                                    ContinueReadingCard(mark) {
                                        nav.navigate("reader/${mark.sourceId}/${encode(mark.mangaUrl)}/${encode(mark.chapterUrl)}")
                                    }
                                }
                            }
                        }
                    }

                    
                    if (s.top10.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(24.dp))
                            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "Top Trending", 
                                    style = MaterialTheme.typography.titleMedium, 
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { nav.navigate("trending") } 
                                )
                                
                            }
                            Spacer(Modifier.height(12.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(s.top10.take(5)) { m -> 
                                    TrendingCard(m) { nav.navigate("manga/eng01/${encode(m.url)}") }
                                }
                            }
                        }
                    }

                    
                    if (s.updates.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(24.dp))
                            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Latest Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "See all", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { nav.navigate("updates") }
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    s.updates.take(5).forEach { m -> 
                                        Row(
                                            Modifier.fillMaxWidth().clickable { nav.navigate("manga/eng01/${encode(m.url)}") }.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            SubcomposeAsyncImage(
                                                model = m.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(m.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("Chapter terbaru", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outlineVariant)
                                        }
                                        if (m != s.updates.take(5).last()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
private fun HomeSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        SkeletonBox(Modifier.fillMaxWidth().height(48.dp), 12.dp)
        Spacer(Modifier.height(24.dp))
        SkeletonBox(Modifier.width(160.dp).height(20.dp), 4.dp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBox(Modifier.width(176.dp).height(180.dp), 12.dp)
            SkeletonBox(Modifier.width(176.dp).height(180.dp), 12.dp)
        }
        Spacer(Modifier.height(24.dp))
        SkeletonBox(Modifier.width(120.dp).height(20.dp), 4.dp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBox(Modifier.width(112.dp).height(128.dp), 12.dp)
            SkeletonBox(Modifier.width(112.dp).height(128.dp), 12.dp)
            SkeletonBox(Modifier.width(112.dp).height(128.dp), 12.dp)
        }
    }
}

@Composable
private fun SkeletonBox(modifier: Modifier, cornerRadius: Dp = 8.dp) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.2f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "alpha"
    )
    Box(modifier.clip(RoundedCornerShape(cornerRadius)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)))
}

@Composable
private fun ContinueReadingCard(mark: ReadMark, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.width(176.dp).clickable { onClick() }
    ) {
        Column {
            Box(Modifier.height(100.dp).fillMaxWidth().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) {
                SubcomposeAsyncImage(model = mark.mangaThumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Column(Modifier.padding(12.dp)) {
                Text(mark.mangaTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(mark.chapterName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { 0.65f }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun TrendingCard(manga: SManga, onClick: () -> Unit) {
    Column(Modifier.width(112.dp).clickable { onClick() }) {
        Box(Modifier.height(128.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            SubcomposeAsyncImage(model = manga.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(8.dp))
        Text(manga.title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}