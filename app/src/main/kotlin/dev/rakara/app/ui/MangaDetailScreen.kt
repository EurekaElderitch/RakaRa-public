package dev.rakara.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(s.manga?.title ?: "...", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (s.manga != null) {
                        TextButton(onClick = vm::toggleLibrary) {
                            Icon(
                                if (s.inLibrary) Icons.Default.Remove else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (s.inLibrary) "Hapus" else "Pustaka")
                        }
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            when {
                s.loading -> LoadingBox()
                s.error != null -> ErrorBox(s.error) { vm.load() }
                s.manga == null -> EmptyBox("Data tidak ditemukan.")
                else -> {
                    val m = s.manga!!
                    Row(Modifier.fillMaxWidth().padding(16.dp)) {
                        SubcomposeAsyncImage(
                            model = m.thumbnailUrl,
                            contentDescription = m.title,
                            modifier = Modifier.width(110.dp).aspectRatio(0.7f),
                            loading = { ThumbnailPlaceholder(Modifier.fillMaxSize()) },
                            error = { ThumbnailPlaceholder(Modifier.fillMaxSize()) }
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(m.title, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(6.dp))
                            m.author?.let {
                                Text("oleh $it", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (m.status != SManga.Status.UNKNOWN) {
                                
                                val statusStr = when (m.status) {
                                    SManga.Status.ONGOING -> "Berjalan"
                                    SManga.Status.COMPLETED -> "Tamat"
                                    SManga.Status.ON_HIATUS -> "Hiatus"
                                    else -> m.status.name
                                }
                                Text("Status: $statusStr",
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                            
                            
                            m.description?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    Text(
                        "CHAPTER (${s.chapters.size})",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn {
                        items(s.chapters, key = { it.url }) { c ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable {
                                        nav.navigate(
                                            "reader/$sourceId/${encode(m.url)}/${encode(c.url)}"
                                        )
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(c.name, style = MaterialTheme.typography.bodyLarge)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }
}