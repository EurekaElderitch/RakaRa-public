package dev.rakara.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun LoadingBox(text: String = "Memuat") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ErrorBox(message: String?, onRetry: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("Terjadi kesalahan", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(6.dp))
            Text(message ?: "-", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            if (onRetry != null) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry) { Text("Coba lagi") }
            }
        }
    }
}

@Composable
fun EmptyBox(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun ThumbnailPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Text("◌", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
    }
}

@Composable
fun FilterChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(text = text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
    }
}




@Composable
fun BottomNav(nav: NavController, current: String) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.height(72.dp)
    ) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        NavigationBarItem(
            selected = current == "home",
            onClick = { 
                if (current != "home") nav.navigate("home") {
                    popUpTo("home") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Filled.Explore, contentDescription = "Discover") },
            label = { Text("Discover", style = MaterialTheme.typography.labelSmall) },
            colors = colors,
        )
        NavigationBarItem(
            selected = current == "explore",
            onClick = { 
                if (current != "explore") nav.navigate("explore") {
                    popUpTo("home") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Filled.Search, contentDescription = "Browse") },
            label = { Text("Browse", style = MaterialTheme.typography.labelSmall) },
            colors = colors,
        )
        NavigationBarItem(
            selected = current == "library",
            onClick = { 
                if (current != "library") nav.navigate("library") {
                    popUpTo("home") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Filled.Bookmark, contentDescription = "Library") },
            label = { Text("Library", style = MaterialTheme.typography.labelSmall) },
            colors = colors,
        )
        NavigationBarItem(
            selected = current == "profile",
            onClick = { 
                if (current != "profile") nav.navigate("profile") {
                    popUpTo("home") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile", style = MaterialTheme.typography.labelSmall) },
            colors = colors,
        )
    }
}