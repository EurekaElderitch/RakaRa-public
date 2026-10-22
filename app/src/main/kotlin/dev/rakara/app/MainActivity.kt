package dev.rakara.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.rakara.app.ui.BrowseScreen
import dev.rakara.app.ui.EnginesScreen
import dev.rakara.app.ui.HomeScreen
import dev.rakara.app.ui.LibraryScreen
import dev.rakara.app.ui.MangaDetailScreen
import dev.rakara.app.ui.ReaderScreen
import dev.rakara.app.ui.RakaraTheme
import dev.rakara.app.ui.SettingsScreen
import dev.rakara.app.ui.SplashScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RakaraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        SplashScreen()
                        LaunchedEffect(Unit) {
                            val start = System.currentTimeMillis()
                            val app = RakaraApp.from(this@MainActivity)

                            
                            runCatching {
                                withTimeoutOrNull(15_000) {
                                    app.engineUpdater.checkAndUpdate()
                                }
                            }

                            
                            val elapsed = System.currentTimeMillis() - start
                            if (elapsed < 1400) delay(1400 - elapsed)

                            showSplash = false
                        }
                    } else {
                        App()
                    }
                }
            }
        }
    }
}

@Composable
private fun App() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "home") {
        composable("home") { HomeScreen(nav) }
        composable("library") { LibraryScreen(nav) }
        composable("settings") { SettingsScreen(nav) }
        composable("engines") { EnginesScreen(nav) }

        composable(
            route = "source/{sourceId}",
            arguments = listOf(navArgument("sourceId") { type = NavType.StringType })
        ) { e ->
            BrowseScreen(nav, e.arguments?.getString("sourceId").orEmpty())
        }

        composable(
            route = "manga/{sourceId}/{url}/{title}",
            arguments = listOf(
                navArgument("sourceId") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType; defaultValue = "" }
            )
        ) { e ->
            val a = e.arguments
            MangaDetailScreen(
                nav,
                a?.getString("sourceId").orEmpty(),
                a?.getString("url").orEmpty(),
            )
        }

        composable(
            route = "reader/{sourceId}/{manga}/{chapter}",
            arguments = listOf(
                navArgument("sourceId") { type = NavType.StringType },
                navArgument("manga") { type = NavType.StringType },
                navArgument("chapter") { type = NavType.StringType }
            )
        ) { e ->
            val a = e.arguments
            ReaderScreen(
                nav,
                a?.getString("sourceId").orEmpty(),
                a?.getString("manga").orEmpty(),
                a?.getString("chapter").orEmpty(),
            )
        }
    }
}