package dev.rakara.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.rakara.app.RakaraApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController) {
    val app = RakaraApp.from(LocalContext.current)
    var engines by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        engines = withContext(Dispatchers.IO) {
            app.engineRepo.installed()
                .filter { it.enabled }
                .map { it.manifest.id to it.manifest.name }
        }
        loading = false
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("RAKARA") }) },
        bottomBar = { BottomNav(nav, current = "home") }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            when {
                loading -> LoadingBox()
                engines.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Belum ada sumber terpasang.")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { nav.navigate("settings") }) {
                            Text("Kelola Sumber")
                        }
                    }
                }
                else -> {
                    Text(
                        "SUMBER",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyColumn {
                        items(engines) { (id, name) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { nav.navigate("source/$id") }
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.weight(1f))
                                Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNav(nav: NavController, current: String) {
    NavigationBar {
        NavigationBarItem(
            selected = current == "home",
            onClick = {
                if (current != "home") nav.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            },
            icon = { Text("◉", style = MaterialTheme.typography.titleLarge) },
            label = { Text("Sumber") },
        )
        NavigationBarItem(
            selected = current == "library",
            onClick = {
                if (current != "library") nav.navigate("library") {
                    popUpTo("home")
                }
            },
            icon = { Text("▤", style = MaterialTheme.typography.titleLarge) },
            label = { Text("Pustaka") },
        )
        NavigationBarItem(
            selected = current == "settings",
            onClick = {
                if (current != "settings") nav.navigate("settings") {
                    popUpTo("home")
                }
            },
            icon = { Text("⚙", style = MaterialTheme.typography.titleLarge) },
            label = { Text("Setelan") },
        )
    }
}