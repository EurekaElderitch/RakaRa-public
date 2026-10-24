package dev.rakara.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.rakara.app.data.UserPreferencesRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(nav: androidx.navigation.NavController, prefsRepo: UserPreferencesRepository) {
    val scope = rememberCoroutineScope()
    val name by prefsRepo.profileName.collectAsState(initial = "User Raka")
    val photoUri by prefsRepo.profilePhotoUri.collectAsState(initial = null)
    val isDarkMode by prefsRepo.isDarkMode.collectAsState(initial = false)

    var showEditModal by remember { mutableStateOf(false) }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("Profile", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black) }) },
        bottomBar = { BottomNav(nav, current = "profile") }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().clickable { showEditModal = true }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (photoUri != null) {
                        AsyncImage(model = photoUri, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(CircleShape))
                    } else {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                            Text(name.take(1).uppercase(), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Ketuk untuk edit profil", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(20.dp))

            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Chapter", "124", Modifier.weight(1f))
                StatCard("Waktu", "12h", Modifier.weight(1f))
                StatCard("Koleksi", "14", Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            
            Text("Pengaturan Aplikasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                Column {
                    
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text("Mode Gelap", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(checked = isDarkMode, onCheckedChange = { scope.launch { prefsRepo.setDarkMode(it) } })
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f))
                    
                    
                    Row(Modifier.fillMaxWidth().clickable { nav.navigate("downloads") }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text("Penyimpanan & Unduhan", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f))
                    
                    
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text("Tentang RakaRa", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text("v0.4.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    
    if (showEditModal) {
        var editName by remember { mutableStateOf(name) }
        var editPhoto by remember { mutableStateOf<Uri?>(photoUri?.let { Uri.parse(it) }) }
        
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) editPhoto = uri
        }

        ModalBottomSheet(onDismissRequest = { showEditModal = false }) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Edit Profil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                if (editPhoto != null) {
                    AsyncImage(model = editPhoto, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(80.dp).clip(CircleShape))
                } else {
                    Box(Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Text(editName.take(1).uppercase(), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.headlineLarge)
                    }
                }
                
                TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text("Pilih Foto Baru")
                }
                
                OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Nama Tampilan") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            prefsRepo.saveProfile(editName, editPhoto?.toString())
                            showEditModal = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)
                ) { Text("Simpan Perubahan") }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = modifier) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}