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
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import dev.rakara.app.update.ApkUpdater
import dev.rakara.app.vm.HomeViewModel
import dev.rakara.source.SManga
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController, prefsRepo: UserPreferencesRepository) {
    val app = RakaraApp.from(LocalContext.current)
    val context = LocalContext.current
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory { initializer { HomeViewModel(app.engineRepo, app.readMarks) } }
    )
    val s by vm.state.collectAsStateWithLifecycle()

    val profileName by prefsRepo.profileName.collectAsState(initial = "User Raka")
    val profilePhotoUri by prefsRepo.profilePhotoUri.collectAsState(initial = null)
    val isDarkMode by prefsRepo.isDarkMode.collectAsState(initial = false)

    val notifSubs by app.notifications.state.collectAsStateWithLifecycle()
    val unreadNotifs = notifSubs.sumOf { it.unreadCount }

    var showMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var hasUpdate by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        val info = ApkUpdater(context).checkForUpdate()
        hasUpdate = info != null
    }

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
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notif",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            if (unreadNotifs > 0) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 2.dp, y = (-2).dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE74C3C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (unreadNotifs > 9) "9+" else unreadNotifs.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
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

                        if (hasUpdate == true) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE74C3C))
                                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                            )
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
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            Text(
                                "Selamat datang kembali, $profileName 👋",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Lanjutkan membaca dari mana kamu berhenti",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    if (s.continueReading.isNotEmpty()) {
                        item {
                            HeroContinueCard(
                                mark = s.continueReading.first(),
                                onContinue = { mark ->
                                    nav.navigate("reader/${mark.sourceId}/${encode(mark.mangaUrl)}/${encode(mark.chapterUrl)}")
                                },
                                onSeeAll = {
                                    nav.navigate("library-history") {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    } else {
                        item {
                            EmptyHeroCard(
                                onExplore = {
                                    nav.navigate("explore") {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }

                    
                    if (s.top10.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(24.dp))
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Top Trending",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "See all",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
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
private fun HeroContinueCard(
    mark: ReadMark,
    onContinue: (ReadMark) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onContinue(mark) }
        ) {
            SubcomposeAsyncImage(
                model = mark.mangaThumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            ) {
                Text(
                    mark.chapterName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(
                    mark.mangaTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatTimeAgo(mark.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onContinue(mark) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Lanjutkan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                "Lihat Semua Riwayat →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSeeAll() }
            )
        }
    }
}

@Composable
private fun EmptyHeroCard(onExplore: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        Column(
            Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.MenuBook,
                null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Belum ada riwayat baca",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Yuk mulai baca komik dari Top Trending di bawah 👇",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun HomeSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        SkeletonBox(Modifier.width(220.dp).height(24.dp), 4.dp)
        Spacer(Modifier.height(8.dp))
        SkeletonBox(Modifier.width(280.dp).height(14.dp), 4.dp)
        Spacer(Modifier.height(16.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(220.dp), 20.dp)
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
private fun TrendingCard(manga: SManga, onClick: () -> Unit) {
    Column(Modifier.width(112.dp).clickable { onClick() }) {
        Box(Modifier.height(128.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            SubcomposeAsyncImage(model = manga.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(8.dp))
        Text(manga.title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatTimeAgo(ts: Long): String {
    if (ts <= 0L) return ""
    val diff = System.currentTimeMillis() - ts
    if (diff < 0) return "Baru saja"
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)
    return when {
        minutes < 1 -> "Baru saja"
        minutes < 60 -> "$minutes menit lalu"
        hours < 24 -> "$hours jam lalu"
        days < 7 -> "$days hari lalu"
        days < 30 -> "${days / 7} minggu lalu"
        days < 365 -> "${days / 30} bulan lalu"
        else -> "${days / 365} tahun lalu"
    }
}