package dev.rakara.app.ui

import androidx.compose.foundation.gestures.scrollBy
import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import dev.rakara.app.R
import dev.rakara.app.RakaraApp
import dev.rakara.app.vm.ReaderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    nav: NavController,
    sourceId: String,
    mangaUrl: String,
    chapterUrl: String,
) {
    val ctx = LocalContext.current
    val app = RakaraApp.from(ctx)
    val vm: ReaderViewModel = viewModel(
        key = "reader-$sourceId-$mangaUrl-$chapterUrl",
        factory = viewModelFactory {
            initializer {
                ReaderViewModel(app.engineRepo, app.readMarks, sourceId, mangaUrl, chapterUrl)
            }
        }
    )
    val s by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var chrome by remember { mutableStateOf(true) }
    var isAutoScrolling by remember { mutableStateOf(false) }
    
    val paper = Color(0xFFF4F0E6) 
    val ink = Color(0xFF403D36)

    
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        
        
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        onDispose {
            
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    
    LaunchedEffect(isAutoScrolling) {
        if (isAutoScrolling) {
            while (isAutoScrolling) {
                delay(50) 
                val currentPos = listState.firstVisibleItemScrollOffset
                listState.scrollBy(3f) 
                
                
                if (!listState.canScrollForward) {
                    isAutoScrolling = false
                }
            }
        }
    }

    
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            if (it is androidx.compose.foundation.interaction.PressInteraction) {
                isAutoScrolling = false
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { idx -> vm.onPageChanged(idx) }
    }

    val progress by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.totalItemsCount == 0) 0f
            else {
                val firstIdx = info.visibleItemsInfo.firstOrNull()?.index ?: 0
                ((firstIdx + 1).toFloat() / info.totalItemsCount).coerceIn(0f, 1f)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(paper)) {
        when {
            s.loading -> LoadingReader(s.mangaTitle, s.chapterName)
            s.error != null -> ReaderError(s.error!!) { vm.load() }
            s.urls.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada halaman.", color = ink)
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { chrome = !chrome })
                        },
                ) {
                    itemsIndexed(s.urls) { idx, url ->
                        PageImage(url = url, index = idx)
                    }
                    item {
                        EndOfChapter(
                            nextChapterUrl = s.nextChapterUrl,
                            nextChapterName = s.nextChapterName,
                            onNext = { nextUrl ->
                                if (nextUrl != null) {
                                    isAutoScrolling = false
                                    nav.navigate("reader/$sourceId/${encode(mangaUrl)}/${encode(nextUrl)}") {
                                        popUpTo("reader/$sourceId/${encode(mangaUrl)}/${encode(chapterUrl)}") { inclusive = true }
                                    }
                                }
                            },
                            onBackToDetail = {
                                nav.navigate("manga/$sourceId/${encode(mangaUrl)}/${encode(s.mangaTitle)}") {
                                    popUpTo("manga/$sourceId/${encode(mangaUrl)}/${encode(s.mangaTitle)}") { inclusive = true }
                                }
                            }
                        )
                    }
                }

                
                AnimatedVisibility(visible = chrome, modifier = Modifier.align(Alignment.TopCenter)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(paper.copy(alpha = 0.96f))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = ink)
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(
                                s.mangaTitle.ifBlank { "..." },
                                color = ink,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                s.chapterName.ifBlank { "..." },
                                color = ink.copy(alpha = 0.55f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                
                if (chrome && progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .padding(bottom = if(chrome) 64.dp else 0.dp), 
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    )
                }

                
                AnimatedVisibility(
                    visible = chrome,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(paper.copy(alpha = 0.96f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        
                        IconButton(
                            onClick = {
                                s.prevChapterUrl?.let { prev ->
                                    isAutoScrolling = false
                                    nav.navigate("reader/$sourceId/${encode(mangaUrl)}/${encode(prev)}") {
                                        popUpTo("reader/$sourceId/${encode(mangaUrl)}/${encode(chapterUrl)}") { inclusive = true }
                                    }
                                }
                            },
                            enabled = s.prevChapterUrl != null
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev", tint = if (s.prevChapterUrl != null) ink else ink.copy(alpha=0.3f))
                        }

                        
                        IconButton(onClick = {  }) {
                            Icon(Icons.Default.List, "Chapter", tint = ink)
                        }

                        
                        IconButton(onClick = {
                            nav.navigate("manga/$sourceId/${encode(mangaUrl)}/${encode(s.mangaTitle)}") {
                                popUpTo("manga/$sourceId/${encode(mangaUrl)}/${encode(s.mangaTitle)}") { inclusive = true }
                            }
                        }) {
                            Icon(Icons.Default.Home, "Detail", tint = ink)
                        }

                        
                        IconButton(onClick = { isAutoScrolling = !isAutoScrolling }) {
                            Icon(
                                if (isAutoScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                                "Auto Scroll",
                                tint = if (isAutoScrolling) MaterialTheme.colorScheme.primary else ink
                            )
                        }

                        
                        IconButton(
                            onClick = {
                                s.nextChapterUrl?.let { next ->
                                    isAutoScrolling = false
                                    nav.navigate("reader/$sourceId/${encode(mangaUrl)}/${encode(next)}") {
                                        popUpTo("reader/$sourceId/${encode(mangaUrl)}/${encode(chapterUrl)}") { inclusive = true }
                                    }
                                }
                            },
                            enabled = s.nextChapterUrl != null
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next", tint = if (s.nextChapterUrl != null) ink else ink.copy(alpha=0.3f))
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun PageImage(url: String, index: Int) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = "Halaman ${index + 1}",
        contentScale = ContentScale.FillWidth,
        modifier = Modifier.fillMaxWidth(),
        loading = { MascotLoader() },
        error = {
            Box(Modifier.fillMaxWidth().padding(32.dp).aspectRatio(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Gagal memuat halaman ${index + 1}", color = Color(0xFF403D36).copy(alpha = 0.6f))
                }
            }
        }
    )
}

@Composable
private fun MascotLoader() {
    Box(Modifier.fillMaxWidth().aspectRatio(0.7f), contentAlignment = Alignment.Center) {
        Image(painter = painterResource(id = R.drawable.mascot_loading), contentDescription = "Memuat...", modifier = Modifier.size(120.dp), alpha = 0.7f)
    }
}

@Composable
private fun LoadingReader(mangaTitle: String, chapterName: String) {
    Box(Modifier.fillMaxSize().background(Color(0xFFF4F0E6)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(id = R.drawable.mascot_loading), contentDescription = null, modifier = Modifier.size(140.dp), alpha = 0.85f)
            Spacer(Modifier.height(16.dp))
            BouncingText("Mengambil halaman", color = Color(0xFF403D36).copy(alpha = 0.7f))
            if (mangaTitle.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("$mangaTitle — $chapterName", color = Color(0xFF403D36).copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ReaderError(msg: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFFF4F0E6)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Gagal memuat", color = Color(0xFF403D36), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(msg, color = Color(0xFF403D36).copy(alpha = 0.6f))
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Coba lagi") }
        }
    }
}

@Composable
private fun EndOfChapter(
    nextChapterUrl: String?,
    nextChapterName: String?,
    onNext: (String?) -> Unit,
    onBackToDetail: () -> Unit,
) {
    val ink = Color(0xFF403D36)
    Column(
        Modifier.fillMaxWidth().background(Color(0xFFF4F0E6)).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalDivider(color = Color(0xFFE2DDD0), thickness = 1.dp, modifier = Modifier.padding(bottom = 24.dp))
        Text("Akhir chapter", color = ink.copy(alpha = 0.5f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(24.dp))
        if (nextChapterUrl != null) {
            Button(onClick = { onNext(nextChapterUrl) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(10.dp)) {
                Text("Lanjut: ${nextChapterName ?: "Chapter berikutnya"}", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(10.dp))
        } else {
            Text("🎉 Ini chapter terbaru", color = ink, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(10.dp))
        }
        OutlinedButton(
            onClick = onBackToDetail,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        ) {
            Text("Kembali ke detail")
        }
    }
}