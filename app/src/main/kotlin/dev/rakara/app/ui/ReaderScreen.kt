package dev.rakara.app.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
            initializer { ReaderViewModel(app.engineRepo, app.readMarks, sourceId, mangaUrl, chapterUrl) }
        }
    )
    val s by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var chrome by remember { mutableStateOf(true) }
    var isAutoScrolling by remember { mutableStateOf(false) }
    var showChapterSheet by remember { mutableStateOf(false) }
    var showSpeedPanel by remember { mutableStateOf(false) }
    var scrollSpeed by remember { mutableFloatStateOf(1.5f) }
    
    
    val readerBg = Color(0xFF151C2B)
    val capsuleBg = Color.Black.copy(alpha = 0.75f)

    val topBarOffset by animateDpAsState(if (chrome) 0.dp else (-150).dp, label = "top")
    val bottomBarOffset by animateDpAsState(if (chrome) 0.dp else 150.dp, label = "bottom")

    
    val view = LocalView.current
    DisposableEffect(chrome) {
        val window = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        if (!chrome) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { insetsController.show(WindowInsetsCompat.Type.systemBars()) }
    }

    
    LaunchedEffect(isAutoScrolling, scrollSpeed) {
        if (isAutoScrolling) {
            while (isAutoScrolling) {
                val delayMs = (100 - (scrollSpeed * 15f)).toLong().coerceAtLeast(10L)
                listState.scrollBy(scrollSpeed)
                delay(delayMs)
                if (!listState.canScrollForward) isAutoScrolling = false
            }
        }
    }

    
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            if (it is androidx.compose.foundation.interaction.PressInteraction) isAutoScrolling = false
        }
    }

    Box(Modifier.fillMaxSize().background(readerBg)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures(onTap = { chrome = !chrome })
            }
        ) {
            itemsIndexed(s.urls) { idx, url ->
                SubcomposeAsyncImage(
                    model = url, contentDescription = "Halaman ${idx+1}", contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
                    loading = { Box(Modifier.fillMaxWidth().aspectRatio(0.7f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) } }
                )
            }
        }

        
        AnimatedVisibility(visible = chrome, modifier = Modifier.align(Alignment.TopCenter).offset(y = topBarOffset)) {
            Surface(color = capsuleBg, shape = RoundedCornerShape(50), modifier = Modifier.padding(top = 24.dp)) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.mangaTitle.ifBlank { "..." }, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(8.dp))
                    Text("•", color = Color.White.copy(alpha = 0.5f))
                    Spacer(Modifier.width(8.dp))
                    Text(s.chapterName.ifBlank { "..." }, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    Spacer(Modifier.width(16.dp))
                    IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                }
            }
        }

        
        AnimatedVisibility(visible = chrome, modifier = Modifier.align(Alignment.BottomCenter).offset(y = bottomBarOffset)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 24.dp)) {
                
                
                if (showSpeedPanel) {
                    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha=0.95f), shape = RoundedCornerShape(16.dp), shadowElevation = 8.dp, modifier = Modifier.padding(bottom = 12.dp).width(280.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Kecepatan Autoscroll", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Slider(value = scrollSpeed, onValueChange = { scrollSpeed = it }, valueRange = 0.5f..3.0f)
                            Button(onClick = { showSpeedPanel = false; isAutoScrolling = true }, modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(8.dp)) { Text("Mulai") }
                        }
                    }
                }

                
                LinearProgressIndicator(progress = { 0.4f }, modifier = Modifier.width(260.dp).height(2.dp), color = MaterialTheme.colorScheme.secondary)

                Surface(color = capsuleBg, shape = RoundedCornerShape(50)) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        
                        IconButton(onClick = {  }, enabled = s.prevChapterUrl != null) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev", tint = Color.White)
                        }
                        
                        IconButton(onClick = { showChapterSheet = true }) { Icon(Icons.Default.List, "List", tint = Color.White) }
                        
                        IconButton(onClick = { nav.navigate("home") { popUpTo("home") { inclusive = true } } }) { Icon(Icons.Default.Home, "Home", tint = Color.White) }
                        
                        IconButton(onClick = { if (isAutoScrolling) isAutoScrolling = false else showSpeedPanel = true }) {
                            Icon(if (isAutoScrolling) Icons.Default.Pause else Icons.Default.PlayArrow, "Auto", tint = if (isAutoScrolling) MaterialTheme.colorScheme.secondary else Color.White)
                        }
                        
                        IconButton(onClick = {  }, enabled = s.nextChapterUrl != null) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next", tint = Color.White)
                        }
                    }
                }
            }
        }

        
        if (showChapterSheet) {
            ModalBottomSheet(onDismissRequest = { showChapterSheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(16.dp)) {
                    Text("Daftar Chapter", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        itemsIndexed(s.chapters) { _, c ->
                            val isCurrent = c.url == chapterUrl
                            Row(
                                Modifier.fillMaxWidth().clickable { 
                                    showChapterSheet = false
                                    if (!isCurrent) nav.navigate("reader/$sourceId/${encode(mangaUrl)}/${encode(c.url)}") { popUpTo("reader/$sourceId/${encode(mangaUrl)}/${encode(chapterUrl)}") { inclusive = true } }
                                }.background(if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha=0.1f) else Color.Transparent).padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(c.chapterNumber.toInt().toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                                Text(c.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                if (isCurrent) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f))
                        }
                    }
                }
            }
        }
    }
}