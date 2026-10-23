package dev.rakara.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pustaka") }) },
        bottomBar = { BottomNav(nav, current = "library") }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (entries.isEmpty()) {
                EmptyBox("Belum ada manga di pustaka.")
                return@Column
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(entries, key = { "${it.sourceId}|${it.mangaUrl}" }) { e ->
                    Column(Modifier.clickable {
                        nav.navigate("manga/${e.sourceId}/${encode(e.mangaUrl)}/${encode(e.title)}")
                    }) {
                        SubcomposeAsyncImage(
                            model = e.thumbnail,
                            contentDescription = e.title,
                            modifier = Modifier.fillMaxWidth().aspectRatio(0.7f),
                            loading = { ThumbnailPlaceholder(Modifier.fillMaxSize()) },
                            error = { ThumbnailPlaceholder(Modifier.fillMaxSize()) }
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            e.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

internal fun encode(s: String): String =
    java.net.URLEncoder.encode(s, "UTF-8")