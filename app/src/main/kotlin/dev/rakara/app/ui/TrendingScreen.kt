package dev.rakara.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import dev.rakara.app.RakaraApp
import dev.rakara.app.vm.TrendingViewModel
import dev.rakara.source.SManga

private val GoldColor = Color(0xFFFFD54F)
private val SilverColor = Color(0xFFCFD8DC)
private val BronzeColor = Color(0xFFD4A373)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingScreen(nav: NavController) {
    val app = RakaraApp.from(LocalContext.current)
    val vm: TrendingViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TrendingViewModel(app.engineRepo) }
        }
    )
    val s = vm.state

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Whatshot, null, tint = Color(0xFFFF6B35))
                        Spacer(Modifier.width(8.dp))
                        Text("Trending", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.load() }) {
                        Icon(Icons.Default.Refresh, "Muat ulang")
                    }
                }
            )
        }
    ) { pad ->
        when {
            s.loading -> TrendingSkeleton(Modifier.padding(pad))
            s.error != null && s.items.isEmpty() ->
                TrendingError(s.error!!, onRetry = { vm.load() })
            s.items.isEmpty() -> TrendingEmpty("Belum ada data trending.")
            else -> {
                val top = s.items.take(3)
                val rest = s.items.drop(3)

                LazyColumn(
                    modifier = Modifier.padding(pad).fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    
                    item {
                        HeroCard(top[0]) {
                            nav.navigate("manga/eng01/${encode(top[0].url)}")
                        }
                    }

                    
                    if (top.size > 1) {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                PodiumCard(
                                    manga = top[1],
                                    rank = 2,
                                    modifier = Modifier.weight(1f),
                                    onClick = { nav.navigate("manga/eng01/${encode(top[1].url)}") }
                                )
                                if (top.size > 2) {
                                    PodiumCard(
                                        manga = top[2],
                                        rank = 3,
                                        modifier = Modifier.weight(1f),
                                        onClick = { nav.navigate("manga/eng01/${encode(top[2].url)}") }
                                    )
                                }
                            }
                        }
                    }

                    
                    itemsIndexed(rest) { index, m ->
                        RankedRow(
                            manga = m,
                            rank = index + 4,
                            onClick = { nav.navigate("manga/eng01/${encode(m.url)}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(manga: SManga, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        SubcomposeAsyncImage(
            model = manga.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )
        
        Surface(
            shape = RoundedCornerShape(50),
            color = GoldColor,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "TOP 1",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }
        
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            manga.genre.firstOrNull()?.takeIf { it.isNotBlank() }?.let { type ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.22f)
                ) {
                    Text(
                        type,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(
                manga.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PodiumCard(
    manga: SManga,
    rank: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val medalColor = when (rank) {
        2 -> SilverColor
        3 -> BronzeColor
        else -> MaterialTheme.colorScheme.primary
    }
    Column(modifier.clickable { onClick() }) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            SubcomposeAsyncImage(
                model = manga.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                        )
                    )
            )
            
            Surface(
                shape = CircleShape,
                color = medalColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        rank.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            }
            
            manga.genre.firstOrNull()?.takeIf { it.isNotBlank() }?.let { type ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                ) {
                    Text(
                        type,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            manga.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RankedRow(manga: SManga, rank: Int, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            
            Text(
                rank.toString().padStart(2, '0'),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(8.dp))
            SubcomposeAsyncImage(
                model = manga.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    manga.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                manga.genre.firstOrNull()?.takeIf { it.isNotBlank() }?.let { type ->
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            type,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        ShimmerBlock(Modifier.fillMaxWidth().height(260.dp), 20.dp)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBlock(Modifier.weight(1f).aspectRatio(0.72f), 16.dp)
            ShimmerBlock(Modifier.weight(1f).aspectRatio(0.72f), 16.dp)
        }
        Spacer(Modifier.height(16.dp))
        repeat(4) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                ShimmerBlock(Modifier.size(60.dp), 10.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    ShimmerBlock(Modifier.fillMaxWidth(0.7f).height(16.dp), 4.dp)
                    Spacer(Modifier.height(8.dp))
                    ShimmerBlock(Modifier.fillMaxWidth(0.4f).height(12.dp), 4.dp)
                }
            }
        }
    }
}

@Composable
private fun TrendingError(msg: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CloudOff, null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text("Gagal memuat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Coba lagi")
            }
        }
    }
}

@Composable
private fun TrendingEmpty(msg: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.MenuBook, null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                msg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ShimmerBlock(modifier: Modifier, cornerRadius: Dp = 8.dp) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
    )
}