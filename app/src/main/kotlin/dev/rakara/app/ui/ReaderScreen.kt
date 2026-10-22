package dev.rakara.app.ui

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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import dev.rakara.app.vm.ReaderViewModel
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
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = s.initialPage)
    val scope = rememberCoroutineScope()

    var chrome by remember { mutableStateOf(true) }

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

    val reachedEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            info.totalItemsCount > 0 && last.index >= info.totalItemsCount - 1
        }
    }

    
    val paper = Color(0xFFF4F0E6) 
    val ink = Color(0xFF403D36)

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
                                    nav.navigate("reader/$sourceId/${encode(mangaUrl)}/${encode(nextUrl)}") {
                                        popUpTo("reader/$sourceId/${encode(mangaUrl)}/${encode(chapterUrl)}") {
                                            inclusive = true
                                        }
                                    }
                                }
                            },
                            onBackToDetail = {
                                nav.navigate("manga/$sourceId/${encode(mangaUrl)}/${encode(s.mangaTitle)}") {
                                    popUpTo("manga/$sourceId/${encode(mangaUrl)}/${encode(s.mangaTitle)}") {
                                        inclusive = true
                                    }
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
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = ink
                            )
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
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    )
                }

                AnimatedVisibility(
                    visible = reachedEnd && chrome,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch { listState.scrollToItem(0) }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Ke Atas")
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
            Box(
                Modifier.fillMaxWidth().padding(32.dp).aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "⚠",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.displayLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Gagal memuat halaman ${index + 1}",
                        color = Color(0xFF403D36).copy(alpha = 0.6f),
                    )
                }
            }
        }
    )
}

@Composable
private fun MascotLoader() {
    Box(
        Modifier.fillMaxWidth().aspectRatio(0.7f),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.mascot_loading),
            contentDescription = "Memuat...",
            modifier = Modifier.size(120.dp),
            alpha = 0.7f,
        )
    }
}

@Composable
private fun LoadingReader(mangaTitle: String, chapterName: String) {
    Box(
        Modifier.fillMaxSize().background(Color(0xFFF4F0E6)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.mascot_loading),
                contentDescription = null,
                modifier = Modifier.size(140.dp),
                alpha = 0.85f,
            )
            Spacer(Modifier.height(16.dp))
            BouncingText(
                "Mengambil halaman",
                color = Color(0xFF403D36).copy(alpha = 0.7f),
            )
            if (mangaTitle.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "$mangaTitle — $chapterName",
                    color = Color(0xFF403D36).copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ReaderError(msg: String, onRetry: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xFFF4F0E6)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Gagal memuat",
                color = Color(0xFF403D36),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                msg,
                color = Color(0xFF403D36).copy(alpha = 0.6f),
            )
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
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFF4F0E6))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalDivider(
            color = Color(0xFFE2DDD0),
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        Text(
            "Akhir chapter",
            color = ink.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(24.dp))

        if (nextChapterUrl != null) {
            Button(
                onClick = { onNext(nextChapterUrl) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    "Lanjut: ${nextChapterName ?: "Chapter berikutnya"}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(Modifier.height(10.dp))
        } else {
            Text(
                "🎉 Ini chapter terbaru",
                color = ink,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(10.dp))
        }

        OutlinedButton(
            onClick = onBackToDetail,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text("Kembali ke detail")
        }
    }
}