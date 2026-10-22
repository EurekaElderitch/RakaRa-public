package dev.rakara.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import dev.rakara.app.RakaraApp
import dev.rakara.app.vm.EnginesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnginesScreen(nav: NavController) {
    val app = RakaraApp.from(LocalContext.current)
    val vm: EnginesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { EnginesViewModel(app.engineRepo, app) }
        }
    )
    val s by vm.state.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) vm.installFromUri(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Engine") },
                navigationIcon = {
                    TextButton(onClick = { nav.popBackStack() }) {
                        Text("‹", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { picker.launch(arrayOf("*/*")) },
                        enabled = !s.busy
                    ) { Text("+ Install .rkx") }
                }
            )
        },
        bottomBar = { BottomNav(nav, current = "engines") }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            s.status?.let {
                Text(it, modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider()
            }
            when {
                s.loading -> LoadingBox()
                s.engines.isEmpty() -> EmptyBox("Belum ada engine. Install .rkx dulu.")
                else -> LazyColumn {
                    items(s.engines, key = { it.manifest.id }) { e ->
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(e.manifest.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "v${e.manifest.version} · ${e.manifest.lang} · " +
                                        if (e.enabled) "aktif" else "nonaktif",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { vm.toggle(e) }) {
                                Text(if (e.enabled) "Matikan" else "Aktifkan")
                            }
                            TextButton(onClick = { vm.uninstall(e) }) { Text("Hapus") }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}