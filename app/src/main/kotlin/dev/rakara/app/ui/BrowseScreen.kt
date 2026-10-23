package dev.rakara.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import dev.rakara.app.vm.BrowseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(nav: NavController, sourceId: String) {
    val app = RakaraApp.from(LocalContext.current)
    val vm: BrowseViewModel = viewModel(
        key = "browse-$sourceId",
        factory = viewModelFactory {
            initializer { BrowseViewModel(app.engineRepo, sourceId) }
        }
    )
    val s by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(s.engineName.ifBlank { sourceId }) },
                    navigationIcon = {
                        TextButton(onClick = { nav.popBackStack() }) {
                            Text("‹", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                )
                OutlinedTextField(
                    value = s.query,
                    onValueChange = vm::setQuery,
                    placeholder = { Text("Cari…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    TextButton(onClick = { vm.search() }) { Text("Cari") }
                    TextButton(onClick = { vm.loadPopular() }) { Text("Populer") }
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            when {
                s.loading -> LoadingBox()
                s.error != null -> ErrorBox(s.error) { vm.loadPopular() }
                s.items.isEmpty() -> EmptyBox("Tidak ada hasil.")
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(s.items, key = { it.url }) { m ->
                        Column(Modifier.clickable {
                            nav.navigate("manga/$sourceId/${encode(m.url)}/${encode(m.title)}")
                        }) {
                            SubcomposeAsyncImage(
                                model = m.thumbnailUrl,
                                contentDescription = m.title,
                                modifier = Modifier.fillMaxWidth().aspectRatio(0.7f),
                                loading = { ThumbnailPlaceholder(Modifier.fillMaxSize()) },
                                error = { ThumbnailPlaceholder(Modifier.fillMaxSize()) }
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                m.title,
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
}